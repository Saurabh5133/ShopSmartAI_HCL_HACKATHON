import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useStore } from '../store';
import { ErrorNote, Loading, Money, ProductImage, Stars, StockBadge, useLoad } from '../ui';

export default function Product() {
  const { id } = useParams();
  const { addToCart, toggleWish, toggleCompare, wishIds, compare } = useStore();
  const { data: p, error, loading } = useLoad(() => api.get(`/products/${id}`), [id]);
  const [qty, setQty] = useState(1);
  if (loading) return <Loading />;
  if (error) return <><ErrorNote msg={error} /><Link to="/">Back to the shop</Link></>;
  const max = Math.min(p.stockQuantity, 100);
  return (
    <section className="detail">
      <p className="small"><Link to="/">Shop</Link>{p.categoryName && <> / <Link to={`/?categoryId=${p.categoryId}`}>{p.categoryName}</Link></>}</p>
      <div className="split">
        <ProductImage p={p} />
        <div>
          <p className="muted">{p.brand}</p>
          <h1>{p.name}</h1>
          <div className="row gap"><Stars v={p.rating} /><StockBadge p={p} /></div>
          <p className="bigprice"><Money v={p.price} /></p>
          <p className="prose">{p.description || 'No description available.'}</p>
          <div className="row gap">
            <label className="inline">Quantity
              <input type="number" min="1" max={max || 1} value={qty} disabled={max === 0}
                onChange={(e) => setQty(Math.max(1, Math.min(max, Number(e.target.value) || 1)))} />
            </label>
            <button className="btn" disabled={max === 0} onClick={() => addToCart(p.id, qty)}>Add to cart</button>
          </div>
          <div className="row gap pad-top">
            <button className="link" onClick={() => toggleWish(p.id)}>{wishIds.includes(p.id) ? 'Remove from wishlist' : 'Save to wishlist'}</button>
            <button className="link" onClick={() => toggleCompare(p.id)}>{compare.includes(p.id) ? 'Remove from comparison' : 'Add to comparison'}</button>
          </div>
        </div>
      </div>
    </section>
  );
}
