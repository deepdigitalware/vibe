import paramiko
import sys

def fix_livekit_conflict():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Stop the LiveKit container that is restarting
        container_name = "livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913"
        print(f"Stopping container {container_name}...")
        client.exec_command(f"docker stop {container_name}")

        # 2. Find and kill the process on port 7880 and 7881
        print("Killing processes on ports 7880 and 7881...")
        commands = [
            "fuser -k 7880/tcp",
            "fuser -k 7881/tcp",
            "fuser -k 7882/tcp"
        ]
        for cmd in commands:
            stdin, stdout, stderr = client.exec_command(cmd)
            print(f"Executed: {cmd}")

        # 3. Start the LiveKit container again
        print(f"Starting container {container_name}...")
        stdin, stdout, stderr = client.exec_command(f"docker start {container_name}")
        print(stdout.read().decode())
        print(stderr.read().decode())

        # 4. Verify status
        print("\n--- Current Port Usage ---")
        stdin, stdout, stderr = client.exec_command("netstat -tulpn | grep -E '7880|7881'")
        print(stdout.read().decode())

        print("\n--- Container Status ---")
        stdin, stdout, stderr = client.exec_command("docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep livekit")
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_livekit_conflict()
