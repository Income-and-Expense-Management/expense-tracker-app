# SYSTEM DESIGN - QuanLiChiTieu

> Tài liệu ngữ cảnh hệ thống (System Context Document) dành cho AI Coding Agent.
> Mọi tính năng mới **bắt buộc** phải tuân theo kiến trúc, quy chuẩn và luồng dữ liệu được mô tả trong tài liệu này.

---

## 1. TỔNG QUAN DỰ ÁN (Project Overview)

### 1.1 Mô tả
QuanLiChiTieu là ứng dụng Android quản lý chi tiêu cá nhân, cho phép người dùng theo dõi thu nhập, chi tiêu, quản lý nhiều ví tiền và thiết lập ngân sách theo danh mục. Ứng dụng hỗ trợ đơn vị tiền tệ VND.

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
| Auth                 | In-memory (Mock) + **Google Sign-In API** + JWT Storage (EncryptedSharedPreferences) |
| Event Driven         | In-house Singleton EventBus                          |

### 1.3 Kiến trúc tổng thể
- **Offline-First**: Toàn bộ dữ liệu lưu tại SQLite local, chưa có Remote API.
- **MVVM (Model-View-ViewModel)**: UI (Fragment/Activity) observe LiveData từ ViewModel; ViewModel gọi Repository/DAO.
- **Repository Pattern**: Interface trừu tượng hóa nguồn dữ liệu; hiện tại dùng Mock implementation, có thể thay bằng API implementation mà không ảnh hưởng UI.
- **DAO Pattern**: Mỗi bảng database có một DAO class riêng biệt, tuân thủ Single Responsibility Principle.

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
│       │   │   │   │   └── DatabaseContract.java        # Schema & SQL constants
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
│       │   │   │   ├── BudgetDatabaseHelper.java
│       │   │   │   └── DatabaseManager.java
│       │   │   ├── model/
│       │   │   │   ├── User.java, Wallet.java, Category.java, Transaction.java, Budget.java
│       │   │   │   ├── TransactionType.java, TransactionGroup.java, Expense.java
│       │   │   ├── remote/
│       │   │   │   ├── ApiConfig.java                   # Cấu hình API remote (Volley)
│       │   │   │   ├── AuthJsonObjectRequest.java       # Custom Volley Request với Token
│       │   │   │   └── VolleySingleton.java             # Quản lý hàng đợi Volley
│       │   │   └── repository/
│       │   │       ├── AuthRepository.java              # Interface cho API Auth
│       │   │       ├── AuthRepositoryImpl.java          # Thực thi API Auth
│       │   │       ├── AuthService.java, MockAuthService.java
│       │   │       ├── GoogleSignInHelper.java          # Tiện ích đăng nhập Google
│       │   │       ├── CategoryRepository.java, BudgetRepository.java
│       │   │       ├── ExpenseRepository.java, MockExpenseRepository.java
│       │   │       ├── TransactionRepository.java, MockTransactionRepository.java
│       │   │       └── TransactionRepositoryImpl.java   # Thực thi DB + Remote cho Transaction
│       │   ├── event/
│       │   │   ├── EventBus.java                        # Singleton cho Event driven
│       │   │   └── BudgetUpdateEvent.java               # Event khi ngân sách cập nhật
│       │   ├── ui/
│       │   │   ├── account/
│       │   │   │   └── AccountFragment.java             # Màn hình tài khoản
│       │   │   ├── budget/
│       │   │   │   ├── BudgetFragment.java              # Fragment ngân sách
│       │   │   │   ├── BudgetViewModel.java
│       │   │   │   ├── adapter/BudgetAdapter.java
│       │   │   │   ├── bottomsheet/
│       │   │   │   │   └── SelectCategoryBottomSheet.java, SelectPeriodBottomSheet.java, SelectWalletBottomSheet.java
│       │   │   │   ├── dialog/
│       │   │   │   │   └── AddBudgetDialogFragment.java, EditBudgetDialogFragment.java, ViewBudgetDialogFragment.java
│       │   │   │   ├── model/BudgetItem.java
│       │   │   │   └── view/BudgetChartView.java
│       │   │   ├── category/
│       │   │   │   ├── CategoryFragment.java, CategoryViewModel.java, CategoryAdapter.java
│       │   │   │   └── IconPickerAdapter.java
│       │   │   ├── common/
│       │   │   │   ├── ArcProgressBar.java              # Bar progress cung tròn
│       │   │   │   └── SimpleLineChart.java
│       │   │   ├── home/
│       │   │   │   ├── HomeFragment.java, HomeViewModel.java
│       │   │   │   └── adapter/TopExpenseAdapter.java
│       │   │   ├── login/
│       │   │   │   ├── LoginActivity.java, RegisterActivity.java
│       │   │   │   └── AuthViewModel.java               # ViewModel cho auth
│       │   │   ├── main/
│       │   │   │   └── MainActivity.java
│       │   │   ├── transaction/
│       │   │   │   ├── TransactionFragment.java, TransactionViewModel.java
│       │   │   │   ├── AddTransactionFragment.java      # Thêm giao dịch
│       │   │   │   ├── AddTransactionViewModel.java
│       │   │   │   └── adapter/TransactionAdapter.java
│       │   │   └── wallet/
│       │   │       ├── WalletFragment.java, WalletViewModel.java, WalletAdapter.java
│       │   │       ├── AddWalletFragment.java, EditWalletFragment.java
│       │   │       └── IconSelectionFragment.java
│       │   └── utils/
│       │       ├── DateUtils.java, IdGenerator.java, ValidationUtils.java
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
| `data.local.contract` | Định nghĩa schema database: tên bảng, tên cột, câu lệnh CREATE TABLE dưới dạng hằng số. Mọi truy vấn SQL phải tham chiếu hằng số từ đây. |
| `data.local.dao` | Mỗi DAO class đảm nhiệm toàn bộ CRUD cho một bảng. Nhận `BudgetDatabaseHelper`, trả về model object. |
| `data.local.token` | TokenStorage quản lý token và thông tin người dùng. `EncryptedTokenStorage` dùng EncryptedSharedPreferences. |
| `data.local.util` | Các utility class cho tầng database: `CursorUtils` đọc Cursor an toàn, tránh crash khi column null. |
| `data.local` | `BudgetDatabaseHelper` (SQLiteOpenHelper) tạo/nâng cấp DB. `DatabaseManager` (Singleton) cung cấp DAO instances và hỗ trợ transaction. |
| `data.model` | Các POJO/entity class đại diện cho bảng database. Các entity sử dụng Builder pattern. `TransactionType` là enum dùng chung. |
| `data.remote` | Package truy cập API: `ApiConfig`, `VolleySingleton`, `AuthJsonObjectRequest` tích hợp Json Request cùng Token tự động. |
| `data.repository` | Interface trừu tượng hóa nguồn dữ liệu + Concrete implementations (`Real/Mock` / `Impl`). Repository tự động chọn dữ liệu API hoặc nội bộ tuỳ logic. `AuthRepository` tích hợp API và Google Sign In. |
| `event` | EventBus đơn giản (EventBus.java) hỗ trợ bắn sự kiện từ Dialog, BottomSheet cho Fragment cập nhật dữ liệu (e.g. `BudgetUpdateEvent`). |
| `ui.login` | Luồng xác thực: `LoginActivity` (launcher), `RegisterActivity`, `AuthViewModel` (kết hợp `AuthRepository`). |
| `ui.main` | `MainActivity` là Single Host Activity chứa `BottomNavigationView` + `FragmentContainer`. Quản lý 5 Fragment chính bằng hide/show. |
| `ui.home` | Trang chủ hiển thị tổng quan: số dư ví, biểu đồ, top chi tiêu. |
| `ui.budget` | Tính năng Ngân sách: Thêm, sửa, xem, theo dõi qua biểu đồ (`ArcProgressBar`), logic tính còn bao nhiêu có thể tiêu... |
| `ui.transaction` | Danh sách giao dịch nhóm theo ngày, bộ lọc tháng. `AddTransactionFragment` xử lý việc thêm giao dịch. |
| `ui.wallet` | Quản lý ví: tạo/sửa (`AddWalletFragment`, `EditWalletFragment`), danh sách ví (`WalletFragment`), chọn icon (`IconSelectionFragment`). |
| `ui.account` | Tài khoản cá nhân và cài đặt (placeholder). |
| `ui.category` | Quản lý danh mục: hiển thị, thêm, sửa, xóa, bật/tắt trạng thái. Tích hợp `IconPickerAdapter`. |
| `ui.common` | Custom View dùng chung (VD: `SimpleLineChart`, `ArcProgressBar`). |
| `utils` | Các helper: `IdGenerator` tạo UUID, `DateUtils` xử lý ngày, `ValidationUtils` xác thực. |

