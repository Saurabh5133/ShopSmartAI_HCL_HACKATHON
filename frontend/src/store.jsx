import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, getToken, setToken } from './api';

const Ctx = createContext(null);
export const useStore = () => useContext(Ctx);

export function StoreProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);
  const [cartCount, setCartCount] = useState(0);
  const [wishIds, setWishIds] = useState([]);
  const [compare, setCompare] = useState(() => {
    try { return JSON.parse(localStorage.getItem('ss_compare') || '[]'); } catch { return []; }
  });
  const [toast, setToast] = useState(null);

  const notify = useCallback((msg, type = 'ok') => setToast({ msg, type, id: Date.now() }), []);
  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3800);
    return () => clearTimeout(t);
  }, [toast]);
  useEffect(() => localStorage.setItem('ss_compare', JSON.stringify(compare)), [compare]);

  const refresh = useCallback(async () => {
    try {
      const [c, w] = await Promise.all([api.get('/cart'), api.get('/wishlist')]);
      setCartCount(c.items.reduce((n, i) => n + i.quantity, 0));
      setWishIds(w.map((p) => p.id));
    } catch { /* header counters are best-effort */ }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    setCartCount(0);
    setWishIds([]);
  }, []);
  useEffect(() => {
    window.addEventListener('ss:logout', logout);
    return () => window.removeEventListener('ss:logout', logout);
  }, [logout]);

  useEffect(() => {
    const onTier = (e) => setUser(e.detail); // membership page refreshed the profile
    window.addEventListener('ss:tier', onTier);
    return () => window.removeEventListener('ss:tier', onTier);
  }, []);

  useEffect(() => {
    (async () => {
      if (getToken()) {
        try {
          setUser(await api.get('/auth/me'));
          await refresh();
        } catch { setToken(null); }
      }
      setReady(true);
    })();
  }, [refresh]);

  const login = async (email, password) => {
    const r = await api.post('/auth/login', { email, password });
    setToken(r.accessToken);
    setUser(r.user);
    await refresh();
    return r.user;
  };
  const register = async (name, email, password) => {
    await api.post('/auth/register', { name, email, password });
    return login(email, password);
  };

  const addToCart = async (productId, quantity = 1) => {
    if (!user) { notify('Log in to add items to your cart', 'err'); return false; }
    try {
      await api.post('/cart/items', { productId, quantity });
      await refresh();
      notify('Added to cart');
      return true;
    } catch (e) { notify(e.message, 'err'); return false; }
  };
  const toggleWish = async (productId) => {
    if (!user) { notify('Log in to save items to your wishlist', 'err'); return; }
    try {
      if (wishIds.includes(productId)) {
        await api.del(`/wishlist/items/${productId}`);
        setWishIds((ids) => ids.filter((i) => i !== productId));
      } else {
        await api.post('/wishlist/items', { productId });
        setWishIds((ids) => [...ids, productId]);
      }
    } catch (e) { notify(e.message, 'err'); }
  };
  const toggleCompare = (id) => {
    if (compare.includes(id)) return setCompare(compare.filter((i) => i !== id));
    if (compare.length >= 4) return notify('You can compare up to 4 products', 'err');
    setCompare([...compare, id]);
  };

  const value = {
    user, ready, cartCount, wishIds, compare, toast, notify, refresh,
    login, register, logout, addToCart, toggleWish, toggleCompare,
    clearCompare: () => setCompare([]),
  };
  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}
