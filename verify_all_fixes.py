import paramiko

def verify_all_fixes():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Check Vibe stack containers
        print("\n--- Vibe Stack Status ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'vibe|livekit-xo4k'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Check Jaimataji Backend status and logs
        print("\n--- Jaimataji Backend Status ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}' | grep backend-mg0sw0"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())
        
        print("\n--- Jaimataji Backend Logs (Last 10 lines) ---")
        cmd = "docker logs backend-mg0sw0c4w0g4wc8g4sws4ksc-085145759310 --tail 10"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    verify_all_fixes()
