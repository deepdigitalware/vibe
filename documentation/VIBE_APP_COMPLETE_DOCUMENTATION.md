# Vibe App - Complete System Documentation

## 📋 OVERVIEW
**Last Updated:** 2026-02-02 13:56:54
**Application:** Vibe Social Network Platform
**Primary Domain:** https://vibe.deepverse.cloud
**Admin Panel:** https://vibe.deepverse.cloud/admin

## 🏗️ SYSTEM ARCHITECTURE

### VPS Configuration
- **Server IP:** 31.97.206.179
- **Operating System:** Ubuntu 24.04 LTS
- **Root Access:** Available
- **SSH Port:** 22
- **Web Server:** Nginx
- **Database:** PostgreSQL
- **Process Manager:** PM2
- **SSL Certificate:** Let's Encrypt

### Network Configuration
```
PORT ALLOCATIONS:
├── 80    : HTTP (Redirects to HTTPS)
├── 443   : HTTPS (SSL/TLS)
├── 9999  : Node.js Admin API Server
└── 5432  : PostgreSQL Database
```

## 📁 FILE STRUCTURE

### VPS Directory Layout
```
/var/www/
├── vibe.deepverse.cloud/           # Main website files
│   ├── index.html                  # Landing page
│   ├── admin.html                  # Admin panel interface
│   ├── admin.js                    # Admin panel JavaScript
│   ├── style.css                   # Admin panel styling
│   └── manifest.json               # Web app manifest
│
└── admin.deepverse.cloud/          # REMOVED - Cleanup complete
    └── (Directory deleted)
```

### Application Files Location
```
MAIN WEBSITE FILES:
├── /var/www/vibe.deepverse.cloud/index.html
├── /var/www/vibe.deepverse.cloud/manifest.json
└── /var/www/vibe.deepverse.cloud/qr-code.html

ADMIN PANEL FILES:
├── /var/www/vibe.deepverse.cloud/admin.html
├── /var/www/vibe.deepverse.cloud/admin.js
└── /var/www/vibe.deepverse.cloud/style.css

ADMIN SERVER FILES:
└── /var/www/admin.deepverse.cloud/vibe/  # REMOVED
    └── (All files moved to main app structure)
```

## ⚙️ SERVICES CONFIGURATION

### Nginx Configuration
**Configuration Files:**
- Main config: `/etc/nginx/sites-available/vibe.deepverse.cloud`
- Enabled site: `/etc/nginx/sites-enabled/vibe.deepverse.cloud`

**SSL Setup:**
```
SSL Certificate: /etc/letsencrypt/live/vibe.deepverse.cloud/fullchain.pem
SSL Private Key: /etc/letsencrypt/live/vibe.deepverse.cloud/privkey.pem
Certificate Status: VALID (89 days remaining)
```

**Service Commands:**
```bash
# Check status
systemctl is-active nginx

# Restart service
systemctl restart nginx

# Test configuration
nginx -t
```

### PostgreSQL Database
**Database Details:**
```
Database Name: vibe
Database User: vibe
Database Password: Deep@Vibe
Database Port: 5432
Host: localhost
```

**Service Commands:**
```bash
# Check status
systemctl is-active postgresql

# Start service
systemctl start postgresql

# Connect to database
sudo -u postgres psql -d vibe
```

### PM2 Node.js Process Manager
**Application Details:**
```
Process Name: vibe-admin
Port: 9999
Working Directory: /var/www/admin.deepverse.cloud/vibe/
Status: ┌────┬───────────────┬─────────────┬─────────┬─────────┬──────────┬────────┬──────┬───────────┬──────────┬──────────┬──────────┬──────────┐
Memory Usage: ~80MB
```

**PM2 Commands:**
```bash
# Check status
pm2 status

# Restart application
pm2 restart vibe-admin

# View logs
pm2 logs vibe-admin

# Stop application
pm2 stop vibe-admin
```

## 🔐 AUTHENTICATION & SECURITY

### Admin Credentials
```
Admin Email: admin@vibenetwork
Admin Password: Deep@Vibe
```

### SSL Certificate Information
    Key Type: ECDSA
    Domains: postiz.deepverse.cloud
    Expiry Date: 2026-04-28 11:01:35+00:00 (VALID: 85 days)
    Certificate Path: /etc/letsencrypt/live/postiz.deepverse.cloud/fullchain.pem
    Private Key Path: /etc/letsencrypt/live/postiz.deepverse.cloud/privkey.pem
  Certificate Name: vibe.deepverse.cloud
    Serial Number: 5aa79c8022bc9cbdd334f4cbe2992afed99
    Key Type: ECDSA
    Domains: vibe.deepverse.cloud
    Expiry Date: 2026-05-03 06:59:59+00:00 (VALID: 89 days)
    Certificate Path: /etc/letsencrypt/live/vibe.deepverse.cloud/fullchain.pem
    Private Key Path: /etc/letsencrypt/live/vibe.deepverse.cloud/privkey.pem
  Certificate Name: whatomate.deepverse.cloud
    Serial Number: 6f5a9ffb83e64ec2961d0756e90fde92bb8
    Key Type: ECDSA
    Domains: whatomate.deepverse.cloud
    Expiry Date: 2026-04-20 11:45:13+00:00 (VALID: 77 days)


