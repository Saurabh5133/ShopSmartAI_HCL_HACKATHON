import { useSearchParams } from 'react-router-dom';
import { api, qs } from '../api';
import { Empty, ErrorNote, Loading, Pager, ProductCard, flattenCategories, useLoad } from '../ui';

const SORTS = [
  ['createdAt:desc', 'Newest'], ['price:asc', 'Price: low to high'], ['price:desc', 'Price: high to low'],
  ['rating:desc', 'Best rated'], ['name:asc', 'Name A to Z'],
];

export default function Catalog() {
  const [sp, setSp] = useSearchParams();
  const get = (k) => sp.get(k) || '';
  const sort = get('sort') || 'createdAt:desc';
  const page = Number(get('page') || 0);
  const [sortBy, sortDir] = sort.split(':');

  const cats = useLoad(() => api.get('/categories'), []);
  const res = useLoad(() => api.get('/products' + qs({
    search: get('search'), categoryId: get('categoryId'), brand: get('brand'),
    minPrice: get('minPrice'), maxPrice: get('maxPrice'), sortBy, sortDir, page, size: 12,
  })), [sp.toString()]);

  const set = (patch) => {
    const n = new URLSearchParams(sp);
    Object.entries(patch).forEach(([k, v]) => (v === '' || v == null ? n.delete(k) : n.set(k, v)));
    if (!('page' in patch)) n.delete('page');
    setSp(n);
  };
  const apply = (e) => {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    set({ brand: f.get('brand').toString().trim(), minPrice: f.get('minPrice'), maxPrice: f.get('maxPrice') });
  };
  const flat = flattenCategories(cats.data);
  const p = res.data;

  return (
    <div className="shop">
      <aside className="filters" aria-label="Filters">
        <h2>Filters</h2>
        <h3>Category</h3>
        <ul className="catlist">
          <li><button className={`link ${!get('categoryId') ? 'on' : ''}`} onClick={() => set({ categoryId: '' })}>All products</button></li>
          {flat.map((c) => (
            <li key={c.id} style={{ paddingLeft: c.depth * 14 }}>
              <button className={`link ${get('categoryId') === String(c.id) ? 'on' : ''}`} onClick={() => set({ categoryId: c.id })}>{c.name}</button>
            </li>
          ))}
        </ul>
        <form key={sp.toString()} className="form" onSubmit={apply}>
          <label>Brand<input name="brand" defaultValue={get('brand')} placeholder="Dell, Lenovo..." /></label>
          <div className="row gap">
            <label>Min price<input name="minPrice" type="number" min="0" defaultValue={get('minPrice')} /></label>
            <label>Max price<input name="maxPrice" type="number" min="0" defaultValue={get('maxPrice')} /></label>
          </div>
          <button className="btn small">Apply filters</button>
          {sp.size > 0 && <button type="button" className="link" onClick={() => setSp({})}>Clear all</button>}
        </form>
      </aside>

      <section>
        <div className="row between wrapflex">
          <h1>{get('search') ? `Results for "${get('search')}"` : 'Shop'}</h1>
          <label className="inline">Sort by
            <select value={sort} onChange={(e) => set({ sort: e.target.value })}>
              {SORTS.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
          </label>
        </div>
        {res.loading && <Loading />}
        <ErrorNote msg={res.error} />
        {p && (
          <>
            <p className="muted small">{p.totalElements} product{p.totalElements === 1 ? '' : 's'}</p>
            {p.content.length === 0
              ? <Empty title="No products match">Try removing a filter or searching for something broader.</Empty>
              : <div className="grid">{p.content.map((x) => <ProductCard key={x.id} p={x} />)}</div>}
            <Pager page={p.page} totalPages={p.totalPages} onPage={(n) => { set({ page: n }); window.scrollTo(0, 0); }} />
          </>
        )}
      </section>
    </div>
  );
}
