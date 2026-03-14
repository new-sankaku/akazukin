# 回避すべきアンチパターン

## アーキテクチャ系
- **God Class**: 1クラスに責務を詰め込みすぎない。1クラス1責務
- **循環依存**: モジュール間の循環依存禁止。依存は常にDomain方向へ
- **Leaky Abstraction**: SNS固有の概念をドメイン層に漏らさない
- **Big Ball of Mud**: パッケージ/モジュールの境界を曖昧にしない
- **Service Locator乱用**: SPI以外でServiceLocatorパターンを使わない。DIを使う
- **Anemic Domain Model**: ドメインモデルにビジネスロジックを持たせる。getter/setterだけのモデルにしない

## コーディング系
- **Pokemon Exception Handling**: `catch (Exception e)` で全てキャッチしない
- **Swallowed Exception**: 例外を握りつぶさない
- **Magic Number/String**: 定数を定義して使う
- **Premature Optimization**: 計測なしの最適化をしない
- **Copy-Paste Programming**: 共通処理は抽出する (ただし過度な抽象化も避ける)
- **Null返却**: Optional を使う。null を返さない
- **Mutable Shared State**: 可変共有状態を避ける。record や不変オブジェクトを優先

## API設計系
- **Chatty API**: 1操作に複数リクエストが必要な設計にしない
- **Inconsistent Naming**: エンドポイントの命名規則を統一 (RESTful)
- **Missing Pagination**: 一覧系APIは必ずページネーション対応
- **Silent Failure**: APIエラー時に200を返さない。適切なHTTPステータスコードを使う
- **Over-fetching**: 不要なデータを返さない。必要に応じてフィールド選択可能に

## DB/永続化系
- **N+1 Problem**: リレーション取得でN+1クエリを発生させない
- **Missing Index**: 検索条件のカラムにインデックスを忘れない
- **God Table**: 1テーブルに全てを入れない。正規化する
- **Soft Delete乱用**: 本当に必要な場合のみ。基本はハードデリート + 監査ログ

## SNSアダプター系
- **Rate Limit無視**: 各SNSのレートリミットを尊重。429を受けたらRetry-After待機
- **Token平文保存**: アクセストークンは必ず暗号化してDB保存
- **同期的な一括投稿**: 複数SNSへの投稿は非同期キュー経由。1つの失敗が他に影響しない
- **エラー翻訳**: SNS APIの生エラーメッセージを変に翻訳・省略しない。原文を保持

## テスト系
- **Test Pollution**: テスト間でデータを共有しない。各テストは独立
- **Flaky Test**: 外部API依存のテストはモック化。タイミング依存を排除
- **Missing Edge Cases**: 正常系だけでなく、エラー系・境界値のテストも書く
