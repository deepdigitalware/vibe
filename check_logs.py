#!/usr/bin/env python3
"""
Check PM2 logs and diagnose port binding issue
"""

import paramiko

def check_logs_and_diagnose():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(hostname, username=username, password=password, timeout=15)
        print("✓ Connected to VPS")
        
        print("\n=== CHECKING PM2 LOGS FOR 'Vibe Network' ===")
        stdin, stdout, stderr = ssh.exec_command('pm2 logs "Vibe Network" --lines 20')
        logs = stdout.read().decode()
        print("PM2 Logs:")
        print(logs if logs else "No logs found")
        
        print("\n=== CHECKING RUNNING PROCESSES ===")
        stdin, stdout, stderr = ssh.exec_command('ps aux | grep node | grep -v grep')
        processes = stdout.read().decode()
        print("Node processes:")
        print(processes if processes else "No node processes found")
        
        print("\n=== CHECKING PORT BINDINGS ===")
        stdin, stdout, stderr = ssh.exec_command('lsof -i :9999')
        port_bindings = stdout.read().decode()
        print("Port 9999 bindings:")
        print(port_bindings if port_bindings else "No bindings on port 9999")
        
        print("\n=== TESTING API MANUALLY ===")
        # Try to start the service manually to see error output
        print("Attempting manual start...")
        stdin, stdout, stderr = ssh.exec_command('cd /root && node simple-api.js')
        time.sleep(2)
        manual_output = stdout.read().decode()
        manual_error = stderr.read().decode()
        
        print("Manual start output:")
        print(manual_output if manual_output else "No output")
        print("Manual start errors:")
        print(manual_error if manual_error else "No errors")
        
        print("\n=== CHECKING NODE VERSION ===")
        stdin, stdout, stderr = ssh.exec_command('node --version')
        node_version = stdout.read().decode()
        print(f"Node version: {node_version.strip()}")
        
        print("\n=== CHECKING EXPRESS INSTALLATION ===")
        stdin, stdout, stderr = ssh.exec_command('npm list express')
        express_status = stdout.read().decode()
        print("Express installation:")
        print(express_status)
        
        ssh.close()
        print("\n✓ Connection closed")
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")

if __name__ == "__main__":
    print("Checking logs and diagnosing port binding...")
    check_logs_and_diagnose()