#!/usr/bin/env python3
"""
Script to fix the admin service that's showing as errored
"""

import paramiko
import os

def fix_admin_service():
    # VPS Configuration
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    print("Fixing admin service on VPS...")
    
    # Create SSH client
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        # Connect to the VPS
        ssh.connect(hostname, username=username, password=password, timeout=10)
        print("✓ Successfully connected to VPS")
        
        print("\n--- Checking admin service logs ---")
        
        # Check PM2 logs for the error
        stdin, stdout, stderr = ssh.exec_command('pm2 logs vibe-admin --lines 20')
        logs_output = stdout.read().decode()
        print("Current logs:")
        print(logs_output)
        
        print("\n--- Stopping and removing the current service ---")
        
        # Stop and delete the errored process
        commands = [
            'pm2 stop vibe-admin || true',
            'pm2 delete vibe-admin || true',
        ]
        
        for cmd in commands:
            stdin, stdout, stderr = ssh.exec_command(cmd)
            stdout.channel.recv_exit_status()
            print(f"Executed: {cmd}")
        
        print("\n--- Checking if required files exist ---")
        
        # Check if the index.js file exists
        stdin, stdout, stderr = ssh.exec_command('ls -la /var/www/admin.deepverse.cloud/vibe/')
        files = stdout.read().decode()
        print("Files in vibe directory:")
        print(files)
        
        print("\n--- Installing missing dependencies ---")
        
        # Reinstall dependencies to make sure everything is there
        stdin, stdout, stderr = ssh.exec_command('cd /var/www/admin.deepverse.cloud/vibe && npm install')
        exit_status = stdout.channel.recv_exit_status()
        print("Dependencies installation completed")
        
        print("\n--- Starting the admin service again ---")
        
        # Start the service with explicit port
        start_commands = [
            'cd /var/www/admin.deepverse.cloud/vibe && PORT=9999 pm2 start index.js --name "vibe-admin" --max-memory-restart 200M',
            'pm2 save',
            'pm2 startup || true'  # This might fail if already setup, which is OK
        ]
        
        for cmd in start_commands:
            stdin, stdout, stderr = ssh.exec_command(cmd)
            exit_status = stdout.channel.recv_exit_status()
            print(f"Started service: {cmd}")
        
        print("\n--- Waiting for service to start ---")
        import time
        time.sleep(5)  # Wait a bit for the service to start
        
        print("\n--- Checking service status again ---")
        
        # Check the status again
        stdin, stdout, stderr = ssh.exec_command('pm2 status vibe-admin')
        status = stdout.read().decode()
        print("Updated status:")
        print(status)
        
        # Check logs again to see if there are any errors
        stdin, stdout, stderr = ssh.exec_command('pm2 logs vibe-admin --lines 10')
        new_logs = stdout.read().decode()
        print("Recent logs:")
        print(new_logs)
        
        print("\n--- Checking if port is listening ---")
        
        # Check if port 9999 is now listening
        stdin, stdout, stderr = ssh.exec_command('netstat -tulnp | grep :9999 || ss -tulnp | grep :9999')
        port_status = stdout.read().decode()
        print(f"Port 9999 status:\n{port_status}")
        
        print("\n" + "="*60)
        print("ADMIN SERVICE FIX ATTEMPTED!")
        print("="*60)
        print("Landing Page: http://vibe.deepverse.cloud:9000")
        print("Admin Panel: http://admin.deepverse.cloud:9999/vibe")
        print("Check if services are now running properly!")
        print("="*60)
        
        # Close connection
        ssh.close()
        print("\n✓ Connection to VPS closed")
        
    except Exception as e:
        print(f"\n✗ Error during fix: {str(e)}")
        if 'ssh' in locals():
            ssh.close()
        raise

if __name__ == "__main__":
    print("Starting admin service fix...")
    fix_admin_service()