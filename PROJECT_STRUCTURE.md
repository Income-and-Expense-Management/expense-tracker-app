# Project Structure

This document describes the package/folder organization for the **QuanLiChiTieu** Android application, following modern Android architecture best practices.

## Folder Tree

```
app/src/main/java/com/ptithcm/quanlichitieu/
├── data/                          # Data Layer
│   ├── local/                     # Local data sources (SQLite, Room, SharedPreferences)
│   │   └── BudgetDatabaseHelper.java
│   ├── model/                     # Data models / entities
│   │   └── Wallet.java
│   └── repository/                # Repository pattern implementations
│       ├── AuthService.java       # Authentication interface
│       └── MockAuthService.java   # Mock implementation for testing
│
├── ui/                            # UI Layer (Presentation)
│   ├── common/                    # Shared/reusable UI components
│   │   └── SimpleLineChart.java   # Custom chart view
│   ├── home/                      # Home feature module
│   │   └── HomeActivity.java
│   ├── login/                     # Login feature module
│   │   └── LoginActivity.java
│   ├── splash/                    # Splash screen module
│   │   └── SplashActivity.java
│   └── wallet/                    # Wallet feature module
│       └── AddWalletActivity.java
│
└── utils/                         # Utility classes (helpers, extensions, constants)
    └── .gitkeep                   # Placeholder for future utilities
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
| `ui/home/` | Home screen feature with its Activity, Fragment, ViewModel, and Adapter classes |
| `ui/login/` | Login/authentication feature with related UI components |
| `ui/splash/` | Splash screen shown on app startup |
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

## Future Expansion

As the app grows, consider adding:

- `data/remote/` - For API/network data sources
- `di/` - For dependency injection modules (Hilt/Dagger)
- `domain/` - For use cases in Clean Architecture
- `ui/[feature]/adapter/` - For RecyclerView adapters
- `ui/[feature]/viewmodel/` - For ViewModel classes
