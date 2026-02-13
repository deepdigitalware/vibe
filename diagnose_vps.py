import paramiko
import sys

def diagnose_restarts():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Get logs for restarting containers
        containers = [
            "livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913",
            "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        ]

        for container in containers:
            print(f"\n--- Logs for {container} (Last 20 lines) ---")
            cmd = f"docker logs --tail 20 {container}"
            stdin, stdout, stderr = client.exec_command(cmd)
            print(stdout.read().decode())
            print(stderr.read().decode())

        # 2. Check resource usage
        print("\n--- VPS Resource Usage ---")
        cmd = "free -m && df -h /"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 3. Check for zombie processes or port conflicts again
        print("\n--- Port Usage (7880, 9999) ---")
        cmd = "netstat -tulpn | grep -E '7880|9999'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    diagnose_restarts()
