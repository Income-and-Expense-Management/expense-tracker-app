# QLCT Server — API Documentation

> **Base URL:** `http://<host>:<port>/api`  
> **Version prefix:** `/api/v1`  
> **Content-Type:** `application/json` cho tất cả requests/responses  
> **Auth:** Bearer Token (JWT) qua header `Authorization: Bearer <token>`

---

## Cấu trúc Response chung

### ✅ Success

```json
{
  "success": true,
  "message": "Thông báo thành công",
  "data": { ... }
}
```

### ❌ Error

```json
{
  "success": false,
  "message": "Thông báo lỗi"
}
```

### ❌ Validation Error (400)

```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "errors": [
    { "field": "email", "message": "Email không hợp lệ" }
  ]
}
```

### 📄 Paginated

```json
{
  "success": true,
  "message": "Success",
  "data": [ ... ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 100,
    "totalPages": 5
  }
}
```

### HTTP Status Code Map

| Code | Ý nghĩa |
|------|---------|
| 200 | Thành công (GET, PATCH) |
| 201 | Tạo mới thành công (POST) |
| 204 | Xóa thành công (DELETE) — không có body |
| 400 | Dữ liệu không hợp lệ |
| 401 | Chưa đăng nhập / token hết hạn |
| 403 | Không có quyền truy cập tài nguyên |
| 404 | Không tìm thấy tài nguyên |
| 409 | Xung đột (email đã tồn tại) |
| 500 | Lỗi server |

> **Lưu ý BigInt:** Các trường `amount`, `initial_balance`, `target_amount`, `current_balance`, `total_spent`, `remaining` luôn được trả về dưới dạng **String** (không phải Number) để tránh mất độ chính xác với số tiền VND lớn.

---

## Health Check

### GET `/api/health`

Kiểm tra trạng thái server và kết nối database. **Public — không cần auth.**

**Response 200:**
```json
{
  "success": true,
  "message": "Service is running normally",
  "data": {
    "service": "QLCT API",
    "version": "1.0.0",
    "status": "healthy",
    "timestamp": "2024-01-15T10:30:00.000Z",
    "database": "connected"
  }
}
```

---

## Auth

### POST `/api/v1/auth/register`

Đăng ký tài khoản mới bằng email/password. **Public.**

