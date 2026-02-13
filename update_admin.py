import paramiko
import sys

def update_admin():
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
        
        # New credentials
        new_email = "admin@deepverse.cloud"
        new_password = "Deep@VibeNetwork"
        
        # First, let's check if the admin table exists or where admins are stored
        # Typically it's 'admins' or 'users' with a role
        check_tables_cmd = f"docker exec {container} psql -U {db_user} -d {db_name} -c \"\\dt\""
        stdin, stdout, stderr = client.exec_command(check_tables_cmd)
        tables = stdout.read().decode()
        print("Existing tables:")
        print(tables)
        
        # Check if 'admins' table exists
        if "admins" in tables:
            target_table = "admins"
        else:
            target_table = "users"
            
        print(f"Targeting table: {target_table}")
        
        # Update or Insert admin user
        # We'll use a single query that updates if exists, or inserts if not
        # Note: We're assuming the password is stored as plain text for now as per user request, 
        # or we might need to hash it if the backend expects hashed passwords.
        # But usually in these quick setup requests, we just set the value.
        
        sql = f"""
        INSERT INTO users (uid, email, role, username, name) 
        VALUES ('admin_user', '{new_email}', 'admin', 'admin', 'Administrator') 
        ON CONFLICT (uid) 
        DO UPDATE SET email = '{new_email}', role = 'admin', username = 'admin';
        """
        
        # If the table doesn't have 'role', we might need to adjust
        # Let's check schema first
        schema_cmd = f"docker exec {container} psql -U {db_user} -d {db_name} -c \"\\d {target_table}\""
        stdin, stdout, stderr = client.exec_command(schema_cmd)
        print(f"Schema for {target_table}:")
        print(stdout.read().decode())
        
        update_cmd = f"docker exec {container} psql -U {db_user} -d {db_name} -c \"{sql}\""
        stdin, stdout, stderr = client.exec_command(update_cmd)
        print("Update Result:")
        print(stdout.read().decode())
        print(stderr.read().decode())
        
    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    update_admin()
