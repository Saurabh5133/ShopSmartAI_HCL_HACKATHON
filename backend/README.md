# ShopSmart AI — Backend (Phases 1–7)

Java 21 · Spring Boot 3.5.6 · Spring AI 1.1.0 · Spring Data JPA · Spring Security (JWT) · PostgreSQL

**Implemented:** foundation + schema, auth (register/login/me, BCrypt, JWT, roles), product catalog
(search/filter/sort/pagination/compare, category tree), admin product & category management,
wishlist, preferences, cart, transactional orders with stock locking, order tracking, CORS, global error handling.

**Not yet implemented:** membership endpoints (Phase 5), AI recommendations (Phase 6), RAG/PgVector (Phase 7).
The `memberships` and `documents` tables already exist; `policy_documents` (VECTOR column) is added in Phase 7.

## Prerequisites
- JDK 21, Maven 3.9+
- PostgreSQL 14+ (pgvector is only needed from Phase 7)

## Database
```sql
CREATE DATABASE shopsmart;
```
Tables are created automatically on startup from `src/main/resources/schema.sql` (idempotent).
Sample categories/products and an admin user are seeded on first run.

## Environment variables (all optional for local dev)
| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/shopsmart` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `postgres` / `postgres` |
| `JWT_SECRET` | dev-only default — **set your own (32+ chars)** |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@shopsmart.com` / `Admin@12345` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5174` |

## Run
```bash
mvn spring-boot:run
curl http://localhost:8080/api/v1/health
```

## Try it
```bash
# register + login
curl -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Rahul Sharma","email":"rahul@example.com","password":"StrongPassword123"}'
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"rahul@example.com","password":"StrongPassword123"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')

# catalog
curl "localhost:8080/api/v1/products?search=laptop&minPrice=40000&maxPrice=70000&sortBy=price&sortDir=asc"
curl "localhost:8080/api/v1/products/compare?ids=1,2,3"
curl localhost:8080/api/v1/categories

# cart -> order -> tracking
curl -X POST localhost:8080/api/v1/cart/items -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
curl -X POST localhost:8080/api/v1/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"deliveryAddress":"Demo address, Jalandhar","deliveryMethod":"HOME_DELIVERY"}'
curl localhost:8080/api/v1/orders/1/tracking -H "Authorization: Bearer $TOKEN"
```
Admin: log in as the seeded admin, then call `/api/v1/admin/...` (customers get 403).

## Phase 5 — Membership
`GET /memberships/me`, `GET /memberships/me/history`, `POST /memberships` `{"tier":"PRIME"}`,
`POST /memberships/me/renew` `{"tier":"PRIME","durationDays":30}` (allowed: 30, 90, 365).
Rules: one ACTIVE membership per user; BASIC never expires; PRIME/PREMIUM = 30 days on activation;
tier change is immediate (old record -> CANCELLED); expired paid tiers become EXPIRED and the user falls back to BASIC.

## Phase 6 — AI recommendations (Spring AI + Gemini)
Spring Boot 3.5.x + Spring AI 1.1.x. Gemini is reached through its OpenAI-compatible endpoint
(`spring-ai-starter-model-openai` with a custom base URL), so only a Gemini API key is needed.

Set the key as an environment variable (never commit it):
- PowerShell: `$env:GEMINI_API_KEY="your-key"` then `mvn spring-boot:run`
- IntelliJ: Run > Edit Configurations > Environment variables > `GEMINI_API_KEY=your-key`
- Optional: `GEMINI_MODEL` (default `gemini-3.5-flash`).

`POST /api/v1/ai/recommendations` `{"query":"Suggest a laptop under 70000 for Java development and AI projects"}`
Flow: LLM parses the request -> PostgreSQL returns active, in-stock candidates -> LLM picks/explains from those only ->
returned ids are validated and price/availability come from the database. Provider errors return `503 AI_UNAVAILABLE`.

## Phase 7 — RAG knowledge assistant (PgVector)
**Endpoints**
- Admin (ROLE_ADMIN): `POST /api/v1/admin/documents` (multipart: `file` PDF/.md/.json, `documentType`, `version`, optional `title`, `effectiveDate` (yyyy-MM-dd), `region`, `productCategory`, `membershipTier`) → **202** + document id/status; `GET /admin/documents`, `GET /admin/documents/{id}`, `DELETE /admin/documents/{id}` (chunks and embeddings are removed too).
- Any logged-in user: `POST /api/v1/ai/knowledge/ask` `{"question": "..."}` → `{answer, confidence, sources[{document, section, version}], followUpRequired}`.

**How it works:** upload → background thread extracts text (PDFBox / Markdown headings / JSON keys) → ~1000-char chunks → Gemini embeddings (`gemini-embedding-001`, 768 dims, via the OpenAI-compatible endpoint) → `policy_documents` (pgvector, cosine, HNSW index). Questions: embed → top 5 chunks from COMPLETED documents → chunks below `app.rag.min-score` are dropped → the LLM answers only from the numbered passages and says which it used → sources come only from passages that really exist.

**Confidence is an evidence label, not a probability:** `HIGH/MEDIUM/LOW` from the cosine similarity of the best cited passage (`app.rag.high-score` 0.75, `medium-score` 0.65). `NONE` = nothing above `min-score` (0.55), the LLM is not called. `LOW`/`NONE` set `followUpRequired=true`. The thresholds are starting values; adjust them after trying your own documents.

**Dimension:** `EMBEDDING_DIMENSION` (default 768) creates `vector(768)`. If you change it, run `DROP TABLE policy_documents;` and restart. Env: `GEMINI_EMBEDDING_MODEL` (default `gemini-embedding-001`). Same `GEMINI_API_KEY` as Phase 6.

**Install pgvector (Windows PostgreSQL):** the app still starts without it, but RAG endpoints return 503 `RAG_UNAVAILABLE`.
- Windows installer / EDB install: use a pgvector build for your PostgreSQL major version (see the pgvector README, "Windows" section: build with Visual Studio C++ Build Tools and `nmake`, then copy `vector.dll`, `vector.control` and the `vector--*.sql` files into the PostgreSQL `lib` and `share\extension` folders; run as Administrator), then restart the PostgreSQL service.
- Docker: `docker run -d --name shopsmart-pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=shopsmart -p 5432:5432 pgvector/pgvector:pg16`
- Check: `CREATE EXTENSION IF NOT EXISTS vector;` in psql/pgAdmin on the `shopsmart` database.

**Demo (PowerShell):** sample file is `samples/shopsmart-policies.md`.
```powershell
$a = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType "application/json" -Body (@{email="admin@shopsmart.com";password="Admin@12345"}|ConvertTo-Json)
$h = @{ Authorization = "Bearer $($a.accessToken)" }
# PowerShell 7+: -Form. (Windows PowerShell 5.1: use Postman, or curl.exe -F as shown below.)
curl.exe -X POST http://localhost:8080/api/v1/admin/documents -H "Authorization: Bearer $($a.accessToken)" `
  -F "file=@samples/shopsmart-policies.md" -F "documentType=POLICY" -F "version=2026.1" -F "title=ShopSmart Policies"
