const API_BASE = ''; 
let token = localStorage.getItem('admin_token');
let cmsData = {};
const socket = io();

// Real-time Listeners
socket.on('connect', () => {
    console.log('Connected to Admin Socket');
    if (token) {
        socket.emit('admin_join', token); // Optional: Authenticate socket if needed
    }
});

socket.on('admin_update', (data) => {
    console.log('Admin Update Received:', data);
    
    // Refresh Data based on event type
    if (data.type === 'user_updated' || data.type === 'user_created') {
        // If Users tab is active, reload
        if (document.getElementById('users').classList.contains('active') || 
            !document.querySelector('.tab-content:not(.hidden)').id) { // or if dashboard
             loadUsers();
        }
        // Also update dashboard stats if visible
        updateDashboardStats();
    }
    
    if (data.type === 'stats_update') {
        updateDashboardStats();
    }
});

function updateDashboardStats() {
    // Lightweight stats refresh
    // For now, full reload is fine as it's admin panel
    loadDashboard();
}

// UI Elements
const loginScreen = document.getElementById('loginScreen');
const adminPanel = document.getElementById('adminPanel');
const loginForm = document.getElementById('loginForm');

// Initialize
if (token) {
    showAdmin();
}

// Login Logic
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    try {
        const res = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        
        if (data.token) {
            token = data.token;
            localStorage.setItem('admin_token', token);
            showAdmin();
        } else {
            alert(data.error || 'Login failed');
        }
    } catch (err) {
        alert('Network error');
    }
});

function logout() {
    localStorage.removeItem('admin_token');
    location.reload();
}

function showAdmin() {
    loginScreen.style.display = 'none';
    adminPanel.classList.remove('hidden');
    loadDashboard();
}

// Navigation Logic
document.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
        // Active State
        document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
        item.classList.add('active');
        
        // Show Content
        document.querySelectorAll('.tab-content').forEach(c => c.classList.add('hidden'));
        const tabId = item.getAttribute('data-tab');
        document.getElementById(tabId).classList.remove('hidden');
        
        // Load specific data if needed
        if (tabId === 'users') loadUsers();
        if (tabId === 'wallet') loadWalletData();
        if (tabId === 'moderation') loadModeration();
        if (tabId === 'system') loadSystemHealth();
    });
});

// Dashboard Data Loading
async function loadDashboard() {
    await Promise.all([loadCMS(), loadUsers(), loadConfig(), loadSystemHealth()]);
}

