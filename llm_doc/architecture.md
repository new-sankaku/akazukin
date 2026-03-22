# アーキテクチャ・モジュール構成

## アーキテクチャ原則
- ヘキサゴナルアーキテクチャ (Ports & Adapters)
- ドメイン層はフレームワーク非依存 (純粋Java)
- SNSアダプターはJava SPI (ServiceLoader) で動的ロード
- 3層分離: SDK (HTTP通信) → Adapter (ドメイン変換) → Domain (共通モデル)

## モジュール構成と依存方向
```
akazukin-web        → akazukin-application → akazukin-domain ← akazukin-infrastructure
akazukin-worker     → akazukin-application → akazukin-domain ← akazukin-infrastructure
                    → akazukin-adapter-*   → akazukin-domain
akazukin-adapter-*  → akazukin-sdk-*  (独自SDKがある場合)
                    → HttpClient + Jackson (直接実装の場合)
akazukin-ai-core    → akazukin-domain, akazukin-sdk-ollama
akazukin-ai-ollama  → akazukin-ai-core, akazukin-sdk-ollama
akazukin-ai-external→ akazukin-ai-core, jackson-databind
akazukin-web        → akazukin-ai-core, akazukin-ai-ollama
```
- 依存は常に **内側 (Domain) に向かう**。Domain が外側に依存してはならない
- `akazukin-domain` は他のモジュールに依存しない
- 全アダプターは HttpClient + Jackson で統一実装 (外部ライブラリ不使用)
- `akazukin-sdk-*` は Quarkus 非依存 (java.net.http.HttpClient + Jackson のみ)

## モジュール全体像 (34モジュール)
```
akazukin/
├── akazukin-domain/                  # ドメイン層 (38モデル, 38ポート, 8サービス, 15列挙型)
├── akazukin-application/             # アプリケーション層 (26ユースケース, 139 DTO)
├── akazukin-infrastructure/          # インフラ層 (21リポジトリ実装, 20マッパー, 21エンティティ)
├── akazukin-web/                     # Web層 (16 REST API, 16コントローラ, 28テンプレート)
├── akazukin-worker/                  # ワーカー (スケジューラ, メトリクス, クリーンアップ)
├── akazukin-ai/
│   ├── akazukin-ai-core/            # エージェント基盤 (7エージェント + オーケストレーター)
│   ├── akazukin-ai-ollama/          # Ollama接続設定
│   └── akazukin-ai-external/        # OpenAI/Anthropic統合
├── akazukin-sdk/
│   ├── akazukin-sdk-twitter/        # Twitter/X SDK (OAuth2 PKCE + OAuth1)
│   ├── akazukin-sdk-bluesky/        # Bluesky SDK (AT Protocol)
│   ├── akazukin-sdk-reddit/         # Reddit SDK
│   ├── akazukin-sdk-pinterest/      # Pinterest SDK
│   ├── akazukin-sdk-tiktok/         # TikTok SDK
│   ├── akazukin-sdk-ollama/         # Ollama SDK
│   ├── akazukin-sdk-mixi2/          # mixi2 SDK
│   ├── akazukin-sdk-note/           # note SDK
│   └── akazukin-sdk-niconico/       # niconico SDK
└── akazukin-adapter-sns/
    ├── akazukin-adapter-core/       # アダプター基盤 (Abstract, FaultTolerant, Factory, AnalyticsFactory)
    ├── akazukin-adapter-twitter/    # SDK経由, PKCE管理, リプライ/メンション
    ├── akazukin-adapter-bluesky/    # SDK経由, JWT DID抽出, リプライ/メンション
    ├── akazukin-adapter-mastodon/   # HttpClient直接, リプライ/メンション
    ├── akazukin-adapter-threads/    # HttpClient直接, 2段階投稿
    ├── akazukin-adapter-instagram/  # HttpClient直接, メディア必須
    ├── akazukin-adapter-reddit/     # SDK経由, User-Agent必須
    ├── akazukin-adapter-telegram/   # HttpClient直接, Bot Token
    ├── akazukin-adapter-vk/         # HttpClient直接, VK API v5.199
    ├── akazukin-adapter-pinterest/  # HttpClient直接, メディア必須
    ├── akazukin-adapter-tiktok/     # SDK経由, 非同期公開
    ├── akazukin-adapter-mixi2/      # SDK経由
    ├── akazukin-adapter-note/       # SDK経由, 長文 (50K文字)
    ├── akazukin-adapter-niconico/   # SDK経由, 動画コメント
    └── akazukin-adapter-devkit/     # 開発用
```

