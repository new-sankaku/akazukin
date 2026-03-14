# ファイル命名規約

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
