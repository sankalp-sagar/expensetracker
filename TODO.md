# TODO - Fix "Registration Failed"

- [x] Identify backend failure cause (inspect auth-service error payload / logs)
- [x] Verify frontend sends correct payload to `/api/auth/register` (field names)
- [x] Verify gateway routing + publicPaths allow POST `/api/auth/register`
- [x] Add better frontend error display for non-JSON/unknown errors (show status + response body)
- [ ] If backend returns 404/401/500 due to misrouting, fix gateway/publicPaths or axios base URL.
- [ ] Add quick curl test against `/api/auth/register` to reproduce.
- [ ] Run unit/integration test or start services and confirm fix.
