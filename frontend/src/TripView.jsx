import { useCallback, useEffect, useState } from 'react';
import { api, money } from './api.js';
import AddExpense from './AddExpense.jsx';

export default function TripView({ id, onBack }) {
  const [t, setT] = useState(null);
  const [err, setErr] = useState('');
  const [newMember, setNewMember] = useState('');

  const load = useCallback(
    () => api.trip(id).then(setT).catch((e) => setErr(e.message)),
    [id]
  );
  useEffect(() => {
    load();
  }, [load]);

  if (!t) return err ? <p className="error">{err}</p> : <p className="muted">Loading…</p>;

  const run = (promise) =>
    promise.then((fresh) => {
      setT(fresh);
      setErr('');
    }).catch((e) => setErr(e.message));

  const biggest = Math.max(1, ...Object.values(t.byCategory));

  return (
    <>
      <button className="link" onClick={onBack}>All trips</button>
      <h1>{t.name}</h1>
      <p className="total">{money(t.total)} <span className="muted">spent by {t.members.length} people</span></p>
      {err && <p className="error">{err}</p>}

      <div className="grid">
        <section>
          <h2>Settle up</h2>
          {t.settlements.length === 0 ? (
            <p className="muted">{t.expenses.length === 0 ? 'Add an expense to see who owes whom.' : 'Everyone is settled.'}</p>
          ) : (
            <ul className="rows">
              {t.settlements.map((s, i) => (
                <li key={i}>
                  <span><strong>{s.fromName}</strong> pays <strong>{s.toName}</strong></span>
                  <span>
                    {money(s.amount)}{' '}
                    <button className="btn small" onClick={() => run(api.settle(id, { from: s.from, to: s.to, amount: s.amount }))}>
                      Mark paid
                    </button>
                  </span>
                </li>
              ))}
            </ul>
          )}

          <h2>Balances</h2>
          <ul className="rows">
            {t.balances.map((b) => (
              <li key={b.memberId}>
                <span>{b.name}</span>
                <span className={b.net > 0 ? 'pos' : b.net < 0 ? 'neg' : 'muted'}>
                  {b.net > 0 ? `gets back ${money(b.net)}` : b.net < 0 ? `owes ${money(-b.net)}` : 'settled'}
                </span>
              </li>
            ))}
          </ul>
          <form
            className="inline"
            onSubmit={(e) => {
              e.preventDefault();
              if (!newMember.trim()) return;
              run(api.addMember(id, newMember.trim()));
              setNewMember('');
            }}
          >
            <input value={newMember} onChange={(e) => setNewMember(e.target.value)} placeholder="Add a person" aria-label="Add a person" />
            <button className="btn small">Add</button>
          </form>

          <h2>Where the money went</h2>
          {Object.keys(t.byCategory).length === 0 && <p className="muted">Categories show up once you add expenses.</p>}
          {Object.entries(t.byCategory).map(([cat, amt]) => (
            <div key={cat} className="bar">
              <span>{cat}</span>
              <div className="track"><div className="fill" style={{ width: `${(amt / biggest) * 100}%` }} /></div>
              <span>{money(amt)}</span>
            </div>
          ))}
        </section>

        <section>
          <h2>Add expense</h2>
          <AddExpense trip={t} onSaved={setT} />

          <h2>Expenses</h2>
          {t.expenses.length === 0 && <p className="muted">Nothing yet. Add the first bill above.</p>}
          <ul className="rows">
            {t.expenses.map((e) => (
              <li key={e.id}>
                <div>
                  <div>{e.description} <span className="tag">{e.category}</span></div>
                  <small className="muted">{e.paidByName} paid · split {Object.keys(e.shares).length} ways</small>
                </div>
                <span>
                  {money(e.amount)}{' '}
                  <button className="link danger" onClick={() => run(api.deleteExpense(id, e.id))}>Delete</button>
                </span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}
