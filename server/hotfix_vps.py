import paramiko
import sys
import os

def hotfix(hostname, username, password, local_file):
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        print(f"Connecting to {hostname}...")
        client.connect(hostname, username=username, password=password)
        
        # Find container ID
        # The container name usually contains 'vibe' and is created by coolify
        stdin, stdout, stderr = client.exec_command("docker ps -q --filter name=vibe")
        output = stdout.read().decode().strip()
        if not output:
             print("Error: Vibe container not found.")
             return
        
        container_id = output.split('\n')[0] # Get first match
        print(f"Found container: {container_id}")
        
        # Upload file to /tmp
        sftp = client.open_sftp()
        remote_path = "/tmp/index.js"
        print(f"Uploading {local_file} to {remote_path}...")
        sftp.put(local_file, remote_path)
        sftp.close()
        
        # Copy to container
        print("Copying to container...")
        stdin, stdout, stderr = client.exec_command(f"docker cp {remote_path} {container_id}:/app/index.js")
        exit_status = stdout.channel.recv_exit_status()
        if exit_status != 0:
            print(f"Error copying file: {stderr.read().decode()}")
            return

        # Restart container
        print("Restarting container...")
        stdin, stdout, stderr = client.exec_command(f"docker restart {container_id}")
        print(stdout.read().decode())
        
        print("Hotfix applied successfully! The backend should now serve APIs correctly.")
        
    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    if len(sys.argv) > 3:
        hotfix(sys.argv[1], sys.argv[2], sys.argv[3], "server/index.js")
    else:
        print("Usage: python hotfix_vps.py <host> <user> <password>")
