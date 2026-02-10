#!/usr/bin/env python3
"""
Script to verify the VPS deployment is working correctly
"""

import paramiko
import requests
import time

def verify_deployment():
    # VPS Configuration
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    print("Verifying VPS deployment...")
    
    # Create SSH client
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        # Connect to the VPS
        ssh.connect(hostname, username=username, password=password, timeout=10)
        print("✓ Successfully connected to VPS")
        
        print("\n--- Checking service status ---")
        
        # Check if PM2 processes are running
        stdin, stdout, stderr = ssh.exec_command('pm2 list')
        pm2_output = stdout.read().decode()
        print("PM2 Status:")
        print(pm2_output)
        
        # Check if nginx is running
        stdin, stdout, stderr = ssh.exec_command('systemctl is-active nginx')
        nginx_status = stdout.read().decode().strip()
        print(f"Nginx Status: {nginx_status}")
        
        # Check if the required ports are listening
        stdin, stdout, stderr = ssh.exec_command('netstat -tulnp | grep -E ":9000|:9999"')
        port_status = stdout.read().decode()
        print(f"Port Status:\n{port_status}")
        
        print("\n--- Deployment Verification ---")
        
        # Test local connectivity to the services
        try:
            # Test admin panel
            stdin, stdout, stderr = ssh.exec_command('curl -s -o /dev/null -w "%{http_code}" http://localhost:9999/')
            admin_status = stdout.read().decode().strip()
            print(f"Admin panel local status: HTTP {admin_status}")
        except:
            print("Could not check admin panel locally")
        
        # Close connection
        ssh.close()
        print("\n✓ Connection to VPS closed")
        
        print("\n" + "="*60)
        print("DEPLOYMENT VERIFICATION COMPLETE!")
        print("="*60)
        print("Landing Page: http://vibe.deepverse.cloud:9000")
        print("Admin Panel: http://admin.deepverse.cloud:9999/vibe")
        print("Both services should now be running properly!")
        print("="*60)
        
    except Exception as e:
        print(f"\n✗ Error during verification: {str(e)}")
        if 'ssh' in locals():
            ssh.close()
        raise

if __name__ == "__main__":
    print("Starting deployment verification...")
    verify_deployment()