# Akazukin 開発ガイドライン

## プロジェクト概要
- 複数SNS一元管理ツール (X, Bluesky, Mastodon, Threads, Instagram, Reddit, Telegram, VK, Pinterest)
- Java 21 + Quarkus + Renarde/HTMX + PostgreSQL + AWS Lambda

## ビルド・実行コマンド
- `./gradlew build` — 全モジュールビルド
- `./gradlew :akazukin-web:quarkusDev` — 開発モード起動
- `./gradlew test` — 全テスト実行
- `./gradlew :akazukin-web:test` — Webモジュールのテストのみ

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
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/bluesky/
│   │       │   └── BlueskyAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   ├── akazukin-adapter-mastodon/            # depends on: adapter-core, BigBone
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/mastodon/
│   │       │   └── MastodonAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   ├── akazukin-adapter-threads/             # depends on: adapter-core, RestFB
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/threads/
│   │       │   └── ThreadsAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   ├── akazukin-adapter-instagram/           # depends on: adapter-core, RestFB
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/instagram/
│   │       │   └── InstagramAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   ├── akazukin-adapter-reddit/              # depends on: adapter-core, sdk-reddit
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/reddit/
│   │       │   └── RedditAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   ├── akazukin-adapter-telegram/            # depends on: adapter-core, TelegramBots
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/telegram/
│   │       │   └── TelegramAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   ├── akazukin-adapter-vk/                  # depends on: adapter-core, vk-java-sdk
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── java/com/akazukin/adapter/vk/
│   │       │   └── VkAdapter.java
│   │       └── resources/META-INF/services/
│   │
│   └── akazukin-adapter-pinterest/           # depends on: adapter-core, sdk-pinterest
│       ├── build.gradle.kts
│       └── src/main/
│           ├── java/com/akazukin/adapter/pinterest/
│           │   └── PinterestAdapter.java
│           └── resources/META-INF/services/
│
└── akazukin-web/                             # ★Webアプリケーション (エントリポイント)
    ├── build.gradle.kts                      # depends on: application, infrastructure, adapter-*, Quarkus
    └── src/
        ├── main/java/com/akazukin/web/
        │   ├── controller/                   # Renarde コントローラ (HTMX)
        │   │   ├── LoginController.java
        │   │   ├── DashboardController.java
        │   │   ├── PostController.java
        │   │   └── AccountController.java
        │   ├── api/                          # REST API (外部クライアント向け)
        │   │   ├── AuthResource.java
        │   │   ├── PostResource.java
        │   │   ├── AccountResource.java
        │   │   └── DashboardResource.java
        │   ├── security/
        │   │   ├── JwtTokenService.java      # JWT生成・検証
        │   │   ├── PasswordHasher.java       # BCrypt
        │   │   └── SecurityFilter.java
        │   ├── error/
        │   │   ├── GlobalExceptionHandler.java   # JAX-RS ExceptionMapper
        │   │   ├── ApiErrorMapper.java           # REST API用
        │   │   └── WebErrorMapper.java           # HTMX UI用
        │   └── config/
        │       └── AppConfig.java
        ├── main/resources/
        │   ├── application.properties        # Quarkus設定
        │   ├── application-dev.properties    # 開発環境設定
        │   ├── application-prod.properties   # 本番環境設定
        │   ├── publicKey.pem                 # JWT検証用公開鍵
        │   ├── privateKey.pem                # JWT署名用秘密鍵
        │   ├── web/app/
        │   │   ├── main.js                   # Web Bundler エントリポイント
        │   │   └── styles.css                # カスタムCSS
        │   └── templates/                    # Qute テンプレート
        │       ├── base.html                 # ベースレイアウト (HTMX, CSS読み込み)
        │       ├── error.html                # エラーページ
        │       ├── LoginController/
        │       │   ├── index.html            # ログインフォーム
        │       │   └── register.html         # ユーザー登録フォーム
        │       ├── DashboardController/
        │       │   └── index.html            # ダッシュボード
        │       ├── PostController/
        │       │   ├── compose.html          # 投稿作成フォーム
        │       │   ├── list.html             # 投稿一覧
        │       │   └── detail.html           # 投稿詳細
        │       └── AccountController/
        │           ├── list.html             # 連携アカウント一覧
        │           └── connect.html          # SNSアカウント連携
        └── test/java/com/akazukin/web/
            ├── api/
            └── controller/
