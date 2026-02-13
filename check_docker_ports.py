import paramiko
import sys

def check_docker_ports():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    print(f"Connecting to {hostname}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected successfully!")

        cmd = "docker ps --format 'table {{.Names}}\t{{.Ports}}' | grep -E '7777|7000|7001'"
        print(f"\nExecuting: {cmd}")
        stdin, stdout, stderr = client.exec_command(cmd)
        out = stdout.read().decode().strip()
        if out: print(out)
        else: print("No docker containers found using those ports.")

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_docker_ports()
