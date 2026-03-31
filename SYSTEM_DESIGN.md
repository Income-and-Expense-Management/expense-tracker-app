# System Design Document -- QuanLiChiTieu (Expense Management)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Analysis of Current Architecture](#2-analysis-of-current-architecture)
   - 2.1 Current Directory Structure
   - 2.2 Module Responsibilities
   - 2.3 Identified Anti-Patterns and Outdated Practices
3. [Proposed Architecture -- Single Activity + MVVM](#3-proposed-architecture----single-activity--mvvm)
   - 3.1 Architectural Patterns
   - 3.2 Proposed Directory Tree
   - 3.3 Module Descriptions and Responsibilities
4. [Data Flow](#4-data-flow)
   - 4.1 Authentication Flow
   - 4.2 Main App Navigation Flow
   - 4.3 Data Loading Flow (MVVM)
5. [Component Specifications](#5-component-specifications)
   - 5.1 Data Layer
   - 5.2 UI Layer -- Activities
   - 5.3 UI Layer -- Fragments
   - 5.4 UI Layer -- ViewModels
   - 5.5 UI Layer -- Adapters & Custom Views
6. [Navigation Architecture](#6-navigation-architecture)
7. [Dependency Graph](#7-dependency-graph)
8. [Migration Guide -- Activity to Fragment](#8-migration-guide----activity-to-fragment)

---

## 1. Executive Summary

**Application**: QuanLiChiTieu -- a Vietnamese-language personal expense management app.

**Platform**: Android (Java), minSdk 27, targetSdk 36.

**Current State**: The application uses a multi-Activity architecture where each BottomNavigation tab (Home, Transaction) is a separate Activity with its own duplicated BottomNavigationView and FloatingActionButton. This is an outdated pattern that leads to poor UX (screen re-creation on tab switch), code duplication, and difficulty in sharing state across tabs.

**Proposed State**: Refactor to **Single Activity Architecture (SAA)** with **MVVM** pattern. One `MainActivity` hosts a shared BottomNavigationView + FAB, and all tab screens become Fragments. `LoginActivity` remains a separate Activity because it belongs to a different authentication flow.

---

## 2. Analysis of Current Architecture

### 2.1 Current Directory Structure

```
com.ptithcm.quanlichitieu
├── data
│   ├── local
│   │   └── BudgetDatabaseHelper.java      # SQLite helper for Wallet CRUD
│   ├── model
│   │   ├── Expense.java                    # Expense POJO
│   │   ├── Transaction.java                # Transaction POJO (income/expense)
│   │   ├── TransactionGroup.java           # Date-grouped transactions
│   │   └── Wallet.java                     # Wallet POJO (mutable)
│   └── repository
│       ├── AuthService.java                # Auth interface
│       ├── MockAuthService.java            # Mock auth (hardcoded credentials)
│       ├── ExpenseRepository.java          # Expense data interface
│       ├── MockExpenseRepository.java      # Mock expense data
│       ├── TransactionRepository.java      # Transaction data interface
│       └── MockTransactionRepository.java  # Mock transaction data
├── ui
│   ├── common
│   │   └── SimpleLineChart.java            # Custom Canvas-drawn line chart
│   ├── home
│   │   ├── HomeActivity.java               # Dashboard screen (Activity)
│   │   └── adapter
│   │       └── TopExpenseAdapter.java      # RecyclerView adapter for expenses
│   ├── login
│   │   └── LoginActivity.java              # Login screen (launcher Activity)
│   ├── transaction
│   │   ├── TransactionActivity.java        # Transaction list screen (Activity)
│   │   └── adapter
│   │       └── TransactionAdapter.java     # Multi-type RecyclerView adapter
│   └── wallet
│       └── AddWalletActivity.java          # Add wallet form (Activity)

res/
├── layout/
│   ├── activity_home.xml                   # Home dashboard layout
│   ├── activity_transaction.xml            # Transaction list layout
│   ├── activity_login.xml                  # Login form layout
│   ├── activity_add_wallet.xml             # Add wallet form layout
│   ├── item_top_expense.xml                # Expense list item
│   ├── item_transaction.xml                # Transaction list item
│   └── item_transaction_date_header.xml    # Date group header
├── menu/
│   └── bottom_nav_menu.xml                 # BottomNav menu (5 items + FAB placeholder)
├── drawable/                               # Icons, backgrounds, shapes
├── color/                                  # Color state lists (nav, segmented button)
├── anim/                                   # Fade in/out transitions
└── values/                                 # Colors, strings, themes
```

### 2.2 Module Responsibilities (Current)

| Package / Class | Current Responsibility |
|---|---|
| `data.local.BudgetDatabaseHelper` | Raw SQLite CRUD for `wallet` table. Creates DB schema, handles insert/query. |
| `data.model.Expense` | Immutable POJO: id, category, description, amount, iconResId, timestamp. |
| `data.model.Transaction` | Immutable POJO: id, category, amount, type (EXPENSE/INCOME), iconResId, walletName, timestamp. |
| `data.model.TransactionGroup` | Groups transactions by date: dayOfWeek, date string, dayTotal, list of Transaction. |
| `data.model.Wallet` | Mutable POJO: id, name, balance. Used for SQLite persistence. |
| `data.repository.AuthService` | Interface defining `login(email, password, callback)`. |
| `data.repository.MockAuthService` | Validates against hardcoded credentials ("admin@test.com" / "password123") with 1.5s simulated delay. |
| `data.repository.ExpenseRepository` | Interface: `getTopExpenses(limit)`, `getCurrentMonthExpenses()`, `getTotalExpenseAmount()`. |
| `data.repository.MockExpenseRepository` | Returns hardcoded Expense list sorted by amount. |
| `data.repository.TransactionRepository` | Interface: `getTransactionsByMonth(offset)`, `getTotalExpense()`, `getTotalIncome()`, `getTotalBalance()`. |
| `data.repository.MockTransactionRepository` | Returns hardcoded transaction groups for display. |
| `ui.common.SimpleLineChart` | Custom View that draws an S-curve Bezier line chart on Canvas with grid lines. |
| `ui.login.LoginActivity` | Launcher Activity. Handles email/password input, calls AuthService, navigates to HomeActivity on success. |
| `ui.home.HomeActivity` | Dashboard Activity. Displays balance, wallet card, monthly report chart, top expenses list. Hosts its own BottomNavigationView and FAB. |
| `ui.home.adapter.TopExpenseAdapter` | RecyclerView.Adapter for expense items with click listener interface. |
| `ui.transaction.TransactionActivity` | Transaction list Activity. Displays balance summary, month tabs, grouped transactions. Hosts its own BottomNavigationView and FAB. |
| `ui.transaction.adapter.TransactionAdapter` | Multi-ViewType adapter (header + item) for date-grouped transactions. |
| `ui.wallet.AddWalletActivity` | Simple form Activity: name + balance inputs, saves to SQLite via BudgetDatabaseHelper. |

### 2.3 Identified Anti-Patterns and Outdated Practices

#### CRITICAL: Multiple Activities for BottomNavigation Tabs

**Problem**: `HomeActivity` and `TransactionActivity` are separate Activities, each containing an identical BottomNavigationView + FAB setup. When the user taps the "Transaction" tab in HomeActivity, it launches a new TransactionActivity via `startActivity(intent)`. When the user taps "Home" in TransactionActivity, it finishes itself and navigates back.

**Consequences**:
- **Duplicated BottomNav/FAB code** -- Both `activity_home.xml` and `activity_transaction.xml` contain identical BottomAppBar + BottomNavigationView + FAB blocks (~40 lines of XML each) and each Activity has its own `setupBottomNav()` Java method.
- **Activity re-creation on tab switch** -- Each tab switch destroys and re-creates an Activity, causing visual jank (no shared element transitions, no smooth crossfade).
- **State loss** -- `HomeActivity`'s scroll position, toggle selection, and wallet data are lost when navigating to `TransactionActivity` and back (unless `CLEAR_TOP | SINGLE_TOP` flags are used, which HomeActivity does not apply when calling TransactionActivity).
- **No shared state** -- There is no way to share a ViewModel or data between tabs because they are separate Activity scopes.
- **Inconsistent back-stack** -- `TransactionActivity` uses `finish()` + Intent flags to go back to HomeActivity, creating a fragile manual back-stack.

#### No ViewModel Layer

All business logic, data loading, and UI state management live directly inside Activity classes:
- `HomeActivity.isMonthSelected` is UI state held in the Activity.
- `HomeActivity.expenseRepository` and `HomeActivity.dbHelper` are data dependencies held in the Activity.
- `TransactionActivity.currentMonthOffset` is UI state held in the Activity.
- Data is loaded in `onResume()` / `onCreate()` synchronously on the main thread.

This violates the Separation of Concerns principle and makes the code untestable.

#### No Lifecycle-Aware Data Observation

Data is loaded imperatively (`loadTopExpenses()`, `loadWalletData()`, `loadData()`) with no reactive observation (no LiveData, no StateFlow). When configuration changes occur (rotation), data must be re-fetched.

#### Direct Repository Instantiation

```java
// In HomeActivity:
expenseRepository = new MockExpenseRepository();

// In TransactionActivity:
transactionRepository = new MockTransactionRepository();
```

Repositories are instantiated directly in Activities. There is no Dependency Injection framework (Hilt/Dagger) and no centralized way to swap implementations.

#### Raw SQLite Instead of Room

`BudgetDatabaseHelper` uses raw `SQLiteOpenHelper` with manual SQL strings, `ContentValues`, and `Cursor` parsing. This is error-prone compared to Room's compile-time verification.

---

## 3. Proposed Architecture -- Single Activity + MVVM

### 3.1 Architectural Patterns

| Pattern | Description | Where Applied |
|---|---|---|
| **Single Activity Architecture (SAA)** | One Activity (`MainActivity`) hosts all BottomNav tab screens as Fragments. `LoginActivity` remains separate for auth flow. | `ui.main.MainActivity` hosts `HomeFragment`, `TransactionFragment`, `WalletFragment`, `AccountFragment` |
| **MVVM (Model-View-ViewModel)** | Each screen has a ViewModel that holds UI state and exposes data via `LiveData`. Fragments observe the data reactively. | `HomeViewModel`, `TransactionViewModel`, `LoginViewModel`, `WalletViewModel` |
| **Repository Pattern** | Data access is abstracted behind interfaces. Repositories are the single source of truth for each data domain. | `ExpenseRepository`, `TransactionRepository`, `AuthService` (already in place) |
| **Observer Pattern** | ViewModels expose `LiveData<T>` objects. Fragments observe them with lifecycle-aware observers, eliminating manual data loading in `onResume()`. | All Fragment-ViewModel pairs |
| **Dependency Inversion** | Fragments and ViewModels depend on repository interfaces, not concrete mock implementations. | Already partially in place; strengthened by ViewModel introduction |

### 3.2 Proposed Directory Tree

```
com.ptithcm.quanlichitieu
│
├── data                                        # ── DATA LAYER ──
│   ├── local
│   │   └── BudgetDatabaseHelper.java           # SQLite helper (Wallet CRUD)
│   ├── model
│   │   ├── Expense.java                        # Expense entity
│   │   ├── Transaction.java                    # Transaction entity
│   │   ├── TransactionGroup.java               # Date-grouped transactions
│   │   └── Wallet.java                         # Wallet entity
│   └── repository
│       ├── AuthService.java                    # Auth contract (interface)
│       ├── MockAuthService.java                # Mock auth implementation
│       ├── ExpenseRepository.java              # Expense contract (interface)
│       ├── MockExpenseRepository.java          # Mock expense implementation
│       ├── TransactionRepository.java          # Transaction contract (interface)
│       └── MockTransactionRepository.java      # Mock transaction implementation
│
├── ui                                          # ── UI / PRESENTATION LAYER ──
│   ├── common
│   │   └── SimpleLineChart.java                # Reusable custom chart View
│   │
│   ├── login                                   # ── Auth Flow (Separate Activity) ──
│   │   ├── LoginActivity.java                  # Standalone login screen
│   │   └── LoginViewModel.java                 # NEW: Login UI state + auth logic
│   │
│   ├── main                                    # ── Main App Shell (Single Activity) ──
│   │   └── MainActivity.java                   # NEW: Hosts BottomNav + FAB + Fragment container
│   │
│   ├── home                                    # ── Home Tab ──
│   │   ├── HomeFragment.java                   # NEW: Replaces HomeActivity
│   │   ├── HomeViewModel.java                  # NEW: Holds balance, wallet, expenses state
│   │   └── adapter
│   │       └── TopExpenseAdapter.java          # Expense list adapter (unchanged)
│   │
│   ├── transaction                             # ── Transaction Tab ──
│   │   ├── TransactionFragment.java            # NEW: Replaces TransactionActivity
│   │   ├── TransactionViewModel.java           # NEW: Holds transactions, totals, month offset
│   │   └── adapter
│   │       └── TransactionAdapter.java         # Transaction list adapter (unchanged)
│   │
│   ├── wallet                                  # ── Wallet Tab ──
│   │   ├── WalletFragment.java                 # NEW: Wallet list (placeholder)
│   │   └── AddWalletFragment.java              # NEW: Replaces AddWalletActivity (dialog/sheet)
│   │
│   └── account                                 # ── Account Tab ──
│       └── AccountFragment.java                # NEW: User profile/settings (placeholder)

res/
├── layout/
│   ├── activity_main.xml                       # NEW: CoordinatorLayout + FrameLayout container
│   │                                           #      + BottomAppBar + BottomNav + FAB (SINGLE COPY)
│   ├── activity_login.xml                      # Login form (unchanged)
│   ├── fragment_home.xml                       # NEW: Home content (extracted from activity_home.xml,
│   │                                           #      WITHOUT BottomNav/FAB)
│   ├── fragment_transaction.xml                # NEW: Transaction content (extracted from
│   │                                           #      activity_transaction.xml, WITHOUT BottomNav/FAB)
│   ├── fragment_wallet.xml                     # NEW: Wallet list placeholder
│   ├── fragment_account.xml                    # NEW: Account placeholder
│   ├── fragment_add_wallet.xml                 # NEW: Replaces activity_add_wallet.xml
│   ├── item_top_expense.xml                    # (unchanged)
│   ├── item_transaction.xml                    # (unchanged)
│   └── item_transaction_date_header.xml        # (unchanged)
├── menu/
│   └── bottom_nav_menu.xml                     # (unchanged)
├── drawable/                                   # (unchanged)
├── color/                                      # (unchanged)
├── anim/                                       # (unchanged)
└── values/                                     # (unchanged)
```

**Files Removed**:
- `activity_home.xml` -- replaced by `activity_main.xml` + `fragment_home.xml`
- `activity_transaction.xml` -- replaced by `fragment_transaction.xml`
- `activity_add_wallet.xml` -- replaced by `fragment_add_wallet.xml`
- `HomeActivity.java` -- replaced by `HomeFragment.java` + `HomeViewModel.java`
- `TransactionActivity.java` -- replaced by `TransactionFragment.java` + `TransactionViewModel.java`
- `AddWalletActivity.java` -- replaced by `AddWalletFragment.java`

**Files Added**:
- `MainActivity.java` -- single shell Activity
- `HomeFragment.java`, `HomeViewModel.java`
- `TransactionFragment.java`, `TransactionViewModel.java`
- `LoginViewModel.java`
- `WalletFragment.java`, `AddWalletFragment.java`
- `AccountFragment.java`
- `activity_main.xml`, `fragment_home.xml`, `fragment_transaction.xml`, `fragment_wallet.xml`, `fragment_account.xml`, `fragment_add_wallet.xml`

### 3.3 Module Descriptions and Responsibilities

#### 3.3.1 Data Layer (`data.*`)

**No changes required.** The data layer is already well-structured with interfaces and mock implementations.

| Component | Responsibility |
|---|---|
| `data.model.*` | Pure data classes (POJOs) representing domain entities. No framework dependencies. Shared across all layers. |
| `data.repository.AuthService` | Defines the authentication contract. Allows swapping mock/real implementations. |
| `data.repository.ExpenseRepository` | Defines the contract for expense data retrieval (top expenses, monthly expenses, totals). |
| `data.repository.TransactionRepository` | Defines the contract for transaction data retrieval (by month, totals). |
| `data.repository.Mock*` | Development/testing implementations returning hardcoded data. |
| `data.local.BudgetDatabaseHelper` | SQLite persistence for wallet data. Handles table creation, CRUD operations. |

#### 3.3.2 UI Layer -- Activities

| Component | Responsibility |
|---|---|
| `ui.login.LoginActivity` | **Standalone auth Activity.** Entry point (launcher). Collects credentials, delegates to `LoginViewModel`, navigates to `MainActivity` on success. Remains a separate Activity because it represents a distinct authentication flow with its own theme (`Theme.QuanLiChiTieu.Splash`). |
| `ui.main.MainActivity` | **Single host Activity for the main app.** Contains the shared `CoordinatorLayout` with `BottomAppBar`, `BottomNavigationView`, and `FloatingActionButton`. Manages Fragment transactions based on BottomNav item selection. Receives username from LoginActivity via Intent extras and passes it to HomeFragment via Fragment arguments. |

#### 3.3.3 UI Layer -- Fragments

| Component | Replaces | Responsibility |
|---|---|---|
| `ui.home.HomeFragment` | `HomeActivity` | Dashboard UI: greeting header, total balance, wallet card, monthly report chart, top expenses RecyclerView, period toggle. Observes `HomeViewModel` for data. No direct repository access. |
| `ui.transaction.TransactionFragment` | `TransactionActivity` | Transaction list UI: total balance/expense/income summary, month tabs, date-grouped RecyclerView. Observes `TransactionViewModel` for data. |
| `ui.wallet.WalletFragment` | *(new)* | Wallet management list screen (placeholder for future implementation). |
| `ui.wallet.AddWalletFragment` | `AddWalletActivity` | Wallet creation form. Can be shown as a full fragment or as a `BottomSheetDialogFragment`. Delegates save to `WalletViewModel` or directly to `BudgetDatabaseHelper`. |
| `ui.account.AccountFragment` | *(new)* | User profile and settings (placeholder for future implementation). |

#### 3.3.4 UI Layer -- ViewModels

| Component | State Managed | Data Sources |
|---|---|---|
| `ui.login.LoginViewModel` | `LiveData<LoginState>` (idle, loading, success, error) | `AuthService` |
| `ui.home.HomeViewModel` | `LiveData<String> username`, `LiveData<Wallet> wallet`, `LiveData<List<Expense>> topExpenses`, `LiveData<Boolean> isMonthSelected` | `ExpenseRepository`, `BudgetDatabaseHelper` |
| `ui.transaction.TransactionViewModel` | `LiveData<List<TransactionGroup>> transactions`, `LiveData<Double> totalBalance/expense/income`, `LiveData<Integer> currentMonthOffset` | `TransactionRepository` |

#### 3.3.5 UI Layer -- Adapters and Custom Views

| Component | Responsibility |
|---|---|
| `ui.home.adapter.TopExpenseAdapter` | Binds `List<Expense>` to RecyclerView items. Supports click listener. **No changes needed.** |
| `ui.transaction.adapter.TransactionAdapter` | Multi-ViewType adapter: binds `TransactionGroup` headers and `Transaction` items. **No changes needed.** |
| `ui.common.SimpleLineChart` | Custom View drawing an S-curve Bezier chart. **No changes needed.** |

---

## 4. Data Flow

### 4.1 Authentication Flow

```
┌──────────────┐      ┌──────────────────┐      ┌──────────────┐
│ LoginActivity │─────>│  LoginViewModel   │─────>│  AuthService  │
│  (UI input)   │      │  (manages state)  │      │  (interface)  │
│               │<─────│                   │<─────│              │
│  observes     │      │ LiveData<State>   │      │ MockAuth     │
│  LoginState   │      │                   │      │ (or Real)    │
└──────┬───────┘      └──────────────────┘      └──────────────┘
       │ on SUCCESS
       │ startActivity(MainActivity) + finish()
       ▼
┌──────────────┐
│ MainActivity  │
│ (hosts frags) │
└──────────────┘
```

**Step-by-step**:
1. User enters email/password in `LoginActivity`.
2. `LoginActivity` calls `loginViewModel.login(email, password)`.
3. `LoginViewModel` calls `authService.login(...)`, updates `LiveData<LoginState>` to LOADING.
4. On callback, ViewModel updates state to SUCCESS(username) or ERROR(message).
5. `LoginActivity` observes the state. On SUCCESS, it launches `MainActivity` with the username extra and calls `finish()`.

### 4.2 Main App Navigation Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │              FrameLayout (Fragment Container)             │    │
│  │                                                           │    │
│  │   ┌──────────┐  ┌──────────────────┐  ┌──────────┐      │    │
│  │   │  Home     │  │  Transaction     │  │  Wallet  │      │    │
│  │   │  Fragment │  │  Fragment        │  │  Fragment │      │    │
│  │   └──────────┘  └──────────────────┘  └──────────┘      │    │
│  │                                           ┌──────────┐   │    │
│  │                                           │ Account   │   │    │
│  │                                           │ Fragment  │   │    │
│  │                                           └──────────┘   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  BottomAppBar + BottomNavigationView + FAB (SHARED)      │    │
│  │  [ Home | Transaction | (+) | Wallet | Account ]         │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

**Navigation Logic in `MainActivity`**:
- BottomNav `setOnItemSelectedListener` performs `FragmentTransaction.replace()` on the container.
- Fragment instances are cached to avoid re-creation (using `show()/hide()` or `FragmentTransaction.replace()` with back-stack management).
- FAB click is handled by `MainActivity` and can delegate to the currently visible Fragment or launch a dialog.

### 4.3 Data Loading Flow (MVVM)

```
┌─────────────────┐   observes    ┌──────────────────┐   calls    ┌────────────────────┐
│                  │──────────────>│                    │──────────>│                     │
│   HomeFragment   │   LiveData    │   HomeViewModel    │           │  ExpenseRepository  │
│   (View Layer)   │<──────────────│   (Logic Layer)    │<──────────│  (Data Layer)       │
│                  │   data events │                    │  returns  │                     │
└─────────────────┘               └──────────────────┘           └────────────────────┘
                                          │
                                          │ calls
                                          ▼
                                  ┌────────────────────┐
                                  │ BudgetDatabaseHelper│
                                  │ (Wallet data)       │
                                  └────────────────────┘
```

**Step-by-step** (HomeFragment example):
1. `HomeFragment.onViewCreated()` obtains `HomeViewModel` via `new ViewModelProvider(this).get(HomeViewModel.class)`.
2. Fragment observes `viewModel.getTopExpenses()`, `viewModel.getWallet()`, etc.
3. When the Fragment becomes visible, ViewModel's `init()` or `loadData()` fetches from repositories.
4. Repository returns data (currently synchronous mock; in production, use background threads).
5. ViewModel posts the result to `LiveData`.
6. Fragment's observer callback updates the RecyclerView adapter, TextViews, etc.
7. On configuration change (rotation), the ViewModel survives and the Fragment re-observes existing data without re-fetching.

---

## 5. Component Specifications

### 5.1 Data Layer (Unchanged)

```
data/
├── local/
│   └── BudgetDatabaseHelper.java
│       - DATABASE_NAME: "budget.db"
│       - TABLE: wallet (id INTEGER PK, name TEXT, balance REAL)
│       - Methods: addWallet(), updateWallet(), getAllWallets(), getFirstWallet()
│
├── model/
│   ├── Expense.java          (immutable: id, category, description, amount, iconResId, timestamp)
│   ├── Transaction.java      (immutable: id, category, amount, type, iconResId, walletName, timestamp)
│   ├── TransactionGroup.java (immutable: dayOfWeek, date, dayTotal, List<Transaction>)
│   └── Wallet.java           (mutable: id, name, balance)
│
└── repository/
    ├── AuthService.java              → interface { login(email, pwd, callback) }
    ├── MockAuthService.java          → validates "admin@test.com" / "password123", 1.5s delay
    ├── ExpenseRepository.java        → interface { getTopExpenses(limit), getCurrentMonthExpenses(), getTotalExpenseAmount() }
    ├── MockExpenseRepository.java    → 5 hardcoded expenses (Food, Shopping, Transport, Entertainment, Bills)
    ├── TransactionRepository.java    → interface { getTransactionsByMonth(offset), getTotalExpense/Income/Balance() }
    └── MockTransactionRepository.java → 4 date groups, 10 transactions, fixed totals
```

### 5.2 UI Layer -- Activities

#### `LoginActivity` (Retained, Minor Refactor)
```
Location: ui/login/LoginActivity.java
Layout:   activity_login.xml
Theme:    Theme.QuanLiChiTieu.Splash (green splash background)
Role:     Launcher activity. Collects credentials.

Changes:
- Extract auth logic into LoginViewModel.
- Activity only observes LoginViewModel.loginState LiveData.
- On SUCCESS: startActivity(MainActivity) with EXTRA_USERNAME, then finish().
```

#### `MainActivity` (NEW -- replaces HomeActivity as main host)
```
Location: ui/main/MainActivity.java
Layout:   activity_main.xml
Theme:    Theme.QuanLiChiTieu (default dark)
Role:     Single Activity shell for the entire post-login app.

Contains:
- CoordinatorLayout as root
- FrameLayout (id: fragmentContainer) for Fragment transactions
- BottomAppBar with BottomNavigationView (single copy, shared by all tabs)
- FloatingActionButton anchored to BottomAppBar

Responsibilities:
- On BottomNav item selected: replace Fragment in container
  - nav_home       → HomeFragment
  - nav_transaction → TransactionFragment
  - nav_wallet     → WalletFragment
  - nav_account    → AccountFragment
- On FAB click: delegate action (e.g., show add-transaction dialog)
- Pass username to HomeFragment via Fragment arguments
- Handle back navigation properly (finish if on Home tab, else switch to Home)
```

### 5.3 UI Layer -- Fragments

#### `HomeFragment` (NEW -- replaces HomeActivity)
```
Location: ui/home/HomeFragment.java
Layout:   fragment_home.xml (activity_home.xml content WITHOUT BottomNav/FAB)

Receives: username via getArguments().getString("username")

Observes from HomeViewModel:
- LiveData<String> username         → tvHomeTitle
- LiveData<Wallet> wallet           → tvBalanceValue, tvWalletDetailName, tvWalletDetailValue
- LiveData<List<Expense>> topExpenses → TopExpenseAdapter.setExpenses()
- LiveData<Boolean> isMonthSelected → toggle button state

User Actions:
- Period toggle (Week/Month) → viewModel.setPeriodFilter(isMonth)
- Wallet card click → navigate to AddWalletFragment
- "See All" click → navigate to AddWalletFragment
- Expense item click → show toast (future: navigate to detail)
```

#### `TransactionFragment` (NEW -- replaces TransactionActivity)
```
Location: ui/transaction/TransactionFragment.java
Layout:   fragment_transaction.xml (activity_transaction.xml content WITHOUT BottomNav/FAB)

Observes from TransactionViewModel:
- LiveData<Double> totalBalance     → tvTotalBalance
- LiveData<Double> totalExpense     → tvTotalExpense
- LiveData<Double> totalIncome      → tvTotalIncome
- LiveData<List<TransactionGroup>>  → TransactionAdapter.setGroups()
- LiveData<Integer> monthOffset     → tab highlight styles

User Actions:
- Month tab click (prev/current/next) → viewModel.setMonthOffset(offset)
- Back button → delegate to MainActivity (switch to Home tab)
- "See Report" button → future navigation
```

#### `WalletFragment` (NEW -- placeholder)
```
Location: ui/wallet/WalletFragment.java
Layout:   fragment_wallet.xml

Displays: Wallet list (future implementation)
Currently: Placeholder text "Feature coming soon"
```

#### `AddWalletFragment` (NEW -- replaces AddWalletActivity)
```
Location: ui/wallet/AddWalletFragment.java
Layout:   fragment_add_wallet.xml

Displays: Name + Balance form
Save action: Validates input, calls BudgetDatabaseHelper.addWallet(), pops back stack
Can be shown as: Regular Fragment in container OR BottomSheetDialogFragment
```

#### `AccountFragment` (NEW -- placeholder)
```
Location: ui/account/AccountFragment.java
Layout:   fragment_account.xml

Displays: User profile and settings (future implementation)
Currently: Placeholder text "Feature coming soon"
```

### 5.4 UI Layer -- ViewModels

#### `LoginViewModel`
```java
public class LoginViewModel extends ViewModel {
    private final AuthService authService;
    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>(LoginState.IDLE);

    // LoginState: IDLE, LOADING, SUCCESS(username), ERROR(message)

    public void login(String email, String password) {
        loginState.setValue(LoginState.LOADING);
        authService.login(email, password, new AuthService.LoginCallback() {
            @Override public void onSuccess(String username) {
                loginState.postValue(LoginState.success(username));
            }
            @Override public void onError(String message) {
                loginState.postValue(LoginState.error(message));
            }
        });
    }

    public LiveData<LoginState> getLoginState() { return loginState; }
}
```

#### `HomeViewModel`
```java
public class HomeViewModel extends ViewModel {
    private final ExpenseRepository expenseRepository;
    private final MutableLiveData<List<Expense>> topExpenses = new MutableLiveData<>();
    private final MutableLiveData<Wallet> wallet = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isMonthSelected = new MutableLiveData<>(true);

    public void loadTopExpenses(int limit) {
        topExpenses.setValue(expenseRepository.getTopExpenses(limit));
    }

    public void loadWallet(BudgetDatabaseHelper dbHelper) {
        wallet.setValue(dbHelper.getFirstWallet());
    }

    public void setPeriodFilter(boolean isMonth) {
        isMonthSelected.setValue(isMonth);
        loadTopExpenses(3); // reload with new filter
    }

    // Getters for LiveData...
}
```

#### `TransactionViewModel`
```java
public class TransactionViewModel extends ViewModel {
    private final TransactionRepository transactionRepository;
    private final MutableLiveData<List<TransactionGroup>> transactions = new MutableLiveData<>();
    private final MutableLiveData<Double> totalBalance = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpense = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncome = new MutableLiveData<>();
    private final MutableLiveData<Integer> monthOffset = new MutableLiveData<>(0);

    public void setMonthOffset(int offset) {
        monthOffset.setValue(offset);
        loadData();
    }

    public void loadData() {
        int offset = monthOffset.getValue() != null ? monthOffset.getValue() : 0;
        transactions.setValue(transactionRepository.getTransactionsByMonth(offset));
        totalBalance.setValue(transactionRepository.getTotalBalance());
        totalExpense.setValue(transactionRepository.getTotalExpense());
        totalIncome.setValue(transactionRepository.getTotalIncome());
    }

    // Getters for LiveData...
}
```

### 5.5 UI Layer -- Adapters & Custom Views (Unchanged)

| Component | Notes |
|---|---|
| `TopExpenseAdapter` | No changes. Works with any `List<Expense>` provided via `setExpenses()`. |
| `TransactionAdapter` | No changes. Works with any `List<TransactionGroup>` provided via `setGroups()`. |
| `SimpleLineChart` | No changes. Standalone custom View. |

---

## 6. Navigation Architecture

### Screen Map

```
App Launch
    │
    ▼
LoginActivity (launcher, separate Activity)
    │ on auth success
    ▼
MainActivity (Single Activity host)
    ├── [Tab: Home]        → HomeFragment
    │       └── [Action: Add Wallet] → AddWalletFragment (overlay/navigate)
    ├── [Tab: Transaction] → TransactionFragment
    ├── [Tab: Wallet]      → WalletFragment (placeholder)
    │       └── [Action: Add Wallet] → AddWalletFragment
    ├── [Tab: Account]     → AccountFragment (placeholder)
    └── [FAB: Add]         → Add Transaction (future dialog/fragment)
```

### Fragment Transaction Strategy

**Recommended approach**: `FragmentTransaction.replace()` with Fragment caching.

```java
// In MainActivity:
private HomeFragment homeFragment;
private TransactionFragment transactionFragment;
private WalletFragment walletFragment;
private AccountFragment accountFragment;
private Fragment activeFragment;

private void switchFragment(Fragment target) {
    if (target == activeFragment) return;

    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

    if (!target.isAdded()) {
        ft.add(R.id.fragmentContainer, target);
    }

    ft.hide(activeFragment);
    ft.show(target);
    ft.commit();

    activeFragment = target;
}
```

This approach:
- **Preserves Fragment state** (scroll position, data) when switching tabs.
- **Avoids re-creation** -- Fragments are created once and shown/hidden.
- **Uses a single BottomNav** -- No duplication across screens.

### AndroidManifest.xml (Updated)

```xml
<application ...>
    <activity
        android:name=".ui.login.LoginActivity"
        android:exported="true"
        android:theme="@style/Theme.QuanLiChiTieu.Splash">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity
        android:name=".ui.main.MainActivity"
        android:exported="false" />
    <!-- HomeActivity, TransactionActivity, AddWalletActivity REMOVED -->
</application>
```

---

## 7. Dependency Graph

```
┌──────────────────────────────────────────────────────────┐
│                     UI LAYER (Presentation)               │
│                                                            │
│  LoginActivity ──> LoginViewModel ──> AuthService          │
│                                                            │
│  MainActivity                                              │
│    ├── HomeFragment ──> HomeViewModel ──┬──> ExpenseRepo   │
│    │                                    └──> DBHelper      │
│    ├── TransactionFragment ──> TransactionViewModel        │
│    │                                ──> TransactionRepo    │
│    ├── WalletFragment ──> (future WalletViewModel)         │
│    │   └── AddWalletFragment ──> DBHelper                  │
│    └── AccountFragment ──> (future)                        │
│                                                            │
│  Adapters: TopExpenseAdapter, TransactionAdapter           │
│  Custom Views: SimpleLineChart                             │
└──────────────────────────┬───────────────────────────────┘
                           │ depends on
                           ▼
┌──────────────────────────────────────────────────────────┐
│                     DATA LAYER                            │
│                                                            │
│  Repository Interfaces     Mock Implementations            │
│  ├── AuthService           ├── MockAuthService             │
│  ├── ExpenseRepository     ├── MockExpenseRepository       │
│  └── TransactionRepository └── MockTransactionRepository   │
│                                                            │
│  Local Storage                                             │
│  └── BudgetDatabaseHelper (SQLite)                         │
│                                                            │
│  Models (shared across all layers)                         │
│  ├── Expense, Transaction, TransactionGroup, Wallet        │
└──────────────────────────────────────────────────────────┘
```

**Key Dependency Rules**:
1. Fragments depend on ViewModels only. Never on repositories directly.
2. ViewModels depend on repository interfaces only. Never on concrete implementations.
3. Models (POJOs) are shared freely across all layers.
4. Adapters are self-contained -- they receive data via setter methods.

---

## 8. Migration Guide -- Activity to Fragment

### Step 1: Create `activity_main.xml`

Extract the shared BottomAppBar + BottomNavigationView + FAB from `activity_home.xml` into a new `activity_main.xml`. Add a `FrameLayout` as the Fragment container.

### Step 2: Create Fragment Layouts

- Copy `activity_home.xml` content (everything inside `NestedScrollView`) into `fragment_home.xml`. Remove BottomAppBar, BottomNavigationView, and FAB.
- Copy `activity_transaction.xml` content (everything inside `NestedScrollView`) into `fragment_transaction.xml`. Remove BottomAppBar, BottomNavigationView, and FAB.
- Copy `activity_add_wallet.xml` into `fragment_add_wallet.xml` (content unchanged).

### Step 3: Create ViewModels

- Extract `HomeActivity`'s data fields (`expenseRepository`, `dbHelper`, `isMonthSelected`) and data-loading methods into `HomeViewModel`. Expose results as `LiveData`.
- Extract `TransactionActivity`'s data fields (`transactionRepository`, `currentMonthOffset`) and data-loading methods into `TransactionViewModel`. Expose results as `LiveData`.
- Extract `LoginActivity`'s `performLogin()` logic into `LoginViewModel`.

### Step 4: Create Fragments

- Convert `HomeActivity` into `HomeFragment`: replace `setContentView()` with `onCreateView()` inflating `fragment_home.xml`. Replace `findViewById()` calls with `view.findViewById()`. Replace direct repository calls with ViewModel observation.
- Convert `TransactionActivity` into `TransactionFragment`: same pattern.
- Convert `AddWalletActivity` into `AddWalletFragment`.

### Step 5: Create `MainActivity`

- Implement Fragment switching logic in `setupBottomNav()`.
- Instantiate all four tab Fragments.
- Show `HomeFragment` by default in `onCreate()`.
- Pass username to HomeFragment via `Bundle` arguments.

### Step 6: Update `AndroidManifest.xml`

- Remove `HomeActivity`, `AddWalletActivity` entries.
- Add `MainActivity` entry.
- Keep `LoginActivity` as launcher.

### Step 7: Update `LoginActivity`

- Change navigation target from `HomeActivity.class` to `MainActivity.class`.

---

*Document Version: 1.0*
*Architecture: Single Activity + MVVM*
*Language: Java 11*
*Min SDK: 27 | Target SDK: 36*
