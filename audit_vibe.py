import paramiko
import sys
import time

def audit_vibe():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS for Audit")

        # 1. Check all Vibe-related containers
        print("\n--- Container Status (Vibe Project) ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'vibe|livekit|postgres-xo4k'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Test Backend API Connectivity (Internal)
        print("\n--- Backend API Health Check (Internal) ---")
        cmd = "curl -s http://localhost:9999/health || echo 'Backend not responding on port 9999'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 3. Check Database Tables and Row Count
        print("\n--- Database Audit ---")
        container = "postgres-xo4k0wo8kwocw4sg8k48os4o-084718715869"
        db_user = "vibenetwork"
        db_name = "vibenetwork"
        sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'; SELECT 'users count:', count(*) FROM users;"
        cmd = f"docker exec {container} psql -U {db_user} -d {db_name} -c \"{sql}\""
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 4. Check Frontend/Landing Page (if applicable via Coolify)
        # Assuming frontend might be on a specific port or behind proxy
        print("\n--- Proxy/Frontend Check ---")
        cmd = "docker ps | grep coolify-proxy"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 5. Check Redis (Vibe usually needs it for Socket.io/Caching)
        print("\n--- Redis Check ---")
        cmd = "docker ps | grep redis"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Audit Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    audit_vibe()
