# Kế Hoạch Thực Hiện: Đồng Bộ & Refactor Source Code

## Tóm tắt vấn đề

> [!NOTE]
> Đã bổ sung phân tích **Navigation Flow** sau khi Save/Edit/Delete cho tất cả màn hình (Budget, Wallet, Transaction, Category).

Sau khi phân tích toàn bộ source code, database schema mới (`database_sql.sql`), và so sánh với `DatabaseContract.java`, đã xác định được **5 nhóm vấn đề** cần giải quyết.

---

## Phân tích hiện trạng

### ✅ Những gì đã ĐỒNG BỘ tốt

| Thành phần | Trạng thái |
|---|---|
| `DatabaseContract.java` | ✅ Đã có `updated_at`, `deleted_at` cho tất cả bảng |
| `WalletDao` (insert/update/delete) | ✅ Xử lý `updated_at`, `deleted_at` đúng |
| `TransactionDao` (insert/update) | ✅ Đã loại bỏ `type`, `icon_id` khỏi transactions table |
| `BudgetDao` (CRUD) | ✅ Đã có `created_at`, `updated_at`, `deleted_at` |
| `CategoryDao` (CRUD) | ✅ Đã có `updated_at`, `deleted_at`, `is_active` |
| Model classes (Wallet, Transaction, Budget, Category) | ✅ Đã có đầy đủ các field mới |
| `BudgetDatabaseHelper` (onCreate) | ✅ Tạo bảng đúng thứ tự dependency |

---

## Nhóm 1: BUG NGHIÊM TRỌNG – Gây lỗi chức năng

### 🔴 Bug 1: `AddTransactionViewModel.loadActiveWallet()` hardcode `null` userId

**File:** `ui/transaction/AddTransactionViewModel.java` (dòng 85, 88, 103)

```java
// ❌ HIỆN TẠI - Luôn query với userId = null, bỏ qua auth
List<Wallet> wallets = walletDao.getByUserId(null);
String savedWalletId = prefs.getString("active_wallet_id_" + null, null); // key = "active_wallet_id_null" !!!
public List<Wallet> getAllWallets() {
    return walletDao.getByUserId(null); // ❌ Không quan tâm đến userId
}
```

**Hậu quả:** Sau khi đăng nhập, người dùng tạo ví với `userId` cụ thể, nhưng `AddTransactionFragment` vẫn query ví với `userId=NULL` → **không hiển thị ví nào**, không thể thêm giao dịch.

**Sửa:** Inject `TokenStorage` vào `AddTransactionViewModel`, lấy `userId` đúng.

---

### 🔴 Bug 2: `CategoryDao.insert()` thiếu `created_at`, `updated_at`

**File:** `data/local/dao/CategoryDao.java`

```java
// ❌ HIỆN TẠI - ContentValues thiếu created_at và updated_at
// SQL_CREATE_TABLE yêu cầu NOT NULL nhưng insert không set → INSERT FAIL
```

**Hậu quả:** Thêm category mới sẽ **fail** hoặc văng exception do vi phạm NOT NULL constraint.

---

### 🔴 Bug 3: `CategoryDao.update()` thiếu `updated_at`

**File:** `data/local/dao/CategoryDao.java`

```java
// ❌ update() không set updated_at → mất tracking sync
```

**Hậu quả:** Server không phát hiện được thay đổi khi sync.

---

### 🔴 Bug 4: `WalletDao.delete()` xóa cứng thay vì soft delete

**File:** `data/local/dao/WalletDao.java`

```java
// ❌ Hard delete, mất dữ liệu, không sync được
public int delete(@NonNull String walletId) {
    return db.delete(WalletEntry.TABLE_NAME, WalletEntry.COLUMN_ID + " = ?", new String[]{walletId});
}
```

**Hậu quả:** Server không biết wallet đã bị xóa. `TransactionDao` và `BudgetDao` đã dùng soft delete đúng cách, `WalletDao` còn sót.

---

### 🔴 Bug 5: `CategoryDao.delete()` xóa cứng thay vì soft delete

**File:** `data/local/dao/CategoryDao.java`

Tương tự Bug 4 với Category.

---

### 🔴 Bug 6: `HomeViewModel.loadWallet()` bỏ qua userId của user đăng nhập

**File:** `ui/home/HomeViewModel.java`

```java
// ❌ Luôn query với userId = null → Home không hiển thị ví sau đăng nhập
List<Wallet> wallets = walletDao.getByUserId(null);
```

