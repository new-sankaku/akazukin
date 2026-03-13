# Akazukin - 複数SNS管理ツール 設計書

## 1. 概要

複数のSNSアカウントを一元管理するWebアプリケーション。
テキスト系SNSの投稿・予約投稿・アナリティクス・ダッシュボードを提供し、
REST APIによるローカルPCからの操作にも対応する。

## 2. 技術スタック

| カテゴリ | 技術 | バージョン | 備考 |
|---------|------|-----------|------|
| 言語 | Java | 21 (LTS) | |
| フレームワーク | Quarkus | 3.x | |
| Web UI | Renarde + HTMX + Qute | 3.1.x | サーバーサイドレンダリング |
| アセット管理 | Quarkus Web Bundler | Quarkiverse | htmxをMaven依存で管理、Node.js不要 |
| REST API | quarkus-rest (RESTEasy Reactive) | Quarkus標準 | |
| 認証 | quarkus-smallrye-jwt | Quarkus標準 | JWT + リフレッシュトークン |
| ORM | Hibernate ORM with Panache | Quarkus標準 | Repository パターン (テスタビリティ重視) |
| DB (RDB) | PostgreSQL (Aurora Serverless v2) | | メインデータ |
| DB (KV) | DynamoDB | | セッション・キャッシュ |
| スケジューラ | EventBridge Scheduler | AWS | ※Lambda上ではQuartzは動作不可 |
| キュー | Amazon SQS | | 投稿キュー・リトライ |
| ビルド | Gradle (Kotlin DSL) | 8.x | マルチモジュール |
| デプロイ | AWS Lambda + API Gateway | | quarkus-amazon-lambda-http |
| CSS | PicoCSS または Water.css | | 軽量CSSフレームワーク |

### 2.1 重要な技術的制約

1. **Lambda + Quartz/Scheduler 非互換**: AWS Lambdaはリクエスト駆動のため、
   `@Scheduled`やQuartzのバックグラウンドジョブは動作しない。
   予約投稿は **EventBridge Scheduler → SQS → Lambda** の構成で実現する。

2. **Renarde 単一継承制限**: `HxController` と `ControllerWithUser` を同時に
   extends できない (Java単一継承)。認証付きHTMXコントローラはComposition パターンで解決する。

3. **quarkus-smallrye-jwt vs quarkus-oidc**: 同一アプリで同じ認証メカニズムには
   併用不可。REST API向けにsmallrye-jwtを使用し、将来のOIDC対応時に分離する。

## 3. SNS対応ライブラリ

### 3.1 既存ライブラリを使用するSNS

| SNS | ライブラリ | Maven座標 | バージョン | 状態 |
|-----|----------|----------|-----------|------|
| Mastodon | BigBone | `io.github.pattafeufeu:bigbone` | 2.x | 活発にメンテナンス |
| Instagram | RestFB | `com.restfb:restfb` | 2025.x | 活発 (Graph API対応) |
| Threads | RestFB | `com.restfb:restfb` | 同上 | Meta Graph API共用 |
| Telegram | TelegramBots | `org.telegram:telegrambots-client` | 9.4.0 | 活発 |
| VK | vk-java-sdk | `com.vk.api:sdk` | 1.0.14 | VK公式 |
| Pinterest | pinterest-sdk | `com.chrisdempewolf:pinterest-sdk` | 4.1.0 | コミュニティ |

### 3.2 REST Clientで直接実装するSNS

以下のSNSは良質なJavaライブラリが存在しない/メンテナンスされていないため、
Quarkus REST Client (MicroProfile REST Client) で公式APIを直接呼び出す。

| SNS | 理由 | API仕様 |
|-----|------|---------|
| X (Twitter) | twitter4j は2022年以降メンテナンスなし、API v2非対応 | Twitter API v2 (OAuth 2.0 PKCE) |
| Bluesky | bsky4j はJitPack限定、Maven Central未公開 | AT Protocol (XRPC) |
| Reddit | JRAW は2018年以降メンテナンスなし | Reddit API (OAuth 2.0) |

### 3.3 設計方針

- **既存ライブラリ優先**: メンテナンスされているライブラリがあれば積極的に採用
- **REST Client統一**: ライブラリがない場合はQuarkus REST Clientインターフェースで実装
- **SPI (ServiceLoader)**: SNSアダプターはJava SPIで動的ロード、JARを追加するだけで新SNS対応可能
- **共通インターフェース**: 全SNSアダプターが同じドメインインターフェースを実装

## 4. アーキテクチャ

### 4.1 ヘキサゴナルアーキテクチャ

