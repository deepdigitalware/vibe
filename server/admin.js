const express = require('express');
const path = require('path');
const fs = require('fs');
const cors = require('cors');
const jwt = require('jsonwebtoken');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8001;
const JWT_SECRET = process.env.JWT_SECRET || 'vibe-secret';
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASS = process.env.ADMIN_PASS || 'password';

console.log(`Starting Vibe admin panel on port ${PORT}`);

// Storage
const DATA_DIR = path.join(__dirname, 'data');
const PUBLIC_DIR = path.join(__dirname, 'public');
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const CONFIG_FILE = path.join(DATA_DIR, 'app-config.json');

// Middlewares
app.use(cors());
app.use(express.json({ limit: '1mb' }
));
app.use(express.urlencoded({ extended: true }));
app.use('/uploads', express.static(UPLOADS_DIR));
app.use('/', express.static(PUBLIC_DIR));

// Serve admin.html for the root route and /admin
app.get(['/', '/admin'], (_, res) => {
  res.sendFile(path.join(PUBLIC_DIR, 'admin.html'));
});

// Serve static files under /admin as well
app.use('/admin', express.static(PUBLIC_DIR));

// Auth
function requireAuth(req, res, next) {
  const auth = req.headers.authorization || '';
  const token = auth.startsWith('Bearer ') ? auth.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'Missing token' });
  try {
    jwt.verify(token, JWT_SECRET);
    next();
  } catch (e) {
    return res.status(401).json({ error: 'Invalid token' });
  }
}

// Admin login
app.post('/admin/login', (req, res) => {
  const { username, password } = req.body || {};
  console.log(`Login attempt: ${username}`);
  if (username === ADMIN_USER && password === ADMIN_PASS) {
    const token = jwt.sign({ u: username }, JWT_SECRET, { expiresIn: '12h' });
    return res.json({ token });
  }
  return res.status(401).json({ error: 'Invalid credentials' });
});

// Update config (admin) - proxy to backend server
app.post('/admin/config', requireAuth, async (req, res) => {
  try {
    const response = await fetch('http://localhost:9999/admin/config', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': req.headers.authorization
      },
      body: JSON.stringify(req.body)
    });
    const data = await response.json();
    res.status(response.status).json(data);
  } catch (e) {
    res.status(500).json({ error: 'Failed to update config' });
  }
});

// Proxy: Analytics
app.get('/admin/analytics', requireAuth, async (req, res) => {
  try {
    const response = await fetch('http://localhost:9999/admin/analytics', {
      headers: { 'Authorization': req.headers.authorization }
    });
    const data = await response.json();
    res.status(response.status).json(data);
  } catch (e) {
    res.status(500).json({ error: 'Failed to fetch analytics' });
  }
});

// Proxy: Users
app.get('/admin/users', requireAuth, async (req, res) => {
  try {
    const response = await fetch('http://localhost:9999/admin/users', {
      headers: { 'Authorization': req.headers.authorization }
    });
    const data = await response.json();
    res.status(response.status).json(data);
  } catch (e) {
    res.status(500).json({ error: 'Failed to fetch users' });
  }
});

// Proxy: Add Wallet Balance
app.post('/admin/wallet/add', requireAuth, async (req, res) => {
  try {
    const response = await fetch('http://localhost:9999/wallet/add', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(req.body)
    });
    const data = await response.json();
    res.status(response.status).json(data);
  } catch (e) {
    res.status(500).json({ error: 'Failed to add balance' });
  }
});

// Upload splash logo - proxy to backend server
app.post('/admin/upload/logo', requireAuth, async (req, res) => {
  try {
    const response = await fetch('http://localhost:9999/admin/upload/logo', {
      method: 'POST',
      headers: {
        'Authorization': req.headers.authorization
      },
      body: req.body
    });
    const data = await response.json();
    res.status(response.status).json(data);
  } catch (e) {
    res.status(500).json({ error: 'Upload failed' });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Vibe admin panel listening on http://0.0.0.0:${PORT}`);
});