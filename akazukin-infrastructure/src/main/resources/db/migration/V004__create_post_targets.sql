CREATE TABLE post_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    sns_account_id UUID NOT NULL REFERENCES sns_accounts(id),
    platform VARCHAR(20) NOT NULL,
    platform_post_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    error_message TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_targets_post_id ON post_targets(post_id);
CREATE INDEX idx_post_targets_status ON post_targets(status);
