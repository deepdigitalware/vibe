#!/usr/bin/env python3
"""
Find the specific dentodent redirect configuration
"""

import paramiko

def find_dentodent_redirect():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        ssh.connect(hostname, username=username, password=password, timeout=10)
        print("Connected to VPS")
        
        # Search for dentodent redirects specifically
        print("=== Searching for dentodent redirects ===")
        commands = [
            "grep -r 'dentodentdentalclinic.com' /etc/nginx/",
            "grep -r 'admin.dentodentdentalclinic.com' /etc/nginx/",
            "grep -r 'return.*dentodent' /etc/nginx/",
            "grep -r 'rewrite.*dentodent' /etc/nginx/"
        ]
        
        for cmd in commands:
            print(f"\n--- Running: {cmd} ---")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            output = stdout.read().decode()
            if output.strip():
                print("Found:")
                print(output)
            else:
                print("Nothing found")
        
        # Check default server configuration
        print("\n=== Checking default server configuration ===")
        stdin, stdout, stderr = ssh.exec_command('cat /etc/nginx/sites-enabled/default')
        default_config = stdout.read().decode()
        print("Default server config:")
        print(default_config)
        
        # Test current redirects
        print("\n=== Testing current redirect behavior ===")
        test_commands = [
            'curl -s -L -D - http://vibe.deepverse.cloud -o /dev/null | grep -i "location\|^http"',
            'curl -s -L -D - http://admin.deepverse.cloud -o /dev/null | grep -i "location\|^http"'
        ]
        
        for cmd in test_commands:
            print(f"\n--- Testing: {cmd} ---")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            print(result)
        
        ssh.close()
        print("\nInvestigation complete")
        
    except Exception as e:
        print(f"Error: {str(e)}")
        if 'ssh' in locals():
            ssh.close()

if __name__ == "__main__":
    find_dentodent_redirect()