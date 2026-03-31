package com.ptithcm.quanlichitieu.data.model;

/**
 * Expense: Data model representing an expense transaction.
 *
 * This is a POJO (Plain Old Java Object) that holds expense data.
 * It is independent of any data source (database, API, etc.) and can be used
 * across all layers of the application.
 *
 * ARCHITECTURE NOTE:
 * - This model lives in the data/model package and represents the domain entity.
 * - When integrating with a real API, you may create a separate DTO (Data Transfer Object)
 *   in data/remote/dto and map it to this model in the repository.
 */
public class Expense {

    private final long id;
    private final String category;      // e.g., "Food & Drinks", "Shopping"
    private final String description;   // e.g., "Starbucks", "Nike Store"
    private final double amount;         // Expense amount in local currency
    private final int iconResId;         // Resource ID for category icon
    private final long timestamp;        // Unix timestamp of the expense

    /**
     * Constructor for creating an Expense instance.
     *
     * @param id          Unique identifier for the expense
     * @param category    Category name (e.g., "Food & Drinks")
     * @param description Short description of the expense
     * @param amount      Amount spent (positive value)
     * @param iconResId   Resource ID for the category icon drawable
     * @param timestamp   Unix timestamp when the expense occurred
     */
    public Expense(long id, String category, String description, double amount, int iconResId, long timestamp) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.iconResId = iconResId;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public int getIconResId() {
        return iconResId;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
