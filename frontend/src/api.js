const BASE = (import.meta.env.VITE_API_URL || '/api/v1').replace(/\/$/, '');
let token = localStorage.getItem('ss_token');

export const getToken = () => token;
export function setToken(t) {
  token = t;
  if (t) localStorage.setItem('ss_token', t);
  else localStorage.removeItem('ss_token');
}

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function request(method, path, body) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  let payload;
  if (body instanceof FormData) payload = body; // browser sets the multipart boundary
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: payload });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the server. Check that the backend is running on port 8080.');
  }
  if (res.status === 204) return null;
  let data = null;
  try { data = await res.json(); } catch { /* empty body */ }
  if (!res.ok) {
    if (res.status === 401 && token) window.dispatchEvent(new Event('ss:logout')); // expired token
    throw new ApiError(res.status, data?.error || 'ERROR', data?.message || `Request failed (${res.status})`);
  }
  return data;
}

/** Builds ?a=1&b=2 and skips empty values. */
export function qs(obj) {
  const p = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => { if (v !== '' && v != null) p.set(k, v); });
  const s = p.toString();
  return s ? `?${s}` : '';
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b ?? {}),
  put: (p, b) => request('PUT', p, b),
  patch: (p, b) => request('PATCH', p, b),
  del: (p) => request('DELETE', p),
};