```
┌──────────────────────────────────────────────────┐
│                  Inbound Adapters                │
│  ┌────────────┐  ┌──────────┐  ┌─────────────┐  │
│  │ Renarde/   │  │ REST API │  │ SQS Consumer│  │
│  │ HTMX UI    │  │ (外部)   │  │ (Lambda)    │  │
│  └─────┬──────┘  └────┬─────┘  └──────┬──────┘  │
├────────┴──────────────┴────────────────┴─────────┤
│                Application Layer                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Auth     │ │ Post     │ │ Analytics        │ │
│  │ UseCase  │ │ UseCase  │ │ UseCase          │ │
│  └──────────┘ └──────────┘ └──────────────────┘ │
├──────────────────────────────────────────────────┤
│                  Domain Layer                    │
│  ┌──────────────────────────────────────┐        │
│  │ SnsAdapter (interface)               │        │
│  │  - post(PostRequest): PostResult     │        │
│  │  - getTimeline(): List<Post>         │        │
│  │  - getProfile(): Profile             │        │
│  │  - getAnalytics(): Analytics         │        │
│  │  - authenticate(): AuthToken         │        │
│  └──────────────────────────────────────┘        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐         │
│  │ User     │ │ Post     │ │ Sns      │         │
│  │ Entity   │ │ Entity   │ │ Account  │         │
│  └──────────┘ └──────────┘ └──────────┘         │
├──────────────────────────────────────────────────┤
│                Outbound Adapters                 │
│  ┌─────┐┌────┐┌─────┐┌──────┐┌───┐┌──────────┐ │
│  │  X  ││ BS ││Mstd ││ Thrd ││...││ DB/SQS   │ │
│  │     ││    ││     ││      ││   ││ Adapter  │ │
│  └─────┘└────┘└─────┘└──────┘└───┘└──────────┘ │
└──────────────────────────────────────────────────┘
```

### 4.2 Gradleマルチモジュール構成

```
akazukin/
├── settings.gradle.kts
├── build.gradle.kts                    # ルート (共通設定)
│
├── akazukin-domain/                    # ドメイン層 (純粋Java, フレームワーク非依存)
│   ├── build.gradle.kts
│   └── src/main/java/
│       └── com/akazukin/domain/
│           ├── model/                  # エンティティ・値オブジェクト
│           │   ├── User.java
│           │   ├── SnsAccount.java
│           │   ├── Post.java
│           │   ├── PostRequest.java
│           │   ├── PostResult.java
│           │   ├── Profile.java
│           │   ├── Analytics.java
│           │   └── SnsPlatform.java    # enum: X, BLUESKY, MASTODON, ...
│           ├── port/                   # ポート (インターフェース)
│           │   ├── SnsAdapter.java     # SNS共通インターフェース
│           │   ├── UserRepository.java
│           │   ├── PostRepository.java
│           │   └── SnsAccountRepository.java
│           └── service/                # ドメインサービス
│               ├── PostService.java
│               └── AccountService.java
│
├── akazukin-application/               # アプリケーション層 (ユースケース)
│   ├── build.gradle.kts
│   └── src/main/java/
│       └── com/akazukin/application/
│           ├── usecase/
│           │   ├── AuthUseCase.java
│           │   ├── PostUseCase.java
│           │   ├── SchedulePostUseCase.java
│           │   └── AnalyticsUseCase.java
│           └── dto/
│               ├── PostRequestDto.java
│               └── PostResponseDto.java
│
├── akazukin-infrastructure/            # インフラ層 (DB・永続化)
│   ├── build.gradle.kts
│   └── src/main/java/
│       └── com/akazukin/infrastructure/
│           ├── persistence/
│           │   ├── entity/             # JPA Panacheエンティティ
│           │   ├── repository/         # Panacheリポジトリ実装
│           │   └── mapper/             # Domain <-> JPA変換
│           └── queue/
│               └── SqsPostPublisher.java
│
├── akazukin-adapter-sns/               # SNSアダプター親モジュール
│   ├── akazukin-adapter-core/          # SNSアダプター共通ユーティリティ
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── com/akazukin/adapter/core/
│   │           ├── SnsAdapterFactory.java      # SPI ServiceLoader
│   │           ├── AbstractSnsAdapter.java     # 共通処理
│   │           └── RateLimiter.java            # レートリミット管理
│   │
│   ├── akazukin-adapter-twitter/       # X (Twitter) - REST Client実装
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── com/akazukin/adapter/twitter/
│   │           ├── TwitterAdapter.java
│   │           ├── TwitterApiClient.java       # Quarkus REST Client
│   │           └── META-INF/services/          # SPI登録
│   │
│   ├── akazukin-adapter-bluesky/       # Bluesky - REST Client実装
│   ├── akazukin-adapter-mastodon/      # Mastodon - BigBone使用
│   ├── akazukin-adapter-threads/       # Threads - RestFB使用
│   ├── akazukin-adapter-instagram/     # Instagram - RestFB使用
│   ├── akazukin-adapter-reddit/        # Reddit - REST Client実装
│   ├── akazukin-adapter-telegram/      # Telegram - TelegramBots使用
│   ├── akazukin-adapter-vk/            # VK - vk-java-sdk使用
│   └── akazukin-adapter-pinterest/     # Pinterest - pinterest-sdk使用
│
├── akazukin-web/                       # Webアプリケーション (Renarde + HTMX)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/
│       │   └── com/akazukin/web/
│       │       ├── controller/         # Renardeコントローラ
│       │       │   ├── LoginController.java
│       │       │   ├── DashboardController.java
│       │       │   └── PostController.java
│       │       ├── api/                # REST API (外部向け)
│       │       │   ├── AuthResource.java
│       │       │   ├── PostResource.java
│       │       │   └── AccountResource.java
│       │       ├── security/
│       │       │   ├── JwtTokenService.java
│       │       │   └── AuthFilter.java
│       │       └── scheduler/
│       │           └── ScheduledPostJob.java
│       └── main/resources/
│           ├── application.properties
│           └── templates/              # Quteテンプレート
│               ├── base.html           # ベースレイアウト (HTMX読み込み)
│               ├── login.html
│               ├── dashboard.html
│               └── post/
│                   ├── compose.html    # 投稿作成
│                   └── list.html       # 投稿一覧
│
└── docs/
    └── DESIGN.md                       # この設計書
```

