import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useStore } from '../store';
import { Empty, ErrorNote, Loading, Money, fmtDate, useLoad } from '../ui';

const label = (s) => s.charAt(0) + s.slice(1).toLowerCase();
const Status = ({ s }) => <span className={`badge st-${s.toLowerCase()}`}>{label(s)}</span>;

export function OrderList() {
  const { data, error, loading } = useLoad(() => api.get('/orders'), []);
  if (loading) return <Loading />;
  if (error) return <ErrorNote msg={error} />;
  if (!data.length) return <Empty title="No orders yet"><Link to="/">Start shopping</Link></Empty>;
  return (
    <section>
      <h1>Your orders</h1>
      <ul className="lines">
        {data.map((o) => (
          <li key={o.id} className="card line">
            <div className="grow">
              <Link to={`/orders/${o.id}`}>Order #{o.id}</Link>
              <p className="muted small">{fmtDate(o.createdAt)} - {o.items.length} item{o.items.length === 1 ? '' : 's'}</p>
            </div>
            <Status s={o.status} />
            <strong><Money v={o.totalAmount} /></strong>
          </li>
        ))}
      </ul>
    </section>
  );
}

export function OrderDetail() {
  const { id } = useParams();
  const { notify } = useStore();
  const order = useLoad(() => api.get(`/orders/${id}`), [id]);
  const track = useLoad(() => api.get(`/orders/${id}/tracking`), [id]);
  const [busy, setBusy] = useState(false);
  if (order.loading) return <Loading />;
  if (order.error) return <><ErrorNote msg={order.error} /><Link to="/orders">Back to orders</Link></>;
  const o = order.data;
  const cancel = async () => {
    if (!window.confirm('Cancel this order? Items go back into stock.')) return;
    setBusy(true);
    try { await api.post(`/orders/${id}/cancel`); notify('Order cancelled'); order.reload(); track.reload(); }
    catch (e) { notify(e.message, 'err'); } finally { setBusy(false); }
  };
  return (
    <section className="split cartpage">
      <div>
        <p className="small"><Link to="/orders">Your orders</Link></p>
        <h1>Order #{o.id} <Status s={o.status} /></h1>
        <ul className="lines">
          {o.items.map((i) => (
            <li key={i.productId} className="card line">
              <Link className="grow" to={`/product/${i.productId}`}>{i.productName}</Link>
              <span className="muted">{i.quantity} x <Money v={i.priceAtPurchase} /></span>
            </li>
          ))}
        </ul>
        <p className="row between"><span>Total</span><strong className="bigprice"><Money v={o.totalAmount} /></strong></p>
        <p className="muted small">Deliver to: {o.deliveryAddress} ({o.deliveryMethod ? label(o.deliveryMethod.replace('_', ' ')) : 'no method'})</p>
        {['PLACED', 'PROCESSING'].includes(o.status) && <button className="btn ghost" disabled={busy} onClick={cancel}>Cancel order</button>}
      </div>
      <aside className="card">
        <h2>Tracking</h2>
        {track.loading && <Loading />}
        <ErrorNote msg={track.error} />
        <ol className="timeline">
          {(track.data?.events || []).map((e, i) => (
            <li key={i}><strong>{label(e.status)}</strong><span className="muted small">{fmtDate(e.createdAt)}</span><span>{e.description}</span></li>
          ))}
        </ol>
      </aside>
    </section>
  );
}
