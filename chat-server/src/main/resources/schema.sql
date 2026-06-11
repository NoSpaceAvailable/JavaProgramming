CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500) DEFAULT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NULL DEFAULT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rooms_name ON rooms(name);

CREATE TABLE IF NOT EXISTS room_members (
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    PRIMARY KEY (room_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_room_members_user ON room_members(user_id);

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT DEFAULT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id BIGINT DEFAULT NULL REFERENCES users(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(10) NOT NULL DEFAULT 'TEXT',
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_messages_room_time ON messages(room_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_private ON messages(sender_id, recipient_id, created_at DESC);
-- Idempotent migration for databases created before the 'edited' column existed.
ALTER TABLE messages ADD COLUMN IF NOT EXISTS edited BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS file_attachments (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    checksum VARCHAR(64) DEFAULT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_attachments_message ON file_attachments(message_id);

CREATE TABLE IF NOT EXISTS reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (message_id, user_id, emoji)
);
CREATE INDEX IF NOT EXISTS idx_reactions_message ON reactions(message_id);

CREATE TABLE IF NOT EXISTS dm_conversations (
    id BIGSERIAL PRIMARY KEY,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user1_id, user2_id)
);
CREATE INDEX IF NOT EXISTS idx_dm_user1 ON dm_conversations(user1_id);
CREATE INDEX IF NOT EXISTS idx_dm_user2 ON dm_conversations(user2_id);

-- ===== Community servers (guilds) =====
CREATE TABLE IF NOT EXISTS servers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invite_code VARCHAR(16) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_servers_invite ON servers(invite_code);

CREATE TABLE IF NOT EXISTS server_members (
    server_id BIGINT NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(12) NOT NULL DEFAULT 'MEMBER', -- OWNER / ADMIN / MODERATOR / MEMBER
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_server_members_user ON server_members(user_id);

CREATE TABLE IF NOT EXISTS server_bans (
    server_id BIGINT NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banned_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reason VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (server_id, user_id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES servers(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_name VARCHAR(100) DEFAULT NULL,
    action VARCHAR(40) NOT NULL,
    detail VARCHAR(500) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_server ON audit_log(server_id, created_at DESC);

-- A room with a non-null server_id is a text channel belonging to that server.
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS server_id BIGINT REFERENCES servers(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_rooms_server ON rooms(server_id);
