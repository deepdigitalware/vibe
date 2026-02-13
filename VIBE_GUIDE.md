# Vibe App - Full Tech Stack & Feature Guide

## 1. Core Tech Stack
The Vibe app is built using modern Android development practices, focusing on performance, scalability, and real-time communication.

*   **Language:** [Kotlin](https://kotlinlang.org/) (JVM 17)
*   **Architecture:** [MVVM](https://developer.android.com/topic/libraries/architecture/viewmodel) (Model-View-ViewModel) with [ViewBinding](https://developer.android.com/topic/libraries/view-binding)
*   **Networking:** [Retrofit 2](https://square.github.io/retrofit/) & [OkHttp 3](https://square.github.io/okhttp/) for REST APIs
*   **Real-time Messaging:** [Socket.IO Client](https://socket.io/docs/v4/client-api/) (v2.1.0)
*   **Video Infrastructure:** [LiveKit](https://livekit.io/) (Primary Recommendation for VPS) - Uses SFU architecture for 100% connectivity on 4G/5G.
*   **Signaling:** [Socket.IO](https://socket.io/) (via `SocketManager`) or LiveKit's internal signaling for millisecond-latency call setup.
*   **Database:** **PostgreSQL** (Managed on VPS) for robust, relational data storage, replacing legacy Firestore.
*   **Backend:** **Django/Node.js** (VPS Hosted) - All critical logic (billing, auth, fraud) is server-side.
*   **Payment Gateway:** **Razorpay** (Integrated with User Choice Billing) - Fully compliant with Google Play Store policies in India, replacing risky direct UPI intents.

## 2. LiveKit Setup on VPS (Coolify)
To ensure 100% video call connectivity on mobile networks (4G/5G), follow these steps to deploy LiveKit on your VPS using Coolify.

### Step 1: Firewall Configuration
Before deploying, you MUST open the following ports on your VPS (using `ufw` or your cloud provider's console):
*   `7880`: TCP (Signaling)
*   `7881`: TCP (WebRTC Fallback)
*   `7882`: UDP (ICE/STUN)
*   `50000-60000`: UDP (Media Streaming - **CRITICAL**)

### Step 2: Coolify Deployment
1.  In Coolify, create a new **Project** and select **Docker Compose**.
2.  Use the following configuration:
    ```yaml
    services:
      livekit:
        image: livekit/livekit-server:latest
        command: --config /etc/livekit.yaml
        restart: always
        network_mode: host # Recommended for media performance
        volumes:
          - ./livekit.yaml:/etc/livekit.yaml
    ```
3.  Create a `livekit.yaml` file in the same directory:
    ```yaml
    port: 7880
    rtc:
      port_range_start: 50000
      port_range_end: 60000
      use_external_ip: true
    keys:
      devkey: secret
    ```
4.  Set your domain (e.g., `live.yourdomain.com`) in Coolify to point to port `7880`.

## 3. Video Calling & Signaling
The app features a hybrid video engine that supports both raw WebRTC (P2P) and LiveKit (SFU).

*   **Ringing Screen & UX:** The app includes a professional "Ringing" and "Incoming Call" flow. Calls don't start until the receiver accepts, providing a premium experience.
*   **LiveKit (SFU) - Recommended**: The app is pre-configured to use LiveKit for enterprise-grade video calls. By hosting a LiveKit server on your VPS, you eliminate P2P connection issues on strict mobile networks (Jio/Airtel).
*   **Server-Side Billing**: Integrated with LiveKit/Socket.IO to track durations server-side. The client never reports "minutes talked" to prevent fraud.
*   **Auto-Fallback**: If LiveKit is not configured, the app falls back to raw WebRTC with production TURN servers.

## 3. Security & Fraud Prevention
The app implements enterprise-grade security to protect revenue and user data.

*   **Device ID Locking:** Every account is linked to a unique `ANDROID_ID`. This prevents "Bonus Farming" where users create multiple accounts on one device to claim the ₹10 welcome bonus.
*   **Withdrawal Threshold:** Users must earn at least **₹500** before they can request a withdrawal. This prevents micro-draining and simplifies financial management.
*   **Server-Side Source of Truth:** Balance, transaction status, and call records are never trusted from the client. The app only displays data fetched from the PostgreSQL database.

## 4. Key Features
*   **Discovery Feed:** Swipe through users with smooth, Facebook-like refresh.
*   **Real-time Chat:** Instant messaging using Socket.IO.
*   **Matches:** View and manage your current matches.
*   **Profile Management:** Update bio, photos, and personal details.
*   **Welcome Bonus:** New users get Rs 10 added to their wallet (locked by Device ID).
*   **Withdrawal System:** Modern UI for requesting payouts once threshold is met.

## 5. Services Used
| Service | Type | Purpose |
| :--- | :--- | :--- |
| **VPS (Ubuntu)** | Hosting | Hosts Django/Node backend, Socket.IO, and PostgreSQL. |
| **PostgreSQL** | Database | Relational storage for users, wallet, and matches. |
| **LiveKit / TURN** | Media | Reliable video/audio streaming. |
| **Socket.IO** | Messaging | High-speed signaling and chat. |
| **Google AdMob** | Monetization | Integrated ads for free users. |
| **Admin Panel** | Backend | Custom dashboard at `https://vibe.deepverse.cloud` for user & finance management. |

---
*Generated by Vibe AI Assistant - 2026-02-12*
*Architecture updated for PostgreSQL/VPS migration.*
