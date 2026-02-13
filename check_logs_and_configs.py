import paramiko
import sys

def check_logs_and_configs():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Check which containers are actually running and their ports
        print("\n--- Running Containers and Ports ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Check logs of the vibe-backend (it's often the one failing health checks)
        print("\n--- Vibe Backend Logs (Last 20 lines) ---")
        cmd = "docker logs vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056 --tail 20"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 3. Check logs of the vibe-livekit
        print("\n--- Vibe LiveKit Logs (Last 20 lines) ---")
        cmd = "docker logs livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913 --tail 20"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 4. Check logs of the standalone LiveKit
        print("\n--- Standalone LiveKit Logs (Last 20 lines) ---")
        cmd = "docker logs livekit-eso0004wokcgksc00c80wccw --tail 20"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 5. Check if there are any health check definitions in the docker inspect
        print("\n--- Vibe Backend Health Check Status ---")
        cmd = "docker inspect --format='{{json .State.Health}}' vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_logs_and_configs()
