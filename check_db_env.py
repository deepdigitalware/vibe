import paramiko
import sys

def check_db_role():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        container = "postgres-xo4k0wo8kwocw4sg8k48os4o-072933524214"
        
        # Try to find user/db from environment variables of the container
        cmd = f"docker inspect {container} --format '{{{{range .Config.Env}}}}{{{{.}}}}\n{{{{end}}}}'"
        stdin, stdout, stderr = client.exec_command(cmd)
        print("Environment Variables:")
        print(stdout.read().decode())
        
    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_db_role()
