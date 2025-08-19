# StreamHive Android

- Name: StreamHive
- Backend: see project root Node R2 server

## Setup

1. Open this folder in Android Studio Iguana+.
2. Place your Firebase `google-services.json` into `app/`.
3. In `app/build.gradle.kts`, set `BACKEND_BASE_URL` to your deployed backend (must end with `/`). Example:
   ```kotlin
   buildConfigField("String", "BACKEND_BASE_URL", "\"https://your-render-url.onrender.com/\"")
   ```
4. Sync Gradle.
5. Run on device/emulator. Upload, list, play, and share should work.

## Ads
- AdMob App ID and unit IDs are stored in `res/values/strings.xml`.

## Signing
- Add your keystore to `app/` or secure path and configure a `signingConfig` in `app/build.gradle.kts`.