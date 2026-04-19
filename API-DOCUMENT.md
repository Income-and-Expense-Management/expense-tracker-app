# Tài Liệu API (API Documentation) - QLCT Server

## Thông tin chung
- **Base URL**: `/api/v1`
- **Authentication**: Các endpoints (ngoại trừ login, register, google auth) đều yêu cầu Header: `Authorization: Bearer <token>`
- **Content-Type**: `application/json`

### Quy ước quan trọng (System Constraints)
- **Tiền tệ (BigInt)**: Do CSDL lưu số tiền ở dạng `BigInt`, tất cả trường liên quan đến tiền tệ (`amount`, `initial_balance`, `target_amount`) PHẢI được gửi lên dưới dạng **string** hoặc number có thể ép sang string (khuyến nghị gửi string ví dụ: `"100000"`) để tránh lỗi sai số thập phân. Dữ liệu trả về (response) có chứa tiền cũng sẽ nằm ở dạng string.
- **Xoá mềm (Soft Delete)**: Hệ thống sử dụng xoá mềm với trường `deleted_at`. Nếu `deleted_at` không phải là `null`, dữ liệu đó được coi là đã xoá.
- **Category Type**: Chỉ nhận giá trị `income` (thu nhập) hoặc `expense` (chi tiêu). `Category` là Single Source of Truth, ảnh hưởng đến toán tĩnh tổng quan trên giao dịch (Transaction).

---

## Tính năng Đồng bộ dữ liệu (Offline-first Synchronization)

App Android và Frontend sử dụng một SQLite/Local DB có kiến trúc gần tương tự Database gốc (gồm `id`, các attributes, `updated_at` và `deleted_at`). Cơ chế đồng bộ được chia ra làm hai pha chính.

### 1. Kéo dữ liệu về (Pull)
Lấy tất cả các thay đổi từ mốc thời gian đồng bộ cuối cùng của client.
- **Endpoint:** `GET /api/v1/sync/pull`
- **Query Params:** `?last_sync_time=<ISO8601 Date String | Timestamp ms | 0>` (Nếu bỏ qua hoặc bằng `0`, server sẽ gửi trả lại toàn bộ dữ liệu từ trước tới nay - Full Sync).
- **Client Action**: Client duyệt qua từng danh sách trong response (`wallets`, `categories`, `transactions`, `budgets`) và thực hiện thao tác **UPSERT** (Nếu chưa có `id` thì Insert, có rồi thì Update) vào Local DB. Chú ý giữ nguyên `deleted_at` từ server trả về.

### 2. Đẩy dữ liệu lên (Push)
Gửi các dữ liệu được tạo/thay đổi/xoá MỚI NHẤT ở local DB lên server kể từ lần đồng bộ cuối cùng.
- **Endpoint:** `POST /api/v1/sync/push`
- **Body Data**: Tất cả dữ liệu thay đổi được gói trong một payload duy nhất.
```json
{
  "wallets": [
    {
      "id": "uuid",
      "user_id": "uuid",
      "name": "Tiền mặt",
      "initial_balance": "5000000",
      "currency": "VND",
      "icon_id": "cash_icon",
      "created_at": "2026-04-18T10:00:00Z",
      "updated_at": "2026-04-18T12:00:00Z",
      "deleted_at": null
    }
  ],
  "categories": [...],
  "transactions": [...],
  "budgets": [...]
}
```
- **Lưu ý Data Types**: Định dạng ngày tháng luôn là `ISO8601 string` với múi giờ hoặc `Z` (ví dụ: `2026-04-18T12:00:00.000Z`). UUID phải chuẩn format. Các trường không có nên là null.
- **Client Action**: Khi nhận được 200 OK, Client nên lấy `server_sync_time` từ JSON response và ghi đè lưu lại thành `last_sync_time` ở Local Storage (SharedPreferences).

---

## Khối lượng Endpoints Chi Tiết

### 1. Authentication (Xác thực)
1. **Đăng ký (Register)**
   - `POST /api/v1/auth/register`
   - **Body:** `{ "email": "abc@gmail.com", "password": "...", "full_name": "Nguyen A", "avatar_url": "..." }`
2. **Đăng nhập (Login)**
   - `POST /api/v1/auth/login`
   - **Body:** `{ "email": "abc@gmail.com", "password": "..." }`
   - **Response Data:** `{ "token": "jwt...", "user": { "id", "email", "full_name"... } }`
3. **Đăng nhập Google**
   - `POST /api/v1/auth/google`
   - **Body:** `{ "id_token": "google_token_here", "email": "...", "full_name": "..." }`
4. **Lấy Profile**
   - `GET /api/v1/auth/profile`
5. **Cập nhật Profile**
   - `PATCH /api/v1/auth/profile`
   - **Body:** `{ "full_name": "...", "avatar_url": "..." }`
