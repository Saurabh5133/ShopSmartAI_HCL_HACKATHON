import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useStore } from '../store';
import { Empty, ErrorNote, Loading, Money, ProductImage, useLoad } from '../ui';

export default function Cart() {
  const { refresh, notify } = useStore();
  const nav = useNavigate();
  const { data: cart, error, loading, reload } = useLoad(() => api.get('/cart'), []);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const act = async (fn) => {
    try { await fn(); await refresh(); reload(); } catch (e) { notify(e.message, 'err'); }
  };
  async function place(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setErr('');
    try {
      const o = await api.post('/orders', { deliveryAddress: f.get('address').toString().trim(), deliveryMethod: f.get('method') });
      await refresh();
      notify('Order placed');
      nav(`/orders/${o.id}`);
    } catch (ex) { setErr(ex.message); } finally { setBusy(false); }
  }

  if (loading && !cart) return <Loading />;
  if (error) return <ErrorNote msg={error} />;
  if (!cart.items.length) return <Empty title="Your cart is empty"><Link to="/">Find something to buy</Link></Empty>;
  return (
    <div className="split cartpage">
      <section>
        <div className="row between"><h1>Your cart</h1>
          <button className="link" onClick={() => act(() => api.del('/cart'))}>Empty cart</button></div>
        <ul className="lines">
          {cart.items.map((i) => (
            <li key={i.itemId} className="card line">
              <ProductImage p={i.product} />
              <div className="grow">
                <Link to={`/product/${i.product.id}`}>{i.product.name}</Link>
                <p className="muted small">Unit price <Money v={i.unitPrice} /></p>
                <div className="row gap">
                  <label className="inline">Qty
                    <input type="number" min="1" max={Math.min(100, i.product.stockQuantity)} defaultValue={i.quantity}
                      onBlur={(e) => { const q = Number(e.target.value); if (q >= 1 && q !== i.quantity) act(() => api.patch(`/cart/items/${i.itemId}`, { quantity: q })); }} />
                  </label>
                  <button className="link" onClick={() => act(() => api.del(`/cart/items/${i.itemId}`))}>Remove</button>
                </div>
              </div>
              <strong><Money v={i.subtotal} /></strong>
            </li>
          ))}
        </ul>
      </section>
      <aside className="card form summary">
        <h2>Checkout</h2>
        <p className="row between"><span>Total</span><span className="bigprice"><Money v={cart.totalAmount} /></span></p>
        <form className="form" onSubmit={place}>
          <label>Delivery address<textarea name="address" required rows="3" placeholder="House, street, city, PIN" /></label>
          <label>Delivery method
            <select name="method" defaultValue="HOME_DELIVERY">
              <option value="HOME_DELIVERY">Home delivery</option>
              <option value="STORE_PICKUP">Store pickup</option>
            </select>
          </label>
          <ErrorNote msg={err} />
          <button className="btn" disabled={busy}>{busy ? 'Placing order...' : 'Place order'}</button>
          <p className="muted small">Payment is simulated in this demo. Prices and stock are checked again when you place the order.</p>
        </form>
      </aside>
    </div>
  );
}
