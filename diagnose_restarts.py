
import paramiko
import time

def diagnose_and_fix_vps():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. List all containers and their status
        print("\n--- Container Status ---")
        stdin, stdout, stderr = client.exec_command("docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'")
        print(stdout.read().decode())

        # 2. Get logs of the backend container
        # Find the backend container name (it might have a long suffix from Coolify)
        stdin, stdout, stderr = client.exec_command("docker ps -a --filter 'name=vibe-backend' --format '{{.Names}}' | head -n 1")
        backend_name = stdout.read().decode().strip()
        
        if backend_name:
            print(f"\n--- Logs for {backend_name} (last 50 lines) ---")
            stdin, stdout, stderr = client.exec_command(f"docker logs --tail 50 {backend_name}")
            print(stdout.read().decode())
            print(stderr.read().decode())
        else:
            print("Backend container not found")

        # 3. Check LiveKit container
        stdin, stdout, stderr = client.exec_command("docker ps -a --filter 'name=livekit' --format '{{.Names}}'")
        livekit_names = stdout.read().decode().splitlines()
        print(f"\n--- LiveKit Containers Found: {livekit_names} ---")
        
        for name in livekit_names:
            print(f"\n--- Logs for {name} (last 20 lines) ---")
            stdin, stdout, stderr = client.exec_command(f"docker logs --tail 20 {name}")
            print(stdout.read().decode())

        # 4. Try to fix the health check by updating the compose file if we can find it
        # Coolify usually stores it in /data/coolify/applications/<id>/docker-compose.yml
        # But we can also check the labels of the running container
        if backend_name:
            print("\n--- Backend Labels ---")
            stdin, stdout, stderr = client.exec_command(f"docker inspect {backend_name} --format '{{{{json .Config.Labels}}}}'")
            print(stdout.read().decode())

    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    diagnose_and_fix_vps()
