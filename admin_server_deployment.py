#!/usr/bin/env python3
"""
Deployment script for Vibe app admin panel and landing page
This script deploys the admin panel to admin.deepverse.cloud/vibe 
and the landing page to vibe.deepverse.cloud
"""

import paramiko
import os
import sys
from pathlib import Path

def deploy_to_vps():
    """
    Deploy the Vibe app admin panel and landing page to VPS
    """
    # VPS Configuration
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    # Create SSH client
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        print("Connecting to VPS...")
        ssh.connect(hostname, username=username, password=password)
        print("Connected successfully!")
        
        # Create an SFTP client
        sftp = ssh.open_sftp()
        
        # Deploy landing page to vibe.deepverse.cloud (port 9000)
        print("\nDeploying landing page to vibe.deepverse.cloud...")
        
        # Create directory for landing page
        try:
            sftp.mkdir('/var/www/vibe.deepverse.cloud')
        except IOError:
            print("Directory already exists")
        
        # Upload landing page files
        local_landing_path = "c:/Users/deepd/D/Vibe/landing_page/index.html"
        if os.path.exists(local_landing_path):
            sftp.put(local_landing_path, '/var/www/vibe.deepverse.cloud/index.html')
            print("Landing page uploaded successfully")
        else:
            print(f"Landing page file not found: {local_landing_path}")
        
        # Deploy admin panel to admin.deepverse.cloud/vibe (port 9999)
        print("\nDeploying admin panel to admin.deepverse.cloud/vibe...")
        
        # Create directory for admin panel
        try:
            sftp.mkdir('/var/www/admin.deepverse.cloud')
        except IOError:
            print("Directory already exists")
        
        try:
            sftp.mkdir('/var/www/admin.deepverse.cloud/vibe')
        except IOError:
            print("Vibe admin directory already exists")
        
        # Upload server files (from the local server directory)
        local_server_dir = "c:/Users/deepd/D/Vibe/server"
        if os.path.exists(local_server_dir):
            for item in os.listdir(local_server_dir):
                local_path = os.path.join(local_server_dir, item)
                if os.path.isfile(local_path):
                    remote_path = f'/var/www/admin.deepverse.cloud/vibe/{item}'
                    sftp.put(local_path, remote_path)
                    print(f"Uploaded {item} to admin panel")
        else:
            print(f"Server directory not found: {local_server_dir}")
        
        # Create Node.js service for admin panel on port 9999
        admin_service_content = """[Unit]
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
"""
        
        # Write admin service file
        with open('vibe_admin.service', 'w') as f:
            f.write(admin_service_content)
        
        sftp.put('vibe_admin.service', '/tmp/vibe_admin.service')
        print("Admin service file uploaded")
        
        # Create Nginx configuration for landing page (port 9000)
        nginx_landing_config = """server {
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
"""
        
        # Write landing page nginx config
        with open('vibe_landing.conf', 'w') as f:
            f.write(nginx_landing_config)
        
        sftp.put('vibe_landing.conf', '/tmp/vibe_landing.conf')
        print("Landing page nginx config uploaded")
        
        # Create Nginx configuration for admin panel (port 9999)
        nginx_admin_config = """server {
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
"""
        
        # Write admin nginx config
        with open('vibe_admin.conf', 'w') as f:
            f.write(nginx_admin_config)
        
        sftp.put('vibe_admin.conf', '/tmp/vibe_admin.conf')
        print("Admin panel nginx config uploaded")
        
        # Execute deployment commands
        print("\nSetting up services...")
        
        # Copy configs to proper locations
        commands = [
            "sudo cp /tmp/vibe_landing.conf /etc/nginx/sites-available/vibe_landing.conf",
            "sudo ln -sf /etc/nginx/sites-available/vibe_landing.conf /etc/nginx/sites-enabled/",
            "sudo cp /tmp/vibe_admin.conf /etc/nginx/sites-available/vibe_admin.conf", 
            "sudo ln -sf /etc/nginx/sites-available/vibe_admin.conf /etc/nginx/sites-enabled/",
            "sudo cp /tmp/vibe_admin.service /etc/systemd/system/vibe_admin.service",
            "sudo chown -R www-data:www-data /var/www/vibe.deepverse.cloud",
            "sudo chown -R www-data:www-data /var/www/admin.deepverse.cloud",
            "sudo npm install -g pm2",
            "sudo systemctl daemon-reload",
            "sudo systemctl enable vibe_admin",
            "sudo systemctl start vibe_admin",
            "sudo systemctl restart nginx",
            "sudo ufw allow 9000",
            "sudo ufw allow 9999",
            "echo 'Deployment completed!'"
        ]
        
        for command in commands:
            print(f"Executing: {command}")
            stdin, stdout, stderr = ssh.exec_command(command)
            exit_status = stdout.channel.recv_exit_status()
            if exit_status == 0:
                print(f"✓ Success: {command}")
            else:
                print(f"✗ Error executing: {command}")
                print(stderr.read().decode())
        
        print("\n" + "="*50)
        print("DEPLOYMENT COMPLETED SUCCESSFULLY!")
        print("="*50)
        print("Landing Page: http://vibe.deepverse.cloud:9000")
        print("Admin Panel: http://admin.deepverse.cloud:9999/vibe")
        print("="*50)
        
        # Close connections
        sftp.close()
        ssh.close()
        
    except Exception as e:
        print(f"Error during deployment: {str(e)}")
        if 'ssh' in locals():
            ssh.close()

