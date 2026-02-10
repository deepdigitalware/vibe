#!/usr/bin/env python3
"""
Comprehensive investigation of all redirect sources on the VPS
"""

import paramiko
import os

def investigate_all_redirects():
    # VPS Configuration
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"
    
    print("=== COMPREHENSIVE REDIRECT INVESTIGATION ===")
    
    # Create SSH client
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        # Connect to the VPS
        ssh.connect(hostname, username=username, password=password, timeout=10)
        print("✓ Successfully connected to VPS")
        
        print("\n=== 1. CHECKING ALL NGINX CONFIGURATIONS ===")
        
        # Check all nginx configuration files
        print("--- All Nginx config files ---")
        stdin, stdout, stderr = ssh.exec_command('find /etc/nginx -type f -name "*.conf" -o -name "*" | grep -v backup')
        config_files = stdout.read().decode()
        print(config_files)
        
        print("\n--- Checking each configuration file for redirects ---")
        for config_file in config_files.strip().split('\n'):
            if config_file:
                stdin, stdout, stderr = ssh.exec_command(f'grep -l "dentodent\|redirect\|return 301\|return 302" {config_file} 2>/dev/null || true')
                matches = stdout.read().decode().strip()
                if matches:
                    print(f"Found redirects in: {config_file}")
                    stdin, stdout, stderr = ssh.exec_command(f'grep -n "dentodent\|redirect\|return 301\|return 302" {config_file}')
                    content = stdout.read().decode()
                    print(content)
                    print("-" * 50)
        
        print("\n=== 2. CHECKING ENABLED SITES ===")
        
        # Check all enabled sites
        stdin, stdout, stderr = ssh.exec_command('ls -la /etc/nginx/sites-enabled/')
        enabled_sites = stdout.read().decode()
        print("Enabled sites:")
        print(enabled_sites)
        
        print("\n--- Content of each enabled site ---")
        for site in enabled_sites.strip().split('\n'):
            if '->' in site:
                site_name = site.split('->')[0].strip().split()[-1]
                print(f"\n{site_name}:")
                stdin, stdout, stderr = ssh.exec_command(f'cat /etc/nginx/sites-enabled/{site_name}')
                content = stdout.read().decode()
                print(content)
                print("=" * 50)
        
        print("\n=== 3. CHECKING DEFAULT NGINX CONFIG ===")
        
        # Check main nginx.conf
        stdin, stdout, stderr = ssh.exec_command('cat /etc/nginx/nginx.conf')
        nginx_conf = stdout.read().decode()
        print("Main nginx.conf:")
        print("=" * 30)
        print(nginx_conf)
        print("=" * 30)
        
        print("\n=== 4. CHECKING HTTP TO HTTPS REDIRECTS ===")
        
        # Check for HTTP to HTTPS redirects
        stdin, stdout, stderr = ssh.exec_command('grep -r "https://admin.dentodentdentalclinic.com" /etc/nginx/')
        https_redirects = stdout.read().decode()
        if https_redirects:
            print("Found HTTPS redirects to dentodent:")
            print(https_redirects)
        else:
            print("No HTTPS redirects to dentodent found")
        
        print("\n=== 5. CHECKING DNS AND HOST RESOLUTION ===")
        
        # Check how the domains resolve
        domains_to_check = ['vibe.deepverse.cloud', 'admin.deepverse.cloud']
        for domain in domains_to_check:
            print(f"\n--- Checking {domain} ---")
            stdin, stdout, stderr = ssh.exec_command(f'nslookup {domain} 2>/dev/null || dig {domain} 2>/dev/null || echo "DNS lookup failed"')
            dns_result = stdout.read().decode()
            print(dns_result)
        
        print("\n=== 6. CHECKING RUNNING PROCESSES ===")
        
        # Check what's actually running on port 80
        stdin, stdout, stderr = ssh.exec_command('netstat -tulnp | grep :80')
        port_80_processes = stdout.read().decode()
        print("Processes on port 80:")
        print(port_80_processes)
        
        print("\n=== 7. CHECKING FOR OTHER WEB SERVERS ===")
        
        # Check if other web servers might be running
        web_servers = ['apache2', 'httpd', 'lighttpd']
        for server in web_servers:
            stdin, stdout, stderr = ssh.exec_command(f'systemctl is-active {server} 2>/dev/null || echo "{server} not active"')
            status = stdout.read().decode().strip()
            if status != f"{server} not active":
                print(f"{server} status: {status}")
        
        print("\n=== 8. CHECKING SYSTEM-WIDE REDIRECTS ===")
        
        # Check system-wide redirect configurations
        stdin, stdout, stderr = ssh.exec_command('cat /etc/hosts 2>/dev/null')
        hosts_file = stdout.read().decode()
        print("/etc/hosts content:")
        print(hosts_file)
        
        # Check for iptables redirects
        stdin, stdout, stderr = ssh.exec_command('iptables -t nat -L 2>/dev/null | grep -i redirect')
        iptables_redirects = stdout.read().decode()
        if iptables_redirects:
            print("Iptables redirects:")
            print(iptables_redirects)
        else:
            print("No iptables redirects found")
        
        print("\n=== 9. TESTING DIRECT ACCESS ===")
        
        # Test direct access to our services
        test_commands = [
            'curl -I http://localhost:9999 2>/dev/null | head -5',
            'curl -I http://127.0.0.1 2>/dev/null | head -5',
            'curl -I http://vibe.deepverse.cloud 2>/dev/null | head -10',
            'curl -I http://admin.deepverse.cloud 2>/dev/null | head -10'
        ]
        
        for cmd in test_commands:
            print(f"\n--- Testing: {cmd} ---")
            stdin, stdout, stderr = ssh.exec_command(cmd)
            result = stdout.read().decode()
            print(result)
        
        print("\n" + "="*80)
        print("INVESTIGATION COMPLETE - CHECK RESULTS ABOVE FOR REDIRECT SOURCES")
        print("="*80)
        
        # Close connection
        ssh.close()
        print("\n✓ Connection to VPS closed")
        
    except Exception as e:
        print(f"\n✗ Error during investigation: {str(e)}")
        if 'ssh' in locals():
            ssh.close()
        raise

if __name__ == "__main__":
    print("Starting comprehensive redirect investigation...")
    investigate_all_redirects()