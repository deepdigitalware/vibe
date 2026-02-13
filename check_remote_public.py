import paramiko

def check_remote_public_dir():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        print(f"Listing /app/public inside {container}...")
        stdin, stdout, stderr = client.exec_command(f"docker exec {container} ls -R /app/public")
        print(stdout.read().decode())
        
        print(f"\nChecking /app/index.js content (first 50 lines)...")
        stdin, stdout, stderr = client.exec_command(f"docker exec {container} head -n 50 /app/index.js")
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_remote_public_dir()
