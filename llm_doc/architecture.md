# アーキテクチャ・モジュール構成

## アーキテクチャ原則
- ヘキサゴナルアーキテクチャ (Ports & Adapters)
- ドメイン層はフレームワーク非依存 (純粋Java)
- SNSアダプターはJava SPI (ServiceLoader) で動的ロード
- 3層分離: SDK (HTTP通信) → Adapter (ドメイン変換) → Domain (共通モデル)

## モジュール構成と依存方向
```
akazukin-web        → akazukin-application → akazukin-domain ← akazukin-infrastructure
                    → akazukin-adapter-*   → akazukin-domain
akazukin-adapter-*  → akazukin-sdk-*  (独自SDKの場合)
                    → 外部ライブラリ   (既存ライブラリの場合)
```
- 依存は常に **内側 (Domain) に向かう**。Domain が外側に依存してはならない
- `akazukin-domain` は他のモジュールに依存しない
- `akazukin-sdk-*` は Quarkus 非依存 (java.net.http.HttpClient + Jackson のみ)

## フォルダ構成

```
akazukin/
├── CLAUDE.md
├── settings.gradle.kts
├── build.gradle.kts                          # ルート: Java 21, 共通プラグイン
├── gradle.properties                         # Quarkus BOM バージョン等
│
├── docs/
│   ├── DESIGN.md
│   └── diagrams/                             # Draw.io 図
│
├── akazukin-domain/                          # ★ドメイン層 (純粋Java, 依存なし)
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/akazukin/domain/
│       │   ├── model/
│       │   │   ├── User.java                 # ドメインエンティティ
│       │   │   ├── SnsAccount.java
│       │   │   ├── Post.java
│       │   │   ├── PostTarget.java
│       │   │   ├── PostRequest.java          # 値オブジェクト (record)
│       │   │   ├── PostResult.java           # 値オブジェクト (record)
│       │   │   ├── SnsProfile.java           # 値オブジェクト (record)
│       │   │   ├── SnsAuthToken.java         # 値オブジェクト (record)
│       │   │   ├── SnsPlatform.java          # enum: X, BLUESKY, MASTODON, ...
│       │   │   ├── PostStatus.java           # enum: DRAFT, SCHEDULED, PUBLISHED, FAILED
│       │   │   └── Role.java                 # enum: ADMIN, USER, VIEWER
│       │   ├── port/
│       │   │   ├── SnsAdapter.java           # SNS共通インターフェース
│       │   │   ├── UserRepository.java       # リポジトリインターフェース
│       │   │   ├── PostRepository.java
│       │   │   ├── PostTargetRepository.java
│       │   │   └── SnsAccountRepository.java
│       │   ├── exception/
│       │   │   ├── DomainException.java      # 業務エラー基底クラス
│       │   │   ├── PostNotFoundException.java
│       │   │   ├── AccountNotFoundException.java
│       │   │   └── SnsApiException.java      # SNS API呼び出しエラー
│       │   └── service/
│       │       ├── PostService.java          # ドメインサービス
│       │       └── AccountService.java
│       └── test/java/com/akazukin/domain/
│           ├── model/
│           └── service/
│
├── akazukin-application/                     # ★アプリケーション層 (ユースケース)
│   ├── build.gradle.kts                      # depends on: akazukin-domain
│   └── src/
│       ├── main/java/com/akazukin/application/
│       │   ├── usecase/
│       │   │   ├── AuthUseCase.java
│       │   │   ├── PostUseCase.java
│       │   │   ├── SchedulePostUseCase.java
│       │   │   ├── AccountUseCase.java
│       │   │   └── AnalyticsUseCase.java
│       │   └── dto/
│       │       ├── LoginRequestDto.java      # record
│       │       ├── LoginResponseDto.java     # record
│       │       ├── RegisterRequestDto.java   # record
│       │       ├── PostRequestDto.java       # record
│       │       ├── PostResponseDto.java      # record
│       │       ├── AccountResponseDto.java   # record
│       │       └── ErrorResponseDto.java     # record (統一エラーフォーマット)
│       └── test/java/com/akazukin/application/
│           └── usecase/
│
├── akazukin-infrastructure/                  # ★インフラ層 (DB, キュー)
│   ├── build.gradle.kts                      # depends on: akazukin-domain, Quarkus Panache, Flyway
│   └── src/
│       ├── main/java/com/akazukin/infrastructure/
│       │   ├── persistence/
│       │   │   ├── entity/                   # JPA Panache エンティティ
│       │   │   │   ├── UserEntity.java
│       │   │   │   ├── SnsAccountEntity.java
│       │   │   │   ├── PostEntity.java
│       │   │   │   └── PostTargetEntity.java
│       │   │   ├── repository/               # Panache リポジトリ実装
│       │   │   │   ├── UserRepositoryImpl.java
│       │   │   │   ├── PostRepositoryImpl.java
│       │   │   │   ├── PostTargetRepositoryImpl.java
│       │   │   │   └── SnsAccountRepositoryImpl.java
│       │   │   └── mapper/                   # Domain ↔ JPA 変換
│       │   │       ├── UserMapper.java
│       │   │       ├── PostMapper.java
│       │   │       └── SnsAccountMapper.java
│       │   ├── queue/
│       │   │   └── SqsPostPublisher.java
│       │   └── crypto/
│       │       └── TokenEncryptor.java       # アクセストークン暗号化
│       ├── main/resources/
│       │   └── db/migration/
│       │       ├── V001__create_users.sql
│       │       ├── V002__create_sns_accounts.sql
│       │       ├── V003__create_posts.sql
│       │       └── V004__create_post_targets.sql
│       └── test/java/com/akazukin/infrastructure/
│           └── persistence/repository/
│
├── akazukin-sdk/                             # ★独自SDK群 (Quarkus非依存)
│   ├── akazukin-sdk-twitter/
│   │   ├── build.gradle.kts                  # depends on: Jackson のみ
│   │   └── src/
│   │       ├── main/java/com/akazukin/sdk/twitter/
│   │       │   ├── TwitterClient.java        # Builder パターン
│   │       │   ├── TwitterConfig.java        # record (apiKey, apiSecret等)
│   │       │   ├── auth/
│   │       │   │   └── OAuth2PkceFlow.java
│   │       │   ├── model/
│   │       │   │   ├── Tweet.java            # record
│   │       │   │   ├── TwitterUser.java      # record
│   │       │   │   └── TwitterTimeline.java  # record
│   │       │   ├── api/
│   │       │   │   ├── TweetApi.java
│   │       │   │   ├── UserApi.java
│   │       │   │   └── TimelineApi.java
│   │       │   └── exception/
│   │       │       └── TwitterApiException.java
│   │       └── test/java/com/akazukin/sdk/twitter/
│   │
│   ├── akazukin-sdk-bluesky/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/akazukin/sdk/bluesky/
│   │       ├── BlueskyClient.java
│   │       ├── BlueskyConfig.java
│   │       ├── auth/
│   │       ├── model/
│   │       ├── api/
│   │       └── exception/
│   │
│   ├── akazukin-sdk-reddit/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/akazukin/sdk/reddit/
│   │       ├── RedditClient.java
│   │       ├── RedditConfig.java
│   │       ├── auth/
│   │       ├── model/
│   │       ├── api/
│   │       └── exception/
│   │
│   └── akazukin-sdk-pinterest/
│       ├── build.gradle.kts
│       └── src/main/java/com/akazukin/sdk/pinterest/
│           ├── PinterestClient.java
│           ├── PinterestConfig.java
│           ├── auth/
│           ├── model/
│           ├── api/
│           └── exception/
│
├── akazukin-adapter-sns/                     # ★SNSアダプター群 (ドメイン変換層)
│   ├── akazukin-adapter-core/
│   │   ├── build.gradle.kts                  # depends on: akazukin-domain
│   │   └── src/main/java/com/akazukin/adapter/core/
│   │       ├── SnsAdapterFactory.java        # SPI ServiceLoader
│   │       ├── AbstractSnsAdapter.java       # 共通テンプレートメソッド
│   │       └── RateLimiter.java              # レートリミット管理
│   │
│   ├── akazukin-adapter-twitter/
│   │   ├── build.gradle.kts                  # depends on: adapter-core, sdk-twitter
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/twitter/
│   │       │   └── TwitterAdapter.java       # SnsAdapter 実装
│   │       └── resources/META-INF/services/
│   │           └── com.akazukin.domain.port.SnsAdapter  # SPI登録
│   │
│   ├── akazukin-adapter-bluesky/             # depends on: adapter-core, sdk-bluesky
│   ├── akazukin-adapter-mastodon/            # depends on: adapter-core, BigBone
│   ├── akazukin-adapter-threads/             # depends on: adapter-core, RestFB
│   ├── akazukin-adapter-instagram/           # depends on: adapter-core, RestFB
│   ├── akazukin-adapter-reddit/              # depends on: adapter-core, sdk-reddit
│   ├── akazukin-adapter-telegram/            # depends on: adapter-core, TelegramBots
│   ├── akazukin-adapter-vk/                  # depends on: adapter-core, vk-java-sdk
│   └── akazukin-adapter-pinterest/           # depends on: adapter-core, sdk-pinterest
│
└── akazukin-web/                             # ★Webアプリケーション (エントリポイント)
    ├── build.gradle.kts                      # depends on: application, infrastructure, adapter-*, Quarkus
    └── src/
        ├── main/java/com/akazukin/web/
        │   ├── controller/                   # Renarde コントローラ (HTMX)
        │   ├── api/                          # REST API (外部クライアント向け)
        │   ├── security/
        │   ├── error/
        │   └── config/
        ├── main/resources/
        │   ├── application.properties
        │   ├── application-dev.properties
        │   ├── application-prod.properties
        │   ├── web/app/                      # JS, CSS
        │   └── templates/                    # Qute テンプレート
        └── test/java/com/akazukin/web/
```
