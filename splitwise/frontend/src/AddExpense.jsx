import { useState } from 'react';
import { api } from './api.js';

const CATEGORIES = ['Food', 'Travel', 'Stay', 'Fun', 'Shopping', 'Other'];

export default function AddExpense({ trip, onSaved }) {
  const members = trip.members;
  const [form, setForm] = useState({
    description: '',
    amount: '',
    category: 'Food',
    paidBy: members[0]?.id ?? '',
    splitType: 'EQUAL',
  });
  const [picked, setPicked] = useState(() => new Set(members.map((m) => m.id)));
  const [values, setValues] = useState({});
  const [err, setErr] = useState('');
  const [scanning, setScanning] = useState(false);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  function toggle(id) {
    setPicked((p) => {
      const next = new Set(p);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  async function scan(e) {
    const file = e.target.files[0];
    if (!file) return;
    setScanning(true);
    setErr('');
    try {
      const r = await api.scan(file);
      setForm((f) => ({ ...f, amount: r.total ?? f.amount, description: f.description || r.merchant || '' }));
      if (r.total == null) setErr("Couldn't find a total on that bill. Enter it by hand.");
    } catch (x) {
      setErr(x.message);
    } finally {
      setScanning(false);
      e.target.value = '';
    }
  }

  async function submit(e) {
    e.preventDefault();
    setErr('');
    const body = {
      description: form.description,
      category: form.category,
      amount: Number(form.amount),
      paidBy: Number(form.paidBy),
      splitType: form.splitType,
    };
    if (form.splitType === 'EQUAL') {
      body.participants = [...picked];
    } else {
      body.values = Object.fromEntries(
        Object.entries(values).filter(([, v]) => v !== '').map(([k, v]) => [k, Number(v)])
      );
    }
    try {
      onSaved(await api.addExpense(trip.id, body));
      setForm((f) => ({ ...f, description: '', amount: '' }));
      setValues({});
    } catch (x) {
      setErr(x.message);
    }
  }

  return (
    <form className="stack" onSubmit={submit}>
      <label className="scan">
        {scanning ? 'Reading the bill…' : 'Scan a bill photo to fill this in'}
        <input type="file" accept="image/*" onChange={scan} disabled={scanning} />
      </label>

      <label>
        What was it for
        <input value={form.description} onChange={(e) => set('description', e.target.value)} placeholder="Dinner at the beach shack" required />
      </label>

      <div className="two">
        <label>
          Amount
          <input type="number" step="0.01" min="0.01" value={form.amount} onChange={(e) => set('amount', e.target.value)} required />
        </label>
        <label>
          Category
          <select value={form.category} onChange={(e) => set('category', e.target.value)}>
            {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
          </select>
        </label>
      </div>

      <div className="two">
        <label>
          Paid by
          <select value={form.paidBy} onChange={(e) => set('paidBy', e.target.value)}>
            {members.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
          </select>
        </label>
        <label>
          Split
          <select value={form.splitType} onChange={(e) => set('splitType', e.target.value)}>
            <option value="EQUAL">Equally</option>
            <option value="PERCENT">By percentage</option>
            <option value="EXACT">By exact amount</option>
          </select>
        </label>
      </div>

      {form.splitType === 'EQUAL' ? (
        <fieldset>
          <legend>Split between</legend>
          {members.map((m) => (
            <label key={m.id} className="check">
              <input type="checkbox" checked={picked.has(m.id)} onChange={() => toggle(m.id)} /> {m.name}
            </label>
          ))}
        </fieldset>
      ) : (
        <fieldset>
          <legend>{form.splitType === 'PERCENT' ? 'Percent each person pays (total 100)' : 'Amount each person pays'}</legend>
          {members.map((m) => (
            <label key={m.id} className="two-col">
              {m.name}
              <input type="number" step="0.01" min="0" value={values[m.id] ?? ''} onChange={(e) => setValues((v) => ({ ...v, [m.id]: e.target.value }))} />
            </label>
          ))}
        </fieldset>
      )}

      {err && <p className="error">{err}</p>}
      <button className="btn">Save expense</button>
    </form>
  );
}