6. **Đổi mật khẩu**
   - `PATCH /api/v1/auth/change-password`
   - **Body:** `{ "oldPassword": "...", "newPassword": "..." }`

### 2. Wallets (Ví)
1. **Tạo Ví**
   - `POST /api/v1/wallets/`
   - **Body:** `{ "id": "uuid-optional", "name": "Ngân hàng", "initial_balance": 1000000, "currency": "VND", "icon_id": "bank_icon" }`
   - **Lưu ý:** Trường `id` là tuỳ chọn. Nếu thiết bị (như Android App) đang offline, có thể tạo sẵn UUID v4 dưới local và gửi lên khi có mạng để sử dụng chính ID đó.
2. **Lấy Danh Sách Ví**
   - `GET /api/v1/wallets/`
3. **Lấy Thông Tin Ví (Bao gồm số dư hiện tại `current_balance`)**
   - `GET /api/v1/wallets/:walletId`
4. **Cập nhật Ví**
   - `PATCH /api/v1/wallets/:walletId`
   - **Body:** `{ "name": "...", "initial_balance": 1000000, "icon_id": "...", "currency": "VND" }`
5. **Xoá Ví**
   - `DELETE /api/v1/wallets/:walletId`
   - **Lưu ý:** API trả về trạng thái HTTP `204 No Content`. (Đây là thao tác soft-delete phía server).

### 3. Categories (Danh mục)
1. **Tạo Danh Mục**
   - `POST /api/v1/categories/`
   - **Body:** `{ "name": "Ăn uống", "type": "expense", "icon_name": "food_icon" }`
   - **Lưu ý Type:** `type` chỉ cho phép là `"income"` hoặc `"expense"`.
2. **Lấy Danh Sách Danh Mục**
   - `GET /api/v1/categories/`
3. **Lấy Danh Mục Chi Tiết**
   - `GET /api/v1/categories/:categoryId`
4. **Cập nhật Danh Mục**
   - `PATCH /api/v1/categories/:categoryId`
   - **Body:** (Giống nội dung POST nhưng tuỳ chọn)
5. **Xoá (Soft Delete/Ẩn) Danh mục**
   - `DELETE /api/v1/categories/:categoryId`

### 4. Transactions (Giao dịch)
Giao dịch có thể trỏ thẳng vào root của transactions hoặc nested theo walletId.

1. **Lấy Danh Sách Tất Cả Giao Dịch**
   - `GET /api/v1/transactions/`
2. **Lấy Giao Dịch Của Một Ví Cụ Thể**
   - `GET /api/v1/wallets/:walletId/transactions/`
3. **Tạo Giao Dịch Mới**
   - `POST /api/v1/transactions/`
   - `POST /api/v1/wallets/:walletId/transactions/` (Hai dạng url này đều hợp lệ tuỳ use model của FE / nested routing) 
   - **Body:** `{ "wallet_id": "uuid", "category_id": "uuid", "amount": 50000, "transaction_date": "2026-04-18T12:00:00Z", "note": "Ăn trưa" }`
4. **Cập nhật Giao Dịch**
   - `PATCH /api/v1/transactions/:transactionId`
5. **Xóa Giao Dịch**
   - `DELETE /api/v1/transactions/:transactionId`
6. **Thống Kê Giao Dịch 1 Ví**
   - `GET /api/v1/wallets/:walletId/transactions/statistics`

### 5. Budgets (Ngân sách)
1. **Tạo Ngân Sách**
   - `POST /api/v1/budgets/`
   - **Body:** `{ "wallet_id": "uuid", "category_id": "uuid", "target_amount": 2000000, "start_date": "2026-04-01T00:00:00.000Z", "end_date": "2026-04-30T00:00:00.000Z" }`
2. **Lấy Danh Sách Ngân Sách**
   - `GET /api/v1/budgets/`
3. **Chi Tiết Ngân Sách**
   - `GET /api/v1/budgets/:budgetId`
4. **Cập Nhật Ngân Sách**
   - `PATCH /api/v1/budgets/:budgetId`
5. **Xoá Ngân Sách**
   - `DELETE /api/v1/budgets/:budgetId`

### Mô hình JSON Response Chuẩn (Chuẩn Trả Về Frontend Cần Xử Lý)
Thành công (200 OK / 201 Created):
```json
{
  "success": true,
  "message": "Nội dung phản hồi (Tiếng Việt)",
  "data": { ... } // hoặc null / array
}
```
Thất bại (400 Bad Request / 401 Unauthorized / 404 Not Found / 500 API Error):
```json
{
  "success": false,
  "message": "Chi tiết lỗi",
  "errors": [ // Array này thường có khi bị sai validation
     { "field": "email", "message": "Email không hợp lệ" }
  ]
}
```
