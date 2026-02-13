import paramiko

def fix_jaimataji_network():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected to VPS")

        # 1. Inspect DB container networks
        print("\n--- DB Container Network Inspect ---")
        cmd = "docker inspect v0so44ows8sc8kkg0wcckk88 --format='{{json .NetworkSettings.Networks}}'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 2. Check all networks
        print("\n--- All Docker Networks ---")
        cmd = "docker network ls"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 3. Connect DB container to Jaimataji network if it's not already
        # The network ID from backend was 456568144d8c... and name mg0sw0c4w0g4wc8g4sws4ksc
        print("\n--- Connecting DB to Jaimataji Network ---")
        cmd = "docker network connect mg0sw0c4w0g4wc8g4sws4ksc v0so44ows8sc8kkg0wcckk88 || echo 'Already connected or failed'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

        # 4. Restart Jaimataji Backend
        print("\n--- Restarting Jaimataji Backend ---")
        cmd = "docker restart backend-mg0sw0c4w0g4wc8g4sws4ksc-085145759310"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_jaimataji_network()
