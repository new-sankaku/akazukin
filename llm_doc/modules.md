# モジュール詳細・関係性

## モジュール一覧 (34モジュール)

### コア層 (4モジュール)
| モジュール | 役割 | 依存先 |
|-----------|------|--------|
| akazukin-domain | ドメインモデル・ポート・ドメインサービス (純粋Java) | なし |
| akazukin-application | ユースケース・DTO (CDI) | domain |
| akazukin-infrastructure | DB永続化・キュー・暗号化 (Quarkus Panache + Flyway) | domain |
| akazukin-web | REST API・コントローラ・テンプレート・セキュリティ | application, infrastructure, adapter-*, ai-* |

### SDK層 (9モジュール) - Quarkus非依存
| モジュール | 対象 | 主要機能 |
|-----------|------|---------|
| akazukin-sdk-twitter | Twitter/X API v2 | OAuth2 PKCE + OAuth1 HMAC-SHA1, ツイートCRUD |
| akazukin-sdk-bluesky | Bluesky AT Protocol | セッション管理, 投稿CRUD, プロフィール取得 |
| akazukin-sdk-reddit | Reddit API | OAuth2 + Basic Auth, サブレディット投稿 |
| akazukin-sdk-pinterest | Pinterest API | ピン作成/削除, ボード一覧 |
| akazukin-sdk-tiktok | TikTok Content Posting API | 動画/写真投稿, 公開ステータス確認 |
| akazukin-sdk-ollama | Ollama API | テキスト生成/チャット, モデル一覧, ヘルスチェック |
| akazukin-sdk-mixi2 | mixi2 API | 投稿CRUD, プロフィール取得 |
| akazukin-sdk-note | note API | 記事投稿/削除, プロフィール取得 |
| akazukin-sdk-niconico | ニコニコ API | コメント投稿, プロフィール取得 |

SDK共通パターン: Config (record) → Client (Builder) → Model (record) → Exception

### アダプター層 (16モジュール)
| モジュール | 実装方式 | 特記事項 |
|-----------|---------|---------|
| akazukin-adapter-core | 基盤 | AbstractSnsAdapter, FaultTolerantSnsAdapter, SnsAdapterFactory (SPI) |
| akazukin-adapter-twitter | SDK経由 | PKCE検証子管理 (10分TTL), リプライ/メンション対応 |
| akazukin-adapter-bluesky | SDK経由 | JWTからDID抽出, xrpcプロトコル, リプライ/メンション対応 |
| akazukin-adapter-mastodon | HttpClient直接 | インスタンスURL可変, リプライ/メンション対応 |
| akazukin-adapter-threads | HttpClient直接 | 2段階投稿 (コンテナ作成→公開) |
| akazukin-adapter-instagram | HttpClient直接 | Facebook Graph API v19.0, メディア必須 |
| akazukin-adapter-reddit | SDK経由 | User-Agent必須, コンテンツ形式: "subreddit:title:body" |
| akazukin-adapter-telegram | HttpClient直接 | Botトークン認証, コンテンツ形式: "chatId:text" |
| akazukin-adapter-vk | HttpClient直接 | VK API v5.199, リフレッシュトークン非対応 |
| akazukin-adapter-pinterest | HttpClient直接 | コンテンツ形式: "boardId:title:description", メディア必須 |
| akazukin-adapter-tiktok | SDK経由 | 非同期公開 (publishId返却), 削除非対応 |
| akazukin-adapter-mixi2 | SDK経由 | SDK薄ラッパー |
| akazukin-adapter-note | SDK経由 | 長文対応 (50K文字), 1行目=タイトル |
| akazukin-adapter-niconico | SDK経由 | 動画コメント投稿, コンテンツ形式: "videoId:comment" |
| akazukin-adapter-devkit | 開発用 | - |

アダプター共通機能: レート制限, プロフィールキャッシュ (5分TTL), サーキットブレーカー連携, パフォーマンスログ

### AI層 (3モジュール)
| モジュール | 役割 | 依存先 |
|-----------|------|--------|
| akazukin-ai-core | エージェント基盤・オーケストレーター | domain, sdk-ollama |
| akazukin-ai-ollama | Ollama接続設定・CDIプロデューサー | ai-core, sdk-ollama |
| akazukin-ai-external | OpenAI/Anthropic外部API統合 | ai-core, Jackson |

### ワーカー層 (1モジュール)
| モジュール | 役割 |
|-----------|------|
| akazukin-worker | スケジュール投稿・メトリクス集約・古データ削除・サーキットブレーカー監視 |

## ユースケース一覧 (26クラス)

### 認証・ユーザー管理
| ユースケース | 機能 |
|------------|------|
| AuthUseCase | ユーザー登録・認証 |
| AccountUseCase | SNSアカウント接続・OAuth処理 |
| TeamUseCase | チーム作成・メンバー管理 |

### 投稿管理
| ユースケース | 機能 |
|------------|------|
| PostUseCase | 投稿CRUD・ターゲット設定 |
| PostPublishUseCase | マルチプラットフォーム並行投稿・リトライ・サーキットブレーカー |
| SchedulePostUseCase | スケジュール投稿処理・再スケジュール |
| PostDetailUseCase | エンゲージメント分析・クロスポスト推奨・コンプライアンスチェック |
| TemplateUseCase | 投稿テンプレートCRUD・使用回数追跡 |