## アダプター共通パターン
- レート制限 (RateLimiter) + サーキットブレーカー (FaultTolerantSnsAdapter)
- プロフィールキャッシュ (ConcurrentHashMap, 5分TTL)
- パフォーマンスログ (100ms超過で警告)
- 例外ラッピング (SnsApiException)
- リトライ (指数バックオフ, 429レート制限対応)
- 分析 (SnsAnalyticsAdapter): 投稿統計・アカウント統計の取得
  - AbstractSnsAdapterのデフォルト実装はOptional.empty()
  - 各プラットフォームAPIに対応するアダプターがオーバーライド
  - SnsAnalyticsAdapterFactory → SnsAnalyticsAdapterLookup (CDI) で注入

## AI関連アーキテクチャ
- ドメイン: AiTaskType, AiTaskProviderSetting, AiInfrastructurePort
- ユースケース: AiContentUseCase (生成/比較/試し書き), AiSettingsUseCase (接続/設定/コスト)
- API: AiResource (/api/v1/ai/) に全AI機能を集約
- 画面: AiController (index/personas/settings) の3画面構成
- Ollamaアクセスはドメインポート経由 (AiInfrastructurePort)
- タスク別AI処理先設定はDB永続化 (ai_task_provider_settings テーブル)
- エージェントパイプライン: ANALYST→COMPOSER→TRANSFORMER(SNS毎)→SENTINEL→COMPLIANCE→SCHEDULER
- 外部AI: AnthropicTextGenerator (claude-sonnet-4-20250514), OpenAiTextGenerator (gpt-4o)

## 投稿パイプライン
```
PostUseCase.createPost()
  → PostRepository.save() [DRAFT/SCHEDULED]
  → PostTargetRepository.save() [プラットフォーム毎]
  → PostPublisher.publishForProcessing() or schedulePost()

PostPublishUseCase.processPost()
  → CircuitBreakerRegistry.isCallPermitted() [各プラットフォーム]
  → SnsAdapter.post() [並行実行, ExecutorService]
  → リトライ (最大2回, 指数バックオフ)
  → PostTarget.markAsPublished() or markAsFailed()
```

## 承認ワークフロー
```
PostUseCase → status=PENDING_APPROVAL
  → ApprovalUseCase.submitForApproval()
  → TeamApprovalUseCase.getAiReview() [SENTINEL + COMPLIANCE エージェント]
  → ApprovalUseCase.decide() [APPROVE/REJECT/REQUEST_CHANGES]
  → status=APPROVED → PostPublisher.publishForProcessing()
  → status=REJECTED/RETURNED → 投稿者に通知
```

## セキュリティスタック
```
リクエスト
  → SecurityFilter (JWT検証, 公開パス除外)
  → AuditLogFilter (監査ログ記録, 機密サニタイズ)
  → RateLimit (Bucket4j: 一般100/分, 投稿30/分, AI10/分)
  → @RolesAllowed (ADMIN/USER/VIEWER)
  → ビジネスロジック
```

## キャッシュ戦略
| キャッシュ | TTL | 最大サイズ | 用途 |
|----------|-----|----------|------|
| analytics-summary | 5分 | 1000 | ダッシュボード統計 |
| sns-profile | 10分 | 500 | SNSプロフィール |
| アダプターprofile | 5分 | - | AbstractSnsAdapter内ConcurrentHashMap |
| HTTPstatic | 7日 | - | 静的アセット |
