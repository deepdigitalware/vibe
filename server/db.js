const { Pool } = require('pg');
require('dotenv').config();

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : false
});

pool.on('error', (err) => {
  console.error('Unexpected error on idle client', err.message);
  // In production/docker, we might want to log this but not necessarily crash
  // if the pool can recover. However, pg docs recommend exiting.
  // We'll log it clearly.
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  pool
};
