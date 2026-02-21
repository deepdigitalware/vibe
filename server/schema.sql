CREATE TABLE IF NOT EXISTS users (
    uid TEXT PRIMARY KEY,
    phone TEXT,
    email TEXT,
    balance DECIMAL(10, 2) DEFAULT 0,
    created_at BIGINT,
    username TEXT UNIQUE,
    name TEXT,
    bio TEXT,
    avatar TEXT,
    cover TEXT,
    gallery JSONB DEFAULT '[]',
    role TEXT DEFAULT 'user',
    is_banned BOOLEAN DEFAULT FALSE,
    earned_cash DECIMAL(10, 2) DEFAULT 0,
    device_id TEXT,
    welcome_bonus_claimed BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS cms (
    section TEXT PRIMARY KEY,
    data JSONB
);

CREATE TABLE IF NOT EXISTS transactions (
    id SERIAL PRIMARY KEY,
    user_id TEXT REFERENCES users(uid),
    amount DECIMAL(10, 2),
    type TEXT,
    description TEXT,
    date BIGINT,
    status TEXT DEFAULT 'PENDING',
    txn_id TEXT UNIQUE,
    approval_ref TEXT
);

CREATE TABLE IF NOT EXISTS uploads (
    id SERIAL PRIMARY KEY,
    filename TEXT,
    url TEXT,
    created_at BIGINT
);

CREATE TABLE IF NOT EXISTS favourites (
    user_id TEXT REFERENCES users(uid) ON DELETE CASCADE,
    favourite_id TEXT REFERENCES users(uid) ON DELETE CASCADE,
    created_at BIGINT,
    PRIMARY KEY (user_id, favourite_id)
);

-- Insert default CMS data if not exists (using ON CONFLICT DO NOTHING)
INSERT INTO cms (section, data) VALUES 
('banner', '{"title": "Welcome to Vibe Platform", "subtitle": "Experience the best video calling", "imageUrl": ""}'),
('about', '{"title": "About Us", "content": "We connect people through seamless video interactions."}'),
('blogs', '[]'),
('gallery', '[]'),
('config', '{"appName": "Vibe", "appVersion": "1.1", "splashLogoUrl": "", "loginBgUrl": "", "heroTitle": "Swipe is Dead. Vibe is Alive.", "heroSubtitle": "Experience the new era of connection", "billingRatePerBlockRupees": 10, "blockDurationMinutes": 2, "paymentsProvider": "razorpay", "termsUrl": "https://vibe.deepverse.cloud/terms", "privacyUrl": "https://vibe.deepverse.cloud/privacy"}')
ON CONFLICT (section) DO NOTHING;

-- App Configuration table for more robust settings
CREATE TABLE IF NOT EXISTS app_config (
    key TEXT PRIMARY KEY,
    value TEXT,
    updated_at BIGINT
);

INSERT INTO app_config (key, value, updated_at) VALUES 
('app_logo', '', EXTRACT(EPOCH FROM NOW())::BIGINT),
('login_video', '', EXTRACT(EPOCH FROM NOW())::BIGINT),
('app_version', '1.1', EXTRACT(EPOCH FROM NOW())::BIGINT),
('hero_text', 'Swipe is Dead. Vibe is Alive.', EXTRACT(EPOCH FROM NOW())::BIGINT),
('terms_url', 'https://vibe.deepverse.cloud/terms', EXTRACT(EPOCH FROM NOW())::BIGINT),
('privacy_url', 'https://vibe.deepverse.cloud/privacy', EXTRACT(EPOCH FROM NOW())::BIGINT)
ON CONFLICT (key) DO NOTHING;
