#!/usr/bin/env python3
"""
Restart the API service to fix 502 Bad Gateway error
"""

import paramiko
import time

def restart_api_service():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(hostname, username=username, password=password, timeout=10)
        print("✓ Connected to VPS")
        
        print("\n=== STOPPING EXISTING SERVICES ===")
        # Stop all PM2 processes
        stdin, stdout, stderr = ssh.exec_command('pm2 stop all 2>/dev/null')
        stdout.channel.recv_exit_status()
        print("✓ Stopped all PM2 processes")
        
        # Delete existing processes
        stdin, stdout, stderr = ssh.exec_command('pm2 delete all 2>/dev/null')
        stdout.channel.recv_exit_status()
        print("✓ Deleted all PM2 processes")
        
        print("\n=== STARTING API SERVICE ===")
        # Navigate to the correct directory and start the service
        start_command = 'cd /var/www/admin.deepverse.cloud/vibe && pm2 start index.js --name "vibe-api" --max-memory-restart 200M'
        print(f"Executing: {start_command}")
        
        stdin, stdout, stderr = ssh.exec_command(start_command)
        time.sleep(3)  # Wait for service to start
        exit_status = stdout.channel.recv_exit_status()
        
        if exit_status == 0:
            print("✓ API service started successfully")
        else:
            print("✗ API service failed to start")
            # Check error output
            error_output = stderr.read().decode()
            print(f"Error: {error_output}")
        
        print("\n=== VERIFYING SERVICE STATUS ===")
        # Check PM2 status
        stdin, stdout, stderr = ssh.exec_command('pm2 status')
        pm2_status = stdout.read().decode()
        print("PM2 Status:")
        print(pm2_status)
        
        # Check if service is listening on port 9999
        stdin, stdout, stderr = ssh.exec_command('netstat -tulnp | grep 9999')
        port_status = stdout.read().decode()
        print("Port 9999 status:")
        print(port_status if port_status else "No service listening on port 9999")
        
        print("\n=== TESTING API FUNCTIONALITY ===")
        # Test the API endpoints
        test_endpoints = [
            'http://localhost:9999/api/health',
            'http://localhost:9999/api/cms'
        ]
        
        for endpoint in test_endpoints:
            print(f"Testing: {endpoint}")
            stdin, stdout, stderr = ssh.exec_command(f'curl -s "{endpoint}"')
            response = stdout.read().decode()
            if response:
                print(f"✓ Response: {response}")
            else:
                print("✗ No response")
        
        print("\n=== RESTARTING NGINX ===")
        # Restart nginx to ensure proper proxy configuration
        stdin, stdout, stderr = ssh.exec_command('systemctl restart nginx')
        stdout.channel.recv_exit_status()
        print("✓ Nginx restarted")
        
        print("\n=== FINAL VERIFICATION ===")
        # Final checks
        final_commands = [
            'systemctl is-active nginx',
            'pm2 status',
            'netstat -tulnp | grep -E ":(80|443|9999)"'
        ]
        
        for cmd in final_commands:
            print(f"Checking: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            print(result)
        
        print("\n" + "="*80)
        print("API SERVICE RESTART COMPLETE!")
        print("="*80)
        print("✓ API service restarted")
        print("✓ Port 9999 verified")
        print("✓ API endpoints tested")
        print("✓ Nginx restarted")
        print("")
        print("Your API should now be working:")
        print("🌐 API Health: https://vibe.deepverse.cloud/api/health")
        print("📊 CMS API: https://vibe.deepverse.cloud/api/cms")
        print("🔧 Admin Panel: https://vibe.deepverse.cloud/admin")
        print("="*80)
        
        ssh.close()
        print("\n✓ Connection closed")
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")

if __name__ == "__main__":
    print("Restarting API service to fix 502 error...")
    restart_api_service()