#!/usr/bin/env python3
import paramiko
import time
import os

def deploy_vibenetwork():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(hostname, username=username, password=password, timeout=20)
        print("✓ Connected to VPS")
        
        # 1. Stop existing services
        print("Stopping services...")
        ssh.exec_command('pm2 stop all; pm2 delete all; pkill -f node')
        
        # 2. Cleanup old directory
        print("Cleaning up /var/www/vibe.deepverse.cloud...")
        ssh.exec_command('rm -rf /var/www/vibe.deepverse.cloud')
        
        # 3. Create new directory
        print("Creating /var/www/vibenetwork...")
        ssh.exec_command('mkdir -p /var/www/vibenetwork')
        
        # 4. Upload Files
        sftp = ssh.open_sftp()
        local_root = r"c:\Users\deepd\D\Vibe"
        remote_root = "/var/www/vibenetwork"
        
        # Upload server files
        print("Uploading server files...")
        server_files = ['index.js', 'admin.js', 'package.json', 'service-account.json']
        ssh.exec_command(f'mkdir -p {remote_root}/server/data {remote_root}/server/public {remote_root}/server/uploads')
        
        for f in server_files:
            try:
                sftp.put(f"{local_root}\\server\\{f}", f"{remote_root}/server/{f}")
            except Exception as e:
                print(f"Skipping {f}: {e}")

        # Upload public files
        print("Uploading public assets...")
        public_files = ['admin.html', 'style.css', 'admin.js'] # admin.js in public?
        for f in os.listdir(f"{local_root}\\server\\public"):
             sftp.put(f"{local_root}\\server\\public\\{f}", f"{remote_root}/server/public/{f}")
             
        # Upload Landing Page
        print("Uploading landing page...")
        ssh.exec_command(f'mkdir -p {remote_root}/landing_page')
        sftp.put(f"{local_root}\\landing_page\\index.html", f"{remote_root}/landing_page/index.html")
        
        # Upload APK
        print("Uploading APK...")
        try:
            sftp.put(f"{local_root}\\app\\build\\outputs\\apk\\debug\\app-debug.apk", f"{remote_root}/landing_page/app-debug.apk")
        except:
            print("APK not found, skipping...")

        sftp.close()
        
        # 5. Setup Server Dependencies
        print("Installing dependencies...")
        ssh.exec_command(f'cd {remote_root}/server && npm install')
        
        # 6. Configure Nginx
        print("Configuring Nginx...")
        nginx_conf = """
server {
    listen 80;
    server_name vibe.deepverse.cloud;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name vibe.deepverse.cloud;

    ssl_certificate /etc/letsencrypt/live/vibe.deepverse.cloud/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/vibe.deepverse.cloud/privkey.pem;

    root /var/www/vibenetwork/landing_page;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /admin {
        proxy_pass http://localhost:9998;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
    
    location /api {
        proxy_pass http://localhost:9999;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
"""
        with open("nginx_vibe.conf", "w") as f:
            f.write(nginx_conf)
            
        sftp = ssh.open_sftp()
        sftp.put("nginx_vibe.conf", "/etc/nginx/sites-available/vibe.deepverse.cloud")
        sftp.close()
        
        ssh.exec_command('ln -sf /etc/nginx/sites-available/vibe.deepverse.cloud /etc/nginx/sites-enabled/')
        ssh.exec_command('nginx -t && systemctl reload nginx')
        
        # 7. Start Admin Server on Port 9998
        print("Starting Servers...")
        
        # Start API Server (index.js) on 9999
        ssh.exec_command(f'cd {remote_root}/server && PORT=9999 pm2 start index.js --name "Vibe Network API"')
        
        # Start Admin Server (admin.js) on 9998
        ssh.exec_command(f'cd {remote_root}/server && PORT=9998 pm2 start admin.js --name "Vibe Network Admin"')
        
        # Save PM2 list
        ssh.exec_command('pm2 save')

        print("✓ Deployment Complete!")
        print("URL: https://vibe.deepverse.cloud")
        print("Admin: https://vibe.deepverse.cloud/admin")
        
    except Exception as e:
        print(f"Deployment failed: {e}")
        import traceback
        traceback.print_exc()
    finally:
        ssh.close()

if __name__ == "__main__":
    deploy_vibenetwork()
