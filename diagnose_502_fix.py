#!/usr/bin/env python3
"""
Comprehensive diagnostic and fix for 502 Bad Gateway error
"""

import paramiko
import time

def diagnose_and_fix_502():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(hostname, username=username, password=password, timeout=15)
        print("✓ Connected to VPS")
        
        print("\n" + "="*60)
        print("COMPREHENSIVE 502 BAD GATEWAY DIAGNOSIS")
        print("="*60)
        
        print("\n=== STEP 1: CHECKING NGINX ERROR LOGS ===")
        stdin, stdout, stderr = ssh.exec_command('tail -20 /var/log/nginx/error.log')
        nginx_errors = stdout.read().decode()
        print("Latest Nginx errors:")
        print(nginx_errors if nginx_errors else "No recent errors found")
        
        print("\n=== STEP 2: CHECKING PM2 SERVICE STATUS ===")
        stdin, stdout, stderr = ssh.exec_command('pm2 status')
        pm2_status = stdout.read().decode()
        print("PM2 Status:")
        print(pm2_status)
        
        print("\n=== STEP 3: CHECKING PM2 SERVICE LOGS ===")
        stdin, stdout, stderr = ssh.exec_command('pm2 logs "Vibe Network" --lines 5 2>/dev/null || pm2 logs vibe-api-final --lines 5 2>/dev/null || echo "No service logs found"')
        service_logs = stdout.read().decode()
        print("Service Logs:")
        print(service_logs)
        
        print("\n=== STEP 4: CHECKING NETWORK LISTENERS ===")
        stdin, stdout, stderr = ssh.exec_command('netstat -tulnp | grep -E ":80|:443|:9999"')
        listeners = stdout.read().decode()
        print("Active listeners:")
        print(listeners if listeners else "No listeners found")
        
        print("\n=== STEP 5: CHECKING NGINX CONFIGURATION ===")
        stdin, stdout, stderr = ssh.exec_command('nginx -t')
        nginx_test = stdout.read().decode()
        print("Nginx configuration test:")
        print(nginx_test)
        
        print("\n=== STEP 6: CHECKING NGINX STATUS ===")
        stdin, stdout, stderr = ssh.exec_command('systemctl status nginx --no-pager -l')
        nginx_status = stdout.read().decode()
        print("Nginx service status:")
        print(nginx_status[:500] + "..." if len(nginx_status) > 500 else nginx_status)
        
        print("\n=== STEP 7: TESTING LOCAL API CONNECTIVITY ===")
        test_commands = [
            'curl -s http://localhost:9999/api/health',
            'curl -s http://localhost:9999/',
            'ps aux | grep node | grep -v grep'
        ]
        
        for cmd in test_commands:
            print(f"\nTesting: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            error = stderr.read().decode()
            if result:
                print(f"Output: {result}")
            if error:
                print(f"Error: {error}")
        
        print("\n" + "="*60)
        print("ATTEMPTING AUTOMATIC FIX")
        print("="*60)
        
        print("\n=== STEP 8: RESTARTING ALL SERVICES ===")
        
        # Stop everything cleanly
        cleanup_commands = [
            'pm2 stop all 2>/dev/null',
            'pm2 delete all 2>/dev/null',
            'systemctl stop nginx 2>/dev/null',
            'pkill -f nginx 2>/dev/null',
            'sleep 2'
        ]
        
        for cmd in cleanup_commands:
            if cmd == 'sleep 2':
                time.sleep(2)
                continue
            print(f"Executing: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            stdout.channel.recv_exit_status()
        
        print("\n=== STEP 9: VERIFYING API SERVER FILE ===")
        stdin, stdout, stderr = ssh.exec_command('ls -la /root/vibe-api-final.js')
        file_status = stdout.read().decode()
        print(f"API server file status: {file_status}")
        
        print("\n=== STEP 10: STARTING API SERVICE ===")
        start_cmd = 'cd /root && PORT=9999 pm2 start vibe-api-final.js --name "Vibe Network" --max-memory-restart 200M'
        print(f"Starting with: {start_cmd}")
        stdin, stdout, stderr = ssh.exec_command(start_cmd)
        time.sleep(3)
        start_status = stdout.channel.recv_exit_status()
        
        if start_status == 0:
            print("✓ API service started successfully")
        else:
            print("✗ Failed to start API service")
            error_output = stderr.read().decode()
            print(f"Error details: {error_output}")
        
        print("\n=== STEP 11: RESTARTING NGINX ===")
        nginx_restart = [
            'nginx -t',
            'systemctl start nginx'
        ]
        
        for cmd in nginx_restart:
            print(f"Executing: {cmd}")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            print(result)
        
        print("\n=== STEP 12: FINAL VERIFICATION ===")
        
        # Final comprehensive check
        final_checks = [
            'echo "=== PM2 STATUS ==="; pm2 status',
            'echo "=== NETWORK LISTENERS ==="; netstat -tulnp | grep -E ":80|:443|:9999"',
            'echo "=== NGINX STATUS ==="; systemctl is-active nginx',
            'echo "=== LOCAL API TEST ==="; curl -s http://localhost:9999/api/health'
        ]
        
        for check_cmd in final_checks:
            print(f"\n{check_cmd.split(';')[0]}")
            stdin, stdout, stderr = ssh.exec_command(check_cmd.split(';')[1])
            result = stdout.read().decode()
            print(result)
        
        print("\n=== STEP 13: EXTERNAL ACCESS TEST ===")
        external_test = 'curl -s -k "https://vibe.deepverse.cloud/api/health" | head -3'
        print(f"Testing external access: {external_test}")
        stdin, stdout, stderr = ssh.exec_command(external_test)
        external_result = stdout.read().decode()
        print(f"External response: {external_result if external_result else 'No response'}")
        
        print("\n" + "="*80)
        print("DIAGNOSIS AND FIX ATTEMPT COMPLETE")
        print("="*80)
        print("Check the above output for error details and resolution status.")
        print("If issues persist, the logs will show exactly what's failing.")
        print("="*80)
        
        ssh.close()
        print("\n✓ Connection closed")
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    print("Running comprehensive 502 Bad Gateway diagnosis...")
    diagnose_and_fix_502()