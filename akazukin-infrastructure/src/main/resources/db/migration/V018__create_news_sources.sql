CREATE TABLE news_sources (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'RSS',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE news_items (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES news_sources(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000),
    summary TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    fetched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_items_source ON news_items(source_id, fetched_at DESC);
