# ドメインモデル詳細

## エンティティ (可変, IDベース等価性)

### ユーザー・チーム
| モデル | 主要フィールド | ビジネスロジック |
|-------|-------------|--------------|
| User | username, email, passwordHash, role | changePassword, updateEmail, hasRole, isAdmin |
| Team | name, ownerUserId | - |
| TeamMember | teamId, userId, role | - |

### 投稿
| モデル | 主要フィールド | ビジネスロジック |
|-------|-------------|--------------|
| Post | userId, content, mediaUrls, status, scheduledAt | isScheduled, isEditable |
| PostTarget | postId, snsAccountId, platform, platformPostId, status, errorMessage | isSuccessful, isFailed, markAsPublished, markAsFailed |
| PostTemplate | userId, name, content, placeholders, platforms, category, usageCount | - |

### SNSアカウント
| モデル | 主要フィールド | ビジネスロジック |
|-------|-------------|--------------|
| SnsAccount | userId, platform, accountIdentifier, accessToken, refreshToken, tokenExpiresAt | isTokenExpired, needsTokenRefresh, updateTokens |

### AI
| モデル | 主要フィールド |
|-------|-------------|
| AiPersona | userId, name, systemPrompt, tone, language, avatarUrl, isDefault |
| AiTaskProviderSetting | userId, taskType (AiTaskType), provider (AiModelProvider) |
| AgentTask | userId, agentType, input, output, status, parentTaskId |

### 分析
| モデル | 主要フィールド |
|-------|-------------|
| ImpressionSnapshot | snsAccountId, platform, followersCount, followingCount, postCount, impressionsCount, engagementRate |
| Interaction | userId, snsAccountId, platform, interactionType, targetPostId, targetUserId, content |

### ワークフロー
| モデル | 主要フィールド |
|-------|-------------|
| ApprovalRequest | postId, requesterId, approverId, teamId, status, comment |
| ApprovalRule | teamId, role, postApprovalRequired, aiCheckRequired, aiAutoReject, minApprovers |
| RiskLevelFlow | teamId, riskLevel, requiredApprovers, adminRequired, legalReviewRequired |

### その他
| モデル | 主要フィールド |
|-------|-------------|
| ABTest | userId, name, variantA/B/C, status, winnerVariant, platforms |
| CalendarEntry | userId, postId, title, scheduledAt, platforms, color |
| FriendTarget | userId, platform, targetIdentifier, displayName, notes |
| MediaAsset | userId, fileName, mimeType, sizeBytes, storageUrl, thumbnailUrl |
| Notification | userId, type, title, body, relatedEntityId, isRead |
| AuditLog | userId, httpMethod, requestPath, responseStatus, durationMs, category |
| NewsSource | userId, name, url, sourceType, isActive |
| NewsItem | sourceId, title, url, summary |

## 値オブジェクト (record, 不変, コンストラクタバリデーション)
| モデル | フィールド |
|-------|---------|
| AiPrompt | systemPrompt, userPrompt, temperature (0.0-2.0), maxTokens (>0) |
| AiResponse | generatedText, tokensUsed, durationMs, modelName |
| PostRequest | content (非空), mediaUrls (防御コピー) |
| PostResult | platformPostId, platformUrl, publishedAt |
| SnsAuthToken | accessToken, refreshToken, expiresAt, scope |
| SnsProfile | accountIdentifier, displayName, avatarUrl, followerCount |
| SnsPostStats | platformPostId, platform, likeCount, replyCount, repostCount, viewCount |
| AccountStats | platform, accountIdentifier, followerCount, followingCount, postCount |
| DashboardSummary | totalPosts, publishedPosts, failedPosts, scheduledPosts, connectedAccounts, postCountByPlatform |
| TranslationRequest | sourceText (非空), sourceLang, targetLang |
| TranslationResult | translatedText, sourceLang, targetLang, confidence |
| ToneAnalysisResult | toneLevel, formalityScore, suggestions |
| ApprovalDecision | action, comment, decidedBy, decidedAt |
| AdapterMetrics | platform, successCount, failureCount, avgLatencyMs, p99LatencyMs |
| CircuitBreakerState | platform, state (CLOSED/OPEN/HALF_OPEN), failureCount, lastFailureTime |

