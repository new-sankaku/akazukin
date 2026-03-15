# Akazukin 開発ガイドライン

- ユーザーに試させるな。お前がやれ。
- 調査結果と指摘に差異がある場合は

## プロジェクト概要
- 複数SNS一元管理ツール (X, Bluesky, Mastodon, Threads, Instagram, Reddit, Telegram, VK, Pinterest, TikTok)
- Java 21 + Quarkus + Renarde/HTMX + PostgreSQL + AWS Lambda

## ビルド・実行コマンド
- `01_install.bat` — ビルド+テスト実行
- `20_start_server.bat` — 開発サーバー起動 (http://localhost:38081)
- `30_stop_server.bat` — 開発サーバー停止

## アーキテクチャ原則
- ヘキサゴナルアーキテクチャ (Ports & Adapters)
- ドメイン層はフレームワーク非依存 (純粋Java)
- SNSアダプターはJava SPI (ServiceLoader) で動的ロード
- 3層分離: SDK (HTTP通信) → Adapter (ドメイン変換) → Domain (共通モデル)
- 依存は常に内側 (Domain) に向かう。Domain が外側に依存してはならない

## 重要ルール (必ず守ること)
- エラーをもみ消さない。空の catch ブロック禁止。必ず再スローまたはエラーレスポンスを返す
- フォールバックしない。失敗をデフォルト値で隠さない
- `Optional` をフィールドに使わない (メソッド戻り値のみ)。null を返さない
- import ワイルドカード禁止 (`import java.util.*` 不可)
- `record` をDTOとValueObjectに積極的に使う
- HttpClient はシングルトンで再利用。リクエスト毎に生成しない
- コメントを書かない。コードで意図を表現する

## 知見の記録
- 開発中に得られた知見・ハマりポイントは `llm_doc/知見.md` に追記すること

## 詳細ドキュメント (llm_doc/)
必要に応じて参照:
- environment.md — 環境変数・システムプロパティ一覧
- architecture.md — モジュール構成・依存方向・フォルダ構成
- naming.md — ファイル命名規約
- error-handling.md — エラーハンドリング方針・ログ戦略
- ui-ux.md — UI/UX戦略・HTMX ベストプラクティス
- performance.md — パフォーマンス・メモリ・Disk IO
- anti-patterns.md — 回避すべきアンチパターン
- code-style.md — コードスタイル
- 知見.md — 開発中に得られた知見・学び