---

## Nhóm 2: VẤN ĐỀ ĐỒNG BỘ SCHEMA – Chưa nhất quán

### 🟡 Issue 1: `TransactionEntry.COLUMN_CATEGORY_ID` khai báo NOT NULL nhưng SQL server cho phép nullable

**File:** `DatabaseContract.java`

```java
// Contract: NOT NULL
COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +
// Nhưng database_sql.sql: category_id varchar(36) nullable (ON DELETE SET NULL)
```

**Sửa:** Thay `TEXT NOT NULL` → `TEXT`. Tăng `DATABASE_VERSION`.

---

### 🟡 Issue 2: `BudgetDatabaseHelper.onCreate()` thiếu 3 indexes

```java
// Thiếu: SQL_CREATE_INDEX_SYNC, idx_budgets_wallet, idx_wallets_user
```

---

### 🟡 Issue 3: SharedPreferences key `active_wallet_id_` không nhất quán

`BudgetViewModel` dùng key `"active_wallet_id_default_user_id"` khi userId null, còn `WalletViewModel` dùng `"active_wallet_id_null"` → **hai nơi dùng key khác nhau** → load sai ví.

---

## Nhóm 5: NAVIGATION FLOW – Kiểm tra luồng sau khi Save/Edit/Delete

### ✅ Budget – Tạo mới (`AddBudgetDialogFragment`)

**Luồng:** `BudgetFragment` → `showAddBudgetDialog()` → `AddBudgetDialogFragment`

- Save thành công → `onBudgetSavedListener.onBudgetSaved()` → `dismiss()`
- Listener set trong `BudgetFragment`: `dialog.setOnBudgetSavedListener(() -> viewModel.refresh())`
- **Kết quả:** ✅ Dialog đóng, BudgetFragment tự refresh.

---

### ✅ Budget – Sửa (`EditBudgetDialogFragment`)

- Save thành công → `onBudgetEditedListener.onBudgetEdited()` → `dismiss()`
- Listener: `dialog.setOnBudgetEditedListener(() -> viewModel.refresh())`
- **Kết quả:** ✅ Đúng.

---

### ✅ Budget – Xem chi tiết (`ViewBudgetDialogFragment`)

- Edit: `dismiss()` → `actionListener.onEditClicked(item)` → `showEditBudgetDialog()` ✅
- Delete: `dismiss()` → `actionListener.onDeleteClicked(item)` → `showDeleteConfirmation()` ✅

### ⚠️ Budget – Xóa: Gọi `refresh()` 2 lần

```java
// ❌ HIỆN TẠI: BudgetFragment.showDeleteConfirmation()
viewModel.deleteBudget(item.getId()); // bên trong đã gọi refresh()
viewModel.refresh();                  // ❌ dư thừa → query DB 2 lần
```

`BudgetViewModel.deleteBudget()` đã tự gọi `refresh()` khi thành công.

**Sửa:** Bỏ `viewModel.refresh()` dư thừa.

---

### ✅ Wallet – Thêm mới (`AddWalletFragment`)

- Save → `popBackStack()` → quay về `WalletFragment` ✅
- ViewModel gọi `loadAllWallets()` ngay sau insert → LiveData cập nhật → Fragment nhận được ✅

> [!WARNING]
> `WalletFragment` **không có `onResume()`**. Nếu orientation change xảy ra trước khi `popBackStack()`, dữ liệu có thể không được reload. Nên thêm `loadAllWallets()` vào `onResume()`.

---

### ⚠️ Wallet – Sửa: `EditWalletFragment` có thể load wallet null

```java
// EditWalletFragment.loadWalletData()
if (viewModel.getWallets().getValue() != null) { // ❌ Có thể null!
    for (Wallet w : viewModel.getWallets().getValue()) { ... }
}
// → currentWallet = null → form trống → không save được
```

Nếu user vào `EditWalletFragment` khi `WalletFragment` chưa load (restart, deep link), form sẽ trống hoàn toàn.

**Sửa:** `EditWalletFragment` nên tự load wallet bằng `walletDao.getWalletById(walletId)` thay vì tìm trong list.

---

### ✅ Wallet – Xóa từ `EditWalletFragment`

- `viewModel.deleteWallet()` → `popBackStack()` ✅
- **Chú ý nhỏ:** Toast "Đã xóa ví" được show thủ công dù delete có thể thất bại. Nên observe `saveResult`.

---

### ✅ Transaction – Thêm mới (`AddTransactionFragment`)

