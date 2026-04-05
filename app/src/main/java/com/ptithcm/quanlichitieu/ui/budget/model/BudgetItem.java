package com.ptithcm.quanlichitieu.ui.budget.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * BudgetItem - UI Model cho hiển thị budget trong RecyclerView.
 * 
 * Chứa tất cả thông tin cần thiết để render một item budget,
 * bao gồm cả dữ liệu được tính toán từ nhiều nguồn.
 */
public class BudgetItem {
    private String id;              // Budget ID từ database
    private String categoryId;      // Category ID
    private String categoryName;    // Tên category
    private String categoryIcon;    // Icon name của category
    private long spent;             // Số tiền đã chi (VND)
    private long limit;             // Hạn mức ngân sách (VND)
    private long startDate;         // Ngày bắt đầu (timestamp)
    private long endDate;           // Ngày kết thúc (timestamp)
    private String walletId;        // Wallet ID
    private String walletName;      // Wallet name (optional)
    private String color;           // Màu hiển thị

    public BudgetItem() {
    }

    private BudgetItem(Builder builder) {
        this.id = builder.id;
        this.categoryId = builder.categoryId;
        this.categoryName = builder.categoryName;
        this.categoryIcon = builder.categoryIcon;
        this.spent = builder.spent;
        this.limit = builder.limit;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.walletId = builder.walletId;
        this.walletName = builder.walletName;
        this.color = builder.color;
    }

    // Legacy constructor for backward compatibility
    public BudgetItem(String categoryName, double spent, double limit, String tags, String color) {
        this.categoryName = categoryName;
        this.spent = (long) spent;
        this.limit = (long) limit;
        this.color = color;
    }

    // Builder Pattern
    public static class Builder {
        private String id;
        private String categoryId;
        private String categoryName;
        private String categoryIcon;
        private long spent;
        private long limit;
        private long startDate;
        private long endDate;
        private String walletId;
        private String walletName;
        private String color;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setCategoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder setCategoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder setCategoryIcon(String categoryIcon) {
            this.categoryIcon = categoryIcon;
            return this;
        }

        public Builder setSpent(long spent) {
            this.spent = spent;
            return this;
        }

        public Builder setLimit(long limit) {
            this.limit = limit;
            return this;
        }

        public Builder setStartDate(long startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder setEndDate(long endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder setWalletId(String walletId) {
            this.walletId = walletId;
            return this;
        }

        public Builder setWalletName(String walletName) {
            this.walletName = walletName;
            return this;
        }

        public Builder setColor(String color) {
            this.color = color;
            return this;
        }

        public BudgetItem build() {
            return new BudgetItem(this);
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getCategoryIcon() {
        return categoryIcon;
    }

    public long getSpent() {
        return spent;
    }

    public long getLimit() {
        return limit;
    }

    public long getStartDate() {
        return startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getWalletName() {
        return walletName;
    }

    public String getColor() {
        return color;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setCategoryIcon(String categoryIcon) {
        this.categoryIcon = categoryIcon;
    }

    public void setSpent(long spent) {
        this.spent = spent;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public void setColor(String color) {
        this.color = color;
    }

    // Computed properties
    public int getProgress() {
        if (limit == 0) return 0;
        int progress = (int) ((spent * 100) / limit);
        return Math.min(progress, 100);
    }

    public boolean isOverBudget() {
        return spent > limit;
    }

    public long getRemainingAmount() {
        return limit - spent;
    }

    /**
     * Format số tiền theo định dạng VND với dấu chấm phân cách.
     * Ví dụ: 1000000 -> "1.000.000 đ"
     */
    public String getFormattedSpent() {
        return formatMoneyVND(spent);
    }

    public String getFormattedLimit() {
        return formatMoneyVND(limit);
    }

    public String getFormattedAmount() {
        return formatMoneyVND(spent) + "/ " + formatMoneyVND(limit);
    }

    public String getFormattedRemaining() {
        long remaining = getRemainingAmount();
        String prefix = remaining >= 0 ? "+ " : "- ";
        return prefix + formatMoneyVND(Math.abs(remaining));
    }

    /**
     * Format tiền theo chuẩn Việt Nam với dấu chấm phân cách hàng nghìn.
     */
    private String formatMoneyVND(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount) + " đ";
    }

    /**
     * Tính số ngày còn lại đến khi hết hạn budget.
     */
    public int getDaysRemaining() {
        long now = System.currentTimeMillis();
        if (now > endDate) return 0;
        long diff = endDate - now;
        return (int) (diff / (24 * 60 * 60 * 1000));
    }
}