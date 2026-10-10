import { useState } from 'react';
import { api } from '../api';
import { useStore } from '../store';
import { ErrorNote, Loading, fmtDate, useLoad } from '../ui';

const csv = (s) => s.split(',').map((x) => x.trim()).filter(Boolean);

function Membership() {
  const { user, notify } = useStore();
  const cur = useLoad(() => api.get('/memberships/me'), []);
  const hist = useLoad(() => api.get('/memberships/me/history'), []);
  const [days, setDays] = useState(30);
  const reloadAll = async () => {
    cur.reload(); hist.reload();
    try { const me = await api.get('/auth/me'); window.dispatchEvent(new CustomEvent('ss:tier', { detail: me })); } catch { /* ignore */ }
  };
  const run = async (fn, msg) => {
    try { await fn(); notify(msg); await reloadAll(); } catch (e) { notify(e.message, 'err'); }
  };
  if (cur.loading) return <Loading />;
  const m = cur.data;
  return (
    <section className="card form">
      <h2>Membership</h2>
      <ErrorNote msg={cur.error} />
      {m && (
        <p>You are on <strong>{m.tier}</strong> ({m.status.toLowerCase()}).{' '}
          {m.endDate ? <>Valid until {fmtDate(m.endDate)}.</> : <>This plan does not expire.</>}</p>
      )}
      <p className="muted small">Prime and Premium last 30 days. No real payment is taken in this demo.</p>
      <div className="row gap wrapflex">
        {['PRIME', 'PREMIUM'].filter((t) => t !== m?.tier).map((t) => (
          <button key={t} className="btn small" onClick={() => run(() => api.post('/memberships', { tier: t }), `Switched to ${t}`)}>Switch to {t}</button>
        ))}
        {m && m.tier !== 'BASIC' && (
          <>
            <label className="inline">Renew for
              <select value={days} onChange={(e) => setDays(Number(e.target.value))}>
                <option value="30">30 days</option><option value="90">90 days</option><option value="365">365 days</option>
              </select>
            </label>
            <button className="btn small ghost" onClick={() => run(() => api.post('/memberships/me/renew', { tier: m.tier, durationDays: days }), 'Membership renewed')}>Renew {m.tier}</button>
          </>
        )}
      </div>
      {hist.data?.length > 0 && (
        <details>
          <summary>Membership history</summary>
          <ul className="plainlist small">
            {hist.data.map((h) => <li key={h.id}>{h.tier} - {h.status.toLowerCase()} - from {fmtDate(h.startDate)}{h.endDate ? ` to ${fmtDate(h.endDate)}` : ''}</li>)}
          </ul>
        </details>
      )}
    </section>
  );
}

function Preferences() {
  const { notify } = useStore();
  const { data, loading, error } = useLoad(() => api.get('/users/me/preferences'), []);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  if (loading) return <Loading />;
  const p = data || {};
  async function save(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    const num = (k) => (f.get(k) === '' ? null : Number(f.get(k)));
    setBusy(true);
    setErr('');
    try {
      await api.put('/users/me/preferences', {
        preferredCategories: csv(f.get('cats')), preferredBrands: csv(f.get('brands')),
        budgetMin: num('min'), budgetMax: num('max'), useCases: csv(f.get('uses')),
      });
      notify('Preferences saved');
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }
  return (
    <form className="card form" onSubmit={save}>
      <h2>Shopping preferences</h2>
      <p className="muted small">The assistant uses these to pick better recommendations. Separate items with commas.</p>
      <ErrorNote msg={error} />
      <label>Preferred categories<input name="cats" defaultValue={(p.preferredCategories || []).join(', ')} placeholder="Electronics" /></label>
      <label>Preferred brands<input name="brands" defaultValue={(p.preferredBrands || []).join(', ')} placeholder="Dell, Lenovo" /></label>
      <div className="row gap">
        <label>Budget from<input name="min" type="number" min="0" defaultValue={p.budgetMin ?? ''} /></label>
        <label>Budget up to<input name="max" type="number" min="0" defaultValue={p.budgetMax ?? ''} /></label>
      </div>
      <label>What you use products for<input name="uses" defaultValue={(p.useCases || []).join(', ')} placeholder="Coding, AI learning" /></label>
      <ErrorNote msg={err} />
      <button className="btn" disabled={busy}>Save preferences</button>
    </form>
  );
}

export default function Account() {
  const { user } = useStore();
  return (
    <section className="narrow2">
      <h1>Your account</h1>
      <p className="muted">{user.name} - {user.email}</p>
      <Membership />
      <Preferences />
    </section>
  );
}
