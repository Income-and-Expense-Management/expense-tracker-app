package com.ptithcm.quanlichitieu.data.model;

public class Transaction {

    public static final int TYPE_EXPENSE = 0;
    public static final int TYPE_INCOME = 1;

    private final long id;
    private final String category;
    private final double amount;
    private final int type;
    private final int iconResId;
    private final String walletName;
    private final long timestamp;

    public Transaction(long id, String category, double amount, int type, int iconResId, String walletName, long timestamp) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.iconResId = iconResId;
        this.walletName = walletName;
        this.timestamp = timestamp;
    }

    public long getId() { return id; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public int getType() { return type; }
    public int getIconResId() { return iconResId; }
    public String getWalletName() { return walletName; }
    public long getTimestamp() { return timestamp; }

    public boolean isExpense() { return type == TYPE_EXPENSE; }
}
