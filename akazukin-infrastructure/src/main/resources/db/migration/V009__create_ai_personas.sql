CREATE TABLE ai_personas (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    system_prompt TEXT NOT NULL,
    tone VARCHAR(30) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'ja',
    avatar_url VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_personas_user_id ON ai_personas(user_id);
