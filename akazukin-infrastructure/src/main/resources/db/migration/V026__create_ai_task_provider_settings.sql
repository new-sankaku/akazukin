CREATE TABLE ai_task_provider_settings (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_type   VARCHAR(30) NOT NULL,
    provider    VARCHAR(30) NOT NULL,
    CONSTRAINT uq_ai_task_provider_user_task UNIQUE (user_id, task_type)
);

CREATE INDEX idx_ai_task_provider_settings_user_id ON ai_task_provider_settings(user_id);
