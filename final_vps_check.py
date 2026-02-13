import paramiko
import sys

def final_vps_check():
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
            "echo '--- All Containers (including stopped) ---'",
            "docker ps -a",
            "echo '--- Coolify Server Table ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT id, name, ip, user, is_reachable FROM servers;\" || docker exec coolify-db psql -U coolify -d coolify -c \"SELECT id, name, ip, user FROM servers;\"",
            "echo '--- Checking if any container is using port 7777 ---'",
            "netstat -tulpn | grep 7777 || echo 'No process on 7777'",
            "echo '--- Checking Coolify Logs for SSH connection errors ---'",
            "docker logs --tail 100 coolify | grep -i 'error' | tail -n 20"
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
    final_vps_check()
