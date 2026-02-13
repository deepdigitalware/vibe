import paramiko
import sys

def check_vibe_project():
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
            "echo '--- Checking Projects in Coolify ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT name, description FROM projects;\"",
            "echo '--- Checking Applications in Coolify ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT name, status, fqdn FROM applications;\""
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
    check_vibe_project()