```

## ファイル命名規約
- エンティティ: `User.java`, `Post.java` (名詞)
- リポジトリ: `UserRepository.java` (interface in domain), `UserRepositoryImpl.java` (impl in infrastructure)
- ユースケース: `PostUseCase.java` (動詞+名詞)
- アダプター: `TwitterAdapter.java` (プラットフォーム名+Adapter)
- SDK クライアント: `TwitterClient.java` (プラットフォーム名+Client)
- コントローラ: `PostController.java` (Renarde), `PostResource.java` (REST API)
- DTO: `PostRequestDto.java`, `PostResponseDto.java` (全て record)
- JPA エンティティ: `UserEntity.java` (ドメインモデルとの名前衝突を避ける)
- マッパー: `UserMapper.java` (Domain ↔ JPA 変換)
- 例外: `TwitterApiException.java`, `PostNotFoundException.java`
- SPI登録: `META-INF/services/com.akazukin.domain.port.SnsAdapter`
- DBマイグレーション: `V{3桁番号}__{説明}.sql` (例: `V001__create_users.sql`)
- テスト: `PostUseCaseTest.java`, `TwitterAdapterTest.java`
- Quteテンプレート: `{ControllerName}/{methodName}.html`
- 設定: `application-{profile}.properties`

## エラーハンドリング方針

### 絶対に守ること
- **エラーをもみ消さない** — catch して何もしない、ログだけ出して握りつぶす、は禁止
- **フォールバックしない** — 失敗をデフォルト値で隠すとユーザー誤認になる
- **エラーはユーザーに見せる** — 何が起きたか、どう対処すべきかを明確に伝える
- **空の catch ブロック禁止** — 必ず再スローまたはエラーレスポンスを返す

### エラーハンドリングパターン
- ドメイン例外を定義して使い分ける (業務エラー vs システムエラー)
- REST API はHTTPステータスコード + エラーレスポンスJSON を統一フォーマットで返す
- HTMX UIはエラー時にユーザーフレンドリーなメッセージを表示する
- SNS APIエラーはプラットフォーム名 + エラーコード + メッセージをそのまま伝搬
- バリデーションエラーはフィールド単位で返す

### エラーレスポンス統一フォーマット
```json
{
  "error": "POST_FAILED",
  "message": "X (Twitter) への投稿に失敗しました",
  "details": "Rate limit exceeded. Retry after 900 seconds.",
  "timestamp": "2026-03-13T12:00:00Z"
}
```

### ログ戦略
- ERROR: ユーザー影響あり、即座に対応が必要
- WARN: 一時的な問題、リトライで復旧の可能性
- INFO: 重要な業務イベント (投稿成功、アカウント連携等)
- DEBUG: 開発時のトラブルシューティング用

## UI/UX戦略

### フレキシブルUI原則
- **レスポンシブデザイン**: モバイル・タブレット・デスクトップ全対応
- **CSSカスタムプロパティ**: 色・間隔・フォントサイズをCSS変数で管理。テーマ変更を容易に
- **コンポーネント分割**: Quteの `{#include}` / `{#insert}` でUIパーツを再利用可能にする
- **レイアウトの柔軟性**: CSS Grid / Flexbox ベース。固定幅ピクセル指定を避ける
- **ダークモード対応**: `prefers-color-scheme` メディアクエリ + CSS変数で切り替え
- **国際化準備**: ハードコードされた日本語文字列をテンプレートに直接書かない。将来のi18n対応に備える

### HTMX ベストプラクティス
- **プログレッシブエンハンスメント**: JS無効でも基本機能は動作させる
- **部分更新**: ページ全体をリロードせず、変更箇所だけ差し替え (`hx-swap`)
- **ローディング表示**: `hx-indicator` で処理中を明示する
- **楽観的UI禁止**: サーバーの応答を待ってからUIを更新する (データ不整合防止)
- **エラー状態の可視化**: `hx-target-error` でエラー時の表示先を分ける
- **URL同期**: `hx-push-url` でブラウザ履歴を適切に管理する

### フォーム設計
- サーバーサイドバリデーション必須 (クライアント側は補助のみ)
- バリデーションエラーはフィールドの横にインラインで表示
- 送信ボタンは処理中にdisabledにする (二重送信防止)
- 成功時はフラッシュメッセージで通知

### ダッシュボード設計
- 一覧は最新順をデフォルト
- ページネーションは無限スクロールではなくページ番号方式 (状態が明確)
- フィルタ・検索はURLパラメータに反映 (ブックマーク・共有可能)
- データがない状態 (Empty State) を必ずデザインする
- ウィジェット型レイアウト: 将来的にユーザーが配置をカスタマイズ可能にする

### レスポンス速度
- サーバーサイドレンダリング: 目標200ms以内
- SNS API呼び出し: タイムアウト10秒、ユーザーにはスピナー表示
- 長時間処理 (一括投稿等): 非同期化してポーリングまたはSSEで進捗通知

## パフォーマンス・メモリ・Disk IO

### パフォーマンス
- **N+1クエリ防止**: `JOIN FETCH` または `@NamedEntityGraph` で関連を一括取得
- **ページネーション必須**: 一覧取得は必ず `LIMIT`/`OFFSET` または keyset pagination
- **不要なデータを取得しない**: `SELECT` で必要カラムのみ。DTOプロジェクションを使う
- **インデックス設計**: WHERE句、ORDER BY句、JOIN条件に使うカラムにインデックス
- **HTTPキャッシュ**: 静的アセットに `Cache-Control` ヘッダ。Quteテンプレートはビルド時コンパイル
- **Lambda コールドスタート対策**: 不要な依存を減らす。SnapStart の検討

### メモリリーク防止
- **HttpClient の再利用**: `java.net.http.HttpClient` はシングルトンで共有。リクエスト毎に生成しない
- **ストリームの確実なクローズ**: try-with-resources を必ず使う。InputStream, Connection 等
- **大量データの一括ロード禁止**: `findAll()` をページネーションなしで呼ばない
- **コレクションの無限成長防止**: キャッシュにはサイズ上限 + TTL を設定
- **イベントリスナーの解除**: 登録したリスナーは不要になったら解除する
- **Lambda環境のメモリ**: ヒープサイズを意識する。Lambda のメモリ設定に合わせたチューニング

### Disk IO
- **一時ファイルの確実な削除**: try-with-resources + `Files.deleteIfExists()` で後始末
- **ファイルアップロード**: メモリに全量読み込まない。ストリーミング処理する
- **ログローテーション**: ログファイルが際限なく成長しない設定にする
- **Lambda の /tmp 制限**: Lambda の一時ストレージは 512MB (デフォルト)。大きなファイルはS3を使う
- **バッファサイズ**: IO操作にはバッファを使う。デフォルトのバッファサイズ (8KB) を意識する

### SDK (HttpClient) パフォーマンス
- **コネクションプール**: HttpClient は内部でコネクションプールを持つ。インスタンスを再利用する
- **タイムアウト設定**: 接続タイムアウト 5秒、読み取りタイムアウト 10秒 を必ず設定
- **レスポンスボディの読み切り**: レスポンスを途中で放棄しない。コネクション再利用のため
- **非同期API**: 大量のSNS API呼び出しは `HttpClient.sendAsync()` で並行実行

## 回避すべきアンチパターン

### アーキテクチャ系
- **God Class**: 1クラスに責務を詰め込みすぎない。1クラス1責務
- **循環依存**: モジュール間の循環依存禁止。依存は常にDomain方向へ
- **Leaky Abstraction**: SNS固有の概念をドメイン層に漏らさない
- **Big Ball of Mud**: パッケージ/モジュールの境界を曖昧にしない
- **Service Locator乱用**: SPI以外でServiceLocatorパターンを使わない。DIを使う
- **Anemic Domain Model**: ドメインモデルにビジネスロジックを持たせる。getter/setterだけのモデルにしない

### コーディング系
- **Pokemon Exception Handling**: `catch (Exception e)` で全てキャッチしない
- **Swallowed Exception**: 例外を握りつぶさない
- **Magic Number/String**: 定数を定義して使う
- **Premature Optimization**: 計測なしの最適化をしない
- **Copy-Paste Programming**: 共通処理は抽出する (ただし過度な抽象化も避ける)
- **Null返却**: Optional を使う。null を返さない
- **Mutable Shared State**: 可変共有状態を避ける。record や不変オブジェクトを優先

### API設計系
- **Chatty API**: 1操作に複数リクエストが必要な設計にしない
- **Inconsistent Naming**: エンドポイントの命名規則を統一 (RESTful)
- **Missing Pagination**: 一覧系APIは必ずページネーション対応
- **Silent Failure**: APIエラー時に200を返さない。適切なHTTPステータスコードを使う
- **Over-fetching**: 不要なデータを返さない。必要に応じてフィールド選択可能に

### DB/永続化系
- **N+1 Problem**: リレーション取得でN+1クエリを発生させない
- **Missing Index**: 検索条件のカラムにインデックスを忘れない
- **God Table**: 1テーブルに全てを入れない。正規化する
- **Soft Delete乱用**: 本当に必要な場合のみ。基本はハードデリート + 監査ログ

### SNSアダプター系
- **Rate Limit無視**: 各SNSのレートリミットを尊重。429を受けたらRetry-After待機
- **Token平文保存**: アクセストークンは必ず暗号化してDB保存
- **同期的な一括投稿**: 複数SNSへの投稿は非同期キュー経由。1つの失敗が他に影響しない
- **エラー翻訳**: SNS APIの生エラーメッセージを変に翻訳・省略しない。原文を保持

### テスト系
- **Test Pollution**: テスト間でデータを共有しない。各テストは独立
- **Flaky Test**: 外部API依存のテストはモック化。タイミング依存を排除
- **Missing Edge Cases**: 正常系だけでなく、エラー系・境界値のテストも書く

## コードスタイル
- Java標準のコーディングスタイルに従う
- インデント: スペース4つ
- 行の最大長: 120文字
- import: ワイルドカード禁止 (`import java.util.*` 不可)
- `var` は型が自明な場合のみ使用
- `record` をDTOとValueObjectに積極的に使う
- `Optional` をフィールドに使わない (メソッド戻り値のみ)
- `@Override` を必ず付ける
