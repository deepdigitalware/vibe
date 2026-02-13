import paramiko

def check_users_and_roles():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        
        container = "postgres-xo4k0wo8kwocw4sg8k48os4o-084718715869"
        db_user = "vibenetwork"
        db_name = "vibenetwork"
        
        # Check users, roles, and emails
        sql = "SELECT uid, email, username, name, role FROM users;"
        cmd = f"docker exec {container} psql -U {db_user} -d {db_name} -c \"{sql}\""
        stdin, stdout, stderr = client.exec_command(cmd)
        print(stdout.read().decode())

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    check_users_and_roles()
