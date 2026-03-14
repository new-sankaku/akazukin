CREATE TABLE interactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sns_account_id UUID NOT NULL REFERENCES sns_accounts(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    interaction_type VARCHAR(20) NOT NULL,
    target_post_id VARCHAR(200),
    target_user_id VARCHAR(200),
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interactions_user ON interactions(user_id, created_at DESC);
CREATE INDEX idx_interactions_account ON interactions(sns_account_id, created_at DESC);
