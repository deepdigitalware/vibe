const db = require('./db');
const fs = require('fs');
const path = require('path');

async function initDb() {
    try {
        const sql = fs.readFileSync(path.join(__dirname, 'schema.sql'), 'utf-8');
        await db.query(sql);
        console.log('Database initialized successfully');
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
