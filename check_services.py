import paramiko
import sys

def check_services():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        cmds = [
            "pm2 list",
            "systemctl list-units --type=service | grep -E 'jaimataji|backend|frontend|admin'",
            "ps aux | grep -E '7777|7000|7001' | grep -v grep"
        ]
        
        for cmd in cmds:
            print(f"\nExecuting: {cmd}")
            stdin, stdout, stderr = client.exec_command(cmd)
            print(stdout.read().decode())
        
    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_services()
