
import paramiko

def final_vps_fix():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Stop and remove any internal livekit containers that might be conflicting
        print("Stopping internal livekit containers...")
        client.exec_command("docker stop livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913")
        client.exec_command("docker rm livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913")

        # 2. Ensure standalone livekit is running
        print("Ensuring standalone livekit is running...")
        client.exec_command("docker start livekit-sw0skgco40g4ww4kw4kcocgc")

        # 3. Check backend health again
        stdin, stdout, stderr = client.exec_command("docker ps --filter 'name=vibe-backend' --format '{{.Names}}\t{{.Status}}'")
        print(f"Backend Status: {stdout.read().decode().strip()}")

        # 4. Check why it might be restarting - look for crash loops
        stdin, stdout, stderr = client.exec_command("docker inspect vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056 --format '{{.RestartCount}}'")
        print(f"Restart Count: {stdout.read().decode().strip()}")

        print("Done. Please trigger a 'Redeploy' in Coolify for the Vibe project to apply the new docker-compose.yaml with the fixed health check.")

    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    final_vps_fix()
