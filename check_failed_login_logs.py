import paramiko

def check_failed_login_logs():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        
        print(f"Checking logs for {container} specifically for login attempts...")
        # Check for POST requests to /login or /api/login
        cmd = f"docker logs --tail 100 {container} 2>&1 | grep -i login"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())
        
        print("\nChecking environment variables again...")
        cmd = f"docker exec {container} env | grep ADMIN"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_failed_login_logs()
