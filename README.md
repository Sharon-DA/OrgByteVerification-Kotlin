# Verification Demo (Android)

A small Kotlin + Jetpack Compose app demonstrating a verification session and status polling.

## What it does

- **Start screen:** calls a mock `initiate()` endpoint and displays the returned session ID.
- **Status screen:** polls the mock status endpoint every 3 seconds while the status is `PENDING`.
- Stops polling when the status becomes `VERIFIED` or `FAILED`.
- Shows loading and error states.
- Does not call a result endpoint.

The mock currently returns `PENDING` twice and then `VERIFIED`. No backend or API keys are required.

## Run it

1. Install Android Studio and a JDK 17 distribution.
2. Open this project folder in Android Studiocv;
3. Allow Gradle to sync and install the Android SDK platform requested by the project (API 35).
4. Start an emulator or connect an Android phone with USB debugging enabled.
5. Press **Run**.

If Gradle sync fails, check that Android Studio is using JDK 17 under **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.

## Main code

- `MainActivity.kt` — UI, mock API, state holder, and polling lifecycle.

 

