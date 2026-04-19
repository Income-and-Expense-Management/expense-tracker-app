# SYSTEM DESIGN - QuanLiChiTieu

> Tài liệu ngữ cảnh hệ thống (System Context Document) dành cho AI Coding Agent.
> Mọi tính năng mới **bắt buộc** phải tuân theo kiến trúc, quy chuẩn và luồng dữ liệu được mô tả trong tài liệu này.

---

## 1. TỔNG QUAN DỰ ÁN (Project Overview)

### 1.1 Mô tả
QuanLiChiTieu là ứng dụng Android quản lý chi tiêu cá nhân, cho phép người dùng theo dõi thu nhập, chi tiêu, quản lý nhiều ví tiền và thiết lập ngân sách theo danh mục. Ứng dụng hỗ trợ đơn vị tiền tệ VND. Kiến trúc hướng tới **Offline-First** với khả năng **đồng bộ dữ liệu lên server** (sync) thông qua các cột `updated_at` và `deleted_at`.

### 1.2 Tech Stack

| Thành phần         | Công nghệ                                          |
|---------------------|-----------------------------------------------------|
| Ngôn ngữ            | Java 11                                             |
| Platform             | Android (minSdk 27, targetSdk 36)                   |
| Build System         | Gradle 8.13.1 + Version Catalog (`libs.versions.toml`) |
| UI Framework         | Android Views (XML Layout) + Material Design 1.13   |
| Navigation           | Fragment-based (hide/show) + BottomNavigationView   |
| State Management     | ViewModel + LiveData (androidx.lifecycle 2.9)        |
| Database             | SQLite (raw SQLiteOpenHelper + DAO Pattern)          |
| Architecture Pattern | MVVM + Repository Pattern + DAO Pattern              |
| DI Strategy          | Manual instantiation (không dùng Hilt/Dagger)        |
| Network              | **Volley** (API Request)                             |
| Auth                 | **Google Sign-In API** + REST API (email/password) + JWT Storage (EncryptedSharedPreferences) |
| Event Driven         | In-house Singleton EventBus                          |

### 1.3 Kiến trúc tổng thể
- **Offline-First**: Toàn bộ dữ liệu lưu tại SQLite local. Các cột `updated_at` và `deleted_at` trên mọi bảng phục vụ cho việc sync với server.
- **MVVM (Model-View-ViewModel)**: UI (Fragment/Activity) observe LiveData từ ViewModel; ViewModel gọi Repository/DAO.
- **Repository Pattern**: Interface trừu tượng hóa nguồn dữ liệu; hiện tại dùng implementation thật (DAO), sẵn sàng thay bằng API implementation mà không ảnh hưởng UI.
- **DAO Pattern**: Mỗi bảng database có một DAO class riêng biệt, tuân thủ Single Responsibility Principle.
- **Soft Delete**: Thay vì xóa vật lý, tất cả các entity (Wallet, Category, Transaction, Budget) dùng cột `deleted_at`. Record bị xóa khi `deleted_at IS NOT NULL`. Server dùng trường này để nhận biết bản ghi đã bị xóa khi sync.

---

## 2. CẤU TRÚC THƯ MỤC VÀ MODULE (Directory & Module Structure)

### 2.1 Cây thư mục tổng quan

