import paramiko

def list_all_containers():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        stdin, stdout, stderr = client.exec_command("docker ps --format '{{.Names}} - {{.Status}}'")
        print(stdout.read().decode())
    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    list_all_containers()
