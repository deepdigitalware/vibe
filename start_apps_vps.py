import paramiko
import sys

def check_proxy_routes_and_start_apps():
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
            "echo '--- Checking Traefik Configuration in coolify-proxy ---'",
            "docker exec coolify-proxy traefik healthcheck || echo 'Healthcheck failed'",
            "echo '--- Checking Traefik Logs for Route Errors ---'",
            "docker logs --tail 100 coolify-proxy | grep -i 'error'",
            "echo '--- Starting Jaimataji Containers ---'",
            "docker start backend-mg0sw0c4w0g4wc8g4sws4ksc-183053240599 frontend-mg0sw0c4w0g4wc8g4sws4ksc-183053260207 admin-mg0sw0c4w0g4wc8g4sws4ksc-183053282988 || echo 'Failed to start some jaimataji containers'",
            "echo '--- Starting Livekit ---'",
            "docker restart livekit-xo4k0wo8kwocw4sg8k48os4o-072933521638",
            "echo '--- Final Check of Running Containers ---'",
            "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
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
    check_proxy_routes_and_start_apps()
