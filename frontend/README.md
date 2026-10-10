# ShopSmart AI - Frontend

React 19 + Vite. Talks to the Spring Boot backend at `http://localhost:8080/api/v1`.

## Run
```powershell
npm install
npm run dev        # http://localhost:5174  (API requests are proxied to the backend on port 8082)
```
Start the backend first. If it runs elsewhere, copy `.env.example` to `.env` and set `VITE_API_URL`.

## Pages
Shop (search, category, brand, price, sort, pages) - product detail - compare (up to 4) - wishlist - cart and checkout -
orders with tracking and cancel - account (membership, renewal, preferences) - Ask ShopSmart (product recommendations
and policy questions with sources) - Admin (products, categories, stock, knowledge documents with live ingestion status, order status).

## Demo logins
Admin: `admin@shopsmart.com` / `Admin@12345` (backend defaults). Register any other account as a customer.

## Notes
- Images: the seed data stores local paths that the backend does not serve, so products without an `http(s)` image URL show a coloured brand tile. Add real image URLs in Admin to replace them.
- The JWT is kept in `localStorage`, which is fine for a hackathon demo. Prices, stock and totals always come from the server.
