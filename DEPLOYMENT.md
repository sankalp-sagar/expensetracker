# ExpenseTracker Deployment Guide

This guide is written for a smooth portfolio/demo deployment. The simplest reliable setup is:

- Run the Java microservices and infrastructure with Docker Compose on one VM.
- Serve the React app as static files from a frontend host.
- Expose only the API Gateway publicly.

The project has two roots:

- `expensetracker/` - Spring Boot microservices, Docker Compose, Kubernetes manifests.
- `frontend/` - React client.

## 1. Pre-Deployment Checklist

Install these locally before building:

- Docker 24+
- Docker Compose v2
- Node.js 20+ or 22+
- npm
- `curl`
- `jq` for API smoke tests

Check the app builds:

```bash
cd frontend
npm install
npm run build

cd ../expensetracker
docker compose build user-service api-gateway auth-service group-service expense-service settlement-service notification-service analytics-service
```

Before sharing the app, verify there are no app-builder/runtime branding references:

```bash
cd ..
rg -i "emergent|emergentbase|assets\\.emergent|posthog|phc_" .
```

That command should return nothing.

## 2. Recommended Demo Architecture

Use this for a recruiter-facing demo:

```text
Browser
  |
  | https://your-frontend-domain.com
  v
Static React host
  |
  | REACT_APP_API_BASE=https://api.your-domain.com
  v
Nginx/Caddy/Cloud load balancer
  |
  | forwards to localhost:8080
  v
Spring Cloud API Gateway
  |
  v
Internal services: auth, user, group, expense, settlement, notification, analytics
  |
  v
Postgres, Redis, Kafka
```

Only expose:

- Frontend HTTPS domain
- API Gateway HTTPS domain

Do not expose service ports `8081` through `8087`, Postgres, Redis, Kafka, Eureka, or Grafana publicly.

## 3. Backend Deploy With Docker Compose

SSH into your VM and clone the repo:

```bash
git clone <your-repo-url>
cd expensetracker/expensetracker
```

Create production environment variables:

```bash
cp .env.example .env
nano .env
```

Set at least these values:

```bash
JWT_SECRET=<generate-a-long-random-secret-at-least-32-chars>
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com

STORAGE_PROVIDER=local
OCR_PROVIDER=none
```

If using Google login:

```bash
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
OAUTH2_REDIRECT_BASE=https://api.your-domain.com
OAUTH2_SUCCESS_REDIRECT=https://your-frontend-domain.com/oauth2/callback
```

In Google Cloud Console, add this authorized redirect URI:

```text
https://api.your-domain.com/login/oauth2/code/google
```

Start the backend:

```bash
docker compose up -d --build
```

Check service health:

```bash
docker compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Watch logs if something fails:

```bash
docker compose logs -f api-gateway
docker compose logs -f auth-service
docker compose logs -f user-service
```

## 4. Add HTTPS Reverse Proxy

Use Caddy for the least friction. Install Caddy on the VM, then create a Caddyfile like:

```caddyfile
api.your-domain.com {
  reverse_proxy localhost:8080
}
```

Reload Caddy:

```bash
sudo caddy reload --config /etc/caddy/Caddyfile
```

Verify:

```bash
curl https://api.your-domain.com/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

## 5. Frontend Deploy

Create a frontend environment file:

```bash
cd ../frontend
cat > .env.production <<'EOF'
REACT_APP_API_BASE=https://api.your-domain.com
REACT_APP_WS_BASE=https://api.your-domain.com
REACT_APP_GOOGLE_OAUTH_ENABLED=false
EOF
```

If Google login is enabled:

```bash
REACT_APP_GOOGLE_OAUTH_ENABLED=true
```

Build:

```bash
npm install
npm run build
```

Deploy the `frontend/build/` folder to one of these:

- Netlify
- Vercel
- S3 + CloudFront
- Nginx static site
- Caddy static site

For React Router, configure SPA fallback so refreshes work:

```text
/* -> /index.html
```

For Nginx:

```nginx
location / {
  try_files $uri /index.html;
}
```

For Netlify, add `frontend/public/_redirects` if needed:

```text
/* /index.html 200
```

## 6. Smoke Test The Live App

Register a user:

```bash
curl -X POST https://api.your-domain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"Password123!","fullName":"Alice Demo"}'
```

