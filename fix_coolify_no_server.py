import paramiko
import sys

def fix_coolify_no_server():
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
            "echo '--- Starting Coolify Sentinel ---'",
            "docker start coolify-sentinel",
            "echo '--- Restarting Coolify Core ---'",
            "docker restart coolify",
            "echo '--- Checking all Coolify containers ---'",
            "docker ps -a --filter 'name=coolify'",
            "echo '--- Checking Coolify Logs for Server Errors ---'",
            "docker logs --tail 50 coolify",
            "echo '--- Checking if Local Server IP is correct in settings (Internal check) ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT name, ip, status FROM servers;\" || echo 'Could not query servers table'"
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
    fix_coolify_no_server()
