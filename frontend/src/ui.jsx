import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useStore } from './store';

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
export const Money = ({ v }) => <span className="money">{v == null ? '-' : inr.format(v)}</span>;
export const fmtDate = (d) => (d ? new Date(d).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-');

/** Loads data when deps change. Returns {data, error, loading, reload}. */
export function useLoad(fn, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(fn, deps);
  const load = useCallback(() => {
    let live = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    run().then((data) => live && setState({ data, error: null, loading: false }))
      .catch((e) => live && setState({ data: null, error: e.message, loading: false }));
    return () => { live = false; };
  }, [run]);
  useEffect(() => load(), [load]);
  return { ...state, reload: load };
}

export const Loading = () => <p className="muted pad" role="status">Loading...</p>;
export const ErrorNote = ({ msg }) => (msg ? <p className="note err" role="alert">{msg}</p> : null);
export const Empty = ({ title, children }) => (
  <div className="empty"><h3>{title}</h3>{children}</div>
);

export function Stars({ v }) {
  if (v == null) return <span className="muted small">No rating yet</span>;
  return <span className="stars" title={`${v} out of 5`}>{Number(v).toFixed(1)} <span aria-hidden>{'★'.repeat(Math.round(v))}{'☆'.repeat(5 - Math.round(v))}</span></span>;
}

export const StockBadge = ({ p }) =>
  p.stockQuantity === 0 ? <span className="badge out">Out of stock</span>
    : p.stockQuantity <= 5 ? <span className="badge low">Only {p.stockQuantity} left</span>
      : <span className="badge in">In stock</span>;

/** Backend stores image paths that may not be served; fall back to a tile coloured by brand. */
export function ProductImage({ p }) {
  const [bad, setBad] = useState(false);
  const real = p.imageUrl && /^https?:/.test(p.imageUrl) && !bad;
  if (real) return <img className="pimg" src={p.imageUrl} alt={p.name} loading="lazy" onError={() => setBad(true)} />;
  const seed = [...(p.brand || p.name)].reduce((n, c) => n + c.charCodeAt(0), 0);
  return (
    <div className="pimg tile" style={{ background: `hsl(${(seed * 37) % 360} 38% 86%)` }} role="img" aria-label={p.name}>
      <span>{(p.brand || p.name).slice(0, 2)}</span>
    </div>
  );
}

export function ProductCard({ p }) {
  const { addToCart, toggleWish, toggleCompare, wishIds, compare } = useStore();
  const wished = wishIds.includes(p.id);
  const comparing = compare.includes(p.id);
  return (
    <article className="card product">
      <Link to={`/product/${p.id}`} className="plain"><ProductImage p={p} /></Link>
      <div className="pbody">
        <p className="muted small">{p.brand}{p.categoryName ? ` in ${p.categoryName}` : ''}</p>
        <h3><Link to={`/product/${p.id}`} className="plain">{p.name}</Link></h3>
        <div className="row between"><Stars v={p.rating} /><StockBadge p={p} /></div>
        <div className="row between pricebar">
          <Money v={p.price} />
          <button className="btn small" disabled={p.stockQuantity === 0} onClick={() => addToCart(p.id)}>Add to cart</button>
        </div>
        <div className="row between small">
          <label className="check"><input type="checkbox" checked={comparing} onChange={() => toggleCompare(p.id)} /> Compare</label>
          <button className="link" aria-pressed={wished} onClick={() => toggleWish(p.id)}>{wished ? 'Saved to wishlist' : 'Save to wishlist'}</button>
        </div>
      </div>
    </article>
  );
}

export function Pager({ page, totalPages, onPage }) {
  if (totalPages <= 1) return null;
  return (
    <nav className="pager" aria-label="Pages">
      <button className="btn ghost small" disabled={page === 0} onClick={() => onPage(page - 1)}>Previous</button>
      <span className="muted small">Page {page + 1} of {totalPages}</span>
      <button className="btn ghost small" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</button>
    </nav>
  );
}

/** Flattens the category tree into [{id, name, depth}] for selects and filters. */
export function flattenCategories(tree, depth = 0, out = []) {
  (tree || []).forEach((c) => { out.push({ id: c.id, name: c.name, depth }); flattenCategories(c.children, depth + 1, out); });
  return out;
}
