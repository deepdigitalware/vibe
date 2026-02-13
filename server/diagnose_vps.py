import paramiko
import time
import sys

def diagnose_vps(hostname, username, password=None, key_filename=None):
    print(f"Connecting to {username}@{hostname}...")
    
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    
    try:
        if key_filename:
            client.connect(hostname, username=username, key_filename=key_filename)
        else:
            client.connect(hostname, username=username, password=password)
            
        print("Connected successfully!")
        
        commands = [
            "uptime",
            "free -h",
            "docker ps -a",
            "netstat -tulpn | grep 9999"
        ]
        
        for cmd in commands:
            print(f"--- {cmd} ---")
            stdin, stdout, stderr = client.exec_command(cmd)
            print(stdout.read().decode().strip())
            
        # Check Traefik Labels
            print("--- Traefik Labels ---")
            stdin, stdout, stderr = client.exec_command("docker ps -q --filter name=vibe")
            container_id = stdout.read().decode().strip().split('\n')[0]
            if container_id:
                stdin, stdout, stderr = client.exec_command(f"docker inspect {container_id} --format '{{json .Config.Labels}}'")
                print(stdout.read().decode().strip())
            else:
                print("Vibe container not found")
                
    except Exception as e:
        print(f"Error: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    print("Vibe VPS Diagnostic Tool")
    print("------------------------")
    
    if len(sys.argv) > 1:
        hostname = sys.argv[1]
        username = sys.argv[2]
        password = sys.argv[3] if len(sys.argv) > 3 else None
        diagnose_vps(hostname, username, password)
    else:
        host = input("VPS Host/IP: ").strip()
        user = input("Username (root): ").strip() or "root"
        auth_type = input("Auth Type (password/key): ").strip().lower()
        
        pwd = None
        key = None
        
        if auth_type == "key":
            key = input("Path to private key: ").strip()
        else:
            pwd = input("Password: ").strip()
            
        diagnose_vps(host, user, password=pwd, key_filename=key)