Invoke-RestMethod -Uri http://localhost:8080/api/v1/admin/documents -Headers $h     # wait for status COMPLETED
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/ai/knowledge/ask -Headers $h -ContentType "application/json" `
  -Body (@{question="Does Prime membership include free delivery?"}|ConvertTo-Json) | ConvertTo-Json -Depth 5
```

## Troubleshooting AI / RAG (read this first if something fails)

**Self-check (admin only):** log in as admin, then `GET /api/v1/admin/ai/status` (Bearer token). It reports the database,
pgvector, indexed documents/chunks and does one live call to the embedding model and one to the chat model, and tells you
exactly which part fails and why.

**RAG data:** the knowledge base starts empty. When `GEMINI_API_KEY` is set and no usable document exists, the bundled
`samples/shopsmart-policies.md` is indexed automatically at startup (set `RAG_SEED_SAMPLE=false` to disable). Check
`GET /api/v1/admin/documents`: status `COMPLETED` and `chunkCount > 0` means RAG has data. `FAILED` shows the reason in `statusMessage`.

| Symptom | Cause | Fix |
|---|---|---|
| 503 `RAG_UNAVAILABLE` "pgvector is not installed" | PostgreSQL has no pgvector | Install pgvector (see above) or use the `pgvector/pgvector:pg16` Docker image, then restart |
| 503 `RAG_UNAVAILABLE` "does not match EMBEDDING_DIMENSION" | Old non-empty `policy_documents` table | `DROP TABLE policy_documents; DELETE FROM documents;` then restart (an *empty* wrong table is repaired automatically) |
| 503 `AI_UNAVAILABLE` "API key missing or invalid" | `GEMINI_API_KEY` not set in the process that runs the backend | Set it in the same terminal/IDE run configuration, restart |
| 503 `AI_UNAVAILABLE` "quota or rate limit" | Free-tier Gemini limit | Wait, or `GEMINI_MODEL=gemini-2.5-flash` |
| Document status `FAILED` | See `statusMessage` | Fix the cause, delete the document, upload again |
