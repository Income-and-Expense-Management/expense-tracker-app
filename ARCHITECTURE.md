# Application Architecture & Refactoring Documentation

## Overview
The application has been refactored to follow a clean architecture approach, separating concerns into distinct layers: **Data**, **UI**, and **Domain/Business Logic**. This structure improves maintainability, scalability, and testability.

## Directory Structure
The source code is organized as follows:

```
com.ptithcm.quanlichitieu
├── data
│   └── source          # Data sources (Interfaces & Implementations)
│       ├── AuthService.java       # Interface for authentication
│       └── MockAuthService.java   # Mock implementation
├── ui
│   ├── home            # Home Screen Feature
│   │   └── HomeActivity.java
│   ├── login           # Login Screen Feature
│   │   └── LoginActivity.java
│   ├── splash          # Splash Screen Feature
│   │   └── SplashActivity.java
│   └── views           # Custom UI Components
│       └── SimpleLineChart.java
```

## Key Components

### 1. Data Layer (`data.source`)
- **AuthService**: An interface defining the contract for authentication. This allows for easy swapping between a mock implementation (for development/testing) and a real backend implementation (e.g., Firebase, REST API).
- **MockAuthService**: A concrete implementation of `AuthService` that simulates network delays and validates against hardcoded credentials (`admin@test.com` / `password123`).

### 2. UI Layer (`ui`)
The UI is feature-grouped. Each feature (Login, Home, Splash) has its own package.
- **SplashActivity**: Handles the initial launch sequence, displaying the app branding.
- **LoginActivity**: Manages user credentials and interacts with `AuthService` to perform login. It follows the **Dependency Inversion Principle** by depending on the `AuthService` interface.
- **HomeActivity**: The main dashboard. It receives the user context (username) and displays relevant financial data using the custom `SimpleLineChart`.
- **SimpleLineChart**: A reusable UI component encapsulating the logic for drawing the financial chart, adhering to the **Single Responsibility Principle**.

## SOLID Principles Applied
- **Single Responsibility Principle (SRP)**: Each class has a single purpose (e.g., `LoginActivity` handles UI interaction, `AuthService` handles auth logic).
- **Open/Closed Principle (OCP)**: The system is open for extension. For example, to add real authentication, you create a new implementation of `AuthService` without modifying `LoginActivity`.
- **Liskov Substitution Principle (LSP)**: `MockAuthService` can be substituted for any other `AuthService` implementation seamlessly.
- **Interface Segregation Principle (ISP)**: Interfaces are kept focused.
- **Dependency Inversion Principle (DIP)**: `LoginActivity` depends on the `AuthService` abstraction, not the concrete `MockAuthService` implementation.

## How to Extend

### Adding a new Data Source
1. Create a new class in `data/source` that implements `AuthService`.
2. Update the initialization in `LoginActivity` (or your Dependency Injection module) to use the new class.

### Adding a new Feature (e.g. Transactions)
1. Create a new package under `ui/` (e.g., `ui/transactions`).
2. Add your `TransactionActivity`, layout, and related classes in that package.
3. Register the Activity in `AndroidManifest.xml`.
4. Update `HomeActivity` bottom navigation logic to launch the new activity.

## Setup & Testing
- Default User: **admin@test.com**
- Default Password: **password123**
- The login process simulates a 1.5-second network delay.
