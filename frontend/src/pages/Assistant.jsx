import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useStore } from '../store';
import { Money } from '../ui';

const EXAMPLES = {
  shop: ['Suggest a laptop under 70000 for Java development and AI projects', 'A phone with a good camera under 30000', 'Noise cancelling headphones for travel'],
  policy: ['Does Prime membership include free delivery?', 'How long do I have to return an item?', 'Can I cancel an order after it has shipped?'],
};
const CONF = {
  HIGH: 'The best matching policy passage is a strong match for your question.',
  MEDIUM: 'The best matching policy passage is a fair match. Check the sources.',
  LOW: 'Weak evidence in the policy documents. Please confirm with support.',
  NONE: 'No relevant policy text was found.',
};

function ShopAnswer({ r }) {
  const { addToCart } = useStore();
  return (
    <div>
      <p><strong>{r.interpretedRequirement}</strong>{r.budget != null && <span className="muted"> - budget <Money v={r.budget} /></span>}</p>
      {r.clarifyingQuestion && <p className="note">{r.clarifyingQuestion}</p>}
      <ul className="lines">
        {r.recommendations.map((x) => (
          <li key={x.productId} className="card line">
            <div className="grow">
              <Link to={`/product/${x.productId}`}>{x.productName}</Link>
              <p className="small">{x.reason}</p>
            </div>
            <div className="right">
              <Money v={x.price} /><br />
              <span className={`badge ${x.availability === 'IN_STOCK' ? 'in' : 'out'}`}>{x.availability === 'IN_STOCK' ? 'In stock' : 'Out of stock'}</span>
            </div>
            <button className="btn small" disabled={x.availability !== 'IN_STOCK'} onClick={() => addToCart(x.productId)}>Add to cart</button>
          </li>
        ))}
      </ul>
      <p className="muted small">Names, prices and stock come from the live catalogue, not from the AI.</p>
    </div>
  );
}

function PolicyAnswer({ r }) {
  return (
    <div>
      <p className="prose">{r.answer}</p>
      <p className="small">
        <span className={`badge conf-${r.confidence.toLowerCase()}`} title={CONF[r.confidence]}>Evidence: {r.confidence.toLowerCase()}</span>{' '}
        <span className="muted">{CONF[r.confidence]}</span>
      </p>
      {r.sources.length > 0 && (
        <ul className="plainlist small">
          {r.sources.map((s, i) => <li key={i}>Source: {s.document}, {s.section}, version {s.version}</li>)}
        </ul>
      )}
      {r.followUpRequired && <p className="note">Not enough evidence in the approved policy documents. Contact ShopSmart support to confirm.</p>}
    </div>
  );
}

export default function Assistant() {
  const [mode, setMode] = useState('shop');
  const [turns, setTurns] = useState([]);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const end = useRef(null);
  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }); }, [turns]);

  async function send(q) {
    q = q.trim();
    if (!q || busy) return;
    const id = Date.now();
    setBusy(true);
    setText('');
    setTurns((t) => [...t, { id, mode, q }]);
    try {
      const r = mode === 'shop' ? await api.post('/ai/recommendations', { query: q }) : await api.post('/ai/knowledge/ask', { question: q });
      setTurns((t) => t.map((x) => (x.id === id ? { ...x, r } : x)));
    } catch (e) {
      setTurns((t) => t.map((x) => (x.id === id ? { ...x, err: e.message } : x)));
    } finally { setBusy(false); }
  }

  return (
    <section className="assistant">
      <h1>Ask ShopSmart</h1>
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={mode === 'shop'} className={mode === 'shop' ? 'on' : ''} onClick={() => setMode('shop')}>Find a product</button>
        <button role="tab" aria-selected={mode === 'policy'} className={mode === 'policy' ? 'on' : ''} onClick={() => setMode('policy')}>Ask about policies</button>
      </div>
      <div className="chat" aria-live="polite">
        {turns.length === 0 && (
          <div className="hint">
            <p>{mode === 'shop' ? 'Describe what you need, with a budget if you have one.' : 'Ask about delivery, returns or membership benefits. Answers come only from approved policy documents.'}</p>
          </div>
        )}
        {turns.map((t) => (
          <div key={t.id} className="turn">
            <p className="ask">{t.q}</p>
            <div className="reply">
              {!t.r && !t.err && <p className="muted">Thinking...</p>}
              {t.err && <p className="note err" role="alert">{t.err}</p>}
              {t.r && (t.mode === 'shop' ? <ShopAnswer r={t.r} /> : <PolicyAnswer r={t.r} />)}
            </div>
          </div>
        ))}
        <div ref={end} />
      </div>
      <div className="chips">
        {EXAMPLES[mode].map((x) => <button key={x} className="chip" disabled={busy} onClick={() => send(x)}>{x}</button>)}
      </div>
      <form className="askbar" onSubmit={(e) => { e.preventDefault(); send(text); }}>
        <input value={text} maxLength={500} onChange={(e) => setText(e.target.value)} aria-label="Your question"
          placeholder={mode === 'shop' ? 'e.g. a laptop under 60000 for video editing' : 'e.g. what is the return window for Prime members?'} />
        <button className="btn" disabled={busy || !text.trim()}>{busy ? 'Working...' : 'Ask'}</button>
      </form>
    </section>
  );
}
