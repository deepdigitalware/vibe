import paramiko

def verify_backend_health():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        
        print(f"Checking health of {container}...")
        stdin, stdout, stderr = client.exec_command(f"docker inspect --format '{{{{.State.Health.Status}}}}' {container}")
        status = stdout.read().decode().strip()
        print(f"Health Status: {status}")
        
        print("\nChecking recent logs...")
        stdin, stdout, stderr = client.exec_command(f"docker logs --tail 20 {container}")
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    verify_backend_health()
