import paramiko

def fix_credentials_v4():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        # 1. Update ADMIN_USER and ADMIN_PASS in backend env (if possible)
        # In Coolify/Docker, we usually need to update the container's env or the server/index.js inside.
        # Let's update the server/index.js inside the container as well.
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        
        print("Updating credentials in server/index.js inside container...")
        
        # Use sed to update the fallback values in the container's index.js
        # Updating ADMIN_USER
        sed_user = "sed -i \"s/const ADMIN_USER = process.env.ADMIN_USER || '[^']*';/const ADMIN_USER = process.env.ADMIN_USER || 'admin@deepverse.cloud';/\" /app/index.js"
        # Updating ADMIN_PASS
        sed_pass = "sed -i \"s/const ADMIN_PASS = process.env.ADMIN_PASS || '[^']*';/const ADMIN_PASS = process.env.ADMIN_PASS || 'Deep@VibeNetwork';/\" /app/index.js"
        
        client.exec_command(f"docker exec {container} {sed_user}")
        client.exec_command(f"docker exec {container} {sed_pass}")
        
        print("Restarting container to apply changes...")
        client.exec_command(f"docker restart {container}")
        
        print("✓ Done. Admin credentials set to admin@deepverse.cloud / Deep@VibeNetwork")

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    fix_credentials_v4()