## 5. ドメインモデル

### 5.1 主要エンティティ

```
User
├── id: UUID
├── username: String
├── email: String
├── passwordHash: String
├── role: Role (ADMIN, USER, VIEWER)  ※将来用、当面はADMINのみ
├── createdAt: Instant
└── updatedAt: Instant

SnsAccount
├── id: UUID
├── userId: UUID (FK -> User)
├── platform: SnsPlatform (enum)
├── accountIdentifier: String (プラットフォーム上のID)
├── displayName: String
├── accessToken: String (暗号化)
├── refreshToken: String (暗号化)
├── tokenExpiresAt: Instant
├── createdAt: Instant
└── updatedAt: Instant

Post
├── id: UUID
├── userId: UUID (FK -> User)
├── content: String
├── mediaUrls: List<String>
├── status: PostStatus (DRAFT, SCHEDULED, PUBLISHING, PUBLISHED, FAILED)
├── scheduledAt: Instant (null = 即時投稿)
├── createdAt: Instant
└── updatedAt: Instant

PostTarget (投稿先SNS)
├── id: UUID
├── postId: UUID (FK -> Post)
├── snsAccountId: UUID (FK -> SnsAccount)
├── platformPostId: String (投稿後のプラットフォーム側ID)
├── status: PostStatus
├── errorMessage: String
├── publishedAt: Instant
└── createdAt: Instant
```

### 5.2 ER図

```
User 1──* SnsAccount
User 1──* Post
Post 1──* PostTarget
SnsAccount 1──* PostTarget
```

## 6. SNSアダプター共通インターフェース

```java
public interface SnsAdapter {

    /** 対応プラットフォームを返す */
    SnsPlatform platform();

    /** OAuth認可URLを生成 */
    String getAuthorizationUrl(String callbackUrl, String state);

    /** コールバックからトークンを取得 */
    SnsAuthToken exchangeToken(String code, String callbackUrl);

    /** トークンのリフレッシュ */
    SnsAuthToken refreshToken(String refreshToken);

    /** プロフィール取得 */
    SnsProfile getProfile(String accessToken);

    /** テキスト投稿 */
    SnsPostResult post(String accessToken, SnsPostRequest request);

    /** タイムライン取得 */
    List<SnsPost> getTimeline(String accessToken, int limit);

    /** 投稿削除 */
    void deletePost(String accessToken, String postId);
}
```

## 7. API設計

### 7.1 認証 API

| Method | Path | 説明 |
|--------|------|------|
| POST | `/api/v1/auth/register` | ユーザー登録 |
| POST | `/api/v1/auth/login` | ログイン (JWT発行) |
| POST | `/api/v1/auth/refresh` | トークンリフレッシュ |

### 7.2 SNSアカウント API

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/v1/accounts` | 連携済みアカウント一覧 |
| GET | `/api/v1/accounts/{platform}/auth` | OAuth認可URL取得 |
| POST | `/api/v1/accounts/{platform}/callback` | OAuthコールバック処理 |
| DELETE | `/api/v1/accounts/{id}` | アカウント連携解除 |

### 7.3 投稿 API

| Method | Path | 説明 |
|--------|------|------|
| POST | `/api/v1/posts` | 投稿作成 (即時 or 予約) |
| GET | `/api/v1/posts` | 投稿一覧 |
| GET | `/api/v1/posts/{id}` | 投稿詳細 |
| PUT | `/api/v1/posts/{id}` | 投稿編集 (下書き・予約のみ) |
| DELETE | `/api/v1/posts/{id}` | 投稿削除 |

### 7.4 ダッシュボード API

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/v1/dashboard/summary` | 全アカウントサマリー |
| GET | `/api/v1/dashboard/timeline` | 統合タイムライン |

