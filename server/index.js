const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const multer = require('multer');
const jwt = require('jsonwebtoken');
const admin = require('firebase-admin');
const { Server } = require('socket.io');
const http = require('http');
require('dotenv').config();

const db = require('./db');
const initDb = require('./init_db');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

const PORT = process.env.PORT || 9999;
const JWT_SECRET = process.env.JWT_SECRET || 'vibe-secret';
const ADMIN_USER = process.env.ADMIN_USER || 'admin';
const ADMIN_PASS = process.env.ADMIN_PASS || 'password';

// Initialize Firebase Admin
try {
  const serviceAccount = require('./service-account.json');
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log("Firebase Admin initialized successfully");
} catch (error) {
  console.error("Firebase Admin initialization failed:", error.message);
}

// Storage
const PUBLIC_DIR = path.join(__dirname, 'public');
const UPLOADS_DIR = path.join(__dirname, 'uploads');

if (!fs.existsSync(UPLOADS_DIR)) fs.mkdirSync(UPLOADS_DIR, { recursive: true });

// Initialize DB
initDb();

// Middlewares
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));
app.use('/uploads', express.static(UPLOADS_DIR));
app.use('/', express.static(PUBLIC_DIR));

// --- Chat Socket.IO Logic ---
io.on('connection', (socket) => {
  console.log('User connected:', socket.id);

  socket.on('join_room', (roomId) => {
    socket.join(roomId);
    console.log(`User ${socket.id} joined room ${roomId}`);
  });

  socket.on('send_message', (data) => {
    io.to(data.roomId).emit('receive_message', data);
  });

  socket.on('disconnect', () => {
    console.log('User disconnected:', socket.id);
  });
});

// --- Multer Configuration ---
const storage = multer.diskStorage({
  destination: (_, __, cb) => cb(null, UPLOADS_DIR),
  filename: (_, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    const ext = path.extname(file.originalname);
    cb(null, file.fieldname + '-' + uniqueSuffix + ext);
  }
});
const upload = multer({ storage });

// --- Auth Middleware ---
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

// --- API Endpoints ---

// Login
app.post('/login', (req, res) => {
  const { username, password } = req.body;
  if (username === ADMIN_USER && password === ADMIN_PASS) {
    const token = jwt.sign({ user: username, role: 'admin' }, JWT_SECRET, { expiresIn: '24h' });
    return res.json({ token });
  }
  return res.status(401).json({ error: 'Invalid credentials' });
});

// App Login (Firebase Bridge)
app.post('/api/auth/login-app', async (req, res) => {
    const { uid, phone, email } = req.body;
    if (!uid) return res.status(400).json({ error: 'Missing UID' });

    try {
        const result = await db.query('SELECT * FROM users WHERE uid = $1', [uid]);
        let user = result.rows[0];

        if (!user) {
            // Create user
            const newUser = {
                uid,
                phone: phone || '',
                email: email || '',
                balance: 0,
                created_at: Date.now(),
                username: `user_${uid.substring(0, 6)}`,
                name: 'New User',
                bio: 'Hey there! I am using Vibe.',
                role: 'user'
            };
            
            await db.query(
                'INSERT INTO users (uid, phone, email, balance, created_at, username, name, bio, role) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)',
                [newUser.uid, newUser.phone, newUser.email, newUser.balance, newUser.created_at, newUser.username, newUser.name, newUser.bio, newUser.role]
            );
            user = newUser;
            io.emit('admin_update', { type: 'user_created', user });
        } else {
             io.emit('admin_update', { type: 'user_online', userId: uid });
        }

        const token = jwt.sign({ uid, role: 'user' }, JWT_SECRET, { expiresIn: '365d' });
        res.json({ token, user });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Database error' });
    }
});

