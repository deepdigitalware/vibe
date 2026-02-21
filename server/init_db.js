const db = require('./db');
const fs = require('fs');
const path = require('path');

async function initDb(retries = 5) {
    while (retries > 0) {
        try {
            const sql = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf-8');
            await db.query(sql);
            console.log('Database initialized successfully');
            
            // Seed initial users if table is empty
            const userCount = await db.query('SELECT COUNT(*) FROM users');
            if (parseInt(userCount.rows[0].count) <= 1) { // Seed if only admin or empty
                console.log('Seeding initial users...');
                const usersData = JSON.parse(fs.readFileSync(path.join(__dirname, 'data', 'users.json'), 'utf-8'));
                for (const uid in usersData) {
                    const user = usersData[uid];
                    // Check if already exists to avoid duplicates
                    const check = await db.query('SELECT 1 FROM users WHERE uid = $1', [uid]);
                    if (check.rows.length === 0) {
                        await db.query(
                            'INSERT INTO users (uid, name, username, bio, avatar, balance, created_at, role) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
                            [uid, user.name, user.username, user.bio, user.avatar, user.balance || 0, Date.now(), 'user']
                        );
                    }
                }
                console.log('Users seeded successfully');
            }
            break; // Success, exit loop
        } catch (err) {
            console.error(`Error initializing database (Retries left: ${retries - 1}):`, err.message);
            retries -= 1;
            if (retries === 0) {
                console.error('Failed to initialize database after multiple attempts.');
                // Don't exit, let the app run and fail on requests if needed
            } else {
                await new Promise(res => setTimeout(res, 5000)); // Wait 5 seconds
            }
        }
    }
}

module.exports = initDb;

if (require.main === module) {
    initDb().then(() => db.pool.end());
}