## 8. 認証フロー

### 8.1 ユーザー認証 (JWT)

```
1. POST /api/v1/auth/login { username, password }
2. → JWTアクセストークン (15分) + リフレッシュトークン (7日) 返却
3. 以降のAPIリクエストに Authorization: Bearer <jwt> ヘッダ付与
4. 期限切れ → POST /api/v1/auth/refresh でトークン更新
```

### 8.2 SNS OAuth連携

```
1. GET /api/v1/accounts/{platform}/auth → 認可URLリダイレクト
2. ユーザーがSNS側で認可
3. コールバック → POST /api/v1/accounts/{platform}/callback
4. アクセストークン取得 → 暗号化してDB保存
5. トークンリフレッシュはバックグラウンドジョブで自動実行
```

## 9. 予約投稿フロー

```
1. ユーザーが投稿作成 (scheduledAt指定)
2. Post (status=SCHEDULED) としてDB保存
3. EventBridge Scheduler にワンタイムスケジュール登録
   (※ LambdaではQuartz/@Scheduledは動作しないため)
4. 予約時刻到来 → EventBridge が SQS にメッセージ送信
5. Lambda (SQSコンシューマー) が各SNSアダプター経由で投稿
6. 結果をPostTarget.statusに反映
7. 失敗時 → SQS DLQ + リトライ (最大3回、Exponential Backoff)
```

## 10. AWS構成

```
                    ┌─────────────┐
                    │ CloudFront  │
                    └──────┬──────┘
                           │
                    ┌──────┴──────┐
                    │ API Gateway │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │    Lambda (Quarkus)     │
              │  - Web UI (Renarde)    │
              │  - REST API            │
              └────────────┬────────────┘
                           │
    ┌──────────┬───────────┴──────────┬──────────┐
    │          │                      │          │
┌───┴────┐ ┌──┴──────────┐    ┌──────┴──┐  ┌───┴────┐
│ Aurora │ │ EventBridge │    │DynamoDB │  │Secrets │
│Serverl.│ │ Scheduler   │    │         │  │Manager │
└────────┘ └──────┬──────┘    └─────────┘  └────────┘
                  │ (予約時刻到来)
            ┌─────┴─────┐
            │    SQS    │
            └─────┬─────┘
            ┌─────┴─────┐
            │  Lambda   │
            │(SQS消費者)│
            │ 投稿実行  │
            └───────────┘
```

## 11. 開発フェーズ

### Phase 1: MVP (今回の実装範囲)
- [ ] プロジェクト骨格 (Gradle マルチモジュール)
- [ ] ユーザー登録・ログイン (JWT)
- [ ] ドメインモデル・DB永続化
- [ ] SNSアダプターSPI基盤
- [ ] 投稿機能 (即時投稿)
- [ ] HTMX ダッシュボード・投稿画面
- [ ] REST API

### Phase 2: 予約投稿・バックグラウンド
- [ ] EventBridge Scheduler 連携
- [ ] SQS連携
- [ ] 予約投稿フロー

### Phase 3: アナリティクス
- [ ] 各SNSからの統計情報取得
- [ ] ダッシュボードグラフ表示

### Phase 4: マルチユーザー
- [ ] 権限管理 (Role)
- [ ] OAuth2/OIDCプロバイダー対応
- [ ] チーム管理

## 12. 主要な依存関係 (Quarkus Extensions)

```kotlin
// build.gradle.kts (akazukin-web)
dependencies {
    // Quarkus Core
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.x"))

    // Web / HTMX
    implementation("io.quarkiverse.renarde:quarkus-renarde")
    implementation("io.quarkiverse.web-bundler:quarkus-web-bundler") // htmx等のアセット管理
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // HTMX (Maven経由で管理、Node.js不要)
    implementation("org.mvnpm:htmx.org:2.0.8")

    // Security
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")

    // Database
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")             // DBマイグレーション

    // REST Client (SNS API呼び出し用)
    implementation("io.quarkus:quarkus-rest-client-jackson")

    // AWS
    implementation("io.quarkus:quarkus-amazon-lambda-http")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-dynamodb")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-secretsmanager")

    // Utility
    implementation("io.quarkus:quarkus-hibernate-validator") // Bean Validation

    // Dev
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}
```