Login and capture token:

```bash
ACCESS=$(curl -s -X POST https://api.your-domain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"Password123!"}' | jq -r .data.accessToken)
```

Check profile:

```bash
curl https://api.your-domain.com/api/users/me \
  -H "Authorization: Bearer $ACCESS"
```

Check the frontend manually:

1. Open `https://your-frontend-domain.com`.
2. Register or login.
3. Create a group.
4. Add an expense.
5. Change Settings default currency to `INR`.
6. Leave Settings and come back. It should still show `INR`.
7. Open browser Network tab and confirm requests go only to your frontend domain, API domain, Google Fonts, and any intentionally enabled OAuth provider.

## 7. Data Persistence And Backups

The Docker Compose file uses named volumes:

- `pg_auth`
- `pg_user`
- `pg_group`
- `pg_expense`
- `pg_settlement`
- `pg_notification`
- `pg_analytics`

Do not run `docker compose down -v` on a deployed demo unless you want to erase the databases.

Backup example:

```bash
docker compose exec postgres-auth pg_dump -U splitwise authdb > authdb.sql
docker compose exec postgres-user pg_dump -U splitwise userdb > userdb.sql
docker compose exec postgres-group pg_dump -U splitwise groupdb > groupdb.sql
docker compose exec postgres-expense pg_dump -U splitwise expensedb > expensedb.sql
docker compose exec postgres-settlement pg_dump -U splitwise settlementdb > settlementdb.sql
```

For a longer-running production deployment, move Postgres, Redis, and Kafka to managed services instead of keeping them on the same VM.

## 8. Updating The Deployment

Backend update:

```bash
cd expensetracker/expensetracker
git pull
docker compose up -d --build
docker compose ps
```

Frontend update:

```bash
cd frontend
git pull
npm install
npm run build
```

Then redeploy the new `build/` folder.

## 9. Kubernetes Option

Kubernetes manifests are in:

```text
expensetracker/k8s/
```

Use them only after the Docker Compose deployment is working. Before applying:

1. Push every service image to your registry.
2. Replace every `image: expensetracker/<service>:1.0.0` with your registry image.
3. Replace `JWT_SECRET`, `DB_USER`, and `DB_PASS` in `00-namespace-config.yaml`.
4. Prefer managed Postgres/Redis/Kafka for production instead of the included single-replica manifests.
5. Add an Ingress for `api.your-domain.com` that routes to `api-gateway`.

Apply:

```bash
kubectl apply -f expensetracker/k8s/00-namespace-config.yaml
kubectl apply -f expensetracker/k8s/01-infrastructure.yaml
kubectl apply -f expensetracker/k8s/02-databases.yaml
kubectl apply -f expensetracker/k8s/03-platform-services.yaml
kubectl apply -f expensetracker/k8s/04-business-services.yaml
```

Check:

```bash
kubectl -n expensetracker get pods
kubectl -n expensetracker logs deploy/api-gateway
```

## 10. Common Issues

`401 Missing bearer token`

The frontend is calling a protected API without login, or `REACT_APP_API_BASE` points to the wrong backend.

`CORS error`

Set backend `.env`:

```bash
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

Then restart:

```bash
docker compose up -d --build auth-service
```

`Currency does not save`

Make sure `user-service`, `postgres-user`, and `api-gateway` are running:

```bash
docker compose ps user-service postgres-user api-gateway
docker compose logs --tail=100 user-service
```

`Frontend refresh returns 404`

Your static host is missing the SPA fallback to `/index.html`.

`WebSocket does not connect`

The current frontend defaults `REACT_APP_WS_BASE` to `http://localhost:8085`. For production, set:

```bash
REACT_APP_WS_BASE=https://api.your-domain.com
```

If you want WebSockets through the gateway, add a gateway route for the settlement service WebSocket endpoint. Otherwise expose settlement-service privately behind a separate HTTPS reverse proxy route.

## 11. Final Recruiter Demo Checklist

- Use HTTPS for frontend and API.
- Use a real `JWT_SECRET`, not the sample value.
- Do not expose internal ports publicly.
- Confirm browser Network tab has no app-builder, analytics, or visual-editing scripts.
- Prepare one seeded demo account with a group and two expenses.
- Keep a short architecture diagram or README section ready to explain service boundaries.
