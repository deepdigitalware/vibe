import paramiko
import sys

def recreate_networks_and_start():
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
            "echo '--- Current Docker Networks ---'",
            "docker network ls",
            "echo '--- Stopping PM2 processes for Jaimataji ---'",
            "pm2 stop jaimataji-admin jaimataji-backend jaimataji-frontend || true",
            "pm2 delete jaimataji-admin jaimataji-backend jaimataji-frontend || true",
            "echo '--- Killing any remaining processes on ports 7777, 7000, 7001 ---'",
            "fuser -k 7777/tcp || true",
            "fuser -k 7000/tcp || true",
            "fuser -k 7001/tcp || true",
            "echo '--- Starting Coolify Jaimataji containers ---'",
            "docker network create mg0sw0c4w0g4wc8g4sws4ksc || echo 'Network already exists or failed'",
            "echo '--- Attempting to start Jaimataji containers again ---'",
            "docker start backend-mg0sw0c4w0g4wc8g4sws4ksc-083207856561 frontend-mg0sw0c4w0g4wc8g4sws4ksc-083207892143 admin-mg0sw0c4w0g4wc8g4sws4ksc-083207962164",
            "echo '--- Final Network Check ---'",
            "docker network ls",
            "echo '--- Final Container Check ---'",
            "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'vibe|jaimataji|backend|frontend|admin'"
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
    recreate_networks_and_start()
