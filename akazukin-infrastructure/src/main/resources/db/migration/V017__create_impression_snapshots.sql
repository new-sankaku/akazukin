CREATE TABLE impression_snapshots (
    id UUID PRIMARY KEY,
    sns_account_id UUID NOT NULL REFERENCES sns_accounts(id) ON DELETE CASCADE,
    platform VARCHAR(20) NOT NULL,
    followers_count INTEGER NOT NULL DEFAULT 0,
    following_count INTEGER NOT NULL DEFAULT 0,
    post_count INTEGER NOT NULL DEFAULT 0,
    impressions_count BIGINT NOT NULL DEFAULT 0,
    engagement_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    snapshot_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_impression_snapshots_account ON impression_snapshots(sns_account_id, snapshot_at DESC);
