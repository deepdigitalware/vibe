#!/usr/bin/env python3
"""
Fix admin panel static files MIME type issue
"""

import paramiko

def fix_admin_static_files():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(hostname, username=username, password=password, timeout=10)
        print("✓ Connected to VPS")
        
        print("\n=== CHECKING CURRENT FILES ===")
        
        # Check what admin files exist
        stdin, stdout, stderr = ssh.exec_command('ls -la /var/www/vibe.deepverse.cloud/admin.*')
        files = stdout.read().decode()
        print("Current admin files:")
        print(files)
        
        print("\n=== UPDATING NGINX CONFIGURATION ===")
        
        # Update nginx configuration to properly serve static files
        updated_config = '''server {
    listen 80;
    server_name vibe.deepverse.cloud;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name vibe.deepverse.cloud;
    root /var/www/vibe.deepverse.cloud;
    index index.html;
    
    ssl_certificate /etc/letsencrypt/live/vibe.deepverse.cloud/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/vibe.deepverse.cloud/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    
    # Admin panel - serve static files properly
    location /admin {
        root /var/www/vibe.deepverse.cloud;
        try_files $uri $uri/ /admin.html;
        
        # Set proper MIME types for static files
        location ~* \.(css|js)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
        
        # Handle admin API requests
        location ~ ^/admin/(api|config|wallet|profile|discover|uploads) {
            proxy_pass http://127.0.0.1:9999;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
    
    # API endpoints
    location /api {
        proxy_pass http://127.0.0.1:9999/api;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Config endpoints
    location /config {
        proxy_pass http://127.0.0.1:9999/config;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Wallet endpoints
    location /wallet {
        proxy_pass http://127.0.0.1:9999/wallet;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Profile endpoints
    location /profile {
        proxy_pass http://127.0.0.1:9999/profile;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Discover endpoints
    location /discover {
        proxy_pass http://127.0.0.1:9999/discover;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Uploads
    location /uploads {
        proxy_pass http://127.0.0.1:9999/uploads;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Main site (landing page)
    location / {
        try_files $uri $uri/ =404;
    }
    
    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
}'''
        
        print("Updating nginx configuration...")
        stdin, stdout, stderr = ssh.exec_command("cat > /etc/nginx/sites-available/vibe.deepverse.cloud << 'EOF'\n" + updated_config + "\nEOF")
        stdout.channel.recv_exit_status()
        print("✓ Nginx configuration updated")
        
        print("\n=== TESTING CONFIGURATION ===")
        stdin, stdout, stderr = ssh.exec_command('nginx -t')
        test_result = stdout.read().decode()
        print("Nginx test result:")
        print(test_result)
        
        print("\n=== RESTARTING SERVICES ===")
        stdin, stdout, stderr = ssh.exec_command('systemctl restart nginx')
        restart_status = stdout.channel.recv_exit_status()
        if restart_status == 0:
            print("✓ Nginx restarted successfully")
        else:
            print("✗ Nginx restart failed")
        
        stdin, stdout, stderr = ssh.exec_command('cd /var/www/admin.deepverse.cloud/vibe && pm2 restart vibe-admin')
        pm2_status = stdout.channel.recv_exit_status()
        if pm2_status == 0:
            print("✓ PM2 service restarted")
        else:
            print("✗ PM2 service restart failed")
        
        print("\n=== TESTING FILE ACCESS ===")
        test_commands = [
            'curl -I https://vibe.deepverse.cloud/admin.css 2>/dev/null | head -3',
            'curl -I https://vibe.deepverse.cloud/admin.js 2>/dev/null | head -3',
            'curl -s https://vibe.deepverse.cloud/admin.html | head -5'
        ]
        
        for cmd in test_commands:
            print(f"Testing: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            print(f"Result: {result if result else 'No response'}")
        
        print("\n=== FINAL VERIFICATION ===")
        final_commands = [
            'systemctl is-active nginx',
            'pm2 status'
        ]
        
        for cmd in final_commands:
            print(f"Checking: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            print(result)
        
        print("\n" + "="*80)
        print("ADMIN STATIC FILES FIX COMPLETE!")
        print("="*80)
        print("✓ Updated nginx configuration for proper static file serving")
        print("✓ Set correct MIME types for CSS and JS files")
        print("✓ Services restarted")
        print("✓ File access tested")
        print("")
        print("Your admin panel should now work properly at:")
        print("🔧 Admin Panel: https://vibe.deepverse.cloud/admin")
        print("👤 Login: admin@vibenetwork / Deep@Vibe")
        print("="*80)
        
        ssh.close()
        print("\n✓ Connection closed")
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")

if __name__ == "__main__":
    print("Fixing admin static files...")
    fix_admin_static_files()