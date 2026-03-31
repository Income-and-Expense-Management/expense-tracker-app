package com.ptithcm.quanlichitieu.data.repository;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.data.model.Expense;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MockExpenseRepository: A mock implementation of ExpenseRepository for development and testing.
 *
 * ARCHITECTURE NOTE:
 * This class provides hardcoded/mock data to simulate a real data source.
 * It implements the ExpenseRepository interface, making it interchangeable with
 * any other implementation (API, Database, etc.) without affecting the UI layer.
 *
 * USAGE:
 * This mock is useful for:
 * - UI development without a backend
 * - Unit testing with predictable data
 * - Demo/prototype builds
 *
 * TO REPLACE WITH REAL API:
 * ─────────────────────────────────────────────────────────────────────────────
 * Create a new class named ApiExpenseRepository (or RemoteExpenseRepository):
 *
 *   public class ApiExpenseRepository implements ExpenseRepository {
 *       private final ExpenseApiService apiService;
 *
 *       public ApiExpenseRepository(ExpenseApiService apiService) {
 *           this.apiService = apiService;
 *       }
 *
 *       @Override
 *       public List<Expense> getTopExpenses(int limit) {
 *           // Call API: apiService.getTopExpenses(limit).execute().body();
 *           // Map DTO to Expense model and return
 *       }
 *   }
 *
 * Then in your Activity/ViewModel, simply swap the implementation:
 *   // OLD: expenseRepository = new MockExpenseRepository();
 *   // NEW: expenseRepository = new ApiExpenseRepository(retrofit.create(ExpenseApiService.class));
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class MockExpenseRepository implements ExpenseRepository {

    // Mock expense data - simulates data that would come from an API
    private final List<Expense> mockExpenses;

    public MockExpenseRepository() {
        mockExpenses = createMockData();
    }

    /**
     * Creates mock expense data matching the design.
     * In a real app, this data would come from an API response.
     */
    private List<Expense> createMockData() {
        List<Expense> expenses = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // Mock data based on the design screenshot
        // Category icons use placeholder - replace with actual drawable resources

        expenses.add(new Expense(
                1,
                "Food & Drinks",
                "Starbucks, McDonald's, etc.",
                850000,  // 850,000 VND
                R.drawable.ic_food,  // Food category icon
                currentTime - 86400000  // 1 day ago
        ));

        expenses.add(new Expense(
                2,
                "Shopping",
                "Clothes, electronics, etc.",
                620000,  // 620,000 VND
                R.drawable.ic_shopping,  // Shopping category icon
                currentTime - 172800000  // 2 days ago
        ));

        expenses.add(new Expense(
                3,
                "Transportation",
                "Grab, taxi, fuel, etc.",
                320000,  // 320,000 VND
                R.drawable.ic_transport,  // Transport category icon
                currentTime - 259200000  // 3 days ago
        ));

        // Additional mock data for demonstration
        expenses.add(new Expense(
                4,
                "Entertainment",
                "Movies, games, etc.",
                275000,
                R.drawable.ic_payment_method,  // Placeholder icon
                currentTime - 345600000
        ));

        expenses.add(new Expense(
                5,
                "Bills & Utilities",
                "Electricity, water, internet",
                450000,
                R.drawable.ic_payment_method,  // Placeholder icon
                currentTime - 432000000
        ));

        return expenses;
    }

    @Override
    public List<Expense> getTopExpenses(int limit) {
        // Sort by amount descending and return top N
        List<Expense> sorted = new ArrayList<>(mockExpenses);
        Collections.sort(sorted, (e1, e2) -> Double.compare(e2.getAmount(), e1.getAmount()));

        // Return only the requested number of items
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    @Override
    public List<Expense> getCurrentMonthExpenses() {
        // For mock, return all expenses
        // In real implementation, filter by current month
        return new ArrayList<>(mockExpenses);
    }

    @Override
    public double getTotalExpenseAmount() {
        double total = 0;
        for (Expense expense : mockExpenses) {
            total += expense.getAmount();
        }
        return total;
    }
}
