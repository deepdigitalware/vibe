import paramiko

def manage_livekit():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        # 1. Start standalone LiveKit
        # Looking for the container name from earlier context: livekit-sw0skgco40g4ww4kw4kcocgc
        standalone_livekit = "livekit-sw0skgco40g4ww4kw4kcocgc"
        print(f"Starting standalone LiveKit: {standalone_livekit}")
        client.exec_command(f"docker start {standalone_livekit}")
        
        # 2. Stop Vibe-internal LiveKit
        # Looking for the container name: vibe-livekit-xo4k0wo8kwocw4sg8k48os4o-084718704056
        internal_livekit = "vibe-livekit-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        print(f"Stopping internal LiveKit: {internal_livekit}")
        client.exec_command(f"docker stop {internal_livekit}")
        
        # 3. Check Vibe Backend connectivity to LiveKit
        # The backend needs to know the LiveKit URL/API Key/Secret
        backend_container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        print("Checking backend LiveKit environment...")
        stdin, stdout, stderr = client.exec_command(f"docker exec {backend_container} env | grep LIVEKIT")
        env_output = stdout.read().decode()
        print(f"Current LiveKit Env:\n{env_output}")
        
        print("✓ LiveKit management completed.")

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    manage_livekit()
