import paramiko
import sys

def fix_proxy_and_vibe():
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
            "echo '--- Checking coolify-proxy logs ---'",
            "docker logs --tail 50 coolify-proxy",
            "echo '--- Killing anything on ports 80 and 443 ---'",
            "fuser -k 80/tcp",
            "fuser -k 443/tcp",
            "echo '--- Restarting coolify-proxy ---'",
            "docker restart coolify-proxy",
            "echo '--- Checking if vibe backend container can be started ---'",
            "docker start $(docker ps -a -q --filter 'name=vibe-backend') || echo 'No vibe container found to start'",
            "echo '--- Final container status ---'",
            "docker ps -a"
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
    fix_proxy_and_vibe()
