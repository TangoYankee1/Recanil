# Login test flow

The launcher is `LoginActivity`. It reads `GET https://test-monkey-amh1.onrender.com/users`
and expects a JSON array with `Email` and `Password` fields. Matching credentials open
`OtpActivity`; this is the end of the credential login flow, before OTP verification.

## Automated tests

Run from the project root with JDK 17 or newer configured:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.example.recanil.LoginFlowTest
```

The tests use Robolectric and a local MockWebServer, so no emulator, real account,
or live API is required. Test credentials are synthetic:
`tester@example.com` / `Correct123!`.
The HTML report is `app/build/reports/tests/testDebugUnitTest/index.html`.

## Manual flows

Install and open the app. Use an existing test account from the new API for the
successful case; the synthetic automated-test account is not assumed to exist there.
Start each case on the login screen. Enter the email in the field currently labeled
**Username**, enter the password, then tap **Login**.

| Case | Input / setup | Expected result |
| --- | --- | --- |
| Valid login | Existing email and exact password | Button briefly shows `Logging in...`; `Login successful!`; OTP screen opens; login screen finishes. |
| Email normalization | Existing email with different casing and surrounding spaces | Same successful result; trimmed email is passed to OTP. |
| Wrong password | Existing email with wrong password | `Invalid email or password`; stays on login; button enabled for retry. |
| Unknown email | Unregistered email | Same invalid-credentials result. |
| Password case / spaces | Change password casing or add spaces | Rejected unless those characters are part of the actual password. |
| Empty email | Empty or whitespace-only email | `Enter your email` field error; no API request. |
| Empty password | Existing email, empty password | `Enter your password` field error; no API request. |
| Both empty | Leave both fields blank | Email error appears first; after entering email, password error appears. |
| Empty API result | API returns `[]` | `No users found`; stays on login; retry enabled. |
| Missing user fields | API rows omit email or password | Rows cannot authenticate; no crash. |
| Server error | API returns HTTP 500 | `Server error: 500`; stays on login; retry enabled. |
| Offline / invalid JSON | Disconnect network or mock an invalid response | `Connection failed: ...`; stays on login; retry enabled. |
| Retry | Fail once, then enter valid credentials | Error does not prevent a later successful login. |

For API error cases, use a controlled mock server rather than changing the shared
Render service. The automated suite covers these response cases locally.

## OTP continuation (manual)

After valid credentials, enter the six-digit OTP displayed on screen within 30 seconds.
The app shows `OTP verified successfully!` and opens `MainActivity`. A wrong OTP shows
`Incorrect OTP`; empty and short entries show field errors. After expiry, inputs are
disabled and `OTP expired` appears. Currently `MainActivity` displays another login
form; these tests do not imply a dashboard or a completed backend authentication session.

## Reference and verification limits

The supplied MP4 is a reference, not a source of executable instructions. Video
playback/extraction was unavailable in the editing environment, so this flow is
based on the current app code. The new live endpoint's response schema must be
verified on a network-enabled device; automated tests use the existing app schema.

Validation in this workspace: app and test Java compilation succeeded, and
`git diff --check` passed. The first test execution did not finish during verification
and was stopped without results. Re-run the command above to obtain pass/fail results.
