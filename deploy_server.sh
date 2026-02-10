#!/bin/bash

# Deployment script for Vibe app server and admin panel
# This script prepares files for deployment to admin.deepverse.cloud/vibe

echo "Preparing Vibe app server deployment..."

# Create deployment directory
mkdir -p vibe_server_deploy

# Copy server files
cp -r ../server/* vibe_server_deploy/

# Create additional admin assets if they don't exist
if [ ! -f vibe_server_deploy/public/admin.html ]; then
    mkdir -p vibe_server_deploy/public
    cat > vibe_server_deploy/public/admin.html << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vibe Admin Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="container mt-5">
        <div class="row">
            <div class="col-12">
                <h1 class="text-center mb-4"><i class="fas fa-heart-circle text-danger"></i> Vibe Admin Dashboard</h1>
                
                <div class="row">
                    <div class="col-md-3 mb-3">
                        <div class="card bg-primary text-white">
                            <div class="card-body text-center">
                                <i class="fas fa-users fa-2x mb-2"></i>
                                <h5>Total Users</h5>
                                <h3 id="totalUsers">0</h3>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="card bg-success text-white">
                            <div class="card-body text-center">
                                <i class="fas fa-coins fa-2x mb-2"></i>
                                <h5>Total Balance</h5>
                                <h3 id="totalBalance">₹0</h3>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="card bg-info text-white">
                            <div class="card-body text-center">
                                <i class="fas fa-video fa-2x mb-2"></i>
                                <h5>Active Calls</h5>
                                <h3 id="activeCalls">0</h3>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <div class="card bg-warning text-white">
                            <div class="card-body text-center">
                                <i class="fas fa-chart-line fa-2x mb-2"></i>
                                <h5>Today's Revenue</h5>
                                <h3 id="todayRevenue">₹0</h3>
                            </div>
                        </div>
                    </div>
                </div>
                
                <div class="card mt-4">
                    <div class="card-header">
                        <h5>Manage Users</h5>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table table-striped">
                                <thead>
                                    <tr>
                                        <th>User ID</th>
                                        <th>Name</th>
                                        <th>Email</th>
                                        <th>Balance</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody id="usersList">
                                    <!-- Users will be loaded here -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Admin dashboard functionality
        async function loadDashboardData() {
            try {
                // Load user statistics
                const analyticsResponse = await fetch('/admin/analytics');
                const analytics = await analyticsResponse.json();
                
                document.getElementById('totalUsers').textContent = analytics.totalUsers || 0;
                document.getElementById('totalBalance').textContent = '₹' + (analytics.totalBalance || 0);
                
                // Load users list
                const usersResponse = await fetch('/admin/users');
                const users = await usersResponse.json();
                
                const usersList = document.getElementById('usersList');
                usersList.innerHTML = '';
                
                users.forEach(user => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${user.uid}</td>
                        <td>${user.name || 'N/A'}</td>
                        <td>${user.email || 'N/A'}</td>
                        <td>₹${user.balance || 0}</td>
                        <td><span class="badge bg-${user.active ? 'success' : 'warning'}">${user.active ? 'Active' : 'Inactive'}</span></td>
                        <td>
                            <button class="btn btn-sm btn-danger" onclick="deleteUser('${user.uid}')">Delete</button>
                        </td>
                    `;
                    usersList.appendChild(row);
                });
            } catch (error) {
                console.error('Error loading dashboard data:', error);
            }
        }
        
        async function deleteUser(userId) {
            if (confirm('Are you sure you want to delete this user?')) {
                try {
                    const response = await fetch(\`/admin/users/\${userId}\`, {
                        method: 'DELETE',
                        headers: {
                            'Authorization': 'Bearer ' + localStorage.getItem('adminToken')
                        }
                    });
                    
                    if (response.ok) {
                        loadDashboardData(); // Reload the user list
                    } else {
                        alert('Failed to delete user');
                    }
                } catch (error) {
                    console.error('Error deleting user:', error);
                }
            }
        }
        
        // Login check
        if (!localStorage.getItem('adminToken')) {
            // Redirect to login if not authenticated
            const loginForm = document.createElement('div');
            loginForm.className = 'modal fade show';
            loginForm.style.display = 'block';
            loginForm.innerHTML = \`
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Admin Login</h5>
                        </div>
                        <div class="modal-body">
                            <form id="loginForm">
                                <div class="mb-3">
                                    <label class="form-label">Username</label>
                                    <input type="text" class="form-control" id="username" required>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label">Password</label>
                                    <input type="password" class="form-control" id="password" required>
                                </div>
                                <button type="submit" class="btn btn-primary">Login</button>
                            </form>
                        </div>
                    </div>
                </div>
            \`;
            document.body.appendChild(loginForm);
            
            document.getElementById('loginForm').addEventListener('submit', async (e) => {
                e.preventDefault();
                const username = document.getElementById('username').value;
                const password = document.getElementById('password').value;
                
                try {
                    const response = await fetch('/login', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({ username, password })
                    });
                    
                    const data = await response.json();
                    if (data.token) {
                        localStorage.setItem('adminToken', data.token);
                        loginForm.remove();
                        loadDashboardData();
                    } else {
                        alert('Invalid credentials');
                    }
                } catch (error) {
                    console.error('Login error:', error);
                }
            });
        } else {
            loadDashboardData();
        }
    </script>
</body>
</html>
EOF
fi

# Create admin style sheet
cat > vibe_server_deploy/public/style.css << 'EOF'
body {
    background-color: #f8f9fa;
}

.card {
    box-shadow: 0 0.125rem 0.25rem rgba(0, 0, 0, 0.075);
    border: 1px solid rgba(0, 0, 0, 0.125);
    border-radius: 0.375rem;
}

.badge {
    border-radius: 0.25rem;
}

.table th {
    border-top: none;
    background-color: #f8f9fa;
    font-weight: 600;
}
EOF

# Create deployment script for VPS
cat > deploy_to_vps.sh << 'EOF'
#!/bin/bash

# VPS Deployment Script
echo "Deploying Vibe app to VPS..."

# VPS configuration
VPS_HOST="31.97.206.179"
VPS_USER="root"
VPS_PASSWORD="Deep@SM#01170628"

# Install required packages
echo "Installing required packages..."
sshpass -p "$VPS_PASSWORD" ssh -o StrictHostKeyChecking=no "$VPS_USER@$VPS_HOST" << 'INNEREOF'
    apt-get update
    apt-get install -y nginx nodejs npm certbot python3-certbot-nginx
    npm install -g pm2
INNEREOF

# Copy files to VPS
echo "Copying files to VPS..."
sshpass -p "$VPS_PASSWORD" scp -r vibe_server_deploy/* "$VPS_USER@$VPS_HOST:/var/www/admin.deepverse.cloud/vibe/"

# Setup Nginx configuration for admin panel
sshpass -p "$VPS_PASSWORD" ssh -o StrictHostKeyChecking=no "$VPS_USER@$VPS_HOST" << 'INNEREOF'
    # Create Nginx config for admin panel
    cat > /etc/nginx/sites-available/vibe_admin << 'NGINXCONF'
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
NGINXCONF

    # Enable the site
    ln -sf /etc/nginx/sites-available/vibe_admin /etc/nginx/sites-enabled/
    systemctl restart nginx
    
    # Setup PM2 to run the server
    cd /var/www/admin.deepverse.cloud/vibe/
    npm install
    pm2 start index.js --name "vibe-admin" --port 9999
    pm2 startup
    pm2 save
INNEREOF

# Setup landing page
sshpass -p "$VPS_PASSWORD" ssh -o StrictHostKeyChecking=no "$VPS_USER@$VPS_HOST" << 'INNEREOF'
    # Create directory for landing page
    mkdir -p /var/www/vibe.deepverse.cloud
    
    # Create Nginx config for landing page
    cat > /etc/nginx/sites-available/vibe_landing << 'NGINXCONF'
server {
    listen 9000;
    server_name vibe.deepverse.cloud;

    root /var/www/vibe.deepverse.cloud;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }

    location /app-debug.apk {
        alias /var/www/vibe.deepverse.cloud/app-debug.apk;
    }
}
NGINXCONF

    # Enable the site
    ln -sf /etc/nginx/sites-available/vibe_landing /etc/nginx/sites-enabled/
    systemctl reload nginx
INNEREOF

echo "Deployment completed!"
echo "Landing page: http://vibe.deepverse.cloud:9000"
echo "Admin panel: http://admin.deepverse.cloud:9999/vibe"
EOF

chmod +x deploy_to_vps.sh

echo "Server deployment prepared in vibe_server_deploy/"
echo "Run ./deploy_to_vps.sh to deploy to your VPS"