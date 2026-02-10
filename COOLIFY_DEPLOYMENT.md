# Vibe Enterprise Backend - Coolify Deployment Guide

This guide details how to deploy the Vibe Enterprise Backend and Admin Panel on a VPS using Coolify, with a PostgreSQL database.

## Prerequisites
- A VPS (Ubuntu 20.04/22.04 recommended).
- Coolify installed on the VPS (see [Coolify Installation](https://coolify.io/docs/installation)).
- This GitHub repository.
- **Firebase Service Account**: You need your `service-account.json` file from Firebase Console.

## Deployment Steps

### Option 1: Docker Compose (Recommended)
1.  **Login to Coolify Dashboard**.
2.  **Create a New Project** (e.g., "Vibe").
3.  **Add a Resource** -> **Git Repository** (or **Docker Compose** directly if you prefer).
    - If using Git: Connect this repository.
    - Build Pack: **Docker Compose**.
    - Coolify will detect `docker-compose.yml` in the root.
4.  **Configuration**:
    - **Name**: Vibe Backend
    - **Domains**: Set to `https://vibe.deepverse.cloud`
    - **Ports Exposes**: `9999`
    - **Environment Variables**:
        - `JWT_SECRET`: `8f9c234207e14996e9fee98932fc91f693cee40a04f6a199a8d5db95443d2ab63b72c3545e15808b7c9d2bc492b6abfd47c90f6b6c389323fc85ba42eba2b59f`
        - `ADMIN_USER`: Your desired admin username.
        - `ADMIN_PASS`: Your desired admin password.
        - `DATABASE_URL`: `postgresql://vibenetwork:Deep%40Vibe@postgres:5432/vibenetwork`
5.  **Firebase Credentials**:
    - The application requires `server/service-account.json` to function.
    - **Method A (Volume Mount)**: If you can upload files to your VPS, upload `service-account.json` to a path (e.g., `/var/vibe/service-account.json`) and update `docker-compose.yml` to mount it.
    - **Method B (Environment Variable - Recommended for Coolify)**:
        - Convert your `service-account.json` to a single-line string.
        - Add a new Environment Variable `FIREBASE_SERVICE_ACCOUNT` with the content of the JSON.
        - *Note: You will need to update `index.js` to support this if not already supported. See "Code Update" below.*

### Code Update for Environment Variable Support
If you prefer using an environment variable for Firebase, ensure `server/index.js` has the following logic (already included in latest update):

```javascript
let serviceAccount;
if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
} else {
    serviceAccount = require('./service-account.json');
}
```

6.  **Deploy**: Click "Deploy".

### Option 2: Separate Services (Manual)
1.  **Create PostgreSQL Service**:
    - User: `vibenetwork`, Password: `Deep@Vibe`, DB: `vibenetwork`.
    - Internal Connection String: `postgresql://vibenetwork:Deep%40Vibe@postgres:5432/vibenetwork`
2.  **Create Application Service**:
    - Build Pack: Dockerfile.
    - Base Directory: `/server`.
    - Env Vars: `DATABASE_URL`, `PORT=9999`, `JWT_SECRET=8f9c234207e14996e9fee98932fc91f693cee40a04f6a199a8d5db95443d2ab63b72c3545e15808b7c9d2bc492b6abfd47c90f6b6c389323fc85ba42eba2b59f`, `FIREBASE_SERVICE_ACCOUNT` (JSON content).
    - Expose Port: `9999`.

## Verification
1.  Visit `http://<your-vps-ip>:9999/admin`.
2.  Login with default or configured credentials.

## Android App Configuration
The Android app reads the backend URL from an asset file.

1.  **Locate the file**: `app/src/main/assets/branding/server_base_url.txt`
2.  **Update the URL**:
    - Open the file.
    - Replace the content with your Coolify backend URL (e.g., `http://<your-vps-ip>:9999` or `https://api.vibe.com`).
    - *Note: Ensure no trailing slashes or whitespace.*
3.  **Rebuild APK**:
    - Open the project in Android Studio.
    - Run `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
    - The app will now connect to your VPS.
