package com.ptithcm.quanlichitieu.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.BudgetEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.CategoryEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.TransactionEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.UserEntry;
import com.ptithcm.quanlichitieu.data.local.contract.DatabaseContract.WalletEntry;

/**
 * SQLiteOpenHelper chính cho ứng dụng quản lý chi tiêu.
 * 
 * Tính năng chính:
 * - Bật Foreign Key constraints trong onConfigure()
 * - Tạo tất cả các bảng theo đúng thứ tự dependency trong onCreate()
 * - Drop bảng theo đúng thứ tự ngược để không vi phạm FK trong onUpgrade()
 * - Singleton pattern để đảm bảo chỉ có một instance duy nhất
 * 
 * Thứ tự dependency của các bảng:
 * 1. users (không phụ thuộc bảng nào)
 * 2. wallets (phụ thuộc users)
 * 3. categories (phụ thuộc users)
 * 4. transactions (phụ thuộc wallets, categories)
 * 5. budgets (phụ thuộc wallets, categories)
 */
public class BudgetDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "BudgetDatabaseHelper";
    
    // Singleton instance
    private static volatile BudgetDatabaseHelper instance;

    /**
     * Lấy instance duy nhất của BudgetDatabaseHelper (Singleton pattern).
     * Thread-safe với double-checked locking.
     */
    public static BudgetDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (BudgetDatabaseHelper.class) {
                if (instance == null) {
                    instance = new BudgetDatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Private constructor cho Singleton pattern.
     * Sử dụng Application context để tránh memory leak.
     */
    private BudgetDatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    /**
     * QUAN TRỌNG: Bật Foreign Key constraints.
     * Phải gọi trước onCreate() và onUpgrade().
     * Đây là điều kiện tiên quyết để các ràng buộc FK hoạt động.
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        Log.d(TAG, "Foreign key constraints enabled");
    }

    /**
     * Tạo tất cả các bảng theo thứ tự dependency.
     * Bảng cha phải được tạo trước bảng con.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables...");

        // 1. Tạo bảng users (không phụ thuộc bảng nào)
        db.execSQL(UserEntry.SQL_CREATE_TABLE);
        Log.d(TAG, "Created table: " + UserEntry.TABLE_NAME);

        // 2. Tạo bảng wallets (phụ thuộc users)
        db.execSQL(WalletEntry.SQL_CREATE_TABLE);
        Log.d(TAG, "Created table: " + WalletEntry.TABLE_NAME);

        // 3. Tạo bảng categories (phụ thuộc users)
        db.execSQL(CategoryEntry.SQL_CREATE_TABLE);
        Log.d(TAG, "Created table: " + CategoryEntry.TABLE_NAME);

        // 4. Tạo bảng transactions (phụ thuộc wallets, categories)
        db.execSQL(TransactionEntry.SQL_CREATE_TABLE);
        Log.d(TAG, "Created table: " + TransactionEntry.TABLE_NAME);

        // 5. Tạo bảng budgets (phụ thuộc wallets, categories)
        db.execSQL(BudgetEntry.SQL_CREATE_TABLE);
        Log.d(TAG, "Created table: " + BudgetEntry.TABLE_NAME);

        // Tạo indexes để tối ưu performance
        db.execSQL(TransactionEntry.SQL_CREATE_INDEX_WALLET);
        db.execSQL(TransactionEntry.SQL_CREATE_INDEX_CATEGORY);
        db.execSQL(TransactionEntry.SQL_CREATE_INDEX_DATE);
        db.execSQL(TransactionEntry.SQL_CREATE_INDEX_SYNC);   // idx_transactions_sync (sync support)
        db.execSQL(BudgetEntry.SQL_CREATE_INDEX_WALLET);      // idx_budgets_wallet
        db.execSQL(WalletEntry.SQL_CREATE_INDEX_USER);        // idx_wallets_user
        Log.d(TAG, "Created indexes for all tables");


        Log.d(TAG, "Database created successfully");
    }

    /**
     * Xử lý nâng cấp database.
     * Drop bảng theo thứ tự ngược (bảng con trước, bảng cha sau)
     * để không vi phạm Foreign Key constraints.
     * 
     * Trong production, nên sử dụng migration scripts thay vì drop toàn bộ.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Tắt tạm FK để drop / alter bảng
        db.execSQL("PRAGMA foreign_keys = OFF");

        // Vì phiên bản 3 có quá nhiều breaking changes theo chuẩn Synchronization Server
        // (xóa cột `type` ở bảng transactions, thêm `updated_at`, `deleted_at`, v.v)
        // Nên sẽ Drop toàn bộ bảng và recreate (Destructive Upgrade).

        // 5. Drop budgets
        db.execSQL(BudgetEntry.SQL_DROP_TABLE);
        
        // 4. Drop transactions
        db.execSQL(TransactionEntry.SQL_DROP_TABLE);
        
        // 3. Drop categories
        db.execSQL(CategoryEntry.SQL_DROP_TABLE);
        
        // 2. Drop wallets
        db.execSQL(WalletEntry.SQL_DROP_TABLE);
        
        // 1. Drop users
        db.execSQL(UserEntry.SQL_DROP_TABLE);

        // Bật lại FK
        db.execSQL("PRAGMA foreign_keys = ON");

        // Recreate tables
        onCreate(db);
    }

    /**
     * Xử lý downgrade database (nếu cần).
     * Mặc định: tái tạo toàn bộ database.
     */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }



    /**
     * Lấy database có thể ghi.
     * Override để thêm logging hoặc xử lý đặc biệt nếu cần.
     */
    @Override
    public SQLiteDatabase getWritableDatabase() {
        Log.d(TAG, "Getting writable database");
        return super.getWritableDatabase();
    }

    /**
     * Lấy database chỉ đọc.
     * Override để thêm logging hoặc xử lý đặc biệt nếu cần.
     */
    @Override
    public SQLiteDatabase getReadableDatabase() {
        Log.d(TAG, "Getting readable database");
        return super.getReadableDatabase();
    }
}
