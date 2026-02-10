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
    role TEXT DEFAULT 'user'
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
    date BIGINT
);

CREATE TABLE IF NOT EXISTS uploads (
    id SERIAL PRIMARY KEY,
    filename TEXT,
    url TEXT,
    created_at BIGINT
);

-- Insert default CMS data if not exists (using ON CONFLICT DO NOTHING)
INSERT INTO cms (section, data) VALUES 
('banner', '{"title": "Welcome to Vibe Platform", "subtitle": "Experience the best video calling", "imageUrl": ""}'),
('about', '{"title": "About Us", "content": "We connect people through seamless video interactions."}'),
('blogs', '[]'),
('gallery', '[]'),
('config', '{"appName": "Vibe", "splashLogoUrl": "", "heroTitle": "Welcome to Vibe", "heroSubtitle": "1:1 video calls with billing", "billingRatePerBlockRupees": 10, "blockDurationMinutes": 2, "paymentsProvider": "mock"}')
ON CONFLICT (section) DO NOTHING;