### Security Headers
Nginx is configured with the following security headers:
- X-Frame-Options: SAMEORIGIN
- X-Content-Type-Options: nosniff
- X-XSS-Protection: 1; mode=block

## 🌐 DOMAINS & ROUTING

### Active Domains
```
PRIMARY DOMAIN:
https://vibe.deepverse.cloud
├── /              : Main landing page
├── /admin         : Admin panel
├── /api/*         : API endpoints
├── /config        : Configuration endpoints
├── /wallet/*      : Wallet management
├── /profile/*     : User profiles
├── /discover/*    : User discovery
└── /uploads/*     : File uploads
```

### Removed Domains
```
REMOVED:
https://admin.deepverse.cloud  # Directory and configuration deleted
```

## 📊 DATABASE SCHEMA

### PostgreSQL Database Structure
```
DATABASE: vibe
USER: vibe
TABLES:
├── users          : User accounts and profiles
├── transactions   : Wallet transactions
├── config         : Application configuration
├── cms            : Content management system
└── analytics      : Usage analytics
```

### Database Connection String
```
postgresql://vibe:Deep@Vibe@localhost:5432/vibe
```

## 🔧 API ENDPOINTS

### Authentication
```
POST /api/login        : Admin login
POST /api/register     : User registration
```

### Admin Functions
```
GET  /admin/analytics  : System analytics
POST /config           : Update configuration
GET  /config           : Get configuration
```

### User Management
```
POST /profile/update   : Update user profile
GET  /profile/:userId  : Get user profile
GET  /discover/users   : Discover users
```

### Wallet System
```
GET  /wallet/balance/:userId  : Get user balance
POST /wallet/add              : Add funds
POST /wallet/deduct           : Deduct funds
```

### Content Management
```
GET  /api/cms          : Get CMS data
POST /api/cms          : Update CMS data
POST /api/upload       : File upload
```

## 📱 MOBILE APP INTEGRATION

### Android App Configuration
```
Package Name: com.vibe
Target SDK: 33
Firebase Integration: Enabled
Google Sign-in: Configured
Phone Authentication: Enabled
```

### APK Build Information
```
Latest Build: app-debug.apk
Size: ~55MB
Location: Local development environment
```

### Real-time Updates
The mobile app connects to the admin panel for:
- User authentication
- Profile management
- Wallet transactions
- Content updates
- Real-time notifications

## 🔧 MAINTENANCE COMMANDS

### System Monitoring
```bash
# Check all service statuses
systemctl is-active nginx postgresql
pm2 status

# View system resources
htop
df -h

# Check disk usage
du -sh /var/www/
```

### Log Management
```bash
# Nginx logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log

# Application logs
pm2 logs vibe-admin

# Database logs
sudo tail -f /var/log/postgresql/postgresql-*.log
```

### Backup Commands
```bash
# Database backup
pg_dump vibe > vibe_backup_$(date +%Y%m%d).sql

# File backup
tar -czf vibe_files_$(date +%Y%m%d).tar.gz /var/www/vibe.deepverse.cloud/

# Configuration backup
tar -czf nginx_config_$(date +%Y%m%d).tar.gz /etc/nginx/
```

## 🆘 TROUBLESHOOTING

### Common Issues and Solutions

**Issue: Site not loading**
```bash
# Check nginx status
systemctl status nginx

# Check PM2 status
pm2 status

# Test configuration
nginx -t
```

**Issue: Database connection failed**
```bash
# Check PostgreSQL status
systemctl status postgresql

# Test database connection
sudo -u postgres psql -d vibe -c "SELECT 1;"
```

**Issue: SSL certificate errors**
```bash
# Check certificate status
certbot certificates

# Renew certificate
certbot renew
```

**Issue: Admin panel not accessible**
```bash
# Check if files exist
ls -la /var/www/vibe.deepverse.cloud/admin.*

# Restart services
systemctl restart nginx
pm2 restart vibe-admin
```

## 📞 SUPPORT INFORMATION

### VPS Access
```bash
ssh root@31.97.206.179
Password: Deep@SM#01170628
```

### Important File Locations
```
Web Root: /var/www/vibe.deepverse.cloud/
Nginx Config: /etc/nginx/sites-available/vibe.deepverse.cloud
SSL Certificates: /etc/letsencrypt/live/vibe.deepverse.cloud/
Database: PostgreSQL (vibe database)
Application Logs: PM2 logs vibe-admin
```

### Current Status
```
Nginx: active
PostgreSQL: active
PM2 Application: Running
SSL Certificate: Valid
Admin Panel: Accessible
Main Site: Live
```

---
*This documentation was automatically generated on 2026-02-02 13:56:54*
*For any issues, contact system administrator*
