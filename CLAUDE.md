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

## ファイル・フォルダ構成規約

### パッケージ命名
- `com.akazukin.domain` — ドメインモデル・ポート
- `com.akazukin.application` — ユースケース・DTO
- `com.akazukin.infrastructure` — DB永続化・キュー
- `com.akazukin.adapter.{platform}` — SNSアダプター
- `com.akazukin.sdk.{platform}` — 独自SDK
- `com.akazukin.web` — Webコントローラ・API・セキュリティ

### ファイル配置ルール
- 1クラス1ファイル (Javaの慣例通り)
- record クラスは model/ パッケージに配置
- インターフェースは port/ パッケージに配置
- 実装クラスはインターフェースと同じパッケージに置かない (パッケージで責務分離)
- テストは src/test/java に同じパッケージ構造で配置
- Quteテンプレートは src/main/resources/templates/{controller名}/ に配置
- Flywayマイグレーションは src/main/resources/db/migration/V{番号}__{説明}.sql

### ファイル命名規約
- エンティティ: `User.java`, `Post.java` (名詞)
- リポジトリ: `UserRepository.java` (interface), `UserRepositoryImpl.java` (実装は infrastructure)
- ユースケース: `PostUseCase.java` (動詞+名詞)
- アダプター: `TwitterAdapter.java` (プラットフォーム名+Adapter)
- SDK クライアント: `TwitterClient.java` (プラットフォーム名+Client)
- コントローラ: `PostController.java` (Renarde), `PostResource.java` (REST API)
- DTO: `PostRequestDto.java`, `PostResponseDto.java`
- 例外: `TwitterApiException.java`, `PostNotFoundException.java`
- テスト: `PostUseCaseTest.java`, `TwitterAdapterTest.java`

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

### HTMX ベストプラクティス
- **プログレッシブエンハンスメント**: JS無効でも基本機能は動作させる
- **部分更新**: ページ全体をリロードせず、変更箇所だけ差し替え (`hx-swap`)
- **ローディング表示**: `hx-indicator` で処理中を明示する
- **楽観的UI禁止**: サーバーの応答を待ってからUIを更新する (データ不整合防止)
- **エラー状態の可視化**: `hx-target-error` でエラー時の表示先を分ける

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

### レスポンス速度
- サーバーサイドレンダリング: 目標200ms以内
- SNS API呼び出し: タイムアウト10秒、ユーザーにはスピナー表示
- 長時間処理 (一括投稿等): 非同期化してポーリングまたはSSEで進捗通知

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
