# Vibe — Android App (Splash + OTP Login + Bottom Nav + RTC Skeleton)

Vibe is a mobile-first app that demonstrates a production-ready flow:

- Splash screen using Android 12+ Splash API (backported via `androidx.core-splashscreen`).
- Mobile OTP login via Firebase Phone Auth (with graceful fallback if Firebase isn’t configured).
- Home screen with Bottom Navigation: `Discover`, `Chats`, `Profile` fragments.
- Wallet + UPI recharge skeleton and timed billing during calls.
- WebRTC preview and signaling skeleton ready to extend into full 1:1 calling.

## Project Structure

- `app/src/main/AndroidManifest.xml` — Declares `SplashActivity` as launcher, `LoginActivity`, `HomeActivity`, `CallActivity`, and `MainActivity`.
- `ui/`
  - `SplashActivity.kt` — Installs splash screen; routes to `Login` or `Home` based on `FirebaseAuth`.
  - `LoginActivity.kt` — Phone OTP login with `PhoneAuthProvider`; navigates to `Home` on success.
  - `HomeActivity.kt` — Hosts `BottomNavigationView` and swaps fragments.
  - `CallActivity.kt` — Manages call lifecycle, billing block countdown, and RTC start/stop.
  - `MainActivity.kt` — Original activity retained; not the launcher.
- `rtc/`
  - `RtcClient.kt` — WebRTC setup (preview, peer connection), coroutine-based offer/answer.
- `signaling/`
  - `FirestoreSignaling.kt` — Firestore-backed signaling helpers + `Task.await()` usage.
- `util/`
  - `CoroutinesExt.kt` — Coroutine wrappers for WebRTC `SdpObserver` operations.
- `wallet/`
  - `WalletManager.kt` — Simple local wallet store (balance, deduction).
- `billing/`
  - `BillingConfig.kt` — Rate per block and block duration.
- `res/layout/`
  - `activity_login.xml`, `activity_home.xml`, `fragment_discover.xml`, `fragment_chats.xml`, `fragment_profile.xml`.
- `res/drawable/`
  - `ic_splash_logo.xml`, `ic_discover.xml`, `ic_chats.xml`, `ic_profile.xml`.
- `res/menu/`
  - `menu_bottom_nav.xml` — Bottom navigation items.
- `res/values/`
  - `themes.xml` — `Theme.Vibe` with SplashScreen attributes.
  - `strings.xml` — App name and strings.

## Features

- `Splash` — Branded startup, consistent on Android 12+ and earlier devices.
- `Login (OTP)` — Send/verify OTP using Firebase Phone Auth. Handles missing Firebase gracefully.
- `Home` — Bottom nav with three sections:
  - `Discover` — Placeholder section for feed or recommendations.
  - `Chats` — Placeholder section for messaging.
  - `Profile` — Placeholder section for user profile, wallet, and settings.
- `Calls` — Starts preview; supports caller/callee flows with Firestore signaling and ICE exchange (skeleton).
- `Billing` — Deducts ₹10 per 2-minute block; ends calls when balance runs out.
- `UPI` — Launches `upi://pay` intent and credits wallet upon user-confirmed success (demo only).

## Tech Stack

- `Kotlin` (1.9)
- `AndroidX` — AppCompat, Fragment KTX, Lifecycle, Navigation, SplashScreen, ConstraintLayout, Material.
- `Coroutines` — `kotlinx-coroutines-android`, `-core`, `-play-services` for `Task.await()`.
- `Firebase` — `Auth` for OTP, `Firestore` for signaling.
- `WebRTC` — `com.infobip:google-webrtc` community artifact.

## Build & Run

- Open with Android Studio (Giraffe+), or use Gradle:
  - Windows: `.\n+gradlew.bat :app:assembleDebug` (if wrapper is present; otherwise install Gradle and run `gradle wrapper` first).
- Set UPI details in `app/build.gradle`:
  - `buildConfigField "String", "UPI_ID", '"your-merchant@upi"'`
  - `buildConfigField "String", "PAYEE_NAME", '"Vibe"'`
