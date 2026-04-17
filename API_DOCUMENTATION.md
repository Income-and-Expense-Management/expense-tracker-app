# QLCT API — Tài Liệu Dành Cho Frontend

> **Base URL:** `http://<host>:<port>/api`  
> **API Version:** `/api/v1`  
> **Format:** Tất cả request/response đều dùng `Content-Type: application/json`  
> **Auth:** Mọi route protected đều cần header `Authorization: Bearer <token>`

---

## Mục Lục

1. [Response Format Chung](#1-response-format-chung)
2. [Health Check](#2-health-check)
3. [Auth](#3-auth)
4. [Wallets (Ví)](#4-wallets-ví)
5. [Transactions (Giao Dịch)](#5-transactions-giao-dịch)
6. [Categories (Danh Mục)](#6-categories-danh-mục)
7. [Budgets (Ngân Sách)](#7-budgets-ngân-sách)
8. [Error Codes](#8-error-codes)

---

## 1. Response Format Chung

### ✅ Thành công (200 / 201)
```json
{
  "success": true,
  "message": "Mô tả thành công",
  "data": { ... }
}
```

### ✅ Thành công — Danh sách
```json
{
  "success": true,
  "message": "Lấy danh sách thành công",
  "data": [ ... ]
}
```

### ✅ Thành công — Xóa (204 No Content)
```json
{
  "success": true,
  "message": "No content"
}
```

### ❌ Lỗi (400 / 401 / 403 / 404 / 409 / 500)
```json
{
  "success": false,
  "message": "Mô tả lỗi"
}
```

### ❌ Lỗi Validation (400)
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errors": [
    { "field": "email", "message": "Email không hợp lệ" },
    { "field": "password", "message": "Mật khẩu phải có ít nhất 6 ký tự" }
  ]
}
```

> ⚠️ **Lưu ý quan trọng:** Các trường tiền tệ (`amount`, `initial_balance`, `target_amount`, `current_balance`, `total_spent`, `remaining`) luôn được trả về dạng **String** (không phải Number) để tránh mất độ chính xác với số lớn (VND).

---

## 2. Health Check

### `GET /api/health`
Kiểm tra trạng thái server và database. Không cần auth.

**Response 200:**
```json
{
  "success": true,
  "message": "Service is running normally",
  "data": {
    "service": "QLCT API",
    "version": "1.0.0",
    "status": "healthy",
    "timestamp": "2024-01-15T08:00:00.000Z",
    "database": "connected"
  }
}
```

| `status` | `database` | Ý nghĩa |
|---|---|---|
| `"healthy"` | `"connected"` | Bình thường |
| `"degraded"` | `"disconnected"` | Database lỗi |

---

## 3. Auth

### `POST /api/v1/auth/register`
Đăng ký tài khoản mới.

**Request Body:**
```json
{
  "email": "user@example.com",       // required, phải là email hợp lệ
  "password": "123456",              // required, tối thiểu 6 ký tự
  "full_name": "Nguyen Van A",       // optional
  "avatar_url": "https://..."        // optional, phải là URL hợp lệ
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Đăng ký thành công",
  "data": {
    "user": {
      "id": "uuid-string",
      "email": "user@example.com",
      "full_name": "Nguyen Van A",
      "avatar_url": null,
      "auth_provider": "local",
      "created_at": "2024-01-15T08:00:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 400 | `"Dữ liệu không hợp lệ"` (kèm `errors`) |
| 409 | `"Email đã được sử dụng"` |

---

### `POST /api/v1/auth/login`
Đăng nhập bằng email/password.

**Request Body:**
```json
{
  "email": "user@example.com",   // required
  "password": "123456"           // required
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đăng nhập thành công",
  "data": {
    "user": {
      "id": "uuid-string",
      "email": "user@example.com",
      "full_name": "Nguyen Van A",
      "avatar_url": null,
      "auth_provider": "local",
      "created_at": "2024-01-15T08:00:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 400 | `"Dữ liệu không hợp lệ"` |
| 401 | `"Email hoặc mật khẩu không đúng"` |
| 401 | `"Tài khoản này sử dụng phương thức đăng nhập khác"` (tài khoản Google) |

---

### `POST /api/v1/auth/google`
Đăng nhập bằng Google OAuth.

**Request Body:**
```json
{
  "id_token": "google-id-token-string",  // required, Google ID token
  "email": "user@gmail.com",             // required
  "full_name": "Nguyen Van A"            // optional
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đăng nhập Google thành công",
  "data": {
    "user": {
      "id": "uuid-string",
      "email": "user@gmail.com",
      "full_name": "Nguyen Van A",
      "avatar_url": "https://lh3.googleusercontent.com/...",
      "auth_provider": "google",
      "created_at": "2024-01-15T08:00:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 401 | `"Google token không hợp lệ hoặc đã hết hạn"` |

---

### `POST /api/v1/auth/logout` 🔒
Đăng xuất. JWT là stateless nên client tự xóa token.

**Response 200:**
```json
{
  "success": true,
  "message": "Đăng xuất thành công",
  "data": null
}
```

---

### `GET /api/v1/auth/profile` 🔒
Lấy thông tin profile của user hiện tại.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin thành công",
  "data": {
    "id": "uuid-string",
    "email": "user@example.com",
    "full_name": "Nguyen Van A",
    "avatar_url": null,
    "auth_provider": "local",
    "created_at": "2024-01-15T08:00:00.000Z"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 401 | `"Vui lòng đăng nhập để tiếp tục"` |
| 404 | `"Không tìm thấy người dùng"` |

---

### `PATCH /api/v1/auth/profile` 🔒
Cập nhật thông tin profile (partial update).

**Request Body:**
```json
{
  "full_name": "Nguyen Van B",           // optional
  "avatar_url": "https://new-url.com"   // optional, null để xóa
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật thông tin thành công",
  "data": {
    "id": "uuid-string",
    "email": "user@example.com",
    "full_name": "Nguyen Van B",
    "avatar_url": "https://new-url.com",
    "auth_provider": "local",
    "created_at": "2024-01-15T08:00:00.000Z"
  }
}
```

---

### `PATCH /api/v1/auth/change-password` 🔒
Đổi mật khẩu.

**Request Body:**
```json
{
  "oldPassword": "123456",    // required
  "newPassword": "newpass123" // required, tối thiểu 6 ký tự
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công",
  "data": {
    "message": "Đổi mật khẩu thành công"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 400 | `"Mật khẩu cũ không đúng"` |

---

## 4. Wallets (Ví)

> Tất cả wallet routes đều cần auth 🔒

### Wallet Object

```json
{
  "id": "uuid-string",
  "user_id": "uuid-string",
  "name": "Tiền mặt",
  "initial_balance": "1000000",    // String (VND)
  "current_balance": "850000",     // String (VND) — tính theo giao dịch thực tế
  "currency": "VND",
  "icon_id": "wallet-icon-1",
  "created_at": "2024-01-15T08:00:00.000Z",
  "updated_at": "2024-01-15T08:00:00.000Z",
  "is_active": true
}
```

> `current_balance = initial_balance + tổng income - tổng expense`

---

### `POST /api/v1/wallets`
Tạo ví mới.

**Request Body:**
```json
{
  "name": "Tiền mặt",          // required, 1-255 ký tự
  "initial_balance": 1000000,  // optional, số nguyên >= 0, default: 0
  "currency": "VND",           // optional, default: "VND"
  "icon_id": "wallet-icon-1"   // optional
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo ví thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Tiền mặt",
    "initial_balance": "1000000",
    "currency": "VND",
    "icon_id": "wallet-icon-1",
    "created_at": "2024-01-15T08:00:00.000Z",
    "updated_at": "2024-01-15T08:00:00.000Z",
    "is_active": true
  }
}
```

> ⚠️ Wallet mới tạo **chưa có** `current_balance` — chỉ có khi gọi GET.

---

### `GET /api/v1/wallets`
Lấy tất cả ví của user (chỉ ví đang active).

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy danh sách ví thành công",
  "data": [
    {
      "id": "uuid-string",
      "user_id": "uuid-string",
      "name": "Tiền mặt",
      "initial_balance": "1000000",
      "current_balance": "850000",
      "currency": "VND",
      "icon_id": "wallet-icon-1",
      "created_at": "2024-01-15T08:00:00.000Z",
      "updated_at": "2024-01-15T08:00:00.000Z",
      "is_active": true
    }
  ]
}
```

---

### `GET /api/v1/wallets/:walletId`
Lấy chi tiết một ví.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin ví thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Tiền mặt",
    "initial_balance": "1000000",
    "current_balance": "850000",
    "currency": "VND",
    "icon_id": "wallet-icon-1",
    "created_at": "2024-01-15T08:00:00.000Z",
    "updated_at": "2024-01-15T08:00:00.000Z",
    "is_active": true
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ví"` |
| 403 | `"Bạn không có quyền truy cập ví này"` |

---

### `PATCH /api/v1/wallets/:walletId`
Cập nhật ví (partial update).

**Request Body:**
```json
{
  "name": "Ví ngân hàng",        // optional
  "initial_balance": 2000000,   // optional, số nguyên >= 0
  "currency": "USD",             // optional
  "icon_id": "bank-icon"         // optional, null để xóa
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật ví thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Ví ngân hàng",
    "initial_balance": "2000000",
    "currency": "USD",
    "icon_id": "bank-icon",
    "created_at": "2024-01-15T08:00:00.000Z",
    "updated_at": "2024-01-15T09:00:00.000Z",
    "is_active": true
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ví"` |
| 403 | `"Bạn không có quyền cập nhật ví này"` |

---

### `DELETE /api/v1/wallets/:walletId`
Xóa ví (soft-delete — ẩn khỏi danh sách, không xóa dữ liệu).

**Response 204:**
```json
{
  "success": true,
  "message": "No content"
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ví"` |
| 403 | `"Bạn không có quyền xóa ví này"` |

---

## 5. Transactions (Giao Dịch)

> Tất cả transaction routes đều cần auth 🔒

### Transaction Object

```json
{
  "id": "uuid-string",
  "wallet_id": "uuid-string",
  "category_id": "uuid-string",           // nullable
  "amount": "150000",                      // String (VND)
  "type": "expense",                       // "income" | "expense"
  "transaction_date": "2024-01-15T08:00:00.000Z",
  "icon_id": "food-icon",                  // nullable
  "note": "Ăn trưa",                       // nullable
  "created_at": "2024-01-15T08:00:00.000Z",
  "updated_at": "2024-01-15T08:00:00.000Z"
}
```

---

### `POST /api/v1/transactions`
Tạo giao dịch mới (không cần biết walletId từ URL).

**Request Body:**
```json
{
  "wallet_id": "uuid-string",              // required
  "category_id": "uuid-string",           // optional
  "amount": 150000,                        // required, số nguyên > 0
  "type": "expense",                       // required, "income" | "expense"
  "transaction_date": "2024-01-15T08:00:00.000Z",  // optional, default: now
  "icon_id": "food-icon",                  // optional
  "note": "Ăn trưa"                        // optional, tối đa 1000 ký tự
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo giao dịch thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": "uuid-string",
    "amount": "150000",
    "type": "expense",
    "transaction_date": "2024-01-15T08:00:00.000Z",
    "icon_id": "food-icon",
    "note": "Ăn trưa",
    "created_at": "2024-01-15T08:00:00.000Z",
    "updated_at": "2024-01-15T08:00:00.000Z"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 400 | `"Dữ liệu không hợp lệ"` |
| 400 | `"Loại giao dịch không hợp lệ. Chỉ chấp nhận income hoặc expense"` |
| 403 | `"Bạn không có quyền tạo giao dịch cho ví này"` |
| 404 | `"Không tìm thấy ví"` |

---

### `GET /api/v1/transactions`
Lấy tất cả giao dịch của user trên mọi ví.

**Query Parameters:**

| Param | Type | Mô tả |
|---|---|---|
| `type` | `"income"` \| `"expense"` | Lọc theo loại |
| `category_id` | UUID string | Lọc theo danh mục |
| `wallet_id` | UUID string | Lọc theo ví |
| `start_date` | ISO date string | Giao dịch từ ngày này |
| `end_date` | ISO date string | Giao dịch đến ngày này |

**Ví dụ:** `GET /api/v1/transactions?type=expense&start_date=2024-01-01T00:00:00Z&end_date=2024-01-31T23:59:59Z`

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy danh sách giao dịch thành công",
  "data": [
    {
      "id": "uuid-string",
      "wallet_id": "uuid-string",
      "category_id": "uuid-string",
      "amount": "150000",
      "type": "expense",
      "transaction_date": "2024-01-15T08:00:00.000Z",
      "icon_id": "food-icon",
      "note": "Ăn trưa",
      "created_at": "2024-01-15T08:00:00.000Z",
      "updated_at": "2024-01-15T08:00:00.000Z"
    }
  ]
}
```

---

### `GET /api/v1/transactions/:transactionId`
Lấy chi tiết một giao dịch.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin giao dịch thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": null,
    "amount": "150000",
    "type": "expense",
    "transaction_date": "2024-01-15T08:00:00.000Z",
    "icon_id": null,
    "note": null,
    "created_at": "2024-01-15T08:00:00.000Z",
    "updated_at": "2024-01-15T08:00:00.000Z"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy giao dịch"` |
| 403 | `"Bạn không có quyền thực hiện thao tác này với giao dịch"` |

---

### `PATCH /api/v1/transactions/:transactionId`
Cập nhật giao dịch (partial update, không thể đổi wallet).

**Request Body:**
```json
{
  "category_id": "uuid-string",           // optional, null để xóa
  "amount": 200000,                        // optional, số nguyên > 0
  "type": "income",                        // optional, "income" | "expense"
  "transaction_date": "2024-01-16T08:00:00.000Z",  // optional
  "icon_id": "new-icon",                   // optional, null để xóa
  "note": "Cập nhật ghi chú"              // optional, null để xóa
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật giao dịch thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": "uuid-string",
    "amount": "200000",
    "type": "income",
    "transaction_date": "2024-01-16T08:00:00.000Z",
    "icon_id": "new-icon",
    "note": "Cập nhật ghi chú",
    "created_at": "2024-01-15T08:00:00.000Z",
    "updated_at": "2024-01-16T08:00:00.000Z"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy giao dịch"` |
| 403 | `"Bạn không có quyền cập nhật giao dịch này"` |

---

### `DELETE /api/v1/transactions/:transactionId`
Xóa giao dịch (hard-delete).

**Response 204:**
```json
{
  "success": true,
  "message": "No content"
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy giao dịch"` |
| 403 | `"Bạn không có quyền xóa giao dịch này"` |

---

### `POST /api/v1/wallets/:walletId/transactions`
Tạo giao dịch trực tiếp trên một ví (wallet_id lấy từ URL, không cần trong body).

**Request Body:**
```json
{
  "category_id": "uuid-string",           // optional
  "amount": 150000,                        // required, số nguyên > 0
  "type": "expense",                       // required, "income" | "expense"
  "transaction_date": "2024-01-15T08:00:00.000Z",  // optional
  "icon_id": "food-icon",                  // optional
  "note": "Ăn trưa"                        // optional
}
```

**Response 201:** (giống `POST /api/v1/transactions`)

---

### `GET /api/v1/wallets/:walletId/transactions`
Lấy danh sách giao dịch của một ví cụ thể.

**Query Parameters:**

| Param | Type | Mô tả |
|---|---|---|
| `type` | `"income"` \| `"expense"` | Lọc theo loại |
| `category_id` | UUID string | Lọc theo danh mục |
| `start_date` | ISO date string | Giao dịch từ ngày này |
| `end_date` | ISO date string | Giao dịch đến ngày này |

**Response 200:** (giống `GET /api/v1/transactions`)

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ví"` |
| 403 | `"Bạn không có quyền truy cập ví này"` |

---

### `GET /api/v1/wallets/:walletId/transactions/statistics`
Lấy thống kê thu/chi của một ví.

**Query Parameters:**

| Param | Type | Mô tả |
|---|---|---|
| `start_date` | ISO date string | Từ ngày |
| `end_date` | ISO date string | Đến ngày |

**Ví dụ:** `GET /api/v1/wallets/uuid/transactions/statistics?start_date=2024-01-01T00:00:00Z&end_date=2024-01-31T23:59:59Z`

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thống kê thành công",
  "data": {
    "total_income": "5000000",     // String, tổng thu nhập
    "total_expense": "3500000",    // String, tổng chi tiêu
    "balance": "1500000",          // String, = total_income - total_expense
    "transaction_count": 12        // Number, tổng số giao dịch
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ví"` |
| 403 | `"Bạn không có quyền truy cập ví này"` |

---

## 6. Categories (Danh Mục)

> Tất cả category routes đều cần auth 🔒

### Category Object

```json
{
  "id": "uuid-string",
  "user_id": "uuid-string",
  "name": "Ăn uống",
  "type": "expense",         // "income" | "expense"
  "icon_name": "food-icon"   // nullable
}
```

---

### `POST /api/v1/categories`
Tạo danh mục mới.

**Request Body:**
```json
{
  "name": "Ăn uống",      // required, 1-255 ký tự
  "type": "expense",      // required, "income" | "expense"
  "icon_name": "food"     // optional
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo danh mục thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Ăn uống",
    "type": "expense",
    "icon_name": "food"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 400 | `"Dữ liệu không hợp lệ"` |
| 400 | `"Loại danh mục không hợp lệ. Chỉ chấp nhận income hoặc expense"` |

---

### `GET /api/v1/categories`
Lấy tất cả danh mục của user.

**Query Parameters:**

| Param | Type | Mô tả |
|---|---|---|
| `type` | `"income"` \| `"expense"` | Lọc theo loại danh mục |

**Ví dụ:** `GET /api/v1/categories?type=expense`

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy danh sách danh mục thành công",
  "data": [
    {
      "id": "uuid-string",
      "user_id": "uuid-string",
      "name": "Ăn uống",
      "type": "expense",
      "icon_name": "food"
    },
    {
      "id": "uuid-string-2",
      "user_id": "uuid-string",
      "name": "Lương",
      "type": "income",
      "icon_name": "salary"
    }
  ]
}
```

---

### `GET /api/v1/categories/:categoryId`
Lấy chi tiết một danh mục.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin danh mục thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Ăn uống",
    "type": "expense",
    "icon_name": "food"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy danh mục"` |
| 403 | `"Bạn không có quyền thực hiện thao tác này với danh mục"` |

---

### `PATCH /api/v1/categories/:categoryId`
Cập nhật danh mục.

**Request Body:**
```json
{
  "name": "Di chuyển",     // optional
  "type": "expense",       // optional, "income" | "expense"
  "icon_name": "car"       // optional, null để xóa
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật danh mục thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Di chuyển",
    "type": "expense",
    "icon_name": "car"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy danh mục"` |
| 403 | `"Bạn không có quyền cập nhật danh mục này"` |

---

### `DELETE /api/v1/categories/:categoryId`
Xóa danh mục (hard-delete).

**Response 204:**
```json
{
  "success": true,
  "message": "No content"
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy danh mục"` |
| 403 | `"Bạn không có quyền xóa danh mục này"` |

---

## 7. Budgets (Ngân Sách)

> Tất cả budget routes đều cần auth 🔒

### Budget Object (List / Create / Update)

```json
{
  "id": "uuid-string",
  "wallet_id": "uuid-string",
  "category_id": "uuid-string",
  "target_amount": "3000000",   // String (VND)
  "start_date": "2024-01-01",   // nullable, date string
  "end_date": "2024-01-31"      // nullable, date string
}
```

### Budget Object (Detail — GET by ID)

```json
{
  "id": "uuid-string",
  "wallet_id": "uuid-string",
  "category_id": "uuid-string",
  "target_amount": "3000000",   // String (VND)
  "start_date": "2024-01-01",
  "end_date": "2024-01-31",
  "total_spent": "1200000",     // String (VND) — thực chi trong kỳ
  "remaining": "1800000"        // String (VND) — = target_amount - total_spent
}
```

---

### `POST /api/v1/budgets`
Tạo ngân sách mới.

**Request Body:**
```json
{
  "wallet_id": "uuid-string",              // required
  "category_id": "uuid-string",           // required
  "target_amount": 3000000,               // required, số nguyên > 0
  "start_date": "2024-01-01T00:00:00Z",  // optional
  "end_date": "2024-01-31T23:59:59Z"     // optional
}
```

**Response 201:**
```json
{
  "success": true,
  "message": "Tạo ngân sách thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": "uuid-string",
    "target_amount": "3000000",
    "start_date": "2024-01-01T00:00:00.000Z",
    "end_date": "2024-01-31T23:59:59.000Z"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 400 | `"Dữ liệu không hợp lệ"` |
| 404 | `"Không tìm thấy ví"` |
| 403 | `"Bạn không có quyền thực hiện thao tác này với ngân sách"` |

---

### `GET /api/v1/budgets`
Lấy tất cả ngân sách của user.

**Query Parameters:**

| Param | Type | Mô tả |
|---|---|---|
| `wallet_id` | UUID string | Lọc theo ví |
| `category_id` | UUID string | Lọc theo danh mục |

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy danh sách ngân sách thành công",
  "data": [
    {
      "id": "uuid-string",
      "wallet_id": "uuid-string",
      "category_id": "uuid-string",
      "target_amount": "3000000",
      "start_date": "2024-01-01T00:00:00.000Z",
      "end_date": "2024-01-31T23:59:59.000Z"
    }
  ]
}
```

> ℹ️ List không bao gồm `total_spent` / `remaining` — chỉ có ở GET by ID.

---

### `GET /api/v1/budgets/:budgetId`
Lấy chi tiết một ngân sách kèm tính toán đã chi / còn lại.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin ngân sách thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": "uuid-string",
    "target_amount": "3000000",
    "start_date": "2024-01-01T00:00:00.000Z",
    "end_date": "2024-01-31T23:59:59.000Z",
    "total_spent": "1200000",
    "remaining": "1800000"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ngân sách"` |
| 403 | `"Bạn không có quyền thực hiện thao tác này với ngân sách"` |

---

### `PATCH /api/v1/budgets/:budgetId`
Cập nhật ngân sách (không thể đổi wallet).

**Request Body:**
```json
{
  "category_id": "new-uuid",               // optional
  "target_amount": 5000000,               // optional, số nguyên > 0
  "start_date": "2024-02-01T00:00:00Z",  // optional, null để xóa
  "end_date": "2024-02-29T23:59:59Z"     // optional, null để xóa
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật ngân sách thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": "new-uuid",
    "target_amount": "5000000",
    "start_date": "2024-02-01T00:00:00.000Z",
    "end_date": "2024-02-29T23:59:59.000Z"
  }
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ngân sách"` |
| 403 | `"Bạn không có quyền cập nhật ngân sách này"` |

---

### `DELETE /api/v1/budgets/:budgetId`
Xóa ngân sách (hard-delete).

**Response 204:**
```json
{
  "success": true,
  "message": "No content"
}
```

**Lỗi có thể xảy ra:**

| HTTP | message |
|---|---|
| 404 | `"Không tìm thấy ngân sách"` |
| 403 | `"Bạn không có quyền xóa ngân sách này"` |

---

## 8. Error Codes

### HTTP Status Codes Tổng Hợp

| HTTP | Ý nghĩa | Khi nào xảy ra |
|---|---|---|
| **200** | OK | GET, PATCH thành công |
| **201** | Created | POST tạo resource thành công |
| **204** | No Content | DELETE thành công |
| **400** | Bad Request | Dữ liệu không hợp lệ / thiếu trường bắt buộc |
| **401** | Unauthorized | Thiếu hoặc sai / hết hạn token |
| **403** | Forbidden | Token hợp lệ nhưng không có quyền |
| **404** | Not Found | Resource không tồn tại |
| **409** | Conflict | Trùng lặp (ví dụ: email đã tồn tại) |
| **500** | Server Error | Lỗi server không xác định |

### Tất cả Error Messages

| Message | HTTP | Tình huống |
|---|---|---|
| `"Email đã được sử dụng"` | 409 | Register với email đã tồn tại |
| `"Email hoặc mật khẩu không đúng"` | 401 | Login sai credentials |
| `"Tài khoản này sử dụng phương thức đăng nhập khác"` | 401 | Login password với tài khoản Google |
| `"Mật khẩu cũ không đúng"` | 400 | Change password sai mật khẩu cũ |
| `"Không tìm thấy người dùng"` | 404 | User bị xóa hoặc không tồn tại |
| `"Google token không hợp lệ hoặc đã hết hạn"` | 401 | Google login thất bại |
| `"Vui lòng đăng nhập để tiếp tục"` | 401 | Thiếu Authorization header |
| `"Access token is missing"` | 401 | Header có nhưng token rỗng |
| `"Invalid or expired access token"` | 401 | Token hết hạn hoặc sai |
| `"Không tìm thấy ví"` | 404 | walletId không tồn tại |
| `"Bạn không có quyền truy cập ví này"` | 403 | Ví của user khác |
| `"Bạn không có quyền cập nhật ví này"` | 403 | PATCH ví của user khác |
| `"Bạn không có quyền xóa ví này"` | 403 | DELETE ví của user khác |
| `"Không tìm thấy giao dịch"` | 404 | transactionId không tồn tại |
| `"Bạn không có quyền thực hiện thao tác này với giao dịch"` | 403 | GET giao dịch của user khác |
| `"Bạn không có quyền tạo giao dịch cho ví này"` | 403 | Tạo giao dịch trên ví user khác |
| `"Bạn không có quyền cập nhật giao dịch này"` | 403 | PATCH giao dịch của user khác |
| `"Bạn không có quyền xóa giao dịch này"` | 403 | DELETE giao dịch của user khác |
| `"Loại giao dịch không hợp lệ. Chỉ chấp nhận income hoặc expense"` | 400 | type không đúng |
| `"Không tìm thấy danh mục"` | 404 | categoryId không tồn tại |
| `"Bạn không có quyền thực hiện thao tác này với danh mục"` | 403 | Danh mục của user khác |
| `"Bạn không có quyền cập nhật danh mục này"` | 403 | PATCH danh mục của user khác |
| `"Bạn không có quyền xóa danh mục này"` | 403 | DELETE danh mục của user khác |
| `"Loại danh mục không hợp lệ. Chỉ chấp nhận income hoặc expense"` | 400 | type không đúng |
| `"Không tìm thấy ngân sách"` | 404 | budgetId không tồn tại |
| `"Bạn không có quyền thực hiện thao tác này với ngân sách"` | 403 | Ngân sách của user khác |
| `"Bạn không có quyền cập nhật ngân sách này"` | 403 | PATCH ngân sách của user khác |
| `"Bạn không có quyền xóa ngân sách này"` | 403 | DELETE ngân sách của user khác |
| `"Dữ liệu không hợp lệ"` | 400 | Validation failed (kèm `errors[]`) |

---

## 9. Ghi Chú Quan Trọng Cho Frontend

### Authentication

Sau khi login/register thành công, lưu `token` và gửi kèm mọi request protected:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Token hết hạn sau **7 ngày** (mặc định). Khi nhận `401 "Invalid or expired access token"` → redirect về màn hình login.

### Xử Lý Số Tiền (BigInt)

Tất cả trường tiền tệ trả về dạng **String**, không phải Number:

```javascript
// ✅ Đúng
const amount = data.amount;              // "150000"
const display = parseInt(amount, 10);   // 150000

// Hoặc dùng BigInt nếu số rất lớn
const bigAmount = BigInt(data.amount);  // 150000n
```

Khi **gửi lên** server, truyền dạng **Number** (integer):

```javascript
{ "amount": 150000 }     // ✅ Đúng
{ "amount": "150000" }   // ✅ Cũng được (Zod tự coerce)
```

### Soft Delete vs Hard Delete

| Resource | Loại xóa | Ý nghĩa |
|---|---|---|
| `Wallet` | **Soft delete** (`is_active = false`) | Dữ liệu vẫn còn trong DB, không hiển thị nữa |
| `Transaction` | **Hard delete** | Xóa vĩnh viễn |
| `Category` | **Hard delete** | Xóa vĩnh viễn |
| `Budget` | **Hard delete** | Xóa vĩnh viễn |

### Format Ngày Tháng

Tất cả datetime fields dùng **ISO 8601** có timezone offset:

```
2024-01-15T08:00:00.000Z      ✅ UTC
2024-01-15T15:00:00.000+07:00 ✅ Vietnam time
```

### Phân Biệt 2 Cách Tạo Transaction

| Route | Khi nào dùng |
|---|---|
| `POST /api/v1/transactions` | Khi chỉ biết walletId, truyền trong body |
| `POST /api/v1/wallets/:walletId/transactions` | Khi đang ở màn hình chi tiết ví |

Cả hai cùng kết quả, khác nhau ở cách truyền `wallet_id`.