```
QuanLiChiTieu/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ptithcm/quanlichitieu/
│       │   ├── data/
│       │   │   ├── local/
│       │   │   │   ├── contract/
│       │   │   │   │   └── DatabaseContract.java        # Schema & SQL constants (version 5)
│       │   │   │   ├── dao/
│       │   │   │   │   ├── UserDao.java
│       │   │   │   │   ├── WalletDao.java
│       │   │   │   │   ├── CategoryDao.java
│       │   │   │   │   ├── TransactionDao.java
│       │   │   │   │   └── BudgetDao.java
│       │   │   │   ├── token/
│       │   │   │   │   ├── TokenStorage.java            # Interface lưu token
│       │   │   │   │   └── EncryptedTokenStorage.java   # SharedPreferences mã hóa
│       │   │   │   ├── util/
│       │   │   │   │   ├── CursorUtils.java
│       │   │   │   │   └── IdGenerator.java
│       │   │   │   ├── BudgetDatabaseHelper.java        # SQLiteOpenHelper (Singleton)
│       │   │   │   └── DatabaseManager.java             # Trung tâm cung cấp DAO (Singleton)
│       │   │   ├── model/
│       │   │   │   ├── User.java
│       │   │   │   ├── Wallet.java
│       │   │   │   ├── Category.java
│       │   │   │   ├── Transaction.java
│       │   │   │   ├── Budget.java
│       │   │   │   ├── TransactionType.java             # Enum: INCOME / EXPENSE
│       │   │   │   ├── TransactionGroup.java            # UI model: nhóm giao dịch theo ngày
│       │   │   │   └── Expense.java                    # UI model: top chi tiêu
│       │   │   ├── remote/
│       │   │   │   ├── ApiConfig.java                   # Cấu hình URL API remote
│       │   │   │   ├── AuthJsonObjectRequest.java       # Custom Volley Request với JWT Token
│       │   │   │   └── VolleySingleton.java             # Quản lý hàng đợi Volley
│       │   │   └── repository/
│       │   │       ├── AuthRepository.java              # Interface cho Auth
│       │   │       ├── AuthRepositoryImpl.java          # Thực thi Auth qua REST API (Volley)
│       │   │       ├── AuthService.java                 # Interface đơn giản hơn cho Auth
│       │   │       ├── GoogleSignInHelper.java          # Tiện ích đăng nhập Google
│       │   │       ├── TransactionRepository.java       # Interface cho Transaction
│       │   │       ├── TransactionRepositoryImpl.java   # Thực thi qua TransactionDao
│       │   │       ├── BudgetRepository.java            # Repository (Singleton) cho Budget
│       │   │       ├── CategoryRepository.java          # Repository cho Category
│       │   │       ├── ExpenseRepository.java           # Interface cho top-expenses
│       │   │       └── DbExpenseRepository.java         # Thực thi qua TransactionDao
│       │   ├── event/
│       │   │   ├── EventBus.java                        # Singleton cho Event driven
│       │   │   └── BudgetUpdateEvent.java               # Event khi ngân sách cập nhật
│       │   ├── ui/
│       │   │   ├── account/
│       │   │   │   └── AccountFragment.java             # Màn hình tài khoản
│       │   │   ├── budget/
│       │   │   │   ├── BudgetFragment.java
│       │   │   │   ├── BudgetViewModel.java
│       │   │   │   ├── adapter/BudgetAdapter.java
│       │   │   │   ├── bottomsheet/
│       │   │   │   │   ├── SelectCategoryBottomSheet.java
│       │   │   │   │   ├── SelectPeriodBottomSheet.java
│       │   │   │   │   └── SelectWalletBottomSheet.java
│       │   │   │   ├── dialog/
│       │   │   │   │   ├── AddBudgetDialogFragment.java
│       │   │   │   │   ├── EditBudgetDialogFragment.java
│       │   │   │   │   └── ViewBudgetDialogFragment.java
│       │   │   │   ├── model/BudgetItem.java
│       │   │   │   └── view/BudgetChartView.java
│       │   │   ├── category/
│       │   │   │   ├── CategoryFragment.java
│       │   │   │   ├── CategoryViewModel.java
│       │   │   │   ├── CategoryAdapter.java
│       │   │   │   └── IconPickerAdapter.java
│       │   │   ├── common/
│       │   │   │   ├── ArcProgressBar.java              # Custom View: progress cung tròn
│       │   │   │   └── SimpleLineChart.java             # Custom View: biểu đồ đường
│       │   │   ├── home/
│       │   │   │   ├── HomeFragment.java
│       │   │   │   ├── HomeViewModel.java
│       │   │   │   └── adapter/TopExpenseAdapter.java
│       │   │   ├── login/
│       │   │   │   ├── LoginActivity.java
│       │   │   │   ├── RegisterActivity.java
│       │   │   │   └── AuthViewModel.java
│       │   │   ├── main/
│       │   │   │   └── MainActivity.java
│       │   │   ├── transaction/
│       │   │   │   ├── TransactionFragment.java
│       │   │   │   ├── TransactionViewModel.java
│       │   │   │   ├── AddTransactionFragment.java
│       │   │   │   ├── AddTransactionViewModel.java
│       │   │   │   └── adapter/TransactionAdapter.java
│       │   │   └── wallet/
│       │   │       ├── WalletFragment.java
│       │   │       ├── WalletViewModel.java
│       │   │       ├── WalletAdapter.java
│       │   │       ├── AddWalletFragment.java
│       │   │       ├── EditWalletFragment.java
│       │   │       └── IconSelectionFragment.java
│       │   └── utils/
│       │       ├── DateUtils.java
│       │       ├── IdGenerator.java
│       │       └── ValidationUtils.java
│       └── res/
│           ├── layout/, drawable/, menu/, anim/, color/, values/, values-night/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### 2.2 Giải thích chức năng từng package

| Package | Nhiệm vụ |
|---------|-----------|
| `data.local.contract` | Định nghĩa schema database: tên bảng, tên cột, câu lệnh CREATE TABLE dưới dạng hằng số. **Mọi truy vấn SQL phải tham chiếu hằng số từ đây, không hardcode.** |
| `data.local.dao` | Mỗi DAO class đảm nhiệm toàn bộ CRUD cho một bảng. Nhận `BudgetDatabaseHelper` qua constructor, trả về model object. Không trả về `Cursor` ra ngoài. |
| `data.local.token` | `TokenStorage` (Interface) + `EncryptedTokenStorage` (impl dùng EncryptedSharedPreferences) quản lý JWT token và thông tin user. |
| `data.local.util` | `CursorUtils`: đọc Cursor an toàn, tránh crash khi column null. `IdGenerator`: tạo UUID và lấy timestamp hiện tại. |
| `data.local` | `BudgetDatabaseHelper` (SQLiteOpenHelper, Singleton) tạo/nâng cấp DB. `DatabaseManager` (Singleton) cung cấp DAO instances (lazy init) và hỗ trợ atomic transaction. |
| `data.model` | POJO/entity class đại diện cho bảng database. Tất cả dùng **Builder pattern**. `TransactionType` là enum (INCOME/EXPENSE) dùng chung. `Transaction` không còn lưu `type` và `icon_id` trực tiếp — hai giá trị này lấy qua quan hệ với `Category`. |
| `data.remote` | Package truy cập REST API: `ApiConfig` (URL constants), `VolleySingleton` (RequestQueue), `AuthJsonObjectRequest` (tự động đính kèm JWT Bearer token). |
| `data.repository` | Tầng trung gian giữa ViewModel và DAO/API. Hiện tất cả đã dùng **implementation thật** (không còn Mock). `AuthRepositoryImpl` gọi REST API. `TransactionRepositoryImpl`, `BudgetRepository`, `DbExpenseRepository` sử dụng DAO. |
| `event` | `EventBus` (Singleton) hỗ trợ giao tiếp sự kiện giữa Dialog/BottomSheet và Fragment (VD: `BudgetUpdateEvent`). |
| `ui.login` | Luồng xác thực: `LoginActivity` (launcher), `RegisterActivity`, `AuthViewModel` (kết hợp `AuthRepository` + Google Sign-In). |
| `ui.main` | `MainActivity` là Single Host Activity chứa `BottomNavigationView` + `FragmentContainer`. Quản lý 5 Fragment chính bằng hide/show. |
| `ui.home` | Trang chủ hiển thị tổng quan: số dư ví, biểu đồ, top chi tiêu. |
| `ui.budget` | Tính năng Ngân sách: Thêm, sửa, xem, theo dõi qua biểu đồ (`ArcProgressBar`). |
| `ui.transaction` | Danh sách giao dịch nhóm theo ngày, bộ lọc tháng. `AddTransactionFragment` xử lý thêm giao dịch. |
| `ui.wallet` | Quản lý ví: tạo/sửa (`AddWalletFragment`, `EditWalletFragment`), danh sách ví, chọn icon (`IconSelectionFragment`). |
| `ui.account` | Tài khoản cá nhân và cài đặt (placeholder). |
| `ui.category` | Quản lý danh mục: hiển thị, thêm, sửa, xóa, bật/tắt trạng thái. Tích hợp `IconPickerAdapter`. |
| `ui.common` | Custom View dùng chung: `SimpleLineChart`, `ArcProgressBar`. |
| `utils` | Các helper: `IdGenerator` (UUID + timestamp), `DateUtils` (xử lý ngày tháng), `ValidationUtils`. |

---

## 3. KIẾN TRÚC DỮ LIỆU & LUỒNG DỮ LIỆU (Data Architecture & Data Flow)

### 3.1 Database Schema (SQLite — version 5)

> **Chú ý quan trọng**: Tất cả các entity đều có `updated_at` và `deleted_at` để hỗ trợ sync. Wallet và Category dùng Soft Delete (`deleted_at`). Transaction dùng Soft Delete (`deleted_at`). Bảng `transactions` **không còn** cột `type` và `icon_id` — hai giá trị này được lấy qua JOIN với bảng `categories`.

```
┌─────────────────┐      ┌────────────────────┐      ┌──────────────────────┐
│     users        │      │     categories     │      │       wallets         │
├─────────────────┤      ├────────────────────┤      ├──────────────────────┤
│ id (PK, TEXT)    │◄──┐  │ id (PK, TEXT)      │  ┌──►│ id (PK, TEXT)         │
│ full_name        │   │  │ user_id (FK,NULL)  │──┘   │ user_id (FK, TEXT)    │
│ email (UNIQUE)   │   │  │ name               │      │ name                  │
│ avatar_url       │   │  │ type               │      │ initial_balance (INT) │
│ auth_provider    │   │  │  CHECK('INCOME',   │      │ currency (DEF 'VND')  │
│ password         │   │  │       'EXPENSE')   │      │ icon_id               │
│ created_at (INT) │   └──┤ icon_name          │      │ created_at (INT)      │
│ updated_at (INT) │      │ is_active (INT 0/1)│      │ updated_at (INT)      │
└─────────────────┘      │ created_at (INT)   │      │ deleted_at (INT,NULL) │
                         │ updated_at (INT)   │      └──────────┬────────────┘
                         │ deleted_at (INT,   │                 │
                         │           NULL)    │                 │
                         └────────┬───────────┘                 │
                                  │                             │
                                  ▼                             ▼
                    ┌─────────────────────────────────────────────────────┐
                    │                   transactions                       │
                    ├─────────────────────────────────────────────────────┤
                    │ id (PK, TEXT)                                        │
                    │ wallet_id (FK, TEXT NOT NULL) ──────────────────────►│
                    │ category_id (FK, TEXT, NULLABLE)  ──────► categories │
                    │   -- NULL khi category bị xóa (ON DELETE SET NULL)   │
                    │ amount (INTEGER NOT NULL)                            │
                    │ transaction_date (INTEGER NOT NULL)                  │
                    │ note (TEXT, NULLABLE)                                │
                    │ created_at  (INTEGER NOT NULL)                       │
                    │ updated_at  (INTEGER NOT NULL)                       │
                    │ deleted_at  (INTEGER, NULLABLE) -- soft delete        │
                    └─────────────────────────────────────────────────────┘
                    
                    ┌────────────────────────────────────┐
                    │              budgets                │
                    ├────────────────────────────────────┤
                    │ id (PK, TEXT)                       │
                    │ wallet_id (FK) ──► wallets.id       │
                    │ category_id (FK) ──► categories.id  │
                    │ target_amount (INTEGER NOT NULL)     │
                    │ start_date (INTEGER, NULLABLE)       │
                    │ end_date   (INTEGER, NULLABLE)       │
                    │ created_at (INTEGER NOT NULL)        │
                    │ updated_at (INTEGER NOT NULL)        │
                    │ deleted_at (INTEGER, NULLABLE)       │
                    └────────────────────────────────────┘
