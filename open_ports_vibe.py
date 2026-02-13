import paramiko
import sys

def open_ports_and_check_vibe():
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
            "echo '--- Opening Ports ---'",
            "ufw allow 9999/tcp",
            "ufw allow 8000/tcp",
            "ufw allow 9000/tcp",
            "ufw reload",
            "echo '--- Checking Vibe Backend Container ---'",
            "docker ps -a --filter 'name=vibe'",
            "echo '--- Checking Logs of Vibe Backend (if exists) ---'",
            "docker logs --tail 50 vibe || echo 'Vibe container not found or no logs'",
            "echo '--- Checking Coolify Proxy Logs ---'",
            "docker logs --tail 20 coolify-proxy",
            "echo '--- Final Port Check ---'",
            "netstat -tulpn | grep -E '80|443|9999|8000'"
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
    open_ports_and_check_vibe()
