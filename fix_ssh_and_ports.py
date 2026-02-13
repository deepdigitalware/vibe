import paramiko
import sys

def check_coolify_user_and_ports():
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
            "echo '--- Checking if user coolify exists ---'",
            "id coolify || echo 'User coolify does not exist'",
            "echo '--- Killing process on 7777 ---'",
            "fuser -k 7777/tcp || echo 'No process on 7777'",
            "echo '--- Checking Coolify internal settings for server 1 ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT name, ip, user, validation_logs FROM servers WHERE id=1;\"",
            "echo '--- Adding root SSH key to coolify user if it exists ---'",
            "if id coolify >/dev/null 2>&1; then mkdir -p /home/coolify/.ssh && cp /root/.ssh/authorized_keys /home/coolify/.ssh/ && chown -R coolify:coolify /home/coolify/.ssh && chmod 700 /home/coolify/.ssh && chmod 600 /home/coolify/.ssh/authorized_keys && echo 'SSH keys synced to coolify user'; else echo 'Skipping SSH key sync'; fi",
            "echo '--- Restarting Coolify one last time to pick up changes ---'",
            "docker restart coolify"
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
    check_coolify_user_and_ports()
