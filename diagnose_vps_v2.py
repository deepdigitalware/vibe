import paramiko
import sys

def diagnose_vps():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    print(f"Connecting to {hostname}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected successfully!")

        commands = [
            "echo '--- Docker Containers Status ---' && docker ps -a",
            "echo '--- UFW Status ---' && ufw status",
            "echo '--- Listening Ports ---' && netstat -tulpn",
            "echo '--- Check Traefik/Coolify specifically ---' && docker ps --filter 'name=coolify' --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'",
            "echo '--- Check if 80/443 are busy ---' && lsof -i :80 && lsof -i :443 || echo 'Ports 80/443 not in use by non-docker processes'",
            "echo '--- System Logs for Docker (last 20) ---' && journalctl -u docker --no-pager | tail -n 20"
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
    diagnose_vps()
