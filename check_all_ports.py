import paramiko

def check_all_ports():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        print("\n--- Listening Ports (Host) ---")
        cmd = "netstat -tulpn | grep LISTEN"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        print("\n--- Jaimataji Backend Inspect ---")
        cmd = "docker inspect backend-mg0sw0c4w0g4wc8g4sws4ksc-085145759310"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_all_ports()
