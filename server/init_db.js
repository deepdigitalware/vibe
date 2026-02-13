const db = require('./db');
const fs = require('fs');
const path = require('path');

async function initDb() {
    try {
        const sql = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf-8');
        await db.query(sql);
        console.log('Database initialized successfully');
        
        // Seed initial users if table is empty
        const userCount = await db.query('SELECT COUNT(*) FROM users');
        if (parseInt(userCount.rows[0].count) === 0) {
            console.log('Seeding initial users...');
            const usersData = JSON.parse(fs.readFileSync(path.join(__dirname, 'data', 'users.json'), 'utf-8'));
            for (const uid in usersData) {
                const user = usersData[uid];
                await db.query(
                    'INSERT INTO users (uid, name, username, bio, avatar, balance, created_at, role) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)',
                    [uid, user.name, user.username, user.bio, user.avatar, user.balance || 0, Date.now(), 'user']
                );
            }
            console.log('Users seeded successfully');
        }
    } catch (err) {
        console.error('Error initializing database:', err);
    } finally {
        // Don't close pool if used in app, but for standalone script we would.
        // db.pool.end();
    }
}

module.exports = initDb;

if (require.main === module) {
    initDb().then(() => db.pool.end());
}
