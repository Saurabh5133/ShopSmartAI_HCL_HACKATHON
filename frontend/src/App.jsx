import { Link, NavLink, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { useStore } from './store';
import { Loading } from './ui';
import Catalog from './pages/Catalog';
import Product from './pages/Product';
import Compare from './pages/Compare';
import Cart from './pages/Cart';
import { OrderList, OrderDetail } from './pages/Orders';
import Wishlist from './pages/Wishlist';
import Account from './pages/Account';
import Assistant from './pages/Assistant';
import Admin from './pages/Admin';
import Login from './pages/Login';

function Guard({ admin, children }) {
  const { user, ready } = useStore();
  const loc = useLocation();
  if (!ready) return <Loading />;
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  if (admin && user.role !== 'ADMIN') return <Navigate to="/" replace />;
  return children;
}

function Header() {
  const { user, cartCount, wishIds, compare, logout } = useStore();
  const nav = useNavigate();
  const search = (e) => {
    e.preventDefault();
    const q = new FormData(e.currentTarget).get('q').toString().trim();
    nav(q ? `/?search=${encodeURIComponent(q)}` : '/');
  };
  return (
    <header className="top">
      <div className="wrap row between topin">
        <Link to="/" className="logo plain">ShopSmart<span>AI</span></Link>
        <form className="searchbar" role="search" onSubmit={search}>
          <input name="q" type="search" placeholder="Search laptops, phones, brands" aria-label="Search products" />
          <button className="btn small">Search</button>
        </form>
        <nav className="row gap" aria-label="Main">
          <NavLink to="/assistant" className="navlink assist">Ask ShopSmart</NavLink>
          {compare.length > 0 && <NavLink to="/compare" className="navlink">Compare ({compare.length})</NavLink>}
          {user && <NavLink to="/wishlist" className="navlink">Wishlist ({wishIds.length})</NavLink>}
          {user && <NavLink to="/cart" className="navlink">Cart ({cartCount})</NavLink>}
          {user ? (
            <>
              <NavLink to="/orders" className="navlink">Orders</NavLink>
              {user.role === 'ADMIN' && <NavLink to="/admin" className="navlink">Admin</NavLink>}
              <NavLink to="/account" className="navlink">{user.name.split(' ')[0]} <span className="tier">{user.membershipTier}</span></NavLink>
              <button className="link" onClick={() => { logout(); nav('/'); }}>Log out</button>
            </>
          ) : <Link to="/login" className="btn small">Log in</Link>}
        </nav>
      </div>
    </header>
  );
}

export default function App() {
  const { toast } = useStore();
  return (
    <>
      <a className="skip" href="#main">Skip to content</a>
      <Header />
      <main id="main" className="wrap">
        <Routes>
          <Route path="/" element={<Catalog />} />
          <Route path="/product/:id" element={<Product />} />
          <Route path="/compare" element={<Compare />} />
          <Route path="/login" element={<Login />} />
          <Route path="/cart" element={<Guard><Cart /></Guard>} />
          <Route path="/wishlist" element={<Guard><Wishlist /></Guard>} />
          <Route path="/orders" element={<Guard><OrderList /></Guard>} />
          <Route path="/orders/:id" element={<Guard><OrderDetail /></Guard>} />
          <Route path="/account" element={<Guard><Account /></Guard>} />
          <Route path="/assistant" element={<Guard><Assistant /></Guard>} />
          <Route path="/admin" element={<Guard admin><Admin /></Guard>} />
          <Route path="*" element={<p className="pad">Page not found. <Link to="/">Back to the shop</Link></p>} />
        </Routes>
      </main>
      {toast && <div key={toast.id} className={`toast ${toast.type}`} role="status">{toast.msg}</div>}
    </>
  );
}
