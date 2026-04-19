package com.ptithcm.quanlichitieu.data.repository;

import com.ptithcm.quanlichitieu.data.model.Expense;

import java.util.List;

/**
 * ExpenseRepository: Interface defining the contract for expense data operations.
 *
 * ARCHITECTURE NOTE - REPOSITORY PATTERN:
 * This interface follows the Repository Pattern, a key principle of Clean Architecture.
 * It abstracts the data source from the rest of the application, allowing:
 *
 * 1. DECOUPLING: UI layer (Activities/Fragments) depends on this interface,
 *    not on concrete implementations.
 *
 * 2. TESTABILITY: Easy to mock for unit testing without needing real data sources.
 *
 * 3. FLEXIBILITY: Swap data sources without changing UI code.
 *
 * HOW TO ADD NEW IMPLEMENTATIONS:
 * ─────────────────────────────────────────
 * 1. Create a new class: ApiExpenseRepository implements ExpenseRepository
 * 2. Implement the methods using Retrofit/OkHttp to call your backend API
 * 3. Inject it into your components
 *
 * The UI code remains unchanged because it depends on the ExpenseRepository interface,
 * not the concrete implementation. This is the Dependency Inversion Principle (D in SOLID).
 */
public interface ExpenseRepository {

    /**
     * Retrieves the top N expenses sorted by amount in descending order.
     *
     * @param limit Maximum number of expenses to return
     * @return List of expenses sorted by amount (highest first)
     *
     * FUTURE API IMPLEMENTATION:
     * This would typically involve:
     * - Making an HTTP GET request to: /api/expenses/top?limit={limit}
     * - Parsing the JSON response into List<Expense>
     * - Optionally caching the result for offline access
     */
    List<Expense> getTopExpenses(int limit);

    /**
     * Retrieves all expenses for the current month.
     *
     * @return List of all expenses in the current month
     */
    List<Expense> getCurrentMonthExpenses();

    /**
     * Calculates the total amount spent in the current month.
     *
     * @return Total expense amount
     */
    double getTotalExpenseAmount();
}
