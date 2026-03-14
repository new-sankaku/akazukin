CREATE TABLE approval_requests (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES users(id),
    approver_id UUID REFERENCES users(id),
    team_id UUID REFERENCES teams(id),
    status VARCHAR(30),
    comment TEXT,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_approval_requests_post_id ON approval_requests(post_id);
CREATE INDEX idx_approval_requests_approver ON approval_requests(approver_id, decided_at);
CREATE INDEX idx_approval_requests_team ON approval_requests(team_id, decided_at);
