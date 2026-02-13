import paramiko

def fix_backend_and_livekit():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        backend_container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        internal_livekit = "livekit-xo4k0wo8kwocw4sg8k48os4o-084718712913"
        standalone_livekit = "livekit-sw0skgco40g4ww4kw4kcocgc"
        
        print(f"--- 1. Fixing Backend Restarts for {backend_container} ---")
        
        # 1a. Disable internal LiveKit (it causes port conflicts)
        print(f"Stopping internal LiveKit: {internal_livekit}")
        client.exec_command(f"docker stop {internal_livekit}")
        
        # 1b. Check if port 7880 is free, if not, find who is using it
        print("Checking port 7880 status...")
        stdin, stdout, stderr = client.exec_command("netstat -tulpn | grep 7880")
        port_status = stdout.read().decode()
        if port_status:
            print(f"Port 7880 is in use:\n{port_status}")
        
        # 2. Start Standalone LiveKit (User requested it)
        print(f"Starting standalone LiveKit: {standalone_livekit}")
        client.exec_command(f"docker start {standalone_livekit}")
        
        # 3. Restart Backend
        print(f"Restarting Backend: {backend_container}")
        client.exec_command(f"docker restart {backend_container}")
        
        # 4. Final Status Check
        print("\n--- Final Container Status ---")
        stdin, stdout, stderr = client.exec_command("docker ps --format '{{.Names}} - {{.Status}}'")
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_backend_and_livekit()