- Save → `popBackStack()` → `TransactionFragment.onResume()` gọi `loadData()` → reload ✅
- `EventBus` post `BudgetUpdateEvent` → `BudgetFragment` tự refresh ✅
- **Kết quả:** ✅ Luồng hoàn chỉnh.

---

### ✅ Category – Thêm, Sửa, Xóa (dialog inline trong `CategoryFragment`)

- Tất cả dùng `AlertDialog` inline → ViewModel CRUD → `addResult`/`updateResult`/`deleteResult` → Toast → tự reload ✅

### ⚠️ Category – Reload sau CRUD dùng sai method

```java
// CategoryViewModel.addCategoryWithIcon()
if (success) {
    loadCategories(userId);              // ❌ → getAllAvailable()
    // Nhưng Fragment ban đầu load bằng:
    // loadCategoriesForManagement()     // → getAllForManagement()
}
```

Sau CRUD thành công, danh sách reload không đồng nhất với lần load ban đầu → có thể thiếu/thừa items (đặc biệt với system categories).

**Sửa:** Trong `addCategoryWithIcon()`, `updateCategory()`, `deleteCategory()`, sau thành công gọi `loadCategoriesForManagement(userId)` thay vì `loadCategories(userId)`.

---

## Nhóm 3: CODE PHỨC TẠP – Cần Refactor

### 🔵 Refactor 1: SQL column list lặp 4 lần trong `TransactionDao`

Các method `getById()`, `getWithDetails()`, `getByDateRangeWithDetails()`, `getByCategoryId()` đều viết lại cùng một column list dài → vi phạm DRY.

**Sửa:** Trích xuất ra constant `QUERY_COLUMNS` và `BASE_JOIN_QUERY`.

---

### 🔵 Refactor 2: `BudgetRepository.getWalletById()` kém hiệu quả

```java
// ❌ Load tất cả wallet rồi loop
List<Wallet> wallets = walletDao.getByUserId(null); // toàn bộ!
for (Wallet w : wallets) { if (w.getId().equals(walletId)) return w; }
```

`WalletDao.getWalletById()` đã có sẵn.

---

### 🔵 Refactor 3: `TransactionViewModel` dùng `Double` thay vì `Long` cho tiền

```java
private final MutableLiveData<Double> totalBalance = new MutableLiveData<>(0.0); // ❌
```

Tất cả tiền trong DB là `INTEGER` (long), không cần floating point với VND.

---

### 🔵 Refactor 4: Inline adapters trong `AddTransactionFragment` (490 dòng)

`WalletPickerAdapter`, `CategoryPickerAdapter` nên tách ra file riêng trong `adapter/`.

---

### 🔵 Refactor 5: `CategoryDao` có 2 method trùng logic

`getAllAvailable()` và `getAllForManagement()` có cùng SQL logic nhưng tên khác nhau gây nhầm lẫn.

---

### 🔵 Refactor 6: `TransactionRepositoryImpl` filter thủ công trong bộ nhớ

```java
// ❌ Load tất cả rồi filter in-memory
transactions = transactionDao.getWithDetails(null, 0);
transactions = filterByDateRange(transactions, startDate, endDate);
```

Nên dùng WHERE clause trong SQL.

---

## Nhóm 4: CẢI THIỆN NHỎ – Nên làm

- **`HomeViewModel`** vẫn dùng `MockExpenseRepository` cho top expenses → cần thay bằng `TransactionDao` thật.
- **`WalletViewModel.saveWallet()`** check `isFirstWallet` sau `setValue()` có thể không đồng bộ.

---

## Kế hoạch thực hiện

### Phase 1: Fix Bug Nghiêm Trọng + Navigation Flow (ưu tiên cao nhất)

| # | File | Thay đổi |
|---|---|---|
| 1 | `CategoryDao.java` | Thêm `created_at`, `updated_at` vào `insert()`. Thêm `updated_at` vào `update()`. Chuyển `delete()` sang soft delete |
| 2 | `WalletDao.java` | Chuyển `delete()` từ hard delete sang soft delete (set `deleted_at`) |
| 3 | `AddTransactionViewModel.java` | Inject `TokenStorage`, fix `userId` cho `loadActiveWallet()` và `getAllWallets()` |
| 4 | `HomeViewModel.java` | Inject `TokenStorage`, dùng đúng `userId` cho `loadWallet()` |
| 5 | `BudgetFragment.java` | Bỏ `viewModel.refresh()` dư thừa trong `showDeleteConfirmation()` |
| 6 | `WalletFragment.java` | Thêm `loadAllWallets()` trong `onResume()` |
| 7 | `EditWalletFragment.java` | Tự load wallet bằng `walletDao.getWalletById(walletId)` thay vì tìm trong list |
| 8 | `CategoryViewModel.java` | Sau CRUD thành công, gọi `loadCategoriesForManagement(userId)` thay vì `loadCategories(userId)` |

