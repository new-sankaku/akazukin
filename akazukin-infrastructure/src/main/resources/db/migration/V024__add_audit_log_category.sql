ALTER TABLE audit_logs ADD COLUMN category VARCHAR(20) DEFAULT 'PAGE' NOT NULL;

CREATE INDEX idx_audit_logs_category ON audit_logs (category);
