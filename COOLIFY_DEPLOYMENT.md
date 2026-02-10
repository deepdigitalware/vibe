# Vibe Enterprise Backend - Coolify Deployment Guide

This guide details how to deploy the Vibe Enterprise Backend and Admin Panel on a VPS using Coolify, with a PostgreSQL database.

## Prerequisites
- A VPS (Ubuntu 20.04/22.04 recommended).
- Coolify installed on the VPS (see [Coolify Installation](https://coolify.io/docs/installation)).
- This GitHub repository.

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
    - **Domains**: Set your domain (e.g., `https://api.vibe.com`) or use the IP:Port (e.g., `http://<your-ip>:9999`).
    - **Environment Variables**:
        - `JWT_SECRET`: Generate a strong secret.
        - `ADMIN_USER`: Your desired admin username.
        - `ADMIN_PASS`: Your desired admin password.
        - `DATABASE_URL`: `postgresql://vibenetwork:Deep%40Vibe@postgres:5432/vibenetwork`
5.  **Deploy**: Click "Deploy".
    - Coolify will build the `api` service and start the `postgres` service.
    - It will automatically set up the network between them.

### Option 2: Separate Services (Manual)
If you want to manage the Database separately in Coolify:

1.  **Create PostgreSQL Service**:
    - In Coolify, Add Resource -> Database -> PostgreSQL.
    - Name: `vibe-db`.
    - User: `vibenetwork`
    - Password: `Deep@Vibe`
    - DB: `vibenetwork`
    - Make sure it is public or attached to the same network.
    - Copy the **Internal Connection String**.

2.  **Create Application Service**:
    - Add Resource -> Git Repository.
    - Select this repo.
    - **Build Pack**: Dockerfile.
    - **Base Directory**: `/server`.
    - **Environment Variables**:
        - `DATABASE_URL`: Paste the Internal Connection String from Step 1.
        - `PORT`: `9999`.
    - **Ports Exposes**: `9999`.
    - **Deploy**.

## Verification
1.  Visit `http://<your-vps-ip>:9999/admin`.
2.  Login with the `ADMIN_USER` and `ADMIN_PASS` you configured.
3.  The database tables will be automatically created on first run.

## Android App Configuration
Ensure your Android app points to the deployed URL.
1.  Update `server_base_url.txt` in the Android project assets or `ApiClient.kt`.
2.  Rebuild the APK.
