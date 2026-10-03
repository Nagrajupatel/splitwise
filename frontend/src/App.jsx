import { useEffect, useState } from 'react';
import { api } from './api.js';
import TripView from './TripView.jsx';

export default function App() {
  const [trips, setTrips] = useState([]);
  const [openId, setOpenId] = useState(null);
  const [name, setName] = useState('');
  const [people, setPeople] = useState('');
  const [err, setErr] = useState('');

  const load = () => api.trips().then(setTrips).catch((e) => setErr(e.message));
  useEffect(() => {
    load();
  }, []);

  async function create(e) {
    e.preventDefault();
    setErr('');
    try {
      const t = await api.createTrip({
        name,
        members: people.split(',').map((s) => s.trim()).filter(Boolean),
      });
      setName('');
      setPeople('');
      load();
      setOpenId(t.id);
    } catch (x) {
      setErr(x.message);
    }
  }

  if (openId) {
    return (
      <main>
        <TripView
          id={openId}
          onBack={() => {
            setOpenId(null);
            load();
          }}
        />
      </main>
    );
  }

  return (
    <main>
      <h1>Splitwise</h1>
      <p className="muted">Track what everyone spent on a trip and who owes whom.</p>

      <h2>Your trips</h2>
      {trips.length === 0 && <p className="muted">No trips yet. Create your first one below.</p>}
      <ul className="rows">
        {trips.map((t) => (
          <li key={t.id}>
            <button className="link" onClick={() => setOpenId(t.id)}>{t.name}</button>
            <span className="muted">{t.memberCount} people</span>
          </li>
        ))}
      </ul>

      <h2>New trip</h2>
      <form className="stack" onSubmit={create}>
        <label>
          Trip name
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Goa weekend" required />
        </label>
        <label>
          Who's coming (separate names with commas)
          <input value={people} onChange={(e) => setPeople(e.target.value)} placeholder="Ravi, Priya, Arjun" />
        </label>
        <button className="btn">Create trip</button>
      </form>
      {err && <p className="error">{err}</p>}
    </main>
  );
}