### Phase 2: Đồng bộ Schema

| # | File | Thay đổi |
|---|---|---|
| 9 | `DatabaseContract.java` | Sửa `TransactionEntry.COLUMN_CATEGORY_ID` thành nullable (`TEXT`). Tăng `DATABASE_VERSION = 5` |
| 10 | `BudgetDatabaseHelper.java` | Thêm missing index calls: `SQL_CREATE_INDEX_SYNC`, `idx_budgets_wallet`, `idx_wallets_user` |
| 11 | `WalletViewModel.java` + `BudgetViewModel.java` | Chuẩn hóa SharedPrefs key `active_wallet_id_` thành helper method dùng chung |

### Phase 3: Refactor

| # | File | Thay đổi |
|---|---|---|
| 12 | `TransactionDao.java` | Extract SQL column list + JOIN base query thành constants/private method |
| 13 | `BudgetRepository.java` | Thay `getWalletById()` loop bằng `walletDao.getWalletById()` trực tiếp |
| 14 | `CategoryDao.java` | Rõ ràng hóa sự khác biệt giữa `getAllAvailable()` và `getAllForManagement()` bằng docstring rõ ràng |
| 15 | `TransactionRepositoryImpl.java` | Loại bỏ memory filter, dùng DAO với date range WHERE clause |
| 16 | `TransactionViewModel.java` | Đổi `Double` → `Long` cho các LiveData tiền tệ |

### Phase 4: Cải thiện nhỏ

| # | File | Thay đổi |
|---|---|---|
| 17 | `HomeViewModel.java` | Thay `MockExpenseRepository` bằng `TransactionDao` thật |
| 18 | `AddTransactionFragment.java` | Tách `WalletPickerAdapter`, `CategoryPickerAdapter` ra file riêng |
| 19 | `EditWalletFragment.java` | Observe `saveResult` để verify delete thành công thay vì Toast cứng |

---

## Verification Plan

### Sau mỗi phase
- Build thành công không có compilation error
- Manual test chức năng tương ứng

### Test cases chính

**Data & Schema:**
1. **Wallet**: Thêm ví → hiển thị đúng trong danh sách, EditFragment, AddTransaction picker
2. **Wallet delete**: Xóa ví → `deleted_at` được set, không xóa khỏi DB
3. **Category insert**: Thêm category mới → không fail, `created_at`/`updated_at` được set
4. **Category delete**: Xóa category → soft delete, `deleted_at` được set
5. **Transaction**: Thêm giao dịch → ví đúng được chọn (không phải ví null)
6. **Budget**: Load đúng budget items theo ví được chọn
7. **Sync fields**: Kiểm tra `updated_at` được cập nhật khi sửa record

**Navigation Flow:**
8. **Budget tạo mới**: Save xong → dialog đóng → BudgetFragment hiện item mới ngay
9. **Budget sửa**: Save xong → dialog đóng → BudgetFragment cập nhật item
10. **Budget xóa**: Xóa xong → item biến mất, DB chỉ được query 1 lần
11. **Wallet thêm**: Save → quay về WalletFragment → danh sách có ví mới (kể cả sau orientation change)
12. **Wallet sửa**: Mở EditWalletFragment → form điền sẵn dữ liệu ví đúng (kể cả sau restart)
13. **Category thêm**: Save → danh sách reload đúng (hiện cả system + user categories)
14. **Category sửa/xóa**: Reload danh sách đồng nhất với lần load ban đầu

> [!IMPORTANT]
> `DATABASE_VERSION` cần tăng lên `5` khi thay đổi schema (sửa `category_id` nullable và thêm indexes). Tất cả dữ liệu cũ sẽ bị xóa (destructive upgrade) do `onUpgrade()` hiện tại drop all tables.

> [!WARNING]
> Các file adapter inline trong `AddTransactionFragment` nếu tách ra cần kiểm tra layout `item_wallet_select.xml` và `item_category_select.xml` đã tồn tại chưa trước khi tách.