// --- System Health ---
async function loadSystemHealth() {
    try {
        const res = await fetch(`${API_BASE}/admin/system`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await res.json();
        
        const container = document.getElementById('systemHealthData');
        if (container) {
            container.innerHTML = `
                <div class="card">
                    <h3>Server Status</h3>
                    <p><strong>Uptime:</strong> ${Math.floor(data.uptime / 60)} minutes</p>
                    <p><strong>Platform:</strong> ${data.platform}</p>
                    <p><strong>Node Version:</strong> ${data.nodeVersion}</p>
                    <p><strong>Memory Usage:</strong> ${(data.memory.heapUsed / 1024 / 1024).toFixed(2)} MB</p>
                </div>
            `;
        }
    } catch (e) { console.error("Error loading system health", e); }
}

// --- Content Moderation ---
async function loadModeration() {
    try {
        const res = await fetch(`${API_BASE}/admin/uploads`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const images = await res.json();
        
        const grid = document.getElementById('moderationGrid');
        if (grid) {
            grid.innerHTML = '';
            images.forEach(img => {
                const div = document.createElement('div');
                div.className = 'gallery-item';
                div.innerHTML = `
                    <img src="${img.url}" style="width:100%; height:150px; object-fit:cover">
                    <div style="padding: 10px;">
                        <small>${new Date(img.date).toLocaleDateString()}</small>
                        <br>
                        <button class="btn btn-sm btn-danger" onclick="deleteUpload('${img.filename}')">Delete</button>
                    </div>
                `;
                grid.appendChild(div);
            });
        }
    } catch (e) { console.error("Error loading moderation", e); }
}

async function deleteUpload(filename) {
    if (!confirm('Delete this file?')) return;
    // Implement delete endpoint if needed, or just hide for now
    alert('File deletion logic to be implemented in backend');
}

// --- Global Wallet ---
async function loadWalletData() {
    // Reuse loadUsers to show user balances in wallet tab for now
    await loadUsers();
    // Copy user table to wallet section if needed or just show stats
    const walletContainer = document.getElementById('walletData');
    if (walletContainer) {
        // Just showing a summary for now
        walletContainer.innerHTML = '<p>Manage user balances via the Users tab.</p>';
    }
}

// --- CMS Logic ---

async function loadCMS() {
    try {
        const res = await fetch(`${API_BASE}/api/cms`);
        cmsData = await res.json();
        
        // Populate Banner
        if (cmsData.banner) {
            document.getElementById('bannerTitle').value = cmsData.banner.title || '';
            document.getElementById('bannerSubtitle').value = cmsData.banner.subtitle || '';
            if (cmsData.banner.imageUrl) {
                document.getElementById('bannerImageUrl').value = cmsData.banner.imageUrl;
                const img = document.getElementById('bannerPreview');
                img.src = cmsData.banner.imageUrl;
                img.classList.add('active');
            }
        }

        // Populate About
        if (cmsData.about) {
            document.getElementById('aboutTitle').value = cmsData.about.title || '';
            document.getElementById('aboutContent').value = cmsData.about.content || '';
        }

        // Populate Blogs
        renderBlogs();

        // Populate Gallery
        renderGallery();
        
        // Stats
        document.getElementById('dashBlogs').innerText = (cmsData.blogs || []).length;
        
    } catch (e) { console.error("Error loading CMS:", e); }
}

async function saveCMS(section = null) {
    try {
        const res = await fetch(`${API_BASE}/api/cms`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(cmsData)
        });
        const data = await res.json();
        if (data.success) {
            alert('Saved successfully!');
        } else {
            alert('Save failed');
        }
    } catch (e) { alert('Error saving data'); }
}

// Banner Section
function saveBanner() {
    if (!cmsData.banner) cmsData.banner = {};
    cmsData.banner.title = document.getElementById('bannerTitle').value;
    cmsData.banner.subtitle = document.getElementById('bannerSubtitle').value;
    cmsData.banner.imageUrl = document.getElementById('bannerImageUrl').value;
    saveCMS();
}

// About Section
function saveAbout() {
    if (!cmsData.about) cmsData.about = {};
    cmsData.about.title = document.getElementById('aboutTitle').value;
    cmsData.about.content = document.getElementById('aboutContent').value;
    saveCMS();
}

// Blog Section
function renderBlogs() {
    const list = document.getElementById('blogsList');
    list.innerHTML = '';
    const blogs = cmsData.blogs || [];
    
    blogs.forEach((blog, index) => {
        const card = document.createElement('div');
        card.className = 'card';
        card.style.display = 'flex';
        card.style.gap = '20px';
        card.innerHTML = `
            <img src="${blog.imageUrl || 'https://via.placeholder.com/150'}" style="width: 150px; height: 100px; object-fit: cover; border-radius: 4px;">
            <div style="flex: 1;">
                <h3 style="margin: 0 0 10px 0;">${blog.title}</h3>
                <p style="color: #666; margin: 0; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">${blog.content}</p>
            </div>
            <div>
                <button class="btn btn-sm" onclick="editBlog(${index})">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteBlog(${index})">Delete</button>
            </div>
        `;
        list.appendChild(card);
    });
}

function openBlogModal() {
    document.getElementById('blogId').value = ''; // New blog
    document.getElementById('blogTitle').value = '';
    document.getElementById('blogContent').value = '';
    document.getElementById('blogImageUrl').value = '';
    document.getElementById('blogImagePreview').classList.remove('active');
    document.getElementById('blogModalTitle').innerText = 'New Blog';
    document.getElementById('blogModal').classList.add('active');
}

function editBlog(index) {
    const blog = cmsData.blogs[index];
    document.getElementById('blogId').value = index;
    document.getElementById('blogTitle').value = blog.title;
    document.getElementById('blogContent').value = blog.content;
    document.getElementById('blogImageUrl').value = blog.imageUrl;
    const img = document.getElementById('blogImagePreview');
    if (blog.imageUrl) {
        img.src = blog.imageUrl;
        img.classList.add('active');
    } else {
        img.classList.remove('active');
    }
    document.getElementById('blogModalTitle').innerText = 'Edit Blog';
    document.getElementById('blogModal').classList.add('active');
}

function saveBlog() {
    const id = document.getElementById('blogId').value;
    const blog = {
        title: document.getElementById('blogTitle').value,
        content: document.getElementById('blogContent').value,
        imageUrl: document.getElementById('blogImageUrl').value,
        date: new Date().toISOString()
    };
    
    if (!cmsData.blogs) cmsData.blogs = [];
    
    if (id === '') {
        cmsData.blogs.push(blog);
    } else {
        cmsData.blogs[parseInt(id)] = blog;
    }
    
    saveCMS();
    renderBlogs();
    closeModal('blogModal');
}

function deleteBlog(index) {
    if(!confirm('Delete this blog?')) return;
    cmsData.blogs.splice(index, 1);
    saveCMS();
    renderBlogs();
}

// Gallery Section
function renderGallery() {
    const grid = document.getElementById('galleryGrid');
    grid.innerHTML = '';
    const images = cmsData.gallery || [];
    
    images.forEach((imgUrl, index) => {
        const div = document.createElement('div');
        div.className = 'gallery-item';
        div.innerHTML = `
            <img src="${imgUrl}">
            <div class="gallery-actions">
                <button class="btn btn-sm btn-danger" onclick="deleteGalleryImage(${index})"><i class="fas fa-trash"></i></button>
            </div>
        `;
        grid.appendChild(div);
    });
}

async function uploadGalleryImage(input) {
    if (input.files && input.files[0]) {
        const formData = new FormData();
        formData.append('file', input.files[0]);
        
        try {
            const res = await fetch(`${API_BASE}/api/upload`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });
            const data = await res.json();
            if (data.url) {
                if (!cmsData.gallery) cmsData.gallery = [];
                cmsData.gallery.push(data.url);
                saveCMS();
                renderGallery();
            }
        } catch (e) { alert('Upload failed'); }
    }
}

