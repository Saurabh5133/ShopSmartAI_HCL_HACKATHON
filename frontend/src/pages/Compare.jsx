import { Link } from 'react-router-dom';
import { api } from '../api';
import { useStore } from '../store';
import { Empty, ErrorNote, Loading, Money, ProductImage, Stars, StockBadge, useLoad } from '../ui';

export default function Compare() {
  const { compare, toggleCompare, clearCompare, addToCart } = useStore();
  const { data, error, loading } = useLoad(
    () => (compare.length >= 2 ? api.get(`/products/compare?ids=${compare.join(',')}`) : Promise.resolve([])), [compare.join(',')]);
  if (compare.length < 2) {
    return <Empty title="Pick at least two products to compare">Tick "Compare" on product cards, then come back here. <Link to="/">Browse products</Link></Empty>;
  }
  if (loading) return <Loading />;
  const list = data || [];
  const cheapest = list.length ? Math.min(...list.map((p) => Number(p.price))) : null;
  const rows = [
    ['Price', (p) => <><Money v={p.price} />{Number(p.price) === cheapest && <span className="badge in"> Lowest</span>}</>],
    ['Rating', (p) => <Stars v={p.rating} />],
    ['Brand', (p) => p.brand || '-'],
    ['Category', (p) => p.categoryName || '-'],
    ['Availability', (p) => <StockBadge p={p} />],
    ['Details', (p) => <span className="small">{p.description || '-'}</span>],
  ];
  return (
    <section>
      <div className="row between"><h1>Compare products</h1><button className="link" onClick={clearCompare}>Clear comparison</button></div>
      <ErrorNote msg={error} />
      <div className="tablewrap">
        <table className="compare">
          <thead>
            <tr><th scope="col"><span className="sr">Feature</span></th>
              {list.map((p) => (
                <th key={p.id} scope="col">
                  <ProductImage p={p} />
                  <Link to={`/product/${p.id}`}>{p.name}</Link>
                  <div className="row gap pad-top">
                    <button className="btn small" disabled={p.stockQuantity === 0} onClick={() => addToCart(p.id)}>Add to cart</button>
                    <button className="link small" onClick={() => toggleCompare(p.id)}>Remove</button>
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map(([label, cell]) => (
              <tr key={label}><th scope="row">{label}</th>{list.map((p) => <td key={p.id}>{cell(p)}</td>)}</tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
