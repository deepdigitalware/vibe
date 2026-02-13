import paramiko
import sys

def finalize_coolify_fix():
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
            "echo '--- Updating Server User to root in Coolify DB ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"UPDATE servers SET user = 'root' WHERE id = 1;\"",
            "echo '--- Verifying the update ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT id, name, ip, user FROM servers;\"",
            "echo '--- Restarting Coolify and Sentinel ---'",
            "docker restart coolify coolify-sentinel",
            "echo '--- Checking Traefik/Proxy one last time ---'",
            "docker ps --filter 'name=coolify-proxy'"
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
    finalize_coolify_fix()