function deleteGalleryImage(index) {
    if(!confirm('Delete this image?')) return;
    cmsData.gallery.splice(index, 1);
    saveCMS();
    renderGallery();
}

// Settings Section
async function loadConfig() {
    try {
        const res = await fetch(`${API_BASE}/config`);
        const config = await res.json();
        document.getElementById('appName').value = config.appName || '';
        if (config.splashLogoUrl) {
            document.getElementById('splashLogoUrl').value = config.splashLogoUrl;
            const img = document.getElementById('logoPreview');
            img.src = config.splashLogoUrl;
            img.classList.add('active');
        }
    } catch(e) {}
}

async function saveSettings() {
    const config = {
        appName: document.getElementById('appName').value,
        splashLogoUrl: document.getElementById('splashLogoUrl').value
    };
    
    try {
        await fetch(`${API_BASE}/config`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json', 
                'Authorization': `Bearer ${token}` 
            },
            body: JSON.stringify(config)
        });
        alert('Settings saved!');
    } catch (e) { alert('Error saving settings'); }
}

// --- Shared Utils ---

async function uploadFile(input, previewId, urlInputId) {
    if (input.files && input.files[0]) {
        const formData = new FormData();
        formData.append('file', input.files[0]);
        
        try {
            const res = await fetch(`${API_BASE}/api/upload`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` },
                body: formData
            });
            const data = await res.json();
            if (data.url) {
                document.getElementById(urlInputId).value = data.url;
                const img = document.getElementById(previewId);
                img.src = data.url;
                img.classList.add('active');
            }
        } catch (e) { alert('Upload failed'); }
    }
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

// --- User Management ---

async function loadUsers() {
    try {
        const res = await fetch(`${API_BASE}/admin/users`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const users = await res.json();
        
        const tbody = document.querySelector('#usersTable tbody');
        tbody.innerHTML = '';
        
        users.forEach(user => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${user.uid.substring(0, 8)}...</td>
                <td>${user.name || 'Unknown'}</td>
                <td>₹${(user.balance || 0).toFixed(2)}</td>
                <td>
                    <button class="btn btn-sm btn-credit" style="background:#28a745" onclick="openWalletModal('${user.uid}', ${user.balance || 0})">Wallet</button>
                    <button class="btn btn-sm btn-delete" style="background:#dc3545" onclick="deleteUser('${user.uid}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        
        // Update Dashboard Stats
        document.getElementById('dashUsers').innerText = users.length;
        const totalBalance = users.reduce((acc, u) => acc + (u.balance || 0), 0);
        document.getElementById('dashBalance').innerText = '₹' + totalBalance.toFixed(2);
        
    } catch (e) { console.error(e); }
}

function openWalletModal(uid, currentBalance) {
    document.getElementById('walletUid').value = uid;
    document.getElementById('currentBalance').innerText = '₹' + currentBalance.toFixed(2);
    document.getElementById('walletAmount').value = '';
    document.getElementById('walletModal').classList.add('active');
}

async function updateWallet(type) {
    const uid = document.getElementById('walletUid').value;
    const amount = parseFloat(document.getElementById('walletAmount').value);
    
    if (!amount || amount <= 0) return alert('Enter valid amount');
    
    const endpoint = type === 'add' ? '/wallet/add' : '/wallet/deduct';
    
    try {
        const res = await fetch(`${API_BASE}${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: uid, amount })
        });
        const data = await res.json();
        if (data.success) {
            alert('Wallet updated!');
            closeModal('walletModal');
            loadUsers();
        } else {
            alert(data.error || data.message || 'Failed');
        }
    } catch (e) { alert('Error updating wallet'); }
}

async function deleteUser(uid) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
        const res = await fetch(`${API_BASE}/admin/users/${uid}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const data = await res.json();
        if (data.success) {
            loadUsers();
        } else {
            alert(data.error || 'Failed');
        }
    } catch (e) { alert('Error deleting user'); }
}
