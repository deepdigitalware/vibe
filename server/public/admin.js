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

let currentUserGallery = [];

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
                <td>
                    <div style="display:flex;align-items:center;gap:12px;">
                        <img src="${user.avatar || '/uploads/default-avatar.png'}" class="user-avatar">
                        <div>
                            <div style="font-weight:600;">${user.name || 'New User'}</div>
                            <div style="font-size:11px;color:var(--text-secondary);">${user.uid}</div>
                        </div>
                    </div>
                </td>
                <td>@${user.username || 'not_set'}</td>
                <td>
                    <div style="font-size:13px;">Wallet: <span style="color:var(--success-color)">₹${(user.balance || 0).toFixed(2)}</span></div>
                    <div style="font-size:13px;">Earned: <span style="color:var(--primary-color)">₹${(user.earned_cash || 0).toFixed(2)}</span></div>
                </td>
                <td>
                    <span class="status-badge ${user.is_banned ? 'offline' : 'active'}">
                        ${user.is_banned ? 'BANNED' : 'ACTIVE'}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm" style="width:auto;padding:8px 15px;background:#333;" onclick="openUserModal(${JSON.stringify(user).replace(/"/g, '&quot;')})">
                        <i class="fas fa-edit"></i> Customize
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        
        // Update Dashboard Stats
        const dashUsers = document.getElementById('dashUsers');
        if (dashUsers) dashUsers.innerText = users.length;
        
        const dashBalance = document.getElementById('dashBalance');
        if (dashBalance) {
            const totalBalance = users.reduce((acc, u) => acc + parseFloat(u.balance || 0), 0);
            dashBalance.innerText = '₹' + totalBalance.toFixed(2);
        }

        const dashEarned = document.getElementById('dashEarned');
        if (dashEarned) {
            const totalEarned = users.reduce((acc, u) => acc + parseFloat(u.earned_cash || 0), 0);
            dashEarned.innerText = '₹' + totalEarned.toFixed(2);
        }

        const dashBanned = document.getElementById('dashBanned');
        if (dashBanned) {
            const bannedCount = users.filter(u => u.is_banned).length;
            dashBanned.innerText = bannedCount + ' Banned';
        }
        
    } catch (e) { console.error(e); }
}

function openUserModal(user) {
    document.getElementById('editUid').value = user.uid;
    document.getElementById('editName').value = user.name || '';
    document.getElementById('editUsername').value = user.username || '';
    document.getElementById('editBalance').value = user.balance || 0;
    document.getElementById('editEarned').value = user.earned_cash || 0;
    document.getElementById('editBio').value = user.bio || '';
    
    document.getElementById('avatarUrl').value = user.avatar || '';
    document.getElementById('avatarPreview').src = user.avatar || '/uploads/default-avatar.png';
    
    document.getElementById('coverUrl').value = user.cover || '';
    document.getElementById('coverPreview').src = user.cover || '/uploads/default-cover.png';
    
    currentUserGallery = user.gallery || [];
    renderUserGallery();

    const banBtn = document.getElementById('banBtn');
    banBtn.innerText = user.is_banned ? 'Unban User' : 'Ban User';
    banBtn.style.background = user.is_banned ? 'var(--success-color)' : 'var(--danger-color)';
    banBtn.onclick = () => toggleUserBan(user.uid, !user.is_banned);

    document.getElementById('userModal').classList.remove('hidden');
}

function renderUserGallery() {
    const grid = document.getElementById('editGalleryGrid');
    grid.innerHTML = '';
    currentUserGallery.forEach((url, idx) => {
        const div = document.createElement('div');
        div.style.position = 'relative';
        div.innerHTML = `
            <img src="${url}" style="width:100%;height:100px;object-fit:cover;border-radius:4px;border:1px solid #333;">
            <button type="button" onclick="deleteFromGallery(${idx})" style="position:absolute;top:2px;right:2px;background:rgba(255,0,0,0.7);border:none;width:20px;height:20px;border-radius:50%;color:white;cursor:pointer;font-size:10px;"><i class="fas fa-times"></i></button>
        `;
        grid.appendChild(div);
    });
}

async function uploadToUserGallery(input) {
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
                currentUserGallery.push(data.url);
                renderUserGallery();
            }
        } catch (e) { alert('Upload failed'); }
    }
}

function deleteFromGallery(idx) {
    currentUserGallery.splice(idx, 1);
    renderUserGallery();
}

function deleteUserPhoto(type) {
    if (type === 'avatar') {
        document.getElementById('avatarUrl').value = '';
        document.getElementById('avatarPreview').src = '/uploads/default-avatar.png';
    } else {
        document.getElementById('coverUrl').value = '';
        document.getElementById('coverPreview').src = '/uploads/default-cover.png';
    }
}

async function saveUserCustomization() {
    const uid = document.getElementById('editUid').value;
    const data = {
        userId: uid,
        name: document.getElementById('editName').value,
        username: document.getElementById('editUsername').value,
        balance: parseFloat(document.getElementById('editBalance').value),
        earnedCash: parseFloat(document.getElementById('editEarned').value),
        bio: document.getElementById('editBio').value,
        avatar: document.getElementById('avatarUrl').value,
        cover: document.getElementById('coverUrl').value,
        gallery: currentUserGallery
    };

    try {
        const res = await fetch(`${API_BASE}/admin/users/update`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                uid: data.userId,
                name: data.name,
                username: data.username,
                balance: data.balance,
                earnedCash: data.earnedCash,
                bio: data.bio
            })
        });
        
        // Also update profile specific fields (avatar, cover, gallery)
        await fetch(`${API_BASE}/profile/update`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(data)
        });

        alert('User profile updated successfully');
        closeModal('userModal');
        loadUsers();
    } catch (e) { alert('Failed to update user'); }
}

async function toggleUserBan(uid, status) {
    try {
        await fetch(`${API_BASE}/admin/users/update`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ uid, isBanned: status })
        });
        alert(`User ${status ? 'Banned' : 'Unbanned'}`);
        closeModal('userModal');
        loadUsers();
    } catch (e) { alert('Action failed'); }
}
