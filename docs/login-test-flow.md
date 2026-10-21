# Signup, OTP, dashboard, and customer verification

The app now uses the Render routes documented in `../backend/README.md`.
Deploy the replacement backend and configure Supabase before a live test.
The old GET /users password download has been removed.

Automated Android tests use Robolectric and MockWebServer:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Run the backend contract tests separately:

```powershell
cd backend
npm ci
npm test
```

Manual device verification:

1. Sign up using a unique email, username, and password of at least six characters.
   Confirm the account appears in Supabase Authentication > Users. If email
   confirmation is enabled, confirm the email before login; disable it for the
   immediate-login school demonstration.
2. Log in. Invalid credentials stay on login; valid credentials open OTP.
3. Enter the displayed six-digit OTP within 30 seconds. Incorrect codes are
   rejected; expired codes cannot be submitted. Rotate the screen and verify the
   same OTP and original deadline are preserved. Back to Login allows a new login.
4. Successful OTP opens Dashboard with Add Customer, Customer List, and Log Out.
5. Attempt to save an empty customer: required fields should show errors.
6. Save all four fields, including a phone number starting with 0. The generated
   customer ID and phone number should appear in Customer List and Supabase.
7. With more than 50 customers, Load More should retrieve the next page.
8. Turn off networking and check retry messages on login, signup, add, and list.
   For an ambiguous failed save, check the list before retrying to avoid duplicates.
9. Log out and confirm Back does not reopen the dashboard.
10. An expired user token must return customer screens to login.

The displayed OTP remains a client-side demo. Accounts are managed by Supabase
Auth; customers are shared between signed-in users. No Supabase secret key is
included in the Android application. The sample hardcoded accounts from the old
backend are not imported.

The reference video is contextual material; automated tests exercise the source
code and mock HTTP responses, not video playback or the live deployed service.

Verified in this workspace on October 22, 2026: `:app:assembleDebug` succeeded,
all 12 Android unit tests passed, all 8 backend tests passed, and `git diff --check`
passed. The APK is `app/build/outputs/apk/debug/app-debug.apk`.
Live Render/Supabase integration and physical-device UI checks remain pending
deployment and environment configuration; no real accounts or customers were
created by the automated tests.
