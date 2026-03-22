# データベーススキーマ

## 概要
- PostgreSQL, Flyway管理 (V001〜V027)
- 全テーブルUUID主キー
- 監査フィールド: created_at (不変) + updated_at (可変エンティティのみ)

## テーブル一覧 (24テーブル)

### ユーザー・チーム
```
users
  id (UUID PK), username (UNIQUE), email (UNIQUE), password_hash, role, created_at, updated_at

teams
  id (UUID PK), name, owner_user_id (FK→users), created_at, updated_at

team_members
  id (UUID PK), team_id (FK→teams), user_id (FK→users), role, joined_at
  UNIQUE(team_id, user_id)
```

### SNSアカウント
```
sns_accounts
  id (UUID PK), user_id (FK→users ON DELETE CASCADE), platform, account_identifier,
  display_name, access_token (TEXT暗号化), refresh_token (TEXT暗号化), token_expires_at,
  created_at, updated_at
  UNIQUE(user_id, platform)
```

### 投稿
```
posts
  id (UUID PK), user_id (FK→users), content (TEXT), media_urls (TEXT),
  status, scheduled_at, created_at, updated_at
  INDEX(user_id, status), PARTIAL INDEX(status='SCHEDULED')

post_targets
  id (UUID PK), post_id (FK→posts), sns_account_id (FK→sns_accounts),
  platform, platform_post_id, status, error_message (TEXT), published_at, created_at
  INDEX(post_id, status)

post_templates
  id (UUID PK), user_id (FK→users), name, content (TEXT), placeholders (TEXT),
  platforms (TEXT), category, usage_count, created_at, updated_at
```

### 承認
```
approval_requests
  id (UUID PK), post_id (FK→posts), requester_id (FK→users),
  approver_id (FK→users NULL), team_id (FK→teams NULL),
  status, comment (TEXT), requested_at, decided_at
  INDEX(approver_id, decided_at), INDEX(team_id, decided_at)

approval_rules
  id (UUID PK), team_id (FK→teams), role,
  post_approval_required, schedule_approval_required, media_approval_required,
  ai_check_required, ai_auto_reject, min_approvers, approval_deadline_hours,
  created_at, updated_at
  UNIQUE(team_id, role)

risk_level_flows
  id (UUID PK), team_id (FK→teams), risk_level,
  required_approvers, admin_required, legal_review_required
  UNIQUE(team_id, risk_level)
```

### AI
```
ai_personas
  id (UUID PK), user_id (FK→users), name, system_prompt (TEXT), tone,
  language (default 'ja'), avatar_url, is_default, created_at, updated_at

ai_generated_content
  id (UUID PK), user_id (FK→users), persona_id (FK→ai_personas SET NULL),
  prompt (TEXT), generated_text (TEXT), model_name, tokens_used, duration_ms, created_at
  INDEX(user_id, created_at DESC)

ai_task_provider_settings
  id (UUID PK), user_id (FK→users), task_type, provider
  UNIQUE(user_id, task_type)

agent_tasks
  id (UUID PK), user_id (FK→users), agent_type, input (TEXT), output (TEXT),
  status, parent_task_id (FK→agent_tasks NULL 自己参照), created_at, completed_at
  INDEX(user_id, created_at DESC), INDEX(parent_task_id)
```

### 分析・インタラクション
```
impression_snapshots
  id (UUID PK), sns_account_id (FK→sns_accounts ON DELETE CASCADE),
  platform, followers_count, following_count, post_count,
  impressions_count, engagement_rate, snapshot_at
  INDEX(sns_account_id, snapshot_at DESC)

interactions
  id (UUID PK), user_id (FK→users), sns_account_id (FK→sns_accounts ON DELETE CASCADE),
  platform, interaction_type, target_post_id, target_user_id, content (TEXT), created_at
  INDEX(user_id, created_at DESC), INDEX(sns_account_id, created_at DESC)

friend_targets
  id (UUID PK), user_id (FK→users), platform, target_identifier, display_name, notes (TEXT), created_at
  UNIQUE(user_id, platform, target_identifier)
```

### カレンダー
```
calendar_entries
  id (UUID PK), user_id (FK→users), post_id (FK→posts NULL), title,
  description (TEXT), scheduled_at, platforms (TEXT), color, created_at, updated_at
  INDEX(user_id, scheduled_at)
```

### A/Bテスト
```
ab_tests
  id (UUID PK), user_id (FK→users), name, variant_a (TEXT), variant_b (TEXT),
  variant_c (TEXT NULL), status, started_at, completed_at, winner_variant,
  platforms, created_at
  INDEX(user_id, created_at DESC)
```

### コンテンツソース
```
news_sources
  id (UUID PK), user_id (FK→users ON DELETE CASCADE), name, url, source_type (default 'RSS'), is_active, created_at

news_items
  id (UUID PK), source_id (FK→news_sources ON DELETE CASCADE), title, url, summary (TEXT),
  published_at, fetched_at
  INDEX(source_id, fetched_at DESC)
```

### メディア
```
media_assets
  id (UUID PK), user_id (FK→users ON DELETE CASCADE), file_name, mime_type,
  size_bytes, storage_url, thumbnail_url, alt_text, created_at
```

### 監査・通知
```
audit_logs
  id (UUID PK), user_id (NULL), username, http_method, request_path,
  query_string, request_body (TEXT), response_status, duration_ms,
  client_ip, user_agent, category (default 'PAGE'), created_at
  INDEX(user_id), INDEX(created_at), INDEX(request_path), INDEX(category)

notifications
  id (UUID PK), user_id (FK→users ON DELETE CASCADE), type, title, body (TEXT),
  related_entity_id (UUID NULL), is_read, created_at
  INDEX(user_id, is_read), INDEX(user_id, created_at DESC)
```

## リスト型データのシリアライズ
| カラム | 区切り文字 | 例 |
|-------|-----------|-----|
| posts.media_urls | 改行 (\n) | url1\nurl2 |
| post_templates.placeholders | 改行 (\n) | {name}\n{date} |
| post_templates.platforms | 改行 (\n) | TWITTER\nBLUESKY |
| calendar_entries.platforms | カンマ (,) | TWITTER,BLUESKY |
| ab_tests.platforms | カンマ (,) | TWITTER,BLUESKY |

## データ保持ポリシー
| データ | 保持期間 |
|-------|---------|
| 監査ログ | 90日 |
| 既読通知 | 30日 |

## マイグレーション履歴
V001-V005: コアテーブル (users, sns_accounts, posts, post_targets, teams)
V006: パフォーマンスインデックス追加
V007: 監査ログ
V009-V018: 機能拡張 (AI, テンプレート, ニュース, カレンダー, A/Bテスト等)
V019: カレンダースケジューリング
V023: エージェントタスクシステム
V024: 監査ログカテゴリ追加
V025: A/Bテスト拡張 (variant_c, platforms)
V026: AIタスク処理先設定
V027: 承認ルール・リスクフロー
