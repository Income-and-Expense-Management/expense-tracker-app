package com.ptithcm.quanlichitieu.data.local.contract;

import android.provider.BaseColumns;

/**
 * Database Contract chứa tất cả các hằng số cho cơ sở dữ liệu.
 * Theo chuẩn Android, giúp tránh lỗi typo khi viết raw SQL.
 * 
 * Cấu trúc:
 * - Mỗi bảng được đại diện bởi một inner class implements BaseColumns
 * - Các hằng số bao gồm: tên bảng, tên cột, và câu lệnh CREATE TABLE
 */
public final class DatabaseContract {

    // Thông tin chung về database
    public static final String DATABASE_NAME = "quanlichitieu.db";
    public static final int DATABASE_VERSION = 4; // Bumping to force drop & create

    // Private constructor để ngăn việc khởi tạo
    private DatabaseContract() {
    }

    // ================= BẢNG USERS =================
    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_FULL_NAME = "full_name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_AVATAR_URL = "avatar_url";
        public static final String COLUMN_AUTH_PROVIDER = "auth_provider";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_FULL_NAME + " TEXT, " +
                        COLUMN_EMAIL + " TEXT UNIQUE, " +
                        COLUMN_AVATAR_URL + " TEXT, " +
                        COLUMN_AUTH_PROVIDER + " TEXT, " +
                        COLUMN_PASSWORD + " TEXT, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL" +
                        ")";

        public static final String SQL_DROP_TABLE = 
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    // ================= BNG WALLETS =================
    public static class WalletEntry implements BaseColumns {
        public static final String TABLE_NAME = "wallets";
        
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_INITIAL_BALANCE = "initial_balance";
        public static final String COLUMN_CURRENCY = "currency";
        public static final String COLUMN_ICON_ID = "icon_id";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_DELETED_AT = "deleted_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_USER_ID + " TEXT NOT NULL, " +
                        COLUMN_NAME + " TEXT NOT NULL, " +
                        COLUMN_INITIAL_BALANCE + " INTEGER DEFAULT 0, " +
                        COLUMN_CURRENCY + " TEXT DEFAULT 'VND', " +
                        COLUMN_ICON_ID + " TEXT, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_DELETED_AT + " INTEGER" +
                        ")";

        public static final String SQL_DROP_TABLE = 
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String SQL_CREATE_INDEX_USER =
                "CREATE INDEX IF NOT EXISTS idx_wallets_user ON " +
                TABLE_NAME + "(" + COLUMN_USER_ID + ")";
    }

    // ================= BNG CATEGORIES =================
    public static class CategoryEntry implements BaseColumns {
        public static final String TABLE_NAME = "categories";
        
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_ICON_NAME = "icon_name";
        public static final String COLUMN_IS_ACTIVE = "is_active";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_DELETED_AT = "deleted_at";

        // CHECK constraint cho type
        public static final String TYPE_INCOME = "INCOME";
        public static final String TYPE_EXPENSE = "EXPENSE";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_USER_ID + " TEXT, " +
                        COLUMN_NAME + " TEXT NOT NULL, " +
                        COLUMN_TYPE + " TEXT CHECK(" + COLUMN_TYPE + " IN ('" + 
                        TYPE_INCOME + "', '" + TYPE_EXPENSE + "')), " +
                        COLUMN_ICON_NAME + " TEXT, " +
                        COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_DELETED_AT + " INTEGER" +
                        ")";

        public static final String SQL_DROP_TABLE = 
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    // ================= BNG TRANSACTIONS =================
    public static class TransactionEntry implements BaseColumns {
        public static final String TABLE_NAME = "transactions";
        
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_WALLET_ID = "wallet_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_TRANSACTION_DATE = "transaction_date";
        public static final String COLUMN_NOTE = "note";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_DELETED_AT = "deleted_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_WALLET_ID + " TEXT NOT NULL, " +
                        COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +
                        COLUMN_AMOUNT + " INTEGER NOT NULL, " +
                        COLUMN_TRANSACTION_DATE + " INTEGER NOT NULL, " +
                        COLUMN_NOTE + " TEXT, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_DELETED_AT + " INTEGER" +
                        ")";

        public static final String SQL_DROP_TABLE = 
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        // Index
        public static final String SQL_CREATE_INDEX_WALLET =
                "CREATE INDEX IF NOT EXISTS idx_transactions_wallet ON " + 
                TABLE_NAME + "(" + COLUMN_WALLET_ID + ")";
        
        public static final String SQL_CREATE_INDEX_CATEGORY = 
                "CREATE INDEX IF NOT EXISTS idx_transactions_category ON " + 
                TABLE_NAME + "(" + COLUMN_CATEGORY_ID + ")";
        
        public static final String SQL_CREATE_INDEX_DATE = 
                "CREATE INDEX IF NOT EXISTS idx_transactions_date ON " + 
                TABLE_NAME + "(" + COLUMN_TRANSACTION_DATE + ")";

        public static final String SQL_CREATE_INDEX_SYNC =
                "CREATE INDEX IF NOT EXISTS idx_transactions_sync ON " +
                TABLE_NAME + "(" + COLUMN_UPDATED_AT + ")";
    }

    // ================= BNG BUDGETS =================
    public static class BudgetEntry implements BaseColumns {
        public static final String TABLE_NAME = "budgets";
        
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_WALLET_ID = "wallet_id";
        public static final String COLUMN_CATEGORY_ID = "category_id";
        public static final String COLUMN_TARGET_AMOUNT = "target_amount";
        public static final String COLUMN_START_DATE = "start_date";
        public static final String COLUMN_END_DATE = "end_date";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        public static final String COLUMN_DELETED_AT = "deleted_at";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_ID + " TEXT PRIMARY KEY, " +
                        COLUMN_WALLET_ID + " TEXT NOT NULL, " +
                        COLUMN_CATEGORY_ID + " TEXT NOT NULL, " +
                        COLUMN_TARGET_AMOUNT + " INTEGER NOT NULL, " +
                        COLUMN_START_DATE + " INTEGER, " +
                        COLUMN_END_DATE + " INTEGER, " +
                        COLUMN_CREATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_UPDATED_AT + " INTEGER NOT NULL, " +
                        COLUMN_DELETED_AT + " INTEGER" +
                        ")";

        public static final String SQL_DROP_TABLE = 
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String SQL_CREATE_INDEX_WALLET =
                "CREATE INDEX IF NOT EXISTS idx_budgets_wallet ON " +
                TABLE_NAME + "(" + COLUMN_WALLET_ID + ")";
    }
}
