CREATE TABLE agent_tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agent_type VARCHAR(30) NOT NULL,
    input TEXT NOT NULL,
    output TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    parent_task_id UUID REFERENCES agent_tasks(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_agent_tasks_user ON agent_tasks(user_id, created_at DESC);
CREATE INDEX idx_agent_tasks_parent ON agent_tasks(parent_task_id);
CREATE INDEX idx_agent_tasks_status ON agent_tasks(status);
