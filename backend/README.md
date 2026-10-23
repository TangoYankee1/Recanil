# Deploy the Recanil API to the existing Render service

This folder contains replacement source for the separate
[Test-monkey backend](https://github.com/TangoYankee1/Test-monkey).
The Android app uses `https://test-monkey-1.onrender.com/`.
These local files have not been pushed to that repository or deployed.

## 1. Prepare Supabase

Project URL: `https://iflndfjfruuepvrmizlw.supabase.co`.

1. Run `customer.sql` in Supabase SQL Editor. It preserves existing rows and
   provides the service role with select/insert access.
2. Open Authentication > Sign In / Providers > Email (the exact dashboard label
   may vary). Enable email/password authentication. For the school demo, turn
   **Confirm email OFF**, so newly registered accounts can log in immediately.
   If it is on, signup instead asks the user to confirm their email.
3. In Settings > API Keys, copy a server-only secret key (`sb_secret_...`).
   Do not put it in Android code or commit it.

Accounts live in Supabase **Authentication > Users**, not the customer table.
Supabase Auth stores password hashes. The old hardcoded demo accounts are not
migrated; create an account through signup.

## 2. Update Test-monkey and Render

1. In a checkout of the Test-monkey repository, replace `server.js` and
   `package.json` with this folder's versions. Copy `package-lock.json` too.
   Copy `server.test.js` if you want to run its tests in that repository.
2. Commit and push those files to the branch watched by your Render service.
3. In Render > your existing Web Service > Environment, set:

   ```text
   SUPABASE_URL=https://iflndfjfruuepvrmizlw.supabase.co
   SUPABASE_SECRET_KEY=<your server-only secret key>
   ```

4. Use Node 22 or newer. Set Build Command to `npm ci` and Start Command to
   `npm start`. The Render service's root directory remains the backend repo root.
5. Redeploy and confirm `GET /` returns `version: 2`.

If instead deploying this Android repository directly, set Render's root directory
to `backend`; keep the same environment variables and commands.

## 3. Test the Render routes in Postman

Import `Recanil-Render.postman_collection.json`. It contains no Supabase keys.
Use a unique test email in Signup. Login's test script stores the user access token
in the collection's `token` variable. Add Customer and Customer List use that token.
Do not replace the user token with the Supabase secret key.

| Method | Path | Body / result |
| --- | --- | --- |
| POST | `/signup` | `{ "username": "Student", "email": "your-test-email", "password": "Correct123!" }` -> 201 |
| POST | `/login` | `{ "email": "your-test-email", "password": "Correct123!" }` -> user token |
| POST | `/customers` | Four required string fields: lastname, firstname, mobileno, address -> customer object, 201 |
| GET | `/customers?offset=0` | Customer array; pages contain up to 50 rows |

## 4. Run the Android flow

Create account -> log in -> copy the displayed six-digit OTP within 30 seconds ->
Dashboard -> Add Customer -> Save -> Customer List. The mobile number's leading
zero must survive. Customer List refreshes when reopened and supports Load More.
Log Out clears the app's navigation stack and user token.

The onscreen OTP is deliberately a local school demonstration, not a second
server authentication factor. The backend authenticates the password and issues a
token before that screen. All signed-in users share the customer list because
the requested schema has no owner column. The token is not persisted or refreshed;
expired tokens return the user to login. A local logout discards the token rather
than revoking an already issued Supabase access token.

## Local verification

```powershell
cd backend
npm ci
npm test
```

API tests use injected fake Supabase clients and an actual local HTTP server.
They do not create real accounts or customers. To run against your real Supabase
project locally, set the two environment variables in your shell, then `npm start`.
`.env.example` is documentation; this server does not automatically load `.env`.

Supabase references: [signUp](https://supabase.com/docs/reference/javascript/auth-signup),
[password login](https://supabase.com/docs/reference/javascript/auth-signinwithpassword),
[getUser](https://supabase.com/docs/reference/javascript/auth-getuser).
