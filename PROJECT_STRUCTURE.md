# Project Structure

This document describes the package/folder organization for the **QuanLiChiTieu** Android application, following modern Android architecture best practices.

## Folder Tree

```
app/src/main/java/com/ptithcm/quanlichitieu/
├── data/                              # Data Layer
│   ├── local/                         # Local data sources (SQLite, Room, SharedPreferences)
│   │   └── BudgetDatabaseHelper.java
│   ├── model/                         # Data models / entities
│   │   ├── Expense.java               # Expense transaction model
│   │   └── Wallet.java                # Wallet model
│   └── repository/                    # Repository pattern implementations
│       ├── AuthService.java           # Authentication interface
│       ├── ExpenseRepository.java     # Expense data interface (SOLID - Interface Segregation)
│       ├── MockAuthService.java       # Mock auth implementation
│       └── MockExpenseRepository.java # Mock expense implementation (swap for API later)
│
├── ui/                                # UI Layer (Presentation)
│   ├── common/                        # Shared/reusable UI components
│   │   └── SimpleLineChart.java       # Custom chart view
│   ├── home/                          # Home feature module
│   │   ├── HomeActivity.java          # Main dashboard screen
│   │   └── adapter/                   # RecyclerView adapters for home
│   │       └── TopExpenseAdapter.java # Adapter for top expenses list
│   ├── login/                         # Login feature module
│   │   └── LoginActivity.java
│   └── wallet/                        # Wallet feature module
│       └── AddWalletActivity.java
│
└── utils/                             # Utility classes (helpers, extensions, constants)
    └── .gitkeep                       # Placeholder for future utilities
```

## Package Descriptions

### `data/` - Data Layer
Contains all data-related classes responsible for data management and persistence.

| Package | Purpose |
|---------|---------|
| `data/local/` | Local data sources including SQLite database helpers, Room DAOs, and SharedPreferences wrappers |
| `data/model/` | Plain data classes (POJOs/entities) that represent the app's domain objects |
| `data/repository/` | Repository interfaces and implementations that abstract data sources from the rest of the app |

### `ui/` - Presentation Layer
Contains all UI-related classes organized by feature.

| Package | Purpose |
|---------|---------|
| `ui/common/` | Shared UI components that can be reused across multiple features (custom views, base classes, dialogs) |
| `ui/home/` | Home screen feature with its Activity and related components |
| `ui/home/adapter/` | RecyclerView adapters specific to the home screen |
| `ui/login/` | Login/authentication feature with related UI components |
| `ui/wallet/` | Wallet management feature (add, edit, list wallets) |

### `utils/` - Utilities
Contains helper classes, extension functions, constants, and other utilities shared across the app.

| Type | Examples |
|------|----------|
| Constants | App-wide constant values |
| Extensions | Kotlin/Java extension functions |
| Helpers | Date formatters, validators, converters |
| Managers | Network manager, permission manager |

## Architecture Guidelines

1. **Separation of Concerns**: Keep data logic in `data/`, UI logic in `ui/`, and shared utilities in `utils/`

2. **Feature-based UI Structure**: Each screen/feature gets its own package under `ui/` containing all related classes (Activity, Fragment, ViewModel, Adapter)

3. **Repository Pattern**: All data access should go through repositories in `data/repository/` to abstract the data source from consumers

4. **Dependency Direction**: `ui/` depends on `data/`, but `data/` should never depend on `ui/`

5. **Reusable Components**: Place shared UI components in `ui/common/` rather than duplicating code across features

## How to Replace Mock Data with Real API

The app uses the **Repository Pattern** to decouple data sources from UI. Here's how to swap mock data for a real API:

### Step 1: Create API Service Interface (Retrofit)
```java
// data/remote/ExpenseApiService.java
public interface ExpenseApiService {
    @GET("expenses/top")
    Call<List<ExpenseDto>> getTopExpenses(@Query("limit") int limit);
}
```

### Step 2: Create API Repository Implementation
```java
// data/repository/ApiExpenseRepository.java
public class ApiExpenseRepository implements ExpenseRepository {
    private final ExpenseApiService apiService;

    public ApiExpenseRepository(ExpenseApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public List<Expense> getTopExpenses(int limit) {
        // Call API and map DTO to Expense model
        Response<List<ExpenseDto>> response = apiService.getTopExpenses(limit).execute();
        return mapDtoToModel(response.body());
    }
}
```

### Step 3: Swap Implementation in Activity
```java
// In HomeActivity.java, change:
// OLD: expenseRepository = new MockExpenseRepository();
// NEW: expenseRepository = new ApiExpenseRepository(retrofit.create(ExpenseApiService.class));
```

## Future Expansion

As the app grows, consider adding:

- `data/remote/` - For API/network data sources and DTOs
- `data/remote/dto/` - For Data Transfer Objects from API responses
- `di/` - For dependency injection modules (Hilt/Dagger)
- `domain/` - For use cases in Clean Architecture
- `ui/[feature]/viewmodel/` - For ViewModel classes (MVVM pattern)
