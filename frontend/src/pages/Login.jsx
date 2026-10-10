import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useStore } from '../store';
import { ErrorNote } from '../ui';

export default function Login() {
  const { login, register } = useStore();
  const nav = useNavigate();
  const loc = useLocation();
  const [mode, setMode] = useState('login');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setErr('');
    try {
      if (mode === 'login') await login(f.get('email'), f.get('password'));
      else await register(f.get('name'), f.get('email'), f.get('password'));
      nav(loc.state?.from || '/', { replace: true });
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  return (
    <section className="narrow">
      <h1>{mode === 'login' ? 'Log in' : 'Create your account'}</h1>
      <form className="card form" onSubmit={submit}>
        {mode === 'register' && <label>Name<input name="name" required maxLength={100} autoComplete="name" /></label>}
        <label>Email<input name="email" type="email" required autoComplete="email" /></label>
        <label>Password<input name="password" type="password" required minLength={mode === 'register' ? 8 : 1} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
          {mode === 'register' && <span className="muted small">At least 8 characters</span>}
        </label>
        <ErrorNote msg={err} />
        <button className="btn" disabled={busy}>{busy ? 'Please wait...' : mode === 'login' ? 'Log in' : 'Create account'}</button>
      </form>
      <p className="muted">
        {mode === 'login' ? 'New to ShopSmart?' : 'Already have an account?'}{' '}
        <button className="link" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErr(''); }}>
          {mode === 'login' ? 'Create an account' : 'Log in'}
        </button>
      </p>
    </section>
  );
}
