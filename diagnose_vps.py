import paramiko
import sys
import getpass
import time

def diagnose_server():
    print("=== Vibe VPS Diagnostic Tool ===")
    print("This tool will connect to your VPS and check why the server might be returning 404.")
    
    # Gather Credentials
    hostname = input("Enter VPS IP (e.g., 123.45.67.89): ").strip()
    username = input("Enter SSH Username (default: root): ").strip() or "root"
    password = getpass.getpass("Enter SSH Password: ")

    try:
        # Connect
        print(f"\nConnecting to {username}@{hostname}...")
        client = paramiko.SSHClient()
        client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        client.connect(hostname, username=username, password=password)
        print("✅ Connected successfully!")

        # 1. Check Docker Containers
        print("\n--- Checking Running Containers ---")
        stdin, stdout, stderr = client.exec_command("docker ps --format 'table {{.ID}}\t{{.Names}}\t{{.Status}}\t{{.Ports}}'")
        output = stdout.read().decode()
        print(output)
        
        if "vibe" not in output and "coolify" not in output:
            print("⚠️ WARNING: No 'vibe' or 'coolify' containers found running.")

        # 2. Check Container Logs (Last 50 lines)
        print("\n--- Checking Backend Logs (Last 20 lines) ---")
        # Try to guess container name from previous output or common names
        container_name = "vibe-backend" # Default for docker-compose
        # If coolify generates random names, we might need to search
        stdin, stdout, stderr = client.exec_command("docker ps -q -f name=vibe")
        container_ids = stdout.read().decode().strip().split()
        
        if container_ids:
            target_id = container_ids[0]
            print(f"Found container ID: {target_id}. Fetching logs...")
            stdin, stdout, stderr = client.exec_command(f"docker logs --tail 20 {target_id}")
            print(stdout.read().decode())
            print(stderr.read().decode())
        else:
            print("❌ No Vibe container found to check logs.")

        # 3. Check Internal Connectivity
        print("\n--- Checking Internal Connectivity (curl localhost:9999) ---")
        stdin, stdout, stderr = client.exec_command("curl -v http://localhost:9999/health")
        curl_out = stdout.read().decode()
        curl_err = stderr.read().decode()
        print(curl_out)
        print(curl_err)
        
        if "200" in curl_out or "OK" in curl_out:
            print("✅ Internal Backend is responding (Health Check Passed).")
            print("👉 The issue is likely in Coolify's Reverse Proxy (Traefik) configuration.")
        else:
            print("❌ Internal Backend did NOT respond correctly.")

    except paramiko.AuthenticationException:
        print("❌ Authentication Failed. Please check your password.")
    except Exception as e:
        print(f"❌ Error: {e}")
    finally:
        client.close()
        print("\n=== Diagnostic Complete ===")

if __name__ == "__main__":
    try:
        # Check if paramiko is installed
        import paramiko
    except ImportError:
        print("Paramiko not found. Installing...")
        import subprocess
        subprocess.check_call([sys.executable, "-m", "pip", "install", "paramiko"])
        import paramiko
    
    diagnose_server()
