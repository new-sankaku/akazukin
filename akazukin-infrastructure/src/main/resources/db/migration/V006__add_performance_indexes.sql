CREATE INDEX IF NOT EXISTS idx_post_targets_sns_account_id ON post_targets(sns_account_id);
CREATE INDEX IF NOT EXISTS idx_sns_accounts_user_platform ON sns_accounts(user_id, platform);
CREATE INDEX IF NOT EXISTS idx_posts_user_status ON posts(user_id, status);
CREATE INDEX IF NOT EXISTS idx_posts_user_scheduled ON posts(user_id, scheduled_at);