// Admin: Get All Users
app.get('/admin/users', requireAuth, async (req, res) => {
    try {
        const result = await db.query('SELECT * FROM users ORDER BY created_at DESC');
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin: Delete User
app.delete('/admin/users/:uid', requireAuth, async (req, res) => {
    const { uid } = req.params;
    try {
        await db.query('DELETE FROM users WHERE uid = $1', [uid]);
        io.emit('admin_update', { type: 'user_deleted', uid });
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin: Analytics
app.get('/admin/analytics', requireAuth, async (req, res) => {
    try {
        const usersResult = await db.query('SELECT COUNT(*) as total_users, SUM(balance) as total_balance FROM users');
        const { total_users, total_balance } = usersResult.rows[0];
        res.json({
            totalUsers: parseInt(total_users),
            totalBalance: parseFloat(total_balance || 0)
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Admin: System Health
app.get('/admin/system', requireAuth, (req, res) => {
    const uptime = process.uptime();
    const memory = process.memoryUsage();
    const cpu = process.cpuUsage();
    
    res.json({
        uptime,
        memory,
        cpu,
        platform: process.platform,
        nodeVersion: process.version
    });
});

// Admin: List Uploads (Content Moderation)
app.get('/admin/uploads', requireAuth, async (req, res) => {
    try {
        const result = await db.query('SELECT * FROM uploads ORDER BY created_at DESC LIMIT 50');
        // If DB uploads table is empty but files exist (migration case), fallback to fs scan or just rely on DB
        // For enterprise, we rely on DB.
        res.json(result.rows);
    } catch (err) {
         // Fallback to FS if DB fails or empty for now? No, strict enterprise.
         res.json([]);
    }
});

// Upload File
app.post('/api/upload', requireAuth, upload.single('file'), async (req, res) => {
    if (!req.file) {
        return res.status(400).json({ error: 'No file uploaded' });
    }
    const fileUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
    
    try {
        await db.query('INSERT INTO uploads (filename, url, created_at) VALUES ($1, $2, $3)', 
            [req.file.filename, fileUrl, Date.now()]);
    } catch(e) { console.error("Upload DB log failed", e); }

    res.json({ url: fileUrl, filename: req.file.filename });
});

// Get CMS Data
app.get('/api/cms', async (req, res) => {
    try {
        const result = await db.query('SELECT * FROM cms');
        const cms = {};
        result.rows.forEach(row => {
            cms[row.section] = row.data;
        });
        res.json(cms);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update CMS Data (Generic)
app.post('/api/cms', requireAuth, async (req, res) => {
    const data = req.body;
    try {
        for (const section in data) {
            await db.query(
                'INSERT INTO cms (section, data) VALUES ($1, $2) ON CONFLICT (section) DO UPDATE SET data = $2',
                [section, JSON.stringify(data[section])]
            );
        }
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Config Endpoints
app.get('/config', async (_, res) => {
    try {
        const result = await db.query("SELECT data FROM cms WHERE section = 'config'");
        if (result.rows.length > 0) {
            res.json(result.rows[0].data);
        } else {
            res.json({});
        }
    } catch (err) {
        res.json({});
    }
});

app.post('/config', requireAuth, async (req, res) => {
    const newConfig = req.body;
    try {
        await db.query(
            "INSERT INTO cms (section, data) VALUES ('config', $1) ON CONFLICT (section) DO UPDATE SET data = $1",
            [JSON.stringify(newConfig)]
        );
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Wallet Endpoints
app.get('/wallet/balance/:userId', async (req, res) => {
    const { userId } = req.params;
    try {
        const result = await db.query('SELECT balance FROM users WHERE uid = $1', [userId]);
        if (result.rows.length > 0) {
            res.json({ balance: parseFloat(result.rows[0].balance) });
        } else {
            res.json({ balance: 0 });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/wallet/add', async (req, res) => {
    const { userId, amount } = req.body;
    if (!userId || !amount) return res.status(400).json({ error: "Missing params" });
    
    try {
        const result = await db.query(
            'UPDATE users SET balance = balance + $1 WHERE uid = $2 RETURNING balance',
            [parseFloat(amount), userId]
        );
        
        if (result.rows.length === 0) {
            // Create user if not exists? Ideally user should exist.
            return res.status(404).json({ error: "User not found" });
        }

        const newBalance = parseFloat(result.rows[0].balance);
        
        // Log transaction
        await db.query(
            'INSERT INTO transactions (user_id, amount, type, description, date) VALUES ($1, $2, $3, $4, $5)',
            [userId, parseFloat(amount), 'credit', 'Admin added funds', Date.now()]
        );

        io.emit('admin_update', { type: 'balance_updated', userId, newBalance });
        res.json({ success: true, newBalance });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.post('/wallet/deduct', async (req, res) => {
    const { userId, amount } = req.body;
    if (!userId || !amount) return res.status(400).json({ error: "Missing params" });

    try {
        const userRes = await db.query('SELECT balance FROM users WHERE uid = $1', [userId]);
        if (userRes.rows.length === 0) return res.status(404).json({ error: "User not found" });
        
        const currentBalance = parseFloat(userRes.rows[0].balance);
        const cost = parseFloat(amount);

        if (currentBalance >= cost) {
            const result = await db.query(
                'UPDATE users SET balance = balance - $1 WHERE uid = $2 RETURNING balance',
                [cost, userId]
            );
            const newBalance = parseFloat(result.rows[0].balance);

            // Log transaction
            await db.query(
                'INSERT INTO transactions (user_id, amount, type, description, date) VALUES ($1, $2, $3, $4, $5)',
                [userId, cost, 'debit', 'Admin deducted funds', Date.now()]
            );

            io.emit('admin_update', { type: 'balance_updated', userId, newBalance });
            res.json({ success: true, newBalance });
        } else {
            res.json({ success: false, message: "Insufficient balance" });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Profile Endpoints
app.post('/profile/update', async (req, res) => {
    const { userId, name, bio, avatar, cover, gallery, username } = req.body;
    
    // Construct dynamic update query
    const fields = [];
    const values = [];
    let idx = 1;

    if (name) { fields.push(`name = $${idx++}`); values.push(name); }
    if (bio) { fields.push(`bio = $${idx++}`); values.push(bio); }
    if (avatar) { fields.push(`avatar = $${idx++}`); values.push(avatar); }
    if (cover) { fields.push(`cover = $${idx++}`); values.push(cover); }
    if (gallery) { fields.push(`gallery = $${idx++}`); values.push(JSON.stringify(gallery)); }
    if (username) { fields.push(`username = $${idx++}`); values.push(username); }

    if (fields.length === 0) return res.json({ success: true }); // Nothing to update

    values.push(userId);
    const query = `UPDATE users SET ${fields.join(', ')} WHERE uid = $${idx}`;

    try {
        await db.query(query, values);
        io.emit('admin_update', { type: 'user_updated', userId, name, bio });
        res.json({ success: true });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/profile/:userId', async (req, res) => {
    const { userId } = req.params;
    try {
        const result = await db.query('SELECT * FROM users WHERE uid = $1', [userId]);
        if (result.rows.length > 0) {
            res.json(result.rows[0]);
        } else {
            res.json({ balance: 0, name: 'User', bio: '' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.get('/discover/users', async (req, res) => {
    try {
        const result = await db.query('SELECT uid as id, name, bio, avatar, cover FROM users LIMIT 100');
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Check Username Availability
app.get('/api/check-username', async (req, res) => {
    const { username } = req.query;
    if (!username) return res.json({ available: false });
    
    if (username.toLowerCase() === 'admin') return res.json({ available: false });

    try {
        const result = await db.query('SELECT uid FROM users WHERE LOWER(username) = LOWER($1)', [username]);
        res.json({ available: result.rows.length === 0 });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Serve admin.html
app.get('/admin', (_, res) => {
  res.sendFile(path.join(PUBLIC_DIR, 'admin.html'));
});

// Serve index.html
app.get('/', (_, res) => {
    if (fs.existsSync(path.join(PUBLIC_DIR, 'index.html'))) {
        res.sendFile(path.join(PUBLIC_DIR, 'index.html'));
    } else {
        res.send('Welcome to Vibe Public Site (Enterprise Backend Running)');
    }
});

server.listen(PORT, () => {
  console.log(`Server running at http://localhost:${PORT}`);
});
