const db = require('./db');

const users = [
  {
    uid: "vibe_girl_001",
    name: "Priya Sharma",
    username: "priya_vibe",
    bio: "Love traveling and music. Let's connect! 💃",
    avatar: "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=800&q=80",
    phone: "+919876543210",
    email: "priya@example.com"
  },
  {
    uid: "vibe_girl_002",
    name: "Ananya Das",
    username: "ananya_kol",
    bio: "Bengali girl | Art lover | Coffee addict ☕",
    avatar: "https://images.unsplash.com/photo-1531123897727-8f129e16fd3c?auto=format&fit=crop&w=800&q=80",
    phone: "+919876543211",
    email: "ananya@example.com"
  },
  {
    uid: "vibe_girl_003",
    name: "Sneha Mukherjee",
    username: "sneha_sweet",
    bio: "Dancing is my passion. Sweet and simple. ✨",
    avatar: "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=800&q=80",
    phone: "+919876543212",
    email: "sneha@example.com"
  },
  {
    uid: "vibe_girl_004",
    name: "Ishani Bose",
    username: "ishani_b",
    bio: "Foodie | Movie buff | Let's chat! 🍿",
    avatar: "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
    phone: "+919876543213",
    email: "ishani@example.com"
  },
  {
    uid: "vibe_girl_005",
    name: "Riya Sen",
    username: "riya_sen_vibe",
    bio: "Living my best life. Always happy! 😊",
    avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=800&q=80",
    phone: "+919876543214",
    email: "riya@example.com"
  }
];

async function seedUsers() {
  console.log('Starting user seeding...');
  for (const user of users) {
    try {
      await db.query(
        `INSERT INTO users (uid, name, username, bio, avatar, phone, email, balance, created_at, role) 
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10) 
         ON CONFLICT (uid) DO UPDATE SET 
         name = EXCLUDED.name, 
         username = EXCLUDED.username, 
         bio = EXCLUDED.bio, 
         avatar = EXCLUDED.avatar`,
        [user.uid, user.name, user.username, user.bio, user.avatar, user.phone, user.email, 0, Date.now(), 'user']
      );
      console.log(`Seeded user: ${user.name}`);
    } catch (err) {
      console.error(`Error seeding user ${user.name}:`, err.message);
    }
  }
  console.log('User seeding completed.');
  process.exit(0);
}

seedUsers();
