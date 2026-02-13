import paramiko
import sys

def check_port_7777():
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
            "echo '--- Checking what is using port 7777 ---'",
            "lsof -i :7777 || netstat -tulpn | grep 7777 || echo 'Nothing on 7777'",
            "echo '--- Killing anything on 7777 ---'",
            "fuser -k 7777/tcp || echo 'Nothing to kill'",
            "echo '--- Checking all project FQDNs in database ---'",
            "docker exec coolify-db psql -U coolify -d coolify -c \"SELECT name, fqdn, status FROM applications;\""
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
    check_port_7777()
