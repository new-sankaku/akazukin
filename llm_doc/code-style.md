# コードスタイル

- Java標準のコーディングスタイルに従う
- インデント: スペース4つ
- 行の最大長: 120文字
- import: ワイルドカード禁止 (`import java.util.*` 不可)
- `var` は型が自明な場合のみ使用
- `record` をDTOとValueObjectに積極的に使う
- `Optional` をフィールドに使わない (メソッド戻り値のみ)
- `@Override` を必ず付ける