- Add `google-services.json` and enable Firebase Auth/Firestore in the console for OTP and signaling.

## Production Settings

- `Release build` — `minifyEnabled true` with `proguard-android-optimize.txt` and `app/proguard-rules.pro`.
- `Splash` — Configured via `themes.xml` (`windowSplashScreenAnimatedIcon`, `windowSplashScreenBackground`, `windowSplashScreenAnimationDuration`, `postSplashScreenTheme`).
- `Auth` — Use reCAPTCHA or SafetyNet verification; throttle attempts; monitor abuse.
- `Payments` — Always verify UPI payments server-side (do not trust client-only success strings).
- `RTC` — Use TURN servers for NAT traversal; secure signaling endpoints; add reconnection logic.

## Key Functions & Flows

- `SplashActivity` — `installSplashScreen()` and route to `Login` or `Home`.
- `LoginActivity` — `sendOtp()`, `verifyOtp()`, `onVerificationCompleted()`; navigates to `Home` on success.
- `HomeActivity` — `BottomNavigationView` -> `DiscoverFragment` | `ChatsFragment` | `ProfileFragment`.
- `RtcClient` — `startPreview()`, `startAsCaller()`, `startAsCallee()`, `endCall()`; uses coroutine wrappers in `CoroutinesExt.kt`.
- `FirestoreSignaling` — `createRoom()`, `getOffer()`, `setAnswer()`, `observeAnswer()`, `sendIceCandidate()`, `observeRemoteIce()`, `clearRoom()`.
- `WalletManager` — `getBalance()`, `credit()`, `deductBlockIfPossible(rate)`.

## Current Implementation Status

- Splash, Login (OTP), Home with Bottom Nav, and fragments — Implemented.
- RTC preview and signaling — Implemented as a functional skeleton; replace or harden for production.
- Billing with timed deduction — Implemented.
- UPI intent and wallet credit — Implemented (demo), requires server-side verification for production.
- Release minify and ProGuard rules — Configured.

## Estimated Costs (Indicative)

- `Firebase` — Free tier covers development; pay-as-you-go beyond quotas.
  - `Auth (Phone)` — Google covers SMS delivery; quotas and anti-abuse apply. Enterprise scale may require dedicated verification.
  - `Firestore` — Reads/writes and bandwidth billed; signaling typically light unless chat/history are stored.
- `TURN Servers` — Budget ₹3,000–₹10,000/month depending on traffic and region (e.g., 1–3 small instances).
- `Backend` — If you add payment verification and business logic, budget ₹1,000–₹5,000/month for a small server.
- `Monitoring/Crashlytics` — Typically free for basic use; premium tools add cost.

Example monthly total for early-stage: ₹4,000–₹12,000.

## Estimated Revenue (Example)

- `Pricing` — ₹10 per 2-minute block.
- `Assumptions` — 5,000 active users; 30% daily active; 10% of DAU start a call; average call 12 minutes (6 blocks).
- `Daily calls` — 5,000 × 30% × 10% = 150 calls/day.
- `Daily revenue` — 150 × 6 × ₹10 = ₹9,000/day.
- `Monthly revenue` — ~₹270,000/month, before platform fees and refunds.

Adjust assumptions to your market; use trials to calibrate conversion and average call length.

## Compliance & Safety

- Verify age and consent, offer reporting/moderation, publish ToS and Privacy Policy.
- Respect local regulations (IT Act, payments, data protection). Add KYC if needed.
- Secure data in transit and at rest; verify payments server-side.

## Roadmap

- Harden WebRTC: TURN, reconnection, network constraints, device compatibility.
- Add chat persistence, profiles with media, and monetization features.
- Implement server for payment verification and post-call settlements.
- Observability: analytics, crash reporting, performance monitoring.

## License

For now, this project is provided without a specific license. Do not use in production without adding a suitable license and completing the compliance and safety requirements.