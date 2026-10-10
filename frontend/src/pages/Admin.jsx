import { useEffect, useState } from 'react';
import { api } from '../api';
import { useStore } from '../store';
import { ErrorNote, Loading, Money, Pager, fmtDate, flattenCategories, useLoad } from '../ui';

const STATUSES = ['PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
const blank = { name: '', categoryId: '', brand: '', description: '', price: '', rating: '', stockQuantity: '0', imageUrl: '' };

function Products() {
  const { notify } = useStore();
  const [page, setPage] = useState(0);
  const list = useLoad(() => api.get(`/products?size=10&page=${page}&sortBy=createdAt&sortDir=desc`), [page]);
  const cats = useLoad(() => api.get('/categories'), []);
  const flat = flattenCategories(cats.data);
  const [form, setForm] = useState(blank);
  const [editId, setEditId] = useState(null);
  const [err, setErr] = useState('');
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  async function save(e) {
    e.preventDefault();
    setErr('');
    const body = {
      name: form.name.trim(), categoryId: form.categoryId ? Number(form.categoryId) : null, brand: form.brand.trim() || null,
      description: form.description.trim() || null, price: Number(form.price), rating: form.rating === '' ? null : Number(form.rating),
      stockQuantity: Number(form.stockQuantity), imageUrl: form.imageUrl.trim() || null,
    };
    try {
      if (editId) await api.put(`/admin/products/${editId}`, body); else await api.post('/admin/products', body);
      notify(editId ? 'Product updated' : 'Product created');
      setForm(blank); setEditId(null); list.reload();
    } catch (ex) { setErr(ex.message); }
  }
  const edit = (p) => {
    setEditId(p.id);
    setForm({ name: p.name, categoryId: p.categoryId ?? '', brand: p.brand ?? '', description: p.description ?? '', price: String(p.price), rating: p.rating ?? '', stockQuantity: String(p.stockQuantity), imageUrl: p.imageUrl ?? '' });
    window.scrollTo(0, 0);
  };
  const stock = async (p, v) => {
    try { await api.patch(`/admin/products/${p.id}/stock`, { stockQuantity: Number(v) }); notify('Stock updated'); list.reload(); } catch (e) { notify(e.message, 'err'); }
  };
  const deactivate = async (p) => {
    if (!window.confirm(`Deactivate "${p.name}"? It will disappear from the shop.`)) return;
    try { await api.del(`/admin/products/${p.id}`); notify('Product deactivated'); list.reload(); } catch (e) { notify(e.message, 'err'); }
  };
  async function addCategory(e) {
    e.preventDefault();
    const form = e.currentTarget;
    const f = new FormData(form);
    try {
      await api.post('/admin/categories', { name: f.get('name').toString().trim(), description: null, parentId: f.get('parent') ? Number(f.get('parent')) : null });
      notify('Category created'); form.reset(); cats.reload();
    } catch (ex) { notify(ex.message, 'err'); }
  }

  return (
    <div>
      <form className="card form grid2" onSubmit={save}>
        <h2 className="span2">{editId ? `Edit product #${editId}` : 'Add a product'}</h2>
        <label>Name<input required value={form.name} onChange={set('name')} maxLength={200} /></label>
        <label>Category<select value={form.categoryId} onChange={set('categoryId')}><option value="">None</option>
          {flat.map((c) => <option key={c.id} value={c.id}>{'- '.repeat(c.depth)}{c.name}</option>)}</select></label>
        <label>Brand<input value={form.brand} onChange={set('brand')} /></label>
        <label>Image URL<input value={form.imageUrl} onChange={set('imageUrl')} placeholder="https://..." /></label>
        <label>Price (INR)<input required type="number" min="0" step="0.01" value={form.price} onChange={set('price')} /></label>
        <label>Stock<input required type="number" min="0" value={form.stockQuantity} onChange={set('stockQuantity')} /></label>
        <label>Rating (0 to 5)<input type="number" min="0" max="5" step="0.1" value={form.rating} onChange={set('rating')} /></label>
        <label className="span2">Description<textarea rows="2" value={form.description} onChange={set('description')} /></label>
        <div className="span2"><ErrorNote msg={err} /></div>
        <div className="row gap span2">
          <button className="btn">{editId ? 'Save changes' : 'Create product'}</button>
          {editId && <button type="button" className="btn ghost" onClick={() => { setEditId(null); setForm(blank); }}>Cancel edit</button>}
        </div>
      </form>

      <form className="row gap wrapflex pad-top" onSubmit={addCategory}>
        <label className="inline">New category<input name="name" required maxLength={100} /></label>
        <label className="inline">Parent<select name="parent"><option value="">None</option>{flat.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
        <button className="btn small ghost">Add category</button>
      </form>

      <h2>Products</h2>
      {list.loading && !list.data && <Loading />}
      <ErrorNote msg={list.error} />
      {list.data && (
        <div className="tablewrap">
          <table className="admin">
            <thead><tr><th>Name</th><th>Price</th><th>Stock</th><th><span className="sr">Actions</span></th></tr></thead>
            <tbody>
              {list.data.content.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}<br /><span className="muted small">{p.brand} - #{p.id}</span></td>
                  <td><Money v={p.price} /></td>
                  <td><input className="qty" type="number" min="0" defaultValue={p.stockQuantity} aria-label={`Stock for ${p.name}`}
                    onBlur={(e) => e.target.value !== String(p.stockQuantity) && stock(p, e.target.value)} /></td>
                  <td className="row gap"><button className="link" onClick={() => edit(p)}>Edit</button><button className="link danger" onClick={() => deactivate(p)}>Deactivate</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pager page={list.data.page} totalPages={list.data.totalPages} onPage={setPage} />
        </div>
      )}
    </div>
  );
}

function Documents() {
  const { notify } = useStore();
  const docs = useLoad(() => api.get('/admin/documents'), []);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const processing = docs.data?.some((d) => d.status === 'PROCESSING');
  useEffect(() => {
    if (!processing) return;
    const t = setInterval(docs.reload, 3000); // ingestion runs in the background
    return () => clearInterval(t);
  }, [processing, docs.reload]);

  async function upload(e) {
    e.preventDefault();
    const form = e.currentTarget;
    const fd = new FormData(form);
    [...fd.keys()].forEach((k) => { if (fd.get(k) === '') fd.delete(k); }); // do not send empty optional fields
    setBusy(true);
    setErr('');
    try {
      await api.post('/admin/documents', fd);
      notify('Upload accepted. Ingestion is running.');
      form.reset();
      docs.reload();
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }
  const remove = async (d) => {
    if (!window.confirm(`Delete "${d.title}" and its indexed chunks?`)) return;
    try { await api.del(`/admin/documents/${d.id}`); notify('Document deleted'); docs.reload(); } catch (ex) { notify(ex.message, 'err'); }
  };

  return (
    <div>
      <form className="card form grid2" onSubmit={upload}>
        <h2 className="span2">Upload a policy document</h2>
        <label className="span2">File (PDF, Markdown or JSON, max 10 MB)<input name="file" type="file" required accept=".pdf,.md,.markdown,.json" /></label>
        <label>Title (shown in citations)<input name="title" placeholder="Membership Benefits" /></label>
        <label>Document type<input name="documentType" required placeholder="POLICY" /></label>
        <label>Version<input name="version" required placeholder="2026.1" /></label>
        <label>Effective date<input name="effectiveDate" type="date" /></label>
        <label>Region<input name="region" /></label>
        <label>Product category<input name="productCategory" /></label>
        <label>Membership tier<select name="membershipTier"><option value="">Any</option><option>BASIC</option><option>PRIME</option><option>PREMIUM</option></select></label>
        <div className="span2"><ErrorNote msg={err} /></div>
        <div className="span2"><button className="btn" disabled={busy}>{busy ? 'Uploading...' : 'Upload and index'}</button></div>
      </form>

      <h2>Documents</h2>
      <ErrorNote msg={docs.error} />
      {docs.loading && !docs.data && <Loading />}
      {docs.data?.length === 0 && <p className="muted">No documents yet.</p>}
      {docs.data?.length > 0 && (
        <div className="tablewrap">
          <table className="admin">
            <thead><tr><th>Title</th><th>Version</th><th>Status</th><th>Chunks</th><th><span className="sr">Actions</span></th></tr></thead>
            <tbody>
              {docs.data.map((d) => (
                <tr key={d.id}>
                  <td>{d.title}<br /><span className="muted small">{d.documentType} - {fmtDate(d.createdAt)}</span></td>
                  <td>{d.version}</td>
                  <td><span className={`badge doc-${d.status.toLowerCase()}`}>{d.status.toLowerCase()}</span><br /><span className="muted small">{d.statusMessage}</span></td>
                  <td>{d.chunkCount}</td>
                  <td><button className="link danger" onClick={() => remove(d)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Orders() {
  const { notify } = useStore();
  const [result, setResult] = useState('');
  async function submit(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    try {
      const o = await api.post(`/admin/orders/${Number(f.get('id'))}/status`, { status: f.get('status') });
      setResult(`Order #${o.id} is now ${o.status.toLowerCase()}`);
      notify('Order status updated');
    } catch (ex) { notify(ex.message, 'err'); }
  }
  return (
    <form className="card form" onSubmit={submit}>
      <h2>Update an order</h2>
      <p className="muted small">Status can only move forward. Cancelling puts the items back in stock.</p>
      <label>Order number<input name="id" type="number" min="1" required /></label>
      <label>New status<select name="status">{STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}</select></label>
      <button className="btn">Update status</button>
      {result && <p role="status">{result}</p>}
    </form>
  );
}

export default function Admin() {
  const [tab, setTab] = useState('products');
  return (
    <section>
      <h1>Admin</h1>
      <div className="tabs" role="tablist">
        {[['products', 'Products'], ['documents', 'Knowledge documents'], ['orders', 'Orders']].map(([k, l]) => (
          <button key={k} role="tab" aria-selected={tab === k} className={tab === k ? 'on' : ''} onClick={() => setTab(k)}>{l}</button>
        ))}
      </div>
      {tab === 'products' && <Products />}
      {tab === 'documents' && <Documents />}
      {tab === 'orders' && <Orders />}
    </section>
  );
}
