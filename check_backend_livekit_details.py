import paramiko

def check_backend_livekit_details():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        
        # Check all environment variables to find LiveKit related ones
        print(f"Checking environment variables for container {container}...")
        stdin, stdout, stderr = client.exec_command(f"docker exec {container} env")
        env_output = stdout.read().decode()
        
        livekit_vars = [line for line in env_output.splitlines() if "LIVEKIT" in line or "LK_" in line]
        print("Found LiveKit related variables:")
        for var in livekit_vars:
            print(var)
            
        # Also check if there's a config file in the backend that might hold these
        print("\nChecking for config files in backend...")
        client.exec_command(f"docker exec {container} ls -R /app/config")
        # (Just guessing a path, let's see if we find anything)

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_backend_livekit_details()
