CREATE TABLE friend_targets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    target_identifier VARCHAR(200) NOT NULL,
    display_name VARCHAR(200),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, platform, target_identifier)
);

CREATE INDEX idx_friend_targets_user_platform ON friend_targets(user_id, platform);
