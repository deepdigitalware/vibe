import paramiko

def check_vibe_labels():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        print(f"Checking labels for {container}...")
        stdin, stdout, stderr = client.exec_command(f"docker inspect --format '{{{{json .Config.Labels}}}}' {container}")
        print(stdout.read().decode())
        
        container2 = "backend-mg0sw0c4w0g4wc8g4sws4ksc-085145759310"
        print(f"\nChecking labels for {container2}...")
        stdin, stdout, stderr = client.exec_command(f"docker inspect --format '{{{{json .Config.Labels}}}}' {container2}")
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_vibe_labels()
