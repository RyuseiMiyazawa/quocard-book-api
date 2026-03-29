# quocard-book-api

クオカードのコーディングテスト仕様を想定した、書籍管理システムのバックエンド API です。

技術スタックは Kotlin / Spring Boot / jOOQ / Flyway / PostgreSQL です。jOOQ のコード生成は Flyway の migration を入力として行います。

## 前提

- Java 21
- Docker / Docker Compose

## 使用技術

- Kotlin 2.1
- Spring Boot 3.5
- jOOQ
- Flyway
- PostgreSQL 16
- Gradle Wrapper

## 設計方針

- DB スキーマは Flyway で管理し、migration を唯一の真実のソースにする
- jOOQ は migration からコード生成し、SQL の型安全性を優先する
- API は Controller / Service / Repository の薄い3層に留めて過剰な抽象化を避ける
- ビジネスルールは Service 層で明示的に検証し、DB 制約と二重で担保する
- エラー応答は `ProblemDetail` に統一する

## 機能

- 著者の登録
- 著者の更新
- 書籍の登録
- 書籍の更新
- 著者に紐づく書籍一覧の取得

## ビジネスルール

- 書籍価格は `0` 以上
- 書籍は最低 1 人の著者を持つ
- 著者は複数の書籍を持てる
- 書籍は複数著者を持てる
- 著者の生年月日は現在日以前
- `PUBLISHED` の書籍は `UNPUBLISHED` に戻せない

## エンドポイント

- `POST /authors`
- `PUT /authors/{authorId}`
- `GET /authors/{authorId}/books`
- `POST /books`
- `PUT /books/{bookId}`

## 起動方法

まず PostgreSQL を起動します。

```bash
docker compose up -d
```

その後、API を起動します。

```bash
./gradlew bootRun
```

デフォルトでは以下の接続先を利用します。

- DB URL: `jdbc:postgresql://localhost:5432/quocard_book_api`
- DB USER: `quocard`
- DB PASSWORD: `quocard`

必要であれば以下の環境変数で上書きできます。

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## API 例

著者を登録:

```bash
curl -X POST http://localhost:8080/authors \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Kent Beck",
    "birthDate": "1961-03-31"
  }'
```

書籍を登録:

```bash
curl -X POST http://localhost:8080/books \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Tidy First?",
    "price": 2800,
    "publicationStatus": "PUBLISHED",
    "authorIds": [1]
  }'
```

著者に紐づく本を取得:

```bash
curl http://localhost:8080/authors/1/books
```

## テスト

```bash
./gradlew test
```

以下を組み合わせています。

- Service 層の単体テスト
- H2 を使った API 統合テスト
- Testcontainers + PostgreSQL を使った API 統合テスト

アプリケーション実行時は PostgreSQL を使用します。

Docker が利用できる環境であれば PostgreSQL 実DBテストまで含めて `./gradlew test` で確認できます。

## OpenAPI

API 仕様は [docs/openapi.yaml](docs/openapi.yaml) に記載しています。

## 実装メモ

- DB スキーマは Flyway で管理
- jOOQ は migration からコード生成
- 例外時は `ProblemDetail` 形式でレスポンス
- オーバーエンジニアリングを避け、単純な層構成に限定
