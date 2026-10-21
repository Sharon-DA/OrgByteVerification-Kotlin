# Implementation decisions

- **Kotlin + Jetpack Compose:** native Android UI with a small amount of code.
- **Mock API:** the task permits a local mock, so the app can be demonstrated without a live backend. The `VerificationApi` interface separates the UI/state logic from the mock implementation.
- **Polling only:** the status screen calls `getStatus(sessionId)` every three seconds while the status is `PENDING`. There is no result endpoint call.
- **Terminal states:** polling stops when the mock returns `VERIFIED` or `FAILED`.
- **Loading and errors:** initiation disables the start button while in progress. Exceptions are shown as errors rather than being treated as successful verification.
- **Simple UI:** plain typography, a single primary action, and a straightforward status display keep attention on the required flow rather than decorative elements.
