# 環境変数

## Twitter (OAuth 2.0 PKCE)
| 環境変数 | システムプロパティ | 説明 |
|---|---|---|
| `TWITTER_CLIENT_ID` | `akazukin.twitter.client-id` | OAuth 2.0 Client ID |
| `TWITTER_CLIENT_SECRET` | `akazukin.twitter.client-secret` | OAuth 2.0 Client Secret |
| `TWITTER_REDIRECT_URI` | `akazukin.twitter.redirect-uri` | OAuth 2.0 コールバックURL |

## BlueSky
- 環境変数不要。サービスURLはデフォルト `https://bsky.social`
- ハンドルとアプリパスワードはGUI上でアカウント連携時に入力
- サービスURL変更が必要な場合: システムプロパティ `akazukin.bluesky.service-url`

## その他
| 環境変数 | デフォルト値 | 説明 |
|---|---|---|
| `AKAZUKIN_CRYPTO_KEY` | (開発用キー) | トークン暗号化キー (Base64 encoded 256-bit) |
| `AKAZUKIN_SQS_QUEUE_URL` | `http://localhost:4566/...` | SQSキューURL |
| `AKAZUKIN_SQS_ENDPOINT` | `http://localhost:4566` | SQSエンドポイント |
| `AWS_REGION` | `ap-northeast-1` | AWSリージョン |
| `AKAZUKIN_SCHEDULER_ROLE_ARN` | (開発用ARN) | EventBridge Scheduler IAMロール |
| `AKAZUKIN_SCHEDULER_TARGET_ARN` | (開発用ARN) | EventBridge Schedulerターゲット |
