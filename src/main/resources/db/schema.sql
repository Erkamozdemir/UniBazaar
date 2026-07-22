CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id INTEGER REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    role VARCHAR(10) NOT NULL DEFAULT 'STUDENT',
    gpa NUMERIC(3,2),
    is_gpa_verified BOOLEAN NOT NULL DEFAULT FALSE,
    rating NUMERIC(3,2) DEFAULT 0.00,
    review_count INTEGER DEFAULT 0,
    CONSTRAINT chk_email_edu CHECK (email LIKE '%.edu.tr'),
    CONSTRAINT chk_role CHECK (role IN ('STUDENT', 'SELLER'))
);

CREATE TABLE listings (
    id SERIAL PRIMARY KEY,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INTEGER REFERENCES categories(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    original_price NUMERIC(10,2) NOT NULL CHECK (original_price >= 0),
    is_campaign_active BOOLEAN NOT NULL DEFAULT FALSE,
    image_url TEXT,
    course_code VARCHAR(20),
    brand VARCHAR(100),
    condition VARCHAR(50) DEFAULT 'Used',
    location VARCHAR(100) DEFAULT 'Campus',
    status VARCHAR(20) NOT NULL DEFAULT 'active'
);

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    listing_id INTEGER NOT NULL REFERENCES listings(id),
    buyer_id UUID NOT NULL REFERENCES users(id),
    final_price NUMERIC(10,2) NOT NULL CHECK (final_price >= 0),
    transaction_date TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'pending'
);

CREATE TABLE chats (
    id SERIAL PRIMARY KEY,
    listing_id INTEGER NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    buyer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(listing_id, buyer_id, seller_id)
);

CREATE TABLE messages (
    id SERIAL PRIMARY KEY,
    chat_id INTEGER NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE reviews (
    id SERIAL PRIMARY KEY,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id INTEGER NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