---

## 3. KIẾN TRÚC DỮ LIỆU & LUỒNG DỮ LIỆU (Data Architecture & Data Flow)

### 3.1 Database Schema (SQLite)

```
┌─────────────┐      ┌──────────────┐      ┌──────────────────┐
│   users      │      │  categories  │      │     wallets       │
├─────────────┤      ├──────────────┤      ├──────────────────┤
│ id (PK)      │◄─┐  │ id (PK)      │  ┌──►│ id (PK)           │
│ full_name    │  │  │ user_id (FK) │──┘   │ user_id (FK)      │
│ email (UQ)   │  │  │ name         │      │ name              │
│ avatar_url   │  │  │ type         │      │ initial_balance   │
│ auth_provider│  │  │ icon_name    │      │ currency          │
│ created_at   │  └──┤              │      │ icon_id           │
└─────────────┘      └──────┬───────┘      │ created_at        │
                            │              │ updated_at        │
                            │              │ is_active         │
                            ▼              └────────┬──────────┘
                   ┌────────────────┐               │
                   │  transactions  │               │
                   ├────────────────┤               │
                   │ id (PK)        │               │
                   │ wallet_id (FK) │◄──────────────┘
                   │ category_id(FK)│◄──────── categories.id (ON DELETE SET NULL)
                   │ amount         │
                   │ type           │  CHECK('INCOME','EXPENSE')
                   │ transaction_date│
                   │ icon_id        │
                   │ note           │
                   │ created_at     │
                   │ updated_at     │
                   └────────────────┘
                   
                   ┌────────────────┐
                   │    budgets     │
                   ├────────────────┤
                   │ id (PK)        │
                   │ wallet_id (FK) │───► wallets.id (ON DELETE CASCADE)
                   │ category_id(FK)│───► categories.id (ON DELETE CASCADE)
                   │ target_amount  │
                   │ start_date     │
                   │ end_date       │
                   └────────────────┘
```