def create_local_admin_dashboard():
    """
    Create a local admin dashboard HTML file to be deployed
    """
    admin_html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vibe Admin Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        body {
            background-color: #f8f9fa;
        }
        .sidebar {
            height: 100vh;
            position: fixed;
            padding: 20px 0;
            background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        .main-content {
            margin-left: 250px;
            padding: 20px;
        }
        .stat-card {
            border-radius: 10px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .table th {
            background-color: #667eea;
            color: white;
        }
    </style>
</head>
<body>
    <!-- Sidebar -->
    <div class="sidebar">
        <div class="p-3">
            <h4 class="text-white"><i class="fas fa-heart-circle me-2"></i>Vibe Admin</h4>
            <hr class="bg-light">
            <ul class="nav flex-column">
                <li class="nav-item mb-2">
                    <a class="nav-link active" href="#"><i class="fas fa-home me-2"></i>Dashboard</a>
                </li>
                <li class="nav-item mb-2">
                    <a class="nav-link" href="#"><i class="fas fa-users me-2"></i>Users</a>
                </li>
                <li class="nav-item mb-2">
                    <a class="nav-link" href="#"><i class="fas fa-chart-bar me-2"></i>Analytics</a>
                </li>
                <li class="nav-item mb-2">
                    <a class="nav-link" href="#"><i class="fas fa-shopping-cart me-2"></i>Payments</a>
                </li>
                <li class="nav-item mb-2">
                    <a class="nav-link" href="#"><i class="fas fa-cog me-2"></i>Settings</a>
                </li>
                <li class="nav-item mb-2">
                    <a class="nav-link" href="#"><i class="fas fa-question-circle me-2"></i>Help</a>
                </li>
            </ul>
        </div>
    </div>

    <!-- Main Content -->
    <div class="main-content">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2>Admin Dashboard</h2>
            <div>
                <button class="btn btn-primary me-2"><i class="fas fa-sync-alt"></i> Refresh</button>
                <button class="btn btn-success"><i class="fas fa-download"></i> Export Data</button>
            </div>
        </div>

        <!-- Stats Cards -->
        <div class="row mb-4">
            <div class="col-md-3 mb-3">
                <div class="card stat-card bg-primary text-white">
                    <div class="card-body">
                        <div class="d-flex justify-content-between">
                            <div>
                                <h5>Total Users</h5>
                                <h3 id="totalUsers">0</h3>
                            </div>
                            <div class="align-self-center">
                                <i class="fas fa-users fa-2x"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-3">
                <div class="card stat-card bg-success text-white">
                    <div class="card-body">
                        <div class="d-flex justify-content-between">
                            <div>
                                <h5>Active Today</h5>
                                <h3 id="activeToday">0</h3>
                            </div>
                            <div class="align-self-center">
                                <i class="fas fa-user-check fa-2x"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-3">
                <div class="card stat-card bg-info text-white">
                    <div class="card-body">
                        <div class="d-flex justify-content-between">
                            <div>
                                <h5>Total Revenue</h5>
                                <h3 id="totalRevenue">₹0</h3>
                            </div>
                            <div class="align-self-center">
                                <i class="fas fa-rupee-sign fa-2x"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-3 mb-3">
                <div class="card stat-card bg-warning text-white">
                    <div class="card-body">
                        <div class="d-flex justify-content-between">
                            <div>
                                <h5>Pending</h5>
                                <h3 id="pendingCount">0</h3>
                            </div>
                            <div class="align-self-center">
                                <i class="fas fa-clock fa-2x"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Charts and Tables -->
        <div class="row">
            <div class="col-md-8 mb-4">
                <div class="card">
                    <div class="card-header">
                        <h5>Recent Activity</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="activityChart" height="250"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-md-4 mb-4">
                <div class="card">
                    <div class="card-header">
                        <h5>Quick Actions</h5>
                    </div>
                    <div class="card-body">
                        <div class="d-grid gap-2">
                            <button class="btn btn-primary"><i class="fas fa-plus me-2"></i> Add User</button>
                            <button class="btn btn-warning"><i class="fas fa-cog me-2"></i> Update Config</button>
                            <button class="btn btn-danger"><i class="fas fa-trash me-2"></i> Cleanup Data</button>
                            <button class="btn btn-info"><i class="fas fa-cloud-upload-alt me-2"></i> Backup</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="card mb-4">
            <div class="card-header d-flex justify-content-between align-items-center">
                <h5>Users Management</h5>
                <button class="btn btn-sm btn-outline-primary"><i class="fas fa-search"></i> Search</button>
            </div>
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-striped table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Name</th>
                                <th>Email</th>
                                <th>Balance</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody id="usersTableBody">
                            <!-- Data populated by JavaScript -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        // Sample data for demonstration
        const sampleUsers = [
            {id: 1, name: 'John Doe', email: 'john@example.com', balance: 1500, status: 'Active'},
            {id: 2, name: 'Jane Smith', email: 'jane@example.com', balance: 2300, status: 'Active'},
            {id: 3, name: 'Bob Johnson', email: 'bob@example.com', balance: 750, status: 'Pending'},
            {id: 4, name: 'Alice Brown', email: 'alice@example.com', balance: 3200, status: 'Active'},
            {id: 5, name: 'Charlie Wilson', email: 'charlie@example.com', balance: 1200, status: 'Blocked'}
        ];

        // Populate stats
        document.getElementById('totalUsers').textContent = sampleUsers.length;
        document.getElementById('activeToday').textContent = sampleUsers.filter(u => u.status === 'Active').length;
        
        let totalRevenue = 0;
        sampleUsers.forEach(user => {
            totalRevenue += user.balance;
        });
        document.getElementById('totalRevenue').textContent = '₹' + totalRevenue.toLocaleString();
        document.getElementById('pendingCount').textContent = sampleUsers.filter(u => u.status === 'Pending').length;

        // Populate users table
        const usersTableBody = document.getElementById('usersTableBody');
        sampleUsers.forEach(user => {
            const row = document.createElement('tr');
            let statusClass = '';
            switch(user.status.toLowerCase()) {
                case 'active': statusClass = 'success'; break;
                case 'pending': statusClass = 'warning'; break;
                case 'blocked': statusClass = 'danger'; break;
                default: statusClass = 'secondary';
            }
            
            row.innerHTML = `
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.email}</td>
                <td>₹${user.balance.toLocaleString()}</td>
                <td><span class="badge bg-${statusClass}">${user.status}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline-primary me-1"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-sm btn-outline-danger"><i class="fas fa-trash"></i></button>
                </td>
            `;
            usersTableBody.appendChild(row);
        });

        // Initialize chart
        const ctx = document.getElementById('activityChart').getContext('2d');
        const activityChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                datasets: [{
                    label: 'Active Users',
                    data: [120, 190, 150, 220, 180, 250],
                    borderColor: '#667eea',
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        display: false
                    }
                }
            }
        });
    </script>
</body>
</html>"""
    
    # Write the admin dashboard to the server public directory
    admin_dir = Path("c:/Users/deepd/D/Vibe/server/public")
    admin_dir.mkdir(parents=True, exist_ok=True)
    
    with open(admin_dir / "admin.html", "w", encoding="utf-8") as f:
        f.write(admin_html)
    
    print("Local admin dashboard created at server/public/admin.html")

if __name__ == "__main__":
    print("Preparing VPS deployment for Vibe app...")
    create_local_admin_dashboard()
    print("\nDeployment script created: admin_server_deployment.py")
    print("This script will:")
    print("1. Deploy the landing page to vibe.deepverse.cloud:9000")
    print("2. Deploy the admin panel to admin.deepverse.cloud:9999/vibe")
    print("3. Set up necessary services and configurations")
    print("\nTo run the deployment, execute this script with Python after connecting to your VPS.")