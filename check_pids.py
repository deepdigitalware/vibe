import paramiko
import sys

def check_pids():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        pids = [116324, 116393, 116480]
        for pid in pids:
            cmd = f"ps -fp {pid} || echo 'PID {pid} not found'"
            stdin, stdout, stderr = client.exec_command(cmd)
            print(f"Info for PID {pid}:")
            print(stdout.read().decode())
        
    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_pids()
