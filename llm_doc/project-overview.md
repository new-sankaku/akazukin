# プロジェクト概要

## akazukin とは
複数SNSへの一括投稿・スケジュール管理・AI支援コンテンツ生成を提供するWebアプリケーション。
Quarkus + HTMX + PostgreSQL で構成し、GraalVM Native Image対応。

## 技術スタック
- Java 21, Quarkus (REST + Renarde SSR + Panache ORM)
- フロント: HTMX 2.0.8 + Qute テンプレート + Web Bundler
- DB: PostgreSQL + Flyway (27マイグレーション, 24テーブル)
- キュー: Amazon SQS
- キャッシュ: Redis + Caffeine
- AI: Ollama (ローカル) / OpenAI / Anthropic
- 認証: JWT (アクセス15分 + リフレッシュ7日) + BCrypt
- 監視: Prometheus + OpenTelemetry
- テスト: JUnit 5 + WireMock + REST-assured
- セキュリティ: OWASP依存関係チェック (CVE>=9.0), レート制限 (Bucket4j), サーキットブレーカー

## 対応SNS (13プラットフォーム)
| SNS | 文字数上限 | SDK方式 | 認証方式 |
|-----|-----------|---------|---------|
| Twitter/X | 280 | 独自SDK | OAuth2 PKCE |
| Bluesky | 300 | 独自SDK | App Password (JWT) |
| Mastodon | 500 | HttpClient直接 | OAuth2 |
| Threads | 500 | HttpClient直接 | OAuth2 |
| Instagram | 2200 | HttpClient直接 | Facebook OAuth |
| Reddit | 40000 | 独自SDK | OAuth2 + Basic Auth |
| Telegram | 4096 | HttpClient直接 | Bot Token |
| VK | 15895 | HttpClient直接 | OAuth2 |
| Pinterest | 500 | 独自SDK | OAuth2 + Basic Auth |
| TikTok | 2200 | 独自SDK | OAuth2 |
| mixi2 | 1000 | 独自SDK | OAuth2 |
| note | 50000 | 独自SDK | API Key |
| niconico | 75 | 独自SDK | API Key |

## 主要機能
1. **投稿管理** - 作成・編集・マルチプラットフォーム同時投稿・スケジュール予約
2. **AI支援** - ペルソナ別コンテンツ生成・比較・試し書き・コンプライアンスチェック
3. **エージェントパイプライン** - 7種AIエージェントによる自動コンテンツ制作フロー
4. **A/Bテスト** - バリアント生成・結果予測・勝ちパターン分析
5. **カレンダー** - 日本の祝日/二十四節気/季節イベント連携、橋渡し休暇戦略
6. **フレンド管理** - エンゲージメント追跡・関係性スコア・バブルマップ
7. **ニュース連携** - RSS取得→投稿アイデア生成・マルチアングル展開
8. **承認ワークフロー** - ロール別ルール・リスクレベル別フロー・AIレビュー
9. **ダッシュボード** - 統計・ファイアウォッチ・季節分析・プラットフォーム相関
10. **チーム管理** - チーム作成・メンバー追加・ロール (ADMIN/USER/VIEWER)
11. **監査ログ** - 全HTTPリクエスト記録・カテゴリ分類・データ保持ポリシー
12. **通知** - 投稿結果・承認依頼・アカウント接続等のイベント通知
13. **テンプレート** - 再利用可能な投稿テンプレート・プレースホルダー・使用回数追跡
14. **メディア管理** - ファイルアップロード・サムネイル・ドラッグ&ドロップ

## AIエージェント (7種)
| エージェント | 役割 |
|------------|------|
| DIRECTOR | 投稿戦略・パブリッシングプラン策定 |
| ANALYST | エンゲージメントデータ分析・トレンド把握 |
| COMPOSER | トピックからSNS投稿文を作成 |
| TRANSFORMER | プラットフォーム別に文体・文字数を最適化 |
| SENTINEL | レピュテーションリスク・不適切コンテンツ検出 |
| COMPLIANCE | 法令・プラットフォームポリシー準拠チェック |
| SCHEDULER | 最適投稿時間の提案 |

## ロール
| ロール | 権限 |
|--------|------|
| ADMIN | 全機能アクセス |
| USER | 投稿・カレンダー・フレンド・A/Bテスト・チーム・ダッシュボード |
| VIEWER | 閲覧のみ (APIアクセス不可) |

## デザインテーマ
NieR:Automata風の暖色ベージュ/クリーム基調。ゴールドアクセント。ダークモード非対応。