## 日本文化関連モデル
| モデル | 用途 |
|-------|------|
| BridgeHolidayPeriod | 連休期間 (GW/お盆/正月) の日英名称・日付範囲 |
| BridgePhase / BridgePhaseEntry | 連休3フェーズ戦略 (告知→期間中→再開) |
| LinkageScenario / LinkageStep | クロスプラットフォーム連携シナリオ (4パターン) |
| SeasonalEvent | 季節イベント (16+) とコンテンツ戦略ヒント |
| SolarTerm | 二十四節気の日付・名称・投稿ヒント |

## 列挙型 (15種)
| 列挙型 | 値 |
|--------|-----|
| SnsPlatform | TWITTER, BLUESKY, MASTODON, THREADS, INSTAGRAM, REDDIT, TELEGRAM, VK, PINTEREST, MIXI2, NOTE, NICONICO, TIKTOK |
| PostStatus | DRAFT, SCHEDULED, PUBLISHING, PUBLISHED, FAILED, PENDING_APPROVAL, APPROVED, REJECTED, RETURNED |
| Role | ADMIN, USER, VIEWER |
| AgentType | DIRECTOR, ANALYST, COMPOSER, TRANSFORMER, SCHEDULER, SENTINEL, COMPLIANCE |
| AiModelProvider | OLLAMA, OPENAI, ANTHROPIC, LOCAL |
| AiTaskType | COMPOSER, TRANSLATE, SENTINEL, COMPLIANCE, ANALYST, DIRECTOR, TRANSFORMER, SCHEDULER |
| ContentTone | CASUAL, FORMAL, HUMOROUS, PROFESSIONAL, FRIENDLY |
| InteractionType | REPLY, MENTION, LIKE, REPOST |
| ABTestStatus | DRAFT, RUNNING, COMPLETED, CANCELLED |
| ApprovalAction | APPROVE, REJECT, REQUEST_CHANGES |
| NotificationType | POST_PUBLISHED, POST_FAILED, POST_SCHEDULED, APPROVAL_REQUESTED, APPROVAL_DECIDED, ACCOUNT_CONNECTED, ACCOUNT_DISCONNECTED, MENTION, SYSTEM |
| CircuitState | CLOSED, OPEN, HALF_OPEN |
| RiskLevel | HIGH, MEDIUM, LOW |
| CalendarEventType | HOLIDAY, SOLAR_TERM, SEASONAL_EVENT |
| MediaType | IMAGE, VIDEO, GIF, DOCUMENT |

## ポートインターフェース (38インターフェース)

### リポジトリポート (18)
UserRepository, PostRepository, PostTargetRepository, SnsAccountRepository,
MediaAssetRepository, NotificationRepository, PostTemplateRepository,
CalendarEntryRepository, FriendTargetRepository, AiPersonaRepository,
NewsSourceRepository, NewsItemRepository, TeamRepository, ABTestRepository,
AgentTaskRepository, AuditLogRepository, ImpressionSnapshotRepository,
InteractionRepository, ApprovalRequestRepository, ApprovalRuleRepository,
AiTaskProviderSettingRepository

### アダプターポート (7)
SnsAdapter, SnsAnalyticsAdapter, SnsInteractionAdapter, SnsGraphAdapter,
AiTextGenerator, AiTranslator, AiInfrastructurePort

### インフラポート (7)
PostPublisher, NotificationSender, MediaStorage, NewsFeedFetcher,
PasswordHasher, AgentOrchestrator, CircuitBreakerRegistry,
ExternalApiAuditPort, AdapterMetricsCollector, DataRetentionPort

## ドメインサービス (8クラス)
| サービス | 役割 |
|---------|------|
| PostService | 投稿作成・更新・削除・コンテンツバリデーション |
| AccountService | SNSアカウント連携・トークン管理 |
| JapaneseToneAnalyzer | 日本語テキストの敬語レベル分析 (敬語/丁寧語/普通体/タメ口) |
| JapaneseHolidayProvider | 日本の祝日・振替休日計算 (春分/秋分の近似含む) |
| SolarTermProvider | 二十四節気の日付・投稿戦略ヒント提供 |
| SeasonalEventProvider | 季節イベント (16+) とマーケティングガイダンス |
| BridgeHolidayDetector | 連休検出 (GW/お盆/正月) と3フェーズ投稿戦略生成 |
| LinkageScenarioProvider | クロスプラットフォーム配信戦略 (4パターン) |