**Request Body:**
```json
{
  "email": "user@example.com",      // required, phải là email hợp lệ
  "password": "password123",        // required, tối thiểu 6 ký tự
  "full_name": "Nguyễn Văn A",      // optional
  "avatar_url": "https://..."       // optional, phải là URL hợp lệ
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
      "full_name": "Nguyễn Văn A",
      "email": "user@example.com",
      "avatar_url": null,
      "auth_provider": "local",
      "created_at": "2024-01-15T10:30:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Dữ liệu không hợp lệ (validation errors) |
| 409 | Email đã được sử dụng |

---

### POST `/api/v1/auth/login`

Đăng nhập bằng email/password. **Public.**

**Request Body:**
```json
{
  "email": "user@example.com",    // required
  "password": "password123"       // required
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
      "full_name": "Nguyễn Văn A",
      "email": "user@example.com",
      "avatar_url": null,
      "auth_provider": "local",
      "created_at": "2024-01-15T10:30:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Dữ liệu không hợp lệ |
| 401 | Email hoặc mật khẩu không đúng |
| 401 | Tài khoản này sử dụng phương thức đăng nhập khác |

---

### POST `/api/v1/auth/google`

Đăng nhập / Đăng ký bằng Google OAuth. **Public.**

**Request Body:**
```json
{
  "id_token": "google-id-token-string",   // required, Google ID Token từ client
  "email": "user@gmail.com",              // required
  "full_name": "Nguyễn Văn A"             // optional
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
      "full_name": "Nguyễn Văn A",
      "email": "user@gmail.com",
      "avatar_url": "https://lh3.googleusercontent.com/...",
      "auth_provider": "google",
      "created_at": "2024-01-15T10:30:00.000Z"
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Dữ liệu không hợp lệ |
| 401 | Google token không hợp lệ hoặc đã hết hạn |

---

### POST `/api/v1/auth/logout`

Đăng xuất. **Cần auth.** JWT là stateless, client xóa token ở phía mình.

**Response 200:**
```json
{
  "success": true,
  "message": "Đăng xuất thành công",
  "data": null
}
```

---

### GET `/api/v1/auth/profile`

Lấy thông tin profile của user hiện tại. **Cần auth.**

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin thành công",
  "data": {
    "id": "uuid-string",
    "full_name": "Nguyễn Văn A",
    "email": "user@example.com",
    "avatar_url": null,
    "auth_provider": "local",
    "created_at": "2024-01-15T10:30:00.000Z"
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 401 | Vui lòng đăng nhập để tiếp tục |
| 404 | Không tìm thấy người dùng |

---

### PATCH `/api/v1/auth/profile`

Cập nhật thông tin profile. **Cần auth.**

**Request Body:** (tất cả optional, chỉ gửi field cần thay đổi)
```json
{
  "full_name": "Nguyễn Văn B",
  "avatar_url": "https://example.com/avatar.jpg"   // null để xóa
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Cập nhật thông tin thành công",
  "data": {
    "id": "uuid-string",
    "full_name": "Nguyễn Văn B",
    "email": "user@example.com",
    "avatar_url": "https://example.com/avatar.jpg",
    "auth_provider": "local",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z"
  }
}
```

---

### PATCH `/api/v1/auth/change-password`

Đổi mật khẩu. **Cần auth.** Chỉ áp dụng cho tài khoản `local`.

**Request Body:**
```json
{
  "oldPassword": "password123",    // required
  "newPassword": "newpass456"      // required, tối thiểu 6 ký tự
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

**Errors:**
| Code | Message |
|------|---------|
| 400 | Mật khẩu cũ không đúng |

---

## Wallets (Ví)

> Tất cả endpoints đều **cần auth.**

### POST `/api/v1/wallets`

Tạo ví mới.

**Request Body:**
```json
{
  "name": "Ví tiền mặt",       // required, 1-255 ký tự
  "initial_balance": 1000000,  // optional, số nguyên >= 0, mặc định 0
  "currency": "VND",           // optional, mặc định "VND"
  "icon_id": "wallet-cash"     // optional
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
    "name": "Ví tiền mặt",
    "initial_balance": "1000000",
    "currency": "VND",
    "icon_id": "wallet-cash",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null
  }
}
```

---

### GET `/api/v1/wallets`

Lấy danh sách tất cả ví của user (chỉ ví chưa bị xóa), kèm số dư thực tế.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy danh sách ví thành công",
  "data": [
    {
      "id": "uuid-string",
      "user_id": "uuid-string",
      "name": "Ví tiền mặt",
      "initial_balance": "1000000",
      "currency": "VND",
      "icon_id": "wallet-cash",
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T10:30:00.000Z",
      "deleted_at": null,
      "current_balance": "850000"
    }
  ]
}
```

> `current_balance = initial_balance + SUM(income) - SUM(expense)`

---

### GET `/api/v1/wallets/:walletId`

Lấy chi tiết một ví theo ID, kèm số dư thực tế.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin ví thành công",
  "data": {
    "id": "uuid-string",
    "user_id": "uuid-string",
    "name": "Ví tiền mặt",
    "initial_balance": "1000000",
    "currency": "VND",
    "icon_id": "wallet-cash",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null,
    "current_balance": "850000"
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền truy cập ví này |
| 404 | Không tìm thấy ví |

---

### PATCH `/api/v1/wallets/:walletId`

Cập nhật thông tin ví. Chỉ gửi fields cần thay đổi.

**Request Body:** (tất cả optional)
```json
{
  "name": "Ví ngân hàng",
  "initial_balance": 5000000,
  "currency": "VND",
  "icon_id": "wallet-bank"
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
    "initial_balance": "5000000",
    "currency": "VND",
    "icon_id": "wallet-bank",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z",
    "deleted_at": null
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền cập nhật ví này |
| 404 | Không tìm thấy ví |

---

### DELETE `/api/v1/wallets/:walletId`

Xóa mềm ví (soft-delete — ví không xuất hiện nữa nhưng lịch sử giao dịch được giữ lại). **Response 204 không có body.**

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền xóa ví này |
| 404 | Không tìm thấy ví |

---

## Transactions (Giao dịch)

> Tất cả endpoints đều **cần auth.**  
> Có 2 nhóm endpoint: **wallet-scoped** (nested dưới `/wallets/:walletId/transactions`) và **flat** (dưới `/transactions`).  
> **Lưu ý:** `Transaction` không lưu `type` hay `icon`. Type được xác định qua `category.type` (income/expense). Icon xác định qua `category.icon_name`.

### POST `/api/v1/wallets/:walletId/transactions`

Tạo giao dịch cho một ví cụ thể (wallet_id lấy từ URL).

**Request Body:**
```json
{
  "category_id": "uuid-string",          // optional, UUID của category
  "amount": 150000,                       // required, số nguyên dương
  "transaction_date": "2024-01-15T10:30:00.000Z",  // optional, ISO 8601, mặc định now
  "note": "Ăn sáng"                       // optional, tối đa 1000 ký tự
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
    "transaction_date": "2024-01-15T10:30:00.000Z",
    "note": "Ăn sáng",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null,
    "category": {
      "id": "uuid-string",
      "user_id": "uuid-string",
      "name": "Ăn uống",
      "type": "expense",
      "icon_name": "food",
      "is_active": true,
      "created_at": "2024-01-01T00:00:00.000Z",
      "updated_at": "2024-01-01T00:00:00.000Z",
      "deleted_at": null
    }
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Dữ liệu không hợp lệ |
| 403 | Bạn không có quyền tạo giao dịch cho ví này |
| 404 | Không tìm thấy ví |

---

### POST `/api/v1/transactions`

Tạo giao dịch (flat endpoint, phải khai báo `wallet_id` trong body).

**Request Body:**
```json
{
  "wallet_id": "uuid-string",             // required
  "category_id": "uuid-string",          // optional
  "amount": 150000,                       // required, số nguyên dương
  "transaction_date": "2024-01-15T10:30:00.000Z",  // optional
  "note": "Ăn sáng"                       // optional
}
```

**Response 201:** _(cùng cấu trúc với endpoint trên)_

---

### GET `/api/v1/wallets/:walletId/transactions`

Lấy danh sách giao dịch của một ví, có thể lọc.

**Query Parameters:**
| Param | Type | Mô tả |
|-------|------|-------|
| `type` | `income` \| `expense` | Lọc theo loại (qua category.type) |
| `category_id` | UUID | Lọc theo danh mục |
| `start_date` | ISO 8601 | Từ ngày |
| `end_date` | ISO 8601 | Đến ngày |

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
      "transaction_date": "2024-01-15T10:30:00.000Z",
      "note": "Ăn sáng",
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T10:30:00.000Z",
      "deleted_at": null,
      "category": {
        "id": "uuid-string",
        "user_id": "uuid-string",
        "name": "Ăn uống",
        "type": "expense",
        "icon_name": "food",
        "is_active": true,
        "created_at": "2024-01-01T00:00:00.000Z",
        "updated_at": "2024-01-01T00:00:00.000Z",
        "deleted_at": null
      }
    }
  ]
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền truy cập ví này |
| 404 | Không tìm thấy ví |

---

### GET `/api/v1/wallets/:walletId/transactions/statistics`

Lấy thống kê thu/chi của một ví.

**Query Parameters:**
| Param | Type | Mô tả |
|-------|------|-------|
| `start_date` | ISO 8601 | Từ ngày |
| `end_date` | ISO 8601 | Đến ngày |

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thống kê thành công",
  "data": {
    "total_income": "5000000",
    "total_expense": "2500000",
    "balance": "2500000",
    "transaction_count": 15
  }
}
```

> `balance = total_income - total_expense` (không phải số dư ví thực tế)

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền truy cập ví này |
| 404 | Không tìm thấy ví |

---

### GET `/api/v1/transactions`

Lấy tất cả giao dịch của user trên mọi ví, có thể lọc.

**Query Parameters:**
| Param | Type | Mô tả |
|-------|------|-------|
| `type` | `income` \| `expense` | Lọc theo loại |
| `category_id` | UUID | Lọc theo danh mục |
| `wallet_id` | UUID | Lọc theo ví |
| `start_date` | ISO 8601 | Từ ngày |
| `end_date` | ISO 8601 | Đến ngày |

**Response 200:** _(cùng cấu trúc mảng với endpoint trên)_

---

### GET `/api/v1/transactions/:transactionId`

Lấy chi tiết một giao dịch.

**Response 200:**
```json
{
  "success": true,
  "message": "Lấy thông tin giao dịch thành công",
  "data": {
    "id": "uuid-string",
    "wallet_id": "uuid-string",
    "category_id": "uuid-string",
    "amount": "150000",
    "transaction_date": "2024-01-15T10:30:00.000Z",
    "note": "Ăn sáng",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null,
    "category": {
      "id": "uuid-string",
      "user_id": "uuid-string",
      "name": "Ăn uống",
      "type": "expense",
      "icon_name": "food",
      "is_active": true,
      "created_at": "2024-01-01T00:00:00.000Z",
      "updated_at": "2024-01-01T00:00:00.000Z",
      "deleted_at": null
    }
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền thực hiện thao tác này với giao dịch |
| 404 | Không tìm thấy giao dịch |

---

### PATCH `/api/v1/transactions/:transactionId`

Cập nhật giao dịch. Chỉ gửi fields cần thay đổi.

**Request Body:** (tất cả optional)
```json
{
  "category_id": "uuid-string",           // null để bỏ liên kết category
  "amount": 200000,
  "transaction_date": "2024-01-15T12:00:00.000Z",
  "note": "Ăn trưa"
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
    "transaction_date": "2024-01-15T12:00:00.000Z",
    "note": "Ăn trưa",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z",
    "deleted_at": null,
    "category": { ... }
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền cập nhật giao dịch này |
| 404 | Không tìm thấy giao dịch |

---

### DELETE `/api/v1/transactions/:transactionId`

Xóa giao dịch (hard delete). **Response 204 không có body.**

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền xóa giao dịch này |
| 404 | Không tìm thấy giao dịch |

---

## Categories (Danh mục)

> Tất cả endpoints đều **cần auth.**  
> Category là **nguồn duy nhất** xác định `type` (income/expense) và `icon` cho Transaction.

### POST `/api/v1/categories`

Tạo danh mục mới.

**Request Body:**
```json
{
  "name": "Ăn uống",           // required, 1-255 ký tự
  "type": "expense",           // required, "income" hoặc "expense"
  "icon_name": "food"          // optional
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
    "icon_name": "food",
    "is_active": true,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Loại danh mục không hợp lệ. Chỉ chấp nhận income hoặc expense |

---

### GET `/api/v1/categories`

Lấy danh sách tất cả danh mục của user (chỉ danh mục đang active, chưa bị xóa).

**Query Parameters:**
| Param | Type | Mô tả |
|-------|------|-------|
| `type` | `income` \| `expense` | Lọc theo loại |

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
      "icon_name": "food",
      "is_active": true,
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T10:30:00.000Z",
      "deleted_at": null
    },
    {
      "id": "uuid-string-2",
      "user_id": "uuid-string",
      "name": "Lương",
      "type": "income",
      "icon_name": "salary",
      "is_active": true,
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T10:30:00.000Z",
      "deleted_at": null
    }
  ]
}
```

---

### GET `/api/v1/categories/:categoryId`

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
    "icon_name": "food",
    "is_active": true,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền thực hiện thao tác này với danh mục |
| 404 | Không tìm thấy danh mục |

---

### PATCH `/api/v1/categories/:categoryId`

Cập nhật danh mục. Chỉ gửi fields cần thay đổi.

**Request Body:** (tất cả optional)
```json
{
  "name": "Ăn uống & Đồ uống",
  "type": "expense",
  "icon_name": "restaurant"
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
    "name": "Ăn uống & Đồ uống",
    "type": "expense",
    "icon_name": "restaurant",
    "is_active": true,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z",
    "deleted_at": null
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Loại danh mục không hợp lệ |
| 403 | Bạn không có quyền cập nhật danh mục này |
| 404 | Không tìm thấy danh mục |

---

### DELETE `/api/v1/categories/:categoryId`

Xóa mềm danh mục (soft-delete — set `is_active=false`, `deleted_at=now`). Giao dịch đã liên kết vẫn được giữ nguyên. **Response 204 không có body.**

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền xóa danh mục này |
| 404 | Không tìm thấy danh mục |

---

## Budgets (Ngân sách)

> Tất cả endpoints đều **cần auth.**

### POST `/api/v1/budgets`

Tạo ngân sách mới cho ví + danh mục + khoảng thời gian.

**Request Body:**
```json
{
  "wallet_id": "uuid-string",     // required
  "category_id": "uuid-string",   // required
  "target_amount": 3000000,       // required, số nguyên dương
  "start_date": "2024-01-01T00:00:00.000Z",  // optional, ISO 8601
  "end_date": "2024-01-31T23:59:59.000Z"     // optional, ISO 8601
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
    "end_date": "2024-01-31T23:59:59.000Z",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 400 | Dữ liệu không hợp lệ |
| 403 | Bạn không có quyền thực hiện thao tác này với ngân sách |
| 404 | Không tìm thấy ví |

---

### GET `/api/v1/budgets`

Lấy danh sách tất cả ngân sách của user.

**Query Parameters:**
| Param | Type | Mô tả |
|-------|------|-------|
| `wallet_id` | UUID | Lọc theo ví |
| `category_id` | UUID | Lọc theo danh mục |

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
      "end_date": "2024-01-31T23:59:59.000Z",
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T10:30:00.000Z",
      "deleted_at": null
    }
  ]
}
```

> **Lưu ý:** Endpoint list KHÔNG trả về `total_spent` / `remaining`. Dùng endpoint detail để lấy.

---

### GET `/api/v1/budgets/:budgetId`

Lấy chi tiết ngân sách kèm thống kê đã chi / còn lại.

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
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "deleted_at": null,
    "total_spent": "1500000",
    "remaining": "1500000"
  }
}
```

> `remaining = target_amount - total_spent` (có thể âm nếu vượt ngân sách)

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền thực hiện thao tác này với ngân sách |
| 404 | Không tìm thấy ngân sách |

---

### PATCH `/api/v1/budgets/:budgetId`

Cập nhật ngân sách. Chỉ gửi fields cần thay đổi.

**Request Body:** (tất cả optional)
```json
{
  "category_id": "uuid-string",
  "target_amount": 5000000,
  "start_date": "2024-02-01T00:00:00.000Z",
  "end_date": "2024-02-29T23:59:59.000Z"
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
    "category_id": "uuid-string",
    "target_amount": "5000000",
    "start_date": "2024-02-01T00:00:00.000Z",
    "end_date": "2024-02-29T23:59:59.000Z",
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z",
    "deleted_at": null
  }
}
```

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền cập nhật ngân sách này |
| 404 | Không tìm thấy ngân sách |

---

### DELETE `/api/v1/budgets/:budgetId`

Xóa ngân sách (hard delete). **Response 204 không có body.**

**Errors:**
| Code | Message |
|------|---------|
| 403 | Bạn không có quyền xóa ngân sách này |
| 404 | Không tìm thấy ngân sách |

---

## Sync (Đồng bộ dữ liệu)

> Tất cả endpoints đều **cần auth.**  
> Dùng cho mobile client cần đồng bộ dữ liệu offline/online.

### GET `/api/v1/sync/pull`

Lấy tất cả records đã thay đổi kể từ `last_sync_time`. Bao gồm cả records đã xóa mềm để client có thể xóa cục bộ.

**Query Parameters:**
| Param | Type | Mô tả |
|-------|------|-------|
| `last_sync_time` | ISO 8601 hoặc Unix timestamp (ms) hoặc `0` | Mốc thời gian đồng bộ cuối. Bỏ qua hoặc `0` = full sync |

**Ví dụ:**
```
GET /api/v1/sync/pull?last_sync_time=2024-01-15T10:30:00.000Z
GET /api/v1/sync/pull?last_sync_time=1705315800000
GET /api/v1/sync/pull                                         (full sync)
```

**Response 200:**
```json
{
  "success": true,
  "message": "Đồng bộ dữ liệu thành công",
  "data": {
    "wallets": [
      {
        "id": "uuid-string",
        "user_id": "uuid-string",
        "name": "Ví tiền mặt",
        "initial_balance": "1000000",
        "currency": "VND",
        "icon_id": "wallet-cash",
        "created_at": "2024-01-15T10:30:00.000Z",
        "updated_at": "2024-01-15T10:30:00.000Z",
        "deleted_at": null
      }
    ],
    "categories": [
      {
        "id": "uuid-string",
        "user_id": "uuid-string",
        "name": "Ăn uống",
        "type": "expense",
        "icon_name": "food",
        "is_active": true,
        "created_at": "2024-01-15T10:30:00.000Z",
        "updated_at": "2024-01-15T10:30:00.000Z",
        "deleted_at": null
      }
    ],
    "transactions": [
      {
        "id": "uuid-string",
        "wallet_id": "uuid-string",
        "category_id": "uuid-string",
        "amount": "150000",
        "transaction_date": "2024-01-15T10:30:00.000Z",
        "note": "Ăn sáng",
        "created_at": "2024-01-15T10:30:00.000Z",
        "updated_at": "2024-01-15T10:30:00.000Z",
        "deleted_at": null
      }
    ],
    "budgets": [
      {
        "id": "uuid-string",
        "wallet_id": "uuid-string",
        "category_id": "uuid-string",
        "target_amount": "3000000",
        "start_date": "2024-01-01T00:00:00.000Z",
        "end_date": "2024-01-31T23:59:59.000Z",
        "created_at": "2024-01-15T10:30:00.000Z",
        "updated_at": "2024-01-15T10:30:00.000Z",
        "deleted_at": null
      }
    ],
    "server_sync_time": "2024-01-15T11:00:00.000Z"
  }
}
```

> **Client nên lưu `server_sync_time`** và dùng làm `last_sync_time` cho lần pull tiếp theo.  
> Records có `deleted_at != null` → client phải xóa cục bộ.

**Errors:**
| Code | Message |
|------|---------|
| 400 | Giá trị last_sync_time không hợp lệ |

---

### POST `/api/v1/sync/push`

Client gửi batch các records đã thay đổi cục bộ lên server. Server áp dụng **Last-Write-Wins**: nếu `client.updated_at > server.updated_at` thì server cập nhật, ngược lại bỏ qua.

**Request Body:**
```json
{
  "wallets": [
    {
      "id": "uuid-string",            // required, UUID
      "user_id": "uuid-string",       // required
      "name": "Ví mới",
      "initial_balance": "500000",    // String hoặc Number đều được
      "currency": "VND",
      "icon_id": null,
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T11:00:00.000Z",
      "deleted_at": null
    }
  ],
  "categories": [
    {
      "id": "uuid-string",
      "user_id": "uuid-string",
      "name": "Di chuyển",
      "type": "expense",
      "icon_name": "transport",
      "is_active": true,
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T11:00:00.000Z",
      "deleted_at": null
    }
  ],
  "transactions": [
    {
      "id": "uuid-string",
      "wallet_id": "uuid-string",
      "category_id": "uuid-string",
      "amount": "50000",              // String hoặc Number
      "transaction_date": "2024-01-15T08:00:00.000Z",
      "note": "Xe buýt",
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T11:00:00.000Z",
      "deleted_at": null
    }
  ],
  "budgets": [
    {
      "id": "uuid-string",
      "wallet_id": "uuid-string",
      "category_id": "uuid-string",
      "target_amount": "2000000",     // String hoặc Number
      "start_date": "2024-01-01T00:00:00.000Z",
      "end_date": "2024-01-31T23:59:59.000Z",
      "created_at": "2024-01-15T10:30:00.000Z",
      "updated_at": "2024-01-15T11:00:00.000Z",
      "deleted_at": null
    }
  ]
}
```

> Tất cả mảng đều **optional**, mặc định là `[]`.  
> Có thể chỉ gửi những model cần push, ví dụ chỉ gửi `{ "transactions": [...] }`.

**Response 200:**
```json
{
  "success": true,
  "message": "Đẩy dữ liệu đồng bộ thành công",
  "data": {
    "applied": {
      "wallets": ["uuid-1", "uuid-2"],
      "categories": ["uuid-3"],
      "transactions": ["uuid-4", "uuid-5", "uuid-6"],
      "budgets": []
    },
    "skipped": {
      "wallets": [],
      "categories": ["uuid-old"],
      "transactions": [],
      "budgets": []
    },
    "server_sync_time": "2024-01-15T11:05:00.000Z"
  }
}
```

> `applied` = các ID đã được tạo hoặc cập nhật trên server.  
> `skipped` = các ID bị bỏ qua vì server có version mới hơn.  
> **Client nên lưu `server_sync_time`** sau khi push thành công.

**Errors:**
| Code | Message |
|------|---------|
| 400 | Dữ liệu không hợp lệ (validation error) |

---

## Lưu ý cho Frontend

### 1. Lưu trữ Token

Lưu JWT token sau khi đăng nhập. Gửi kèm mọi request cần auth:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2. BigInt / Số tiền

Các trường tiền tệ (`amount`, `initial_balance`, `target_amount`, `current_balance`, `total_spent`, `remaining`) **luôn là String** trong response. Khi hiển thị hoặc tính toán, parse sang `BigInt` hoặc dùng thư viện xử lý số lớn:

```javascript
// Đúng
const balance = BigInt(data.current_balance);
const formatted = Number(data.current_balance).toLocaleString('vi-VN');

// Sai — có thể mất độ chính xác với số lớn
const balance = parseInt(data.current_balance);
```

### 3. Xác định loại giao dịch

Transaction **không có field `type`**. Phải đọc từ `transaction.category.type`:
```javascript
const isExpense = transaction.category?.type === 'expense';
const isIncome = transaction.category?.type === 'income';
const icon = transaction.category?.icon_name; // icon name
```

### 4. Xóa mềm

- **Wallet** và **Category**: xóa mềm — `deleted_at != null` nghĩa là đã xóa, không hiển thị.
- **Transaction** và **Budget**: xóa cứng — sau DELETE không còn tồn tại.

### 5. Sync flow

```
Lần đầu (full sync):
  GET /sync/pull → lưu toàn bộ data + lưu server_sync_time

Lần sau:
  GET /sync/pull?last_sync_time=<server_sync_time_đã_lưu>
  → nhận chỉ những gì thay đổi
  → xóa local những record có deleted_at != null

Push local changes:
  POST /sync/push với batch records thay đổi
  → server trả về applied/skipped + server_sync_time mới
```

### 6. Date format

Tất cả datetime gửi lên phải là **ISO 8601 với timezone offset**, ví dụ:
- `2024-01-15T10:30:00.000Z` (UTC)
- `2024-01-15T17:30:00.000+07:00` (UTC+7)