```

**Indexes được tạo:**
- `idx_transactions_wallet` ON transactions(wallet_id)
- `idx_transactions_category` ON transactions(category_id)
- `idx_transactions_date` ON transactions(transaction_date)
- `idx_transactions_sync` ON transactions(updated_at)  ← phục vụ sync
- `idx_budgets_wallet` ON budgets(wallet_id)
- `idx_wallets_user` ON wallets(user_id)

**Quy tắc dữ liệu quan trọng:**
- Tất cả ID dùng UUID (TEXT) do client tạo (`IdGenerator.generateUUID()`).
- Tất cả timestamp lưu dạng `long` (milliseconds since epoch).
- Tiền tệ lưu dạng `INTEGER` (VND, không dùng số thập phân).
- Boolean lưu dạng `INTEGER` (0/1) trong SQLite.
- Enum `TransactionType` lưu dạng `TEXT` ("INCOME" / "EXPENSE") trong cột `type` ở bảng `categories`.
- Foreign Key constraints được **bật** trong `onConfigure()` bằng `setForeignKeyConstraintsEnabled(true)`.
- Category hệ thống có `user_id = NULL`, category người dùng có `user_id` cụ thể.
- **Soft Delete**: Record bị "xóa" khi `deleted_at IS NOT NULL`. Mọi query đọc dữ liệu đều phải thêm điều kiện `deleted_at IS NULL`.
- **Bảng `transactions` KHÔNG có cột `type` và `icon_id`**: lấy qua JOIN với `categories` (JOIN type = LEFT JOIN để xử lý `category_id = NULL`).
- `DATABASE_VERSION = 5`. Khi nâng cấp schema, phải tăng version và xử lý `onUpgrade()`.

### 3.2 Luồng dữ liệu cơ bản (Data Flow)

```
┌──────────────────────────────────────────────────────────────────┐
│                         UI LAYER                                  │
│  Fragment/Activity ──observe──► LiveData ◄──setValue── ViewModel  │
│       │                                                    │      │
│       │  (user action)                        (business logic)   │
│       ▼                                                    ▼      │
│  ViewModel.methodCall()  ──────────────────►  Repository/DAO     │
└──────────────────────────────────────────────────────────────────┘
                                                       │
                                                       ▼
