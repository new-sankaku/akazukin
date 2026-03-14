CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID,
    username      VARCHAR(50),
    http_method   VARCHAR(10)   NOT NULL,
    request_path  VARCHAR(2048) NOT NULL,
    query_string  VARCHAR(2048),
    request_body  TEXT,
    response_status INTEGER     NOT NULL,
    duration_ms   BIGINT        NOT NULL,
    client_ip     VARCHAR(45),
    user_agent    VARCHAR(512),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user_id    ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_path       ON audit_logs (request_path);
