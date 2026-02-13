import paramiko

def check_backend_logs_full():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "vibe-backend-xo4k0wo8kwocw4sg8k48os4o-084718704056"
        
        # Check logs
        cmd = f"docker logs {container} --tail 50"
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_backend_logs_full()
