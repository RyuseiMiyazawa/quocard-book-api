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

テストでは `test` プロファイルを使い、インメモリ H2 で実行しています。アプリケーション実行時は PostgreSQL を使用します。

## 実装メモ

- DB スキーマは Flyway で管理
- jOOQ は migration からコード生成
- 例外時は `ProblemDetail` 形式でレスポンス
- オーバーエンジニアリングを避け、単純な層構成に限定
