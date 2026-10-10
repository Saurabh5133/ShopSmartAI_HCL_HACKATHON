# ShopSmart AI (backend + frontend)

- `backend/`  Java 21, Spring Boot 3.5.6, Spring AI 1.1.0, PostgreSQL + pgvector  -> http://localhost:8080
- `frontend/` React 19 + Vite                                                      -> http://localhost:5174

## 1. Database
Create a database named `shopsmart` (defaults: user `postgres`, password `postgres`; override with SPRING_DATASOURCE_* env vars).
For the policy assistant (RAG) install pgvector, see `backend/README.md` ("Install pgvector"). Without it the app still starts
and only the knowledge endpoints answer 503.

## 2. Backend (IntelliJ or terminal)
Set the Gemini key ONLY as an environment variable (IntelliJ: Run Configuration > Environment variables):
`GEMINI_API_KEY=<your key>`. Never put it in a file.
```powershell
cd backend
mvn spring-boot:run
```
Look for `RAG ready (vector(768))` in the log, and test `http://localhost:8080/api/v1/health`.

## 3. Frontend
```powershell
cd frontend
npm install
npm run dev
```
Open http://localhost:5174. Admin login: `admin@shopsmart.com` / `Admin@12345`.

## 4. Quick demo
1. Log in as admin > Admin > Knowledge documents > upload `backend/samples/shopsmart-policies.md`
   (type POLICY, version 2026.1) and wait for status "completed".
2. Ask ShopSmart > "Ask about policies" > "Does Prime membership include free delivery?"
3. Ask ShopSmart > "Find a product" > "a laptop under 70000 for Java development".
4. Add to cart > checkout > Orders > tracking.