**Quy tắc dữ liệu quan trọng:**
- Tất cả ID dùng UUID (TEXT) do client tạo (`IdGenerator.generateUUID()`).
- Tất cả timestamp lưu dạng `long` (milliseconds since epoch).
- Tiền tệ lưu dạng `INTEGER` (VND, không dùng số thập phân).
- Boolean lưu dạng `INTEGER` (0/1) trong SQLite.
- Enum `TransactionType` lưu dạng `TEXT` ("INCOME" / "EXPENSE").
- Foreign Key constraints được **bật** trong `onConfigure()`.
- Category hệ thống có `user_id = NULL`, category người dùng có `user_id` cụ thể.
- Wallet hỗ trợ **soft delete** (đặt `is_active = 0`).

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
│  Repository (Interface)                                           │
│       │                                                           │
│       ├── MockRepository (mock data, dùng khi chưa có DB/API)   │
│       │                                                           │
│       └── DAO (truy vấn trực tiếp SQLite qua DatabaseManager)   │
│              │                                                    │
│              ▼                                                    │
│  DatabaseManager (Singleton)                                      │
│       │                                                           │
│       ▼                                                           │
│  BudgetDatabaseHelper (SQLiteOpenHelper)                         │
│       │                                                           │
│       ▼                                                           │
│  SQLite Database (quanlichitieu.db)                              │
└──────────────────────────────────────────────────────────────────┘
```

**Chi tiết luồng cho một thao tác ghi (VD: Tạo Wallet mới):**

1. `AddWalletFragment` → User nhập tên + số dư, nhấn "Lưu".
2. `AddWalletFragment.saveWallet()` → Gọi `WalletViewModel.saveWallet(name, balanceStr)`.
3. `WalletViewModel` → Validate input → Tạo `Wallet` object bằng Builder → Gọi `WalletDao.insert(wallet)`.
4. `WalletDao.insert()` → Tạo UUID nếu chưa có → Tạo `ContentValues` → `db.insert()`.
5. `WalletViewModel` → Cập nhật `MutableLiveData<SaveResult>`.
6. `AddWalletFragment` → Observe `SaveResult` → Hiển thị Toast → Pop back stack nếu thành công.

**Chi tiết luồng cho một thao tác đọc (VD: Load danh sách giao dịch):**

1. `TransactionFragment.onViewCreated()` → Gọi `TransactionViewModel.loadData()`.
2. `TransactionViewModel.loadData()` → Gọi `TransactionRepository.getTransactionsByMonth(offset)`.
3. `MockTransactionRepository` → Trả về mock data (sau này thay bằng DAO query).
4. `TransactionViewModel` → `transactions.setValue(data)`.
5. `TransactionFragment` → Observe `transactions` LiveData → `TransactionAdapter.setGroups(groups)`.

### 3.3 Quản lý trạng thái (State Management)

- **ViewModel** giữ toàn bộ state dưới dạng `MutableLiveData`.
- **Fragment/Activity** chỉ observe `LiveData` (read-only) và gọi method trên ViewModel.
- **Login state** dùng pattern `LoginState` (sealed-like class) với các trạng thái: `IDLE`, `LOADING`, `SUCCESS`, `ERROR`.
- **Save operations** trả về `SaveResult` object chứa `success` flag và `message`.

### 3.4 Cơ chế đồng bộ (Sync)

Hiện tại **chưa có cơ chế sync** giữa local và remote. Ứng dụng hoạt động hoàn toàn offline. Khi tích hợp API:
- Tạo class `ApiXxxRepository implements XxxRepository` trong `data.repository`.
- Sử dụng Retrofit/OkHttp cho network calls.
- Xem xét thêm `data.remote.dto` package cho Data Transfer Objects.
- DatabaseManager đã hỗ trợ `executeInTransaction()` để đảm bảo atomic operations khi sync.

### 3.5 Navigation Flow

```
LoginActivity (Launcher)
    ├── [Login thành công] ──► MainActivity
    └── [Nhấn "Sign Up"]  ──► RegisterActivity
                                    └── [Đăng ký thành công] ──► MainActivity

