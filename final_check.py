import paramiko

def final_check_and_fix():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Check Vibe status
        print("\n--- Vibe Project Status ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'vibe|livekit-xo4k'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Check Jaimataji Backend logs (more specifically)
        print("\n--- Jaimataji Backend Logs (Full) ---")
        cmd = "docker logs backend-mg0sw0c4w0g4wc8g4sws4ksc-085145759310"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())
        print(stderr.read().decode())

        # 3. Check what's listening on port 7777 (likely jaimataji port)
        print("\n--- Port 7777 Check ---")
        cmd = "lsof -i :7777"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 4. Check Standalone LiveKit (should be stopped)
        print("\n--- Standalone LiveKit Status ---")
        cmd = "docker ps -a | grep livekit-eso"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    final_check_and_fix()
