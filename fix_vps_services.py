import paramiko
import sys

def fix_vps_services():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    print(f"Connecting to {hostname}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected successfully!")

        # Step 1: Check Traefik and Coolify containers
        commands = [
            "docker ps -a --filter 'name=coolify' --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'",
            "docker ps -a --filter 'name=traefik' --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'",
            "ufw status",
            "netstat -tulpn | grep -E ':80|:443|:8000|:9999'",
            # Try to restart the proxy if it's down
            "docker start coolify-proxy || echo 'Failed to start coolify-proxy'",
            # Check for zombie processes on key ports
            "fuser -k 80/tcp || echo 'Port 80 clear'",
            "fuser -k 443/tcp || echo 'Port 443 clear'",
            # Restart all coolify containers
            "docker restart coolify coolify-db coolify-proxy || echo 'Failed to restart some coolify core containers'"
        ]

        for cmd in commands:
            print(f"\nExecuting: {cmd}")
            stdin, stdout, stderr = client.exec_command(cmd)
            out = stdout.read().decode().strip()
            err = stderr.read().decode().strip()
            if out: print(out)
            if err: print(f"Error: {err}")

    except Exception as e:
        print(f"Failed to connect or execute commands: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_vps_services()