MainActivity (Host)
    ├── HomeFragment        (tab 1 - mặc định)
    │       └── [Nhấn "Xem tất cả" / cardWallet] ──► Add/Edit WalletFragment (replace)
    ├── TransactionFragment (tab 2)
    │       └── [Nhấn "+"] ──► AddTransactionFragment (replace + backstack)
    ├── BudgetFragment      (tab 3)
    │       └── [Nhấn "Thêm"] ──► Dialog AddBudget, etc.
    ├── WalletFragment      (tab 4)
    └── AccountFragment     (tab 5 - placeholder)
```

- Các tab Fragment được **add tất cả lúc khởi tạo** và dùng **hide/show** để chuyển tab (không recreate).
- Fragment con (VD: `AddWalletFragment`, `AddTransactionFragment`) dùng **replace + addToBackStack** trên `R.id.fragmentContainer`.
- Back navigation: quay về tab đang chọn trước, sau đó mới pop back stack, hoặc exit app.

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
| Class (Repository Interface) | PascalCase + hậu tố `Repository`/`Service` | `ExpenseRepository`, `AuthService` |
| Class (Mock Implementation) | `Mock` + tên interface | `MockExpenseRepository`, `MockAuthService` |
| Interface Callback | PascalCase + hậu tố `Callback` | `LoginCallback`, `RegisterCallback` |
| Enum | PascalCase, giá trị UPPER_SNAKE_CASE | `TransactionType.INCOME` |
| Hằng số | UPPER_SNAKE_CASE | `EXTRA_USERNAME`, `TABLE_NAME` |
| Biến instance | camelCase | `walletDao`, `transactionDate` |
| LiveData | camelCase, mutable thêm prefix `_` hoặc dùng cặp private/public | `MutableLiveData<> wallets` (private) → `LiveData<> getWallets()` (public) |
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
Wallet wallet = new Wallet.Builder()
        .setUserId(currentUserId)
        .setName("Ví chính")
        .setInitialBalance(1000000)
        .setCurrency("VND")
        .setIsActive(true)
        .build();
```

