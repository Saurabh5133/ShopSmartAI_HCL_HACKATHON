import { Link } from 'react-router-dom';
import { api } from '../api';
import { useStore } from '../store';
import { Empty, ErrorNote, Loading, ProductCard, useLoad } from '../ui';

export default function Wishlist() {
  const { wishIds } = useStore();
  const { data, error, loading } = useLoad(() => api.get('/wishlist'), [wishIds.length]);
  if (loading && !data) return <Loading />;
  if (error) return <ErrorNote msg={error} />;
  if (!data.length) return <Empty title="Your wishlist is empty">Use "Save to wishlist" on any product. <Link to="/">Browse products</Link></Empty>;
  return (
    <section>
      <h1>Wishlist</h1>
      <div className="grid">{data.map((p) => <ProductCard key={p.id} p={p} />)}</div>
    </section>
  );
}
