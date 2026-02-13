import paramiko

def check_jaimataji_and_health():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Check Jaimataji Backend logs
        print("\n--- Jaimataji Backend Logs ---")
        cmd = "docker logs backend-mg0sw0c4w0g4wc8g4sws4ksc-085145759310 --tail 20"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Check Jaimataji Admin and Frontend
        print("\n--- Jaimataji Admin/Frontend Check ---")
        cmd = "docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E 'admin|frontend'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_jaimataji_and_health()