┌──────────────────────────────────────────────────────────────────┐
│                        DATA LAYER                                 │
│                                                                   │
│  Repository (Interface / Concrete Class)                          │
│       │                                                           │
│       └── DAO (truy vấn trực tiếp SQLite qua DatabaseManager)   │
│              │                                                    │
│              ▼                                                    │
│  DatabaseManager (Singleton)                                      │
│       │                                                           │
│       ▼                                                           │
│  BudgetDatabaseHelper (SQLiteOpenHelper, Singleton)              │
│       │                                                           │
│       ▼                                                           │
│  SQLite Database (quanlichitieu.db)                              │
└──────────────────────────────────────────────────────────────────┘
```

**Chi tiết luồng cho một thao tác ghi (VD: Tạo Transaction mới):**

1. `AddTransactionFragment` → User nhập thông tin, nhấn "Lưu".
2. `AddTransactionFragment` → Gọi `AddTransactionViewModel.saveTransaction(...)`.
3. `AddTransactionViewModel` → Validate → Tạo `Transaction` object bằng Builder (KHÔNG set `type` hay `icon_id` trực tiếp vào Transaction, chỉ set `categoryId`).
4. `TransactionDao.insert(transaction)` → Set UUID + timestamp → Tạo ContentValues → `db.insert()`.
5. `AddTransactionViewModel` → Cập nhật `MutableLiveData<SaveResult>`.
6. `AddTransactionFragment` → Observe `SaveResult` → Hiển thị Toast → Pop back stack.

**Chi tiết luồng cho một thao tác đọc (VD: Load danh sách giao dịch):**

1. `TransactionFragment.onViewCreated()` → Gọi `TransactionViewModel.loadData(walletId, monthOffset)`.
2. `TransactionViewModel` → Gọi `TransactionRepositoryImpl.getTransactionsByWalletAndMonth(walletId, offset)`.
3. `TransactionRepositoryImpl` → Tính `startDate`/`endDate` → Gọi `TransactionDao.getByDateRangeWithDetails(walletId, start, end)`.
4. `TransactionDao` → Thực hiện JOIN query (transactions LEFT JOIN categories INNER JOIN wallets) → Trả về `List<Transaction>` (có đầy đủ `category.name`, `category.type`, `category.iconName`, `walletName`).
5. `TransactionRepositoryImpl` → Nhóm theo ngày → Trả về `List<TransactionGroup>`.
6. `TransactionViewModel` → `transactions.setValue(groups)`.
7. `TransactionFragment` → Observe LiveData → `TransactionAdapter.setGroups(groups)`.

### 3.3 Cơ chế lấy `type` và `icon` của Transaction

**QUAN TRỌNG:** Bảng `transactions` không còn cột `type` và `icon_id`. Hai giá trị này được lấy từ bảng `categories` qua JOIN.

`TransactionDao` sử dụng `BASE_JOIN_QUERY` với LEFT JOIN để lấy kèm thông tin category:
```sql
SELECT t.*, c.name AS category_name, c.type AS category_type, c.icon_name AS icon_name, w.name AS wallet_name
FROM transactions t
LEFT JOIN categories c ON t.category_id = c.id
INNER JOIN wallets w ON t.wallet_id = w.id
WHERE t.deleted_at IS NULL
```

Model `Transaction` vẫn cung cấp convenience getters:
- `transaction.getType()` → `category.getType()` (có thể null nếu category bị xóa)
- `transaction.getIconId()` → `category.getIconName()`
- `transaction.getCategoryName()` → `category.getName()`
- `transaction.isExpense()` / `transaction.isIncome()` → kiểm tra null-safe

### 3.4 Quản lý trạng thái (State Management)

- **ViewModel** giữ toàn bộ state dưới dạng `MutableLiveData`.
- **Fragment/Activity** chỉ observe `LiveData` (read-only) và gọi method trên ViewModel.
- **Login state** dùng pattern với các trạng thái: `IDLE`, `LOADING`, `SUCCESS`, `ERROR`.
- **Save operations** trả về `SaveResult` object chứa `success` flag và `message`.
- **Wallet selection state** lưu trong `SharedPreferences` (không lưu `is_active` trong DB). `WalletViewModel` là ViewModel shared giữa các Fragment (scope = Activity).

### 3.5 Cơ chế đồng bộ (Sync Design — Local-First)

Cơ sở dữ liệu hỗ trợ sẵn sàng cho đồng bộ thông qua quá trình:
- `updated_at`: Timestamp cuối phiên bản. Client dựa vào đây để xác định bản ghi mới nhất.
- `deleted_at`: Soft delete. Server và client nhận biết bản ghi đã bị xóa mà không mất dữ liệu liên kết.
- Index `idx_transactions_sync` ON `updated_at` phục vụ lấy danh sách thay đổi nhanh chóng.

**Chiến lược đồng bộ hiện tại: Local-First**
- **Thao tác Ghi (Push)**: Được thực hiện **ngay lập tức** vào SQLite trên background thread để phản hồi UI tức thì. Ngay sau đó, trigger một request Volley (fire-and-forget) lên REST API của server.
- **Thao tác Đọc (Pull / UPSERT)**: Kéo danh sách mới từ server về (`fetchFromServer`), tiến hành UPSERT vào local database:
  - Nếu `id` chưa tồn tại -> `insertFromServer()`.
  - Nếu `id` đã tồn tại và `updated_at` server > local -> `updateFromServer()`.
  - Giữ **nguyên** timestamp từ server, không dùng dấu thời gian của client lúc pull.

**Các quy ước Networking (API-DOCUMENT):**
- **Client-generated UUID**: Client tự sinh `id` ngay khi tạo mới và **bắt buộc gửi lên server** lúc tạo (POST) thay vì để server tự sinh, tránh việc lệch UUID giữa local và remote.
- **Tiền tệ kiểu String**: Do server dùng BigInt, tất cả thông tin tiền (`initial_balance`, `amount`) gửi lên qua dạng chuỗi (VD `"1000000"`).
- **ISO8601 Time**: Thời gian nhận về luôn dạng chuỗi (`2026-04-18T10:00:00Z`). Client có hàm parse sang `long` epoch ms.
- **Thao tác xóa (DELETE)**: Server trả về HTTP `204 No Content`. Client sử dụng custom Volley Request để tránh lỗi parse.

### 3.6 Navigation Flow

```
LoginActivity (Launcher)
    ├── [Login thành công] ──► MainActivity
    └── [Nhấn "Sign Up"]  ──► RegisterActivity
                                    └── [Đăng ký thành công] ──► MainActivity