#### 4.2.2 Singleton Pattern cho Database & Service
- `DatabaseManager.getInstance(context)` — Trung tâm quản lý DB.
- `BudgetDatabaseHelper.getInstance(context)` — SQLiteOpenHelper.
- `MockAuthService.getInstance()` — Auth service.
- Dùng **double-checked locking** với `volatile`.

#### 4.2.3 Repository Pattern cho Data Access
- Khai báo **Interface** trong `data.repository`.
- Tạo **Mock Implementation** để phát triển UI trước.
- Khi tích hợp thật, tạo Implementation class mới, không sửa UI code.

#### 4.2.4 DAO Pattern cho Database
- Mỗi bảng có **đúng một DAO class** trong `data.local.dao`.
- DAO nhận `BudgetDatabaseHelper` qua constructor.
- DAO trả về model object, không trả về `Cursor`.
- Mapping Cursor → Model nằm trong private method `cursorToXxx()`.

#### 4.2.5 LiveData cho State
- ViewModel expose `LiveData<T>` (read-only) ra ngoài.
- Bên trong ViewModel dùng `MutableLiveData<T>`.
- Fragment/Activity **chỉ observe**, không trực tiếp set giá trị.

### 4.3 Quy tắc xử lý Database

- **Luôn** dùng hằng số từ `DatabaseContract` khi viết SQL. Không hardcode tên bảng/cột.
- **Luôn** đóng `Cursor` trong block `finally`.
- **Luôn** tạo UUID bằng `IdGenerator.generateUUID()`.
- **Luôn** dùng `CursorUtils.getXxx()` để đọc Cursor an toàn.
- **Luôn** convert Enum ↔ String khi đọc/ghi database (`TransactionType.getValue()` / `TransactionType.fromValue()`).
- **Luôn** convert Boolean ↔ int khi đọc/ghi database (`getIsActiveAsInt()` / `setActiveFromInt()`).
- Dùng `DatabaseManager.executeInTransaction()` cho các thao tác multi-step cần atomic.

### 4.4 Quy tắc UI

- **Fragment** là đơn vị UI chính, không tạo Activity mới (trừ luồng Auth).
- Dùng `ViewModelProvider(this).get(XxxViewModel.class)` để lấy ViewModel.
- Format tiền: `String.format(Locale.getDefault(), "%,d đ", amount)` cho long, `"%,.0f đ"` cho double.
- RecyclerView Adapter dùng `notifyDataSetChanged()` (chưa dùng DiffUtil).
- Click listener dùng interface callback pattern (VD: `OnExpenseClickListener`).
- Các prefix ID cho view: `tv` (TextView), `et` (EditText), `btn` (Button), `rv` (RecyclerView), `img` (ImageView), `fab` (FloatingActionButton).

### 4.5 Quy tắc ViewModel