### AI・コンテンツ生成
| ユースケース | 機能 |
|------------|------|
| AiContentUseCase | ペルソナ別生成・比較・試し書き |
| AiSettingsUseCase | Ollama接続管理・AI処理先設定・コスト追跡 |
| AgentPipelineUseCase | 7段階パイプライン実行 (ANALYST→COMPOSER→TRANSFORMER→SENTINEL→COMPLIANCE→SCHEDULER) |
| ABTestUseCase | A/Bテスト管理・バリアント生成・予測・勝ちパターン分析 |

### 分析・監視
| ユースケース | 機能 |
|------------|------|
| AnalyticsUseCase | ダッシュボード統計・プラットフォーム別分析 |
| DashboardUseCase | サマリー集計・ファイアウォッチ・キュー状態 |
| ImpressionUseCase | インプレッション時系列データ管理 |
| GrowthStrategyUseCase | 成長アドバイス (AIエージェント活用) |

### ソーシャル・コンテンツ
| ユースケース | 機能 |
|------------|------|
| FriendUseCase | フレンド管理・エンゲージメント分析・関係性プラン・バブルマップ |
| NewsPostUseCase | ニュース取得・投稿アイデア生成・マルチアングル展開 |
| CalendarUseCase | カレンダー管理・祝日連携・橋渡し休暇・連携シナリオ |

### ワークフロー
| ユースケース | 機能 |
|------------|------|
| ApprovalUseCase | 承認依頼・判定・通知 |
| TeamApprovalUseCase | チーム承認ルール管理・AIレビュー・リスクフロー |
| NotificationUseCase | 通知一覧・既読管理 |
| MediaUseCase | メディアアップロード・一覧・削除 |

## Web層の構成

### REST API (16リソース, 93エンドポイント)
| リソース | パス | エンドポイント数 | ロール |
|---------|------|--------------|--------|
| AuthResource | /api/v1/auth | 3 | 公開 |
| PostResource | /api/v1/posts | 10 | ADMIN |
| DashboardResource | /api/v1/dashboard | 16 | ADMIN, USER |
| CalendarResource | /api/v1/calendar | 13 | ADMIN, USER |
| AiResource | /api/v1/ai | 12 | ADMIN |
| ABTestResource | /api/v1/ab-tests | 11 | ADMIN, USER |
| ApprovalResource | /api/v1/approvals | 9 | ADMIN |
| FriendResource | /api/v1/friends | 8 | ADMIN, USER |
| AgentResource | /api/v1/agents | 6 | ADMIN |
| TeamResource | /api/v1/teams | 6 | ADMIN, USER |
| NewsResource | /api/v1/news | 10 | ADMIN, USER |
| NotificationResource | /api/v1/notifications | 5 | ADMIN |
| AccountResource | /api/v1/accounts | 4 | ADMIN, USER |
| TemplateResource | /api/v1/templates | 4 | ADMIN |
| MediaResource | /api/v1/media | 3 | ADMIN |
| AuditLogResource | /api/v1/audit-logs | 1 | ADMIN |

### コントローラ (16コントローラ, 28テンプレート)
SSRはQuarkus Renardeで実装。全ルートはGETのみでtext/html返却。
APIはHTMXからfetchで呼び出し、JWTをlocalStorageで管理。

### セキュリティ
| コンポーネント | 役割 |
|--------------|------|
| SecurityFilter | JWT認証フィルター (公開パス除外) |
| JwtTokenService | トークン生成/検証 (RSA鍵, アクセス15分/リフレッシュ7日) |
| PasswordHasher | BCrypt (コスト12) |
| AuditLogFilter | 全HTTPリクエストの監査ログ記録 (機密情報サニタイズ) |

### エラーハンドリング (5ハンドラー)
| ハンドラー | 対象例外 | HTTPステータス |
|----------|---------|-------------|
| GlobalExceptionHandler | DomainException系 | 400/401/404/500 |
| GenericExceptionHandler | 未処理Throwable | 500 (エラーID付き) |
| IllegalArgumentExceptionHandler | IllegalArgumentException | 400 |
| DateTimeParseExceptionHandler | DateTimeParseException | 400 |
| InvalidRefreshTokenExceptionHandler | InvalidRefreshTokenException | 401 |

## テスト構成 (28テストクラス, 400+テストメソッド)

### アプリケーション層テスト (13クラス)
ABTestUseCaseTest, AiContentUseCaseTest, CalendarUseCaseTest, AuthUseCaseTest,
PostUseCaseTest, FriendUseCaseTest, TemplateUseCaseTest, ImpressionUseCaseTest,
NotificationUseCaseTest, MediaUseCaseTest, AccountUseCaseTest,
AgentPipelineUseCaseTest, GrowthStrategyUseCaseTest

### Web層テスト (15クラス)
AuthResourceTest, PostResourceTest, AccountResourceTest, DashboardResourceTest,
ABTestResourceTest, ApprovalResourceTest, NotificationResourceTest, FriendResourceTest,
TeamResourceTest, SecurityFilterTest, PasswordHasherTest, RateLimitBucketManagerTest,
EndpointCategoryResolverTest + ユーティリティ2クラス