MainActivity (Host)
    ├── HomeFragment        (tab 1 - mặc định)
    │       └── [Xem ví / top expense] ──► các Fragment con
    ├── TransactionFragment (tab 2)
    │       └── [Nhấn "+"] ──► AddTransactionFragment (replace + backstack)
    ├── BudgetFragment      (tab 3)
    │       └── [Nhấn "Thêm"] ──► Dialog AddBudget, etc.
    ├── WalletFragment      (tab 4)
    │       └── [Thêm/Sửa ví] ──► AddWalletFragment / EditWalletFragment (replace + backstack)
    └── AccountFragment     (tab 5 - placeholder)
```

- Các tab Fragment được **add tất cả lúc khởi tạo** và dùng **hide/show** để chuyển tab (không recreate).
- Fragment con dùng **replace + addToBackStack** trên `R.id.fragmentContainer`.
- Back navigation: pop back stack, sau đó quay về tab đang chọn, hoặc exit app.

---

## 4. QUY CHUẨN LẬP TRÌNH (Coding Conventions & Rules)

### 4.1 Quy tắc đặt tên

| Thành phần | Quy tắc | Ví dụ |
|------------|---------|-------|
| Package | lowercase, phân cấp theo feature | `ui.home`, `data.local.dao` |
| Class (Activity) | PascalCase + hậu tố `Activity` | `LoginActivity`, `MainActivity` |
| Class (Fragment) | PascalCase + hậu tố `Fragment` | `HomeFragment`, `AddWalletFragment` |
| Class (ViewModel) | PascalCase + hậu tố `ViewModel` | `HomeViewModel`, `WalletViewModel` |
| Class (DAO) | PascalCase + hậu tố `Dao` | `TransactionDao`, `WalletDao` |
| Class (Adapter) | PascalCase + hậu tố `Adapter` | `TopExpenseAdapter`, `TransactionAdapter` |
| Class (Model/Entity) | PascalCase, danh từ đơn | `Transaction`, `Wallet`, `Category` |
| Class (Repository) | PascalCase + hậu tố `Repository` hoặc `RepositoryImpl` | `BudgetRepository`, `TransactionRepositoryImpl` |
| Interface Callback | PascalCase + hậu tố `Callback` | `LoginCallback`, `RegisterCallback` |
| Enum | PascalCase, giá trị UPPER_SNAKE_CASE | `TransactionType.INCOME` |
| Hằng số | UPPER_SNAKE_CASE | `EXTRA_USERNAME`, `TABLE_NAME` |
| Biến instance | camelCase | `walletDao`, `transactionDate` |
| LiveData | cặp private MutableLiveData / public LiveData getter | `MutableLiveData<> _wallets` → `LiveData<> getWallets()` |
| Layout XML | snake_case với prefix loại | `activity_login.xml`, `fragment_home.xml`, `item_transaction.xml` |
| View ID | camelCase với prefix viết tắt loại view | `tvHomeTitle`, `rvTopExpenses`, `etName`, `btnSave` |
| Drawable | snake_case với prefix | `ic_food.xml`, `bg_card_dark_rounded.xml` |
| Color | snake_case với prefix feature | `home_accent_green`, `login_bg_dark_green` |
| String resource | snake_case | `login_success`, `enter_email_password` |
| Database column | snake_case | `wallet_id`, `transaction_date`, `created_at` |
| Database table | snake_case, số nhiều | `users`, `wallets`, `transactions` |

### 4.2 Design Patterns bắt buộc

#### 4.2.1 Builder Pattern cho Model
Tất cả model class **phải** có inner `Builder` class:
```java
Transaction transaction = new Transaction.Builder()
        .setWalletId(walletId)
        .setCategoryId(categoryId)       // lưu FK, KHÔNG set type hay iconId trực tiếp
        .setAmount(amount)
        .setTransactionDate(System.currentTimeMillis())
        .setNote(note)
        .build();
```

#### 4.2.2 Singleton Pattern cho Database & Service
- `DatabaseManager.getInstance(context)` — Trung tâm quản lý DB.
- `BudgetDatabaseHelper.getInstance(context)` — SQLiteOpenHelper.
- `BudgetRepository.getInstance(context)` — Budget repository.
- Dùng **double-checked locking** với `volatile`.

#### 4.2.3 Repository Pattern cho Data Access
- `TransactionRepositoryImpl` implements `TransactionRepository` — sử dụng DAO thật.
- `AuthRepositoryImpl` implements `AuthRepository` — gọi REST API qua Volley.
- `BudgetRepository` — Singleton, kết hợp BudgetDao + CategoryDao + TransactionDao.
- `DbExpenseRepository` implements `ExpenseRepository` — tính top expense từ DAO.
- Khi tích hợp thêm remote sync: tạo implementation class mới, không sửa UI code.

#### 4.2.4 DAO Pattern cho Database
- Mỗi bảng có **đúng một DAO class** trong `data.local.dao`.
- DAO nhận `BudgetDatabaseHelper` qua constructor.
- DAO trả về model object, không trả về `Cursor`.
- Mapping Cursor → Model trong private method `cursorToXxx()`.
- Dùng `CursorUtils.getXxx()` cho mọi thao tác đọc Cursor.

#### 4.2.5 LiveData cho State
- ViewModel expose `LiveData<T>` (read-only) ra ngoài.
- Bên trong ViewModel dùng `MutableLiveData<T>`.
- Fragment/Activity **chỉ observe**, không trực tiếp set giá trị.

### 4.3 Quy tắc xử lý Database

- **Luôn** dùng hằng số từ `DatabaseContract` khi viết SQL. Không hardcode tên bảng/cột.
- **Luôn** đóng `Cursor` trong block `finally` (hoặc dùng try-with-resources).
- **Luôn** tạo UUID bằng `IdGenerator.generateUUID()`.
- **Luôn** lấy timestamp bằng `IdGenerator.getCurrentTimestamp()`.
- **Luôn** dùng `CursorUtils.getXxx()` để đọc Cursor an toàn.
- **Luôn** convert Enum ↔ String khi đọc/ghi database (`TransactionType.getValue()` / `TransactionType.fromValue()`).
- **Luôn** thêm `WHERE deleted_at IS NULL` cho mọi query đọc dữ liệu active.
- **Luôn** dùng Soft Delete: ghi `deleted_at = NOW` thay vì `DELETE`.
- **Luôn** cập nhật `updated_at = NOW` khi insert hoặc update bất kỳ record nào.
- Dùng `DatabaseManager.executeInTransaction()` cho các thao tác multi-step cần atomic.
- **KHÔNG** thêm cột `type` hay `icon_id` vào bảng `transactions`. Lấy qua JOIN với `categories`.

### 4.4 Quy tắc UI

- **Fragment** là đơn vị UI chính, không tạo Activity mới (trừ luồng Auth).
- Dùng `ViewModelProvider(this).get(XxxViewModel.class)` để lấy ViewModel.
- ViewModel shared (scope Activity): `ViewModelProvider(requireActivity()).get(XxxViewModel.class)`.
- Format tiền: `String.format(Locale.getDefault(), "%,d đ", amount)` cho long.
- RecyclerView Adapter dùng `notifyDataSetChanged()` (chưa dùng DiffUtil).
- Click listener dùng interface callback pattern.
- Prefix ID cho view: `tv` (TextView), `et` (EditText), `btn` (Button), `rv` (RecyclerView), `img` (ImageView), `fab` (FloatingActionButton).

### 4.5 Quy tắc ViewModel

- ViewModel cần Database access: extend `AndroidViewModel` (cần `Application` context).
- ViewModel thuần (không cần context): extend `ViewModel`.
- Mọi business logic (validate, format, query) nằm trong ViewModel, **không nằm trong Fragment**.
- State cho async operation: `enum State { IDLE, LOADING, SUCCESS, ERROR }` + wrapper class.
- Background operations (DB query) nên chạy trên background thread (hiện tại chưa dùng coroutines/RxJava — nếu query nặng, dùng `ExecutorService` trong ViewModel).

---

## 5. HƯỚNG DẪN THÊM TÍNH NĂNG MỚI (Guide for AI Agents)

### 5.1 Checklist: Thêm một Entity/Bảng mới

```
□ Bước 1: Cập nhật DatabaseContract
  - Thêm inner class mới trong DatabaseContract.java implements BaseColumns.
  - Định nghĩa: TABLE_NAME, COLUMN_xxx (bao gồm COLUMN_CREATED_AT, COLUMN_UPDATED_AT, COLUMN_DELETED_AT),
    SQL_CREATE_TABLE, SQL_DROP_TABLE.
  - Thêm indexes nếu cần (COLUMN_UPDATED_AT để hỗ trợ sync).

□ Bước 2: Cập nhật BudgetDatabaseHelper
  - Thêm db.execSQL(NewEntry.SQL_CREATE_TABLE) trong onCreate() theo đúng thứ tự dependency.
  - Thêm db.execSQL(NewEntry.SQL_DROP_TABLE) trong onUpgrade() theo thứ tự ngược.
  - Tăng DATABASE_VERSION trong DatabaseContract.

□ Bước 3: Tạo Model class
  - Tạo file trong data/model/.
  - Fields bắt buộc: id (String UUID), createdAt (long), updatedAt (long), deletedAt (Long nullable).
  - Thêm constructor đầy đủ tham số, Builder inner class, getters/setters.

□ Bước 4: Tạo DAO class
  - Tạo file trong data/local/dao/.
  - Constructor nhận BudgetDatabaseHelper.
  - Implement CRUD: insert(), getById(), getAll/getByXxx(), update(), delete() (soft delete).
  - insert(): auto-set UUID, createdAt, updatedAt.
  - update(): auto-set updatedAt.
  - delete(): soft delete — set deleted_at = NOW, không dùng db.delete().
  - Mọi query đọc: thêm WHERE deleted_at IS NULL.
  - Viết cursorToXxx() private method cho mapping.
  - Dùng CursorUtils cho mọi thao tác đọc Cursor.

□ Bước 5: Đăng ký DAO trong DatabaseManager
  - Thêm field: private XxxDao xxxDao;
  - Thêm getter method: public synchronized XxxDao getXxxDao() { ... }
  - Thêm xxxDao = null; trong close().
```

### 5.2 Checklist: Thêm một màn hình UI mới

```
□ Bước 1: Tạo Layout XML
  - Tạo file res/layout/fragment_xxx.xml (hoặc activity_xxx.xml).
  - Đặt tên View ID theo quy tắc: tvXxx, etXxx, btnXxx, rvXxx.

□ Bước 2: Tạo ViewModel
  - Tạo file ui/feature/XxxViewModel.java.
  - Extend AndroidViewModel nếu cần DB context, ViewModel nếu không.
  - Khai báo MutableLiveData cho mỗi state cần track.
  - Expose ra ngoài bằng LiveData getter.
  - Đặt mọi business logic và data fetching trong ViewModel.

□ Bước 3: Tạo Fragment
  - Tạo file ui/feature/XxxFragment.java.
  - Override onCreateView() → inflate layout.
  - Override onViewCreated() → initViews(), setupXxx(), observeViewModel().
  - Lấy ViewModel: new ViewModelProvider(this).get(XxxViewModel.class).
  - Observe LiveData trong observeViewModel().

□ Bước 4: Tạo Adapter (nếu có RecyclerView)
  - Tạo file ui/feature/adapter/XxxAdapter.java.
  - Extend RecyclerView.Adapter<XxxViewHolder>.
  - Tạo inner ViewHolder class.
  - Thêm setData() method + callback interface cho click.
  - Tạo layout res/layout/item_xxx.xml cho item view.