- ViewModel cho Fragment có Database access: extend `AndroidViewModel` (cần `Application` context).
- ViewModel thuần (chỉ dùng mock/in-memory): extend `ViewModel`.
- Mọi business logic (validate, format, query) nằm trong ViewModel, **không nằm trong Fragment**.
- State cho async operation dùng pattern: `enum State { IDLE, LOADING, SUCCESS, ERROR }` + wrapper class.

---

## 5. HƯỚNG DẪN THÊM TÍNH NĂNG MỚI (Guide for AI Agents)

### 5.1 Checklist: Thêm một Entity/Bảng mới

```
□ Bước 1: Cập nhật DatabaseContract
  - Thêm inner class mới trong DatabaseContract.java implements BaseColumns.
  - Định nghĩa: TABLE_NAME, COLUMN_xxx, SQL_CREATE_TABLE, SQL_DROP_TABLE.
  - Thêm indexes nếu cần.

□ Bước 2: Cập nhật BudgetDatabaseHelper
  - Thêm db.execSQL(NewEntry.SQL_CREATE_TABLE) trong onCreate() theo đúng thứ tự dependency.
  - Thêm db.execSQL(NewEntry.SQL_DROP_TABLE) trong onUpgrade() theo thứ tự ngược.
  - Tăng DATABASE_VERSION trong DatabaseContract nếu DB đã tồn tại trên thiết bị.

□ Bước 3: Tạo Model class
  - Tạo file trong data/model/.
  - Thêm constructor, Builder inner class, getters/setters.
  - Xử lý type conversion nếu cần (Enum ↔ String, Boolean ↔ int).

□ Bước 4: Tạo DAO class
  - Tạo file trong data/local/dao/.
  - Constructor nhận BudgetDatabaseHelper.
  - Implement CRUD: insert(), getById(), getAll/getByXxx(), update(), delete().
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

### 5.3 Checklist: Thay thế Mock bằng Implementation thật

```
□ Bước 1: Giữ nguyên Interface trong data/repository/.

□ Bước 2: Tạo class mới implement Interface.
  - VD: RealTransactionRepository implements TransactionRepository.
  - Nếu dùng local DB: inject DAO qua constructor hoặc DatabaseManager.
  - Nếu dùng remote API: tạo package data/remote/ cho Retrofit service + DTO.

□ Bước 3: Thay đổi khởi tạo trong ViewModel.
  - Thay: this.repository = new MockXxxRepository();
  - Bằng: this.repository = new RealXxxRepository(dao);

□ Bước 4: KHÔNG sửa code Fragment/Activity.
```

### 5.4 Checklist: Thêm một Repository mới

```
□ Bước 1: Tạo Interface trong data/repository/.
  - Định nghĩa các method cần thiết.
  - Annotate @NonNull/@Nullable cho parameters và return types.

□ Bước 2: Tạo Mock Implementation.
  - Class MockXxxRepository implements XxxRepository.
  - Trả về hardcoded data phù hợp với UI design.

□ Bước 3: Sử dụng trong ViewModel.
  - Khai báo field kiểu Interface: private final XxxRepository repository;
  - Khởi tạo trong constructor: this.repository = new MockXxxRepository();
```

### 5.5 Quy tắc bổ sung khi thêm tính năng

- **Drawable icon**: Đặt trong `res/drawable/`, đặt tên `ic_xxx.xml` (vector drawable).
- **Color mới**: Thêm vào `res/values/colors.xml` với prefix feature name (VD: `budget_accent_blue`).
- **String mới**: Thêm vào `res/values/strings.xml`, dùng snake_case. Hỗ trợ format string: `%1$s`, `%2$d`.
- **Animation**: Đặt trong `res/anim/`. Hiện có `fade_in.xml` và `fade_out.xml`.
- **Default categories**: Nếu cần thêm danh mục mặc định, thêm SQL INSERT trong `BudgetDatabaseHelper.insertDefaultCategories()`.
- **Không import thư viện mới** mà không thêm dependency vào `app/build.gradle.kts` và `libs.versions.toml`.
