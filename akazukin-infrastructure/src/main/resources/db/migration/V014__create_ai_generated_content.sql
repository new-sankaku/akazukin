CREATE TABLE ai_generated_content (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    persona_id UUID REFERENCES ai_personas(id) ON DELETE SET NULL,
    prompt TEXT NOT NULL,
    generated_text TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    tokens_used INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_generated_content_user ON ai_generated_content(user_id, created_at DESC);