□ Bước 5: Đăng ký Navigation
  - Nếu là tab chính: Thêm vào MainActivity (initFragments, restoreFragments, setupBottomNav).
  - Nếu là màn hình con: Dùng FragmentTransaction.replace() + addToBackStack().
  - Cập nhật res/menu/bottom_nav_menu.xml nếu thêm tab.
```

### 5.3 Checklist: Thêm trường dữ liệu mới vào Entity có sẵn

```
□ Bước 1: Thêm COLUMN_XXX trong DatabaseContract (inner class tương ứng).
□ Bước 2: Cập nhật SQL_CREATE_TABLE trong DatabaseContract.
□ Bước 3: Tăng DATABASE_VERSION.
□ Bước 4: Cập nhật onUpgrade() nếu dùng migration (hoặc vẫn dùng destructive upgrade).
□ Bước 5: Thêm field vào Model class + Builder setter + getter.
□ Bước 6: Cập nhật DAO: thêm trường vào ContentValues trong insert()/update(), và đọc trong cursorToXxx().
```

### 5.4 Checklist: Viết query JOIN Transaction

```
□ Luôn dùng BASE_JOIN_QUERY trong TransactionDao làm base.
□ Transaction type/icon → lấy qua alias category_type, icon_name từ JOIN.
□ Dùng LEFT JOIN categories (không phải INNER JOIN) vì category_id có thể NULL.
□ Dùng INNER JOIN wallets vì wallet_id NOT NULL.
□ Luôn thêm WHERE t.deleted_at IS NULL.
□ Map Cursor dùng CursorUtils với tên alias (VD: "category_type", "icon_name", "wallet_name").
```

### 5.5 Quy tắc bổ sung khi thêm tính năng

- **Drawable icon**: Đặt trong `res/drawable/`, đặt tên `ic_xxx.xml` (vector drawable).
- **Color mới**: Thêm vào `res/values/colors.xml` với prefix feature name (VD: `budget_accent_blue`).
- **String mới**: Thêm vào `res/values/strings.xml`, dùng snake_case.
- **Animation**: Đặt trong `res/anim/`. Hiện có `fade_in.xml` và `fade_out.xml`.
- **Default categories**: Nếu cần thêm danh mục mặc định, thêm SQL INSERT trong `BudgetDatabaseHelper.insertDefaultCategories()`.
- **Không import thư viện mới** mà không thêm dependency vào `app/build.gradle.kts` và `libs.versions.toml`.
- **Không thêm MockRepository mới** — dự án đã chuyển sang dùng implementation thật.

---

## 6. THAY ĐỔI QUAN TRỌNG SO VỚI PHIÊN BẢN TRƯỚC

### 6.1 Database Schema (từ version ≤4 → version 5)

| Thay đổi | Chi tiết |
|---------|---------|
| **Thêm `updated_at`** | Tất cả bảng (users, wallets, categories, transactions, budgets) đều có `updated_at NOT NULL`. Auto-set khi insert/update. |
| **Thêm `deleted_at`** | Wallets, categories, transactions, budgets có `deleted_at` (NULL = active). Đây là cơ chế **Soft Delete** thay cho hard delete hoặc `is_active`. |
| **Xóa `type` khỏi transactions** | Bảng `transactions` không còn cột `type`. Lấy qua `categories.type` bằng JOIN. |
| **Xóa `icon_id` khỏi transactions** | Bảng `transactions` không còn cột `icon_id`. Lấy qua `categories.icon_name` bằng JOIN. |
| **`category_id` nullable** | Bảng `transactions.category_id` là TEXT nullable (không NOT NULL) — hỗ trợ ON DELETE SET NULL khi category bị xóa. |
| **Xóa `is_active` khỏi wallets** | Wallet selection dùng SharedPreferences (không còn cột `is_active` trong DB). Soft delete dùng `deleted_at`. |
| **Thêm indexes** | `idx_transactions_sync` ON updated_at, `idx_budgets_wallet`, `idx_wallets_user`. |

### 6.2 Repository Layer

| Thay đổi | Chi tiết |
|---------|---------|
| **Xóa Mock classes** | `MockAuthService`, `MockExpenseRepository`, `MockTransactionRepository` đã bị xóa. |
| **Thêm `DbExpenseRepository`** | Implementation thật cho `ExpenseRepository` dùng `TransactionDao`. |
| **`TransactionRepositoryImpl`** | Đã dùng thật DAO, hỗ trợ filter theo ví (`walletId`) và tháng. |
| **`BudgetRepository`** | Singleton, kết hợp nhiều DAO, tính toán spent amount từ DB. |

### 6.3 Model Layer

| Model | Thay đổi |
|-------|---------|
| `Transaction` | Bỏ field `type` và `icon_id` trực tiếp. Thêm `updatedAt`, `deletedAt`. Thêm embedded `Category category` (lấy qua JOIN) và `walletName`. Convenience getters: `getType()`, `getIconId()`, `getCategoryName()`, `isExpense()`, `isIncome()`. |
| `Wallet` | Bỏ `isActive`. Thêm `updatedAt`, `deletedAt`. Soft delete thay cho `is_active`. |
| `Category` | Thêm `updatedAt`, `deletedAt`. |
| `Budget` | Thêm `updatedAt`, `deletedAt`. |
| `User` | Thêm `updatedAt`. |
