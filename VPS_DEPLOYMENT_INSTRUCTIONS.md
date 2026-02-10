# VPS Deployment Instructions for Vibe App

This document outlines the steps to deploy the Vibe app landing page and admin panel to your VPS.

## Prerequisites

- VPS with SSH access (IP: 31.97.206.179, User: root, Password: Deep@SM#01170628)
- Domain names configured: 
  - vibe.deepverse.cloud pointing to your VPS
  - admin.deepverse.cloud pointing to your VPS

## Deployment Steps

### 1. Prepare Local Files

The following files have been prepared for deployment:

- Landing page: Located in `landing_page/` directory
- Server files: Located in `server/` directory
- Admin dashboard: Will be created at `server/public/admin.html`

### 2. Automated Deployment Script

We've created a Python script to deploy to your VPS:

```bash
python admin_server_deployment.py
```

This script will:
- Connect to your VPS using the provided credentials
- Deploy the landing page to vibe.deepverse.cloud:9000
- Deploy the admin panel to admin.deepverse.cloud:9999/vibe
- Set up necessary services and configurations

### 3. Manual Deployment Alternative

If you prefer manual deployment, follow these steps:

#### A. Deploy Landing Page (Port 9000)

1. Upload the `landing_page/index.html` file to `/var/www/vibe.deepverse.cloud/`
2. Create Nginx configuration for port 9000:
```nginx
server {
    listen 9000;
    server_name vibe.deepverse.cloud;

    root /var/www/vibe.deepverse.cloud;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header X-Content-Type-Options "nosniff" always;
}
```

#### B. Deploy Admin Panel (Port 9999)

1. Upload the entire `server/` directory to `/var/www/admin.deepverse.cloud/vibe/`
2. Install dependencies:
```bash
cd /var/www/admin.deepverse.cloud/vibe/
npm install
```

3. Create systemd service file:
```ini
[Unit]
Description=Vibe Admin Panel
After=network.target

[Service]
Type=simple
User=www-data
ExecStart=/usr/bin/node /var/www/admin.deepverse.cloud/vibe/index.js
WorkingDirectory=/var/www/admin.deepverse.cloud/vibe
Restart=always
Environment=PORT=9999
Environment=NODE_ENV=production

[Install]
WantedBy=multi-user.target
```

4. Create Nginx configuration for admin panel:
```nginx
server {
    listen 9999;
    server_name admin.deepverse.cloud;

    location /vibe {
        proxy_pass http://127.0.0.1:9999;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

### 4. Finalize Deployment

1. Enable and start services:
```bash
sudo systemctl daemon-reload
sudo systemctl enable vibe_admin
sudo systemctl start vibe_admin
sudo systemctl restart nginx
```

2. Open required ports:
```bash
sudo ufw allow 9000
sudo ufw allow 9999
```

## Access Information

- Landing Page: http://vibe.deepverse.cloud:9000
- Admin Panel: http://admin.deepverse.cloud:9999/vibe
- Admin Login: Username: admin, Password: password (change after first login)

## Post-Deployment Tasks

1. Update the APK file location in the landing page
2. Change the default admin credentials
3. Set up SSL certificates for HTTPS
4. Configure backup procedures

## Troubleshooting

If you encounter issues:

1. Check service status: `sudo systemctl status vibe_admin`
2. View logs: `pm2 logs vibe-admin` or `journalctl -u vibe_admin`
3. Verify file permissions
4. Ensure firewall allows the required ports