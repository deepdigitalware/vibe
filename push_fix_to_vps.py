import paramiko
import os

def push_fix_to_vps():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        
        # Read the local index.js
        local_path = r"c:\Users\deepd\D\Vibe\server\index.js"
        with open(local_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Write to a temporary file on VPS
        temp_remote_path = "/tmp/index.js"
        sftp = client.open_sftp()
        with sftp.file(temp_remote_path, 'w') as f:
            f.write(content)
        sftp.close()

        print(f"Copying fixed index.js into container {container}...")
        client.exec_command(f"docker cp {temp_remote_path} {container}:/app/index.js")
        
        print("Restarting container to apply changes...")
        client.exec_command(f"docker restart {container}")
        
        print("✓ Admin credentials hardcoded and container restarted.")

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    push_fix_to_vps()
