import paramiko

def fix_livekit_conflict():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Stop the standalone LiveKit that is blocking the vibe one
        print("\n--- Stopping Standalone LiveKit ---")
        cmd = "docker stop livekit-eso0004wokcgksc00c80wccw"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Restart the Vibe LiveKit
        print("\n--- Restarting Vibe LiveKit ---")
        cmd = "docker restart livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 3. Check status
        print("\n--- Final Status Check ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'vibe|livekit'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_livekit_conflict()
