import paramiko
import sys

def populate_data():
    hostname = "31.97.206.179"
    username = "root"
    password = "Deep@SM#01170628"

    print(f"Connecting to {hostname}...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

    try:
        client.connect(hostname, username=username, password=password, timeout=15)
        print("Connected successfully!")

        # Vibe Postgres details from docker ps
        # vibe-backend-xo4k0wo8kwocw4sg8k48os4o-072933513976
        # postgres-xo4k0wo8kwocw4sg8k48os4o-072933524214
        
        container_name = "postgres-xo4k0wo8kwocw4sg8k48os4o-084718715869"
        
        # SQL to insert some real-looking data
        # Table: users (uid, phone, email, balance, created_at, username, name, bio, avatar, cover, gallery, role)
        sql = """
        INSERT INTO users (uid, username, name, bio, avatar, role, balance, created_at, email) VALUES
        ('u1', 'priya22', 'Priya Singh', 'Lover of music and travel. Bengali girl living in Kolkata.', 'https://images.unsplash.com/photo-1589156229687-496a31ad1d1f?auto=format&fit=crop&q=80&w=1000', 'user', 100.00, 1718284800, 'priya@vibe.com'),
        ('u2', 'anjali24', 'Anjali Sharma', 'Yoga enthusiast and foodie from Mumbai.', 'https://images.unsplash.com/photo-1614283233556-f35b0c801ef1?auto=format&fit=crop&q=80&w=1000', 'user', 50.00, 1718284800, 'anjali@vibe.com'),
        ('u3', 'sneha21', 'Sneha Das', 'Art and culture lover. From beautiful Bengal.', 'https://images.unsplash.com/photo-1594744803329-e58b31de3957?auto=format&fit=crop&q=80&w=1000', 'user', 75.00, 1718284800, 'sneha@vibe.com'),
        ('u4', 'riya23', 'Riya Patel', 'Dance is my passion. Let''s connect!', 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=80&w=1000', 'user', 200.00, 1718284800, 'riya@vibe.com'),
        ('u5', 'ishani25', 'Ishani Mukherjee', 'Professional photographer and dreamer.', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=1000', 'user', 150.00, 1718284800, 'ishani@vibe.com')
        ON CONFLICT (uid) DO UPDATE SET 
            name = EXCLUDED.name,
            bio = EXCLUDED.bio,
            avatar = EXCLUDED.avatar,
            username = EXCLUDED.username,
            email = EXCLUDED.email;
        """
        
        # We need to know the database name. Usually it's 'vibe' or 'postgres'
        # Let's try to find the DB name first
        find_db_cmd = f"docker exec {container_name} psql -U postgres -l"
        print(f"\nListing databases: {find_db_cmd}")
        stdin, stdout, stderr = client.exec_command(find_db_cmd)
        print(stdout.read().decode())
        
        # Database details from environment variables
        db_user = "vibenetwork"
        db_name = "vibenetwork"
        
        # Execute the insert
        insert_cmd = f"docker exec {container_name} psql -U {db_user} -d {db_name} -c \"{sql}\""
        print(f"\nExecuting: {insert_cmd}")
        stdin, stdout, stderr = client.exec_command(insert_cmd)
        out = stdout.read().decode().strip()
        err = stderr.read().decode().strip()
        if out: print(f"Output: {out}")
        if err: print(f"Error: {err}")

    except Exception as e:
        print(f"Failed: {e}")
    finally:
        client.close()

if __name__ == "__main__":
    populate_data()
