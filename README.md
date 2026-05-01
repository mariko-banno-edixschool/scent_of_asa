# SCENT OF ASA

SCENT OF ASA のワークショップ予約サイトです。  
現状は「公開予約フロントはかなり進んでいて、バックエンドを順次接続している段階」です。

詳細方針は [agent.md](./agent.md) を参照してください。  
特に重要なのは次の 3 点です。

- 公開ページの正本は `src/main/resources/static/public`
- 予約ルールの正本は Java 側
- スキーマ変更は Flyway (`src/main/resources/db/migration`)

## 起動

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

テスト:

```powershell
.\mvnw.cmd -q test
```

## いま出来ていること

- 公開予約画面から予約を作成できる
- 予約データは `public_reservations` テーブルへ保存される
- confirmation 画面には予約直後の内容を表示できる
- スタッフ側で休業日管理ができる
- スタッフ側で月次の枠管理ができる
- ガイド側で自分の担当枠を調整できる
- スタッフ側の `reservation_detail.html` で予約一覧を DB から閲覧できる
- スタッフ側の `reservation_detail.html` で予約ステータスを更新できる

## 画面ルート

### 公開ページ

- `/`
  - `PublicPageController` 経由で言語に応じたトップへリダイレクト
- `/about`
  - `PublicPageController` 経由で紹介ページへリダイレクト
- `/booking`
  - `PublicPageController` 経由で予約ページへリダイレクト
- `/confirmation`
  - `PublicPageController` 経由で確認ページへリダイレクト

直接静的ファイルを開く場合:

- `/public/index.html`
- `/public/index-ja.html`
- `/public/about.html`
- `/public/about-ja.html`
- `/public/booking.html`
- `/public/booking-ja.html`
- `/public/confirmation.html`
- `/public/confirmation-ja.html`

### スタッフページ

- `/staff/holiday_control.html`
  - 休業日 API 接続済み
- `/staff/slot.html`
  - 枠管理 API 接続済み
- `/staff/guide_schedule.html`
  - ガイド API 接続済み
- `/staff/reservation_detail.html`
  - 予約一覧 API 接続済み

### まだ見た目中心のページ

- `/staff/dashboard.html`
- `/staff/check_in.html`
- `/staff/sales_report.html`
- `/staff/login.html`
- `/staff/guide_login.html`

上記は UI はあるものの、バックエンド接続がまだ弱いか未実装です。

## 接続済み API

### 公開予約

- `GET /api/public/availability?year=2026&month=5&language=ja`
  - 予約可能日と残数を返す
- `POST /api/public/reservations`
  - 予約を作成して `public_reservations` に保存する

リクエスト例:

```json
{
  "reservationDate": "2026-05-12",
  "timeSlot": "13:00",
  "guideLanguage": "ja",
  "guestCount": 2,
  "customerName": "Yamada Hanako",
  "customerEmail": "hanako@example.com",
  "customerPhone": "090-1234-5678",
  "notes": "Fragrance allergy note"
}
```

### 休業日管理

- `GET /api/admin/holidays`
- `GET /api/admin/holidays/closed`
- `POST /api/admin/holidays`
- `PUT /api/admin/holidays/{id}`
- `DELETE /api/admin/holidays/{id}`
- `POST /api/admin/holidays/apply-rule`

利用画面:

- `/staff/holiday_control.html`

### 枠管理

- `GET /api/admin/slots?year=2026&month=5`
- `PUT /api/admin/slots/{id}`

利用画面:

- `/staff/slot.html`

### ガイド管理

- `GET /api/admin/guide-staff`

利用画面:

- `/staff/slot.html`
- `/staff/guide_schedule.html`

### ガイド本人用

- `GET /api/guide/me?loginId=guide_ja_1`
- `GET /api/guide/slots?loginId=guide_ja_1&year=2026&month=5`
- `PUT /api/guide/slots/{id}`

利用画面:

- `/staff/guide_schedule.html`

### 予約一覧

- `GET /api/admin/reservations`
  - フィルタ:
    - `date=2026-05-12`
    - `customerName=Smith`
    - `guideLanguage=ja|en`
- `PUT /api/admin/reservations/{id}/status`

利用画面:

- `/staff/reservation_detail.html`

更新リクエスト例:

```json
{
  "reservationStatus": "PAID"
}
```

利用可能なステータス:

- `PENDING`
- `CONFIRMED`
- `PAID`
- `CHECKED_IN`
- `CANCELLED`
- `NO_SHOW`

## 主要テーブル

- `public_reservations`
  - 公開予約の本体
- `store_holidays`
  - 休業日
- `admin_slots`
  - 管理者が持つ月次の枠データ
- `guide_staff`
  - ガイド情報

## 補足

- `confirmation.html` / `confirmation-ja.html` は現状、予約直後の表示に `sessionStorage` も併用しています
- ただし予約そのものは `POST /api/public/reservations` で DB 保存されています
- スタッフ側で実データを見る場合は `/staff/reservation_detail.html` を使ってください
