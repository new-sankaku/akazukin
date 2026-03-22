CREATE TABLE approval_rules (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    post_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    schedule_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    media_approval_required BOOLEAN NOT NULL DEFAULT FALSE,
    ai_check_required BOOLEAN NOT NULL DEFAULT TRUE,
    ai_auto_reject BOOLEAN NOT NULL DEFAULT FALSE,
    min_approvers INT NOT NULL DEFAULT 1,
    approval_deadline_hours INT NOT NULL DEFAULT 24,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, role)
);

CREATE TABLE risk_level_flows (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    risk_level VARCHAR(20) NOT NULL,
    required_approvers INT NOT NULL DEFAULT 1,
    admin_required BOOLEAN NOT NULL DEFAULT FALSE,
    legal_review_required BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(team_id, risk_level)
);

CREATE INDEX idx_approval_rules_team ON approval_rules(team_id);
CREATE INDEX idx_risk_level_flows_team ON risk_level_flows(team_id);
