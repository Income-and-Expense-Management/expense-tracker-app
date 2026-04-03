package com.ptithcm.quanlichitieu.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.ptithcm.quanlichitieu.data.local.dao.BudgetDao;
import com.ptithcm.quanlichitieu.data.local.dao.CategoryDao;
import com.ptithcm.quanlichitieu.data.local.dao.TransactionDao;
import com.ptithcm.quanlichitieu.data.local.dao.UserDao;
import com.ptithcm.quanlichitieu.data.local.dao.WalletDao;

/**
 * DatabaseManager - Trung tâm quản lý truy cập cơ sở dữ liệu.
 * 
 * Vai trò:
 * - Singleton pattern đảm bảo một instance duy nhất
 * - Cung cấp các DAO instances cho các thành phần khác
 * - Quản lý database transactions
 * - Đóng database connection khi cần
 * 
 * Đã được refactor theo DAO Pattern và Single Responsibility Principle (SRP):
 * - Logic CRUD đã được tách ra các DAO riêng biệt
 * - DatabaseManager chỉ đóng vai trò "Trung tâm quản lý"
 * 
 * Cách sử dụng:
 * <pre>
 * DatabaseManager dbManager = DatabaseManager.getInstance(context);
 * 
 * // Lấy DAO cần thiết
 * UserDao userDao = dbManager.getUserDao();
 * WalletDao walletDao = dbManager.getWalletDao();
 * 
 * // Thực hiện thao tác CRUD
 * User user = userDao.getById(userId);
 * List&lt;Wallet&gt; wallets = walletDao.getByUserId(userId);
 * 
 * // Thực hiện trong transaction
 * dbManager.executeInTransaction(() -> {
 *     transactionDao.insert(transaction);
 *     walletDao.update(wallet);
 * });
 * </pre>
 */
public class DatabaseManager {

    private static final String TAG = "DatabaseManager";

    private static volatile DatabaseManager instance;

    private final BudgetDatabaseHelper dbHelper;

    // DAO instances - Lazy initialization
    private UserDao userDao;
    private WalletDao walletDao;
    private CategoryDao categoryDao;
    private TransactionDao transactionDao;
    private BudgetDao budgetDao;

    /**
     * Singleton pattern với double-checked locking.
     * Thread-safe và hiệu quả.
     * 
     * @param context Application context
     * @return DatabaseManager instance
     */
    public static DatabaseManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * Private constructor cho Singleton pattern.
     */
    private DatabaseManager(Context context) {
        dbHelper = BudgetDatabaseHelper.getInstance(context);
    }

    // ==================== DAO GETTERS ====================

    /**
     * Lấy UserDao instance.
     * Lazy initialization - chỉ tạo khi cần.
     * 
     * @return UserDao instance
     */
    public synchronized UserDao getUserDao() {
        if (userDao == null) {
            userDao = new UserDao(dbHelper);
        }
        return userDao;
    }

    /**
     * Lấy WalletDao instance.
     * Lazy initialization - chỉ tạo khi cần.
     * 
     * @return WalletDao instance
     */
    public synchronized WalletDao getWalletDao() {
        if (walletDao == null) {
            walletDao = new WalletDao(dbHelper);
        }
        return walletDao;
    }

    /**
     * Lấy CategoryDao instance.
     * Lazy initialization - chỉ tạo khi cần.
     * 
     * @return CategoryDao instance
     */
    public synchronized CategoryDao getCategoryDao() {
        if (categoryDao == null) {
            categoryDao = new CategoryDao(dbHelper);
        }
        return categoryDao;
    }

    /**
     * Lấy TransactionDao instance.
     * Lazy initialization - chỉ tạo khi cần.
     * 
     * @return TransactionDao instance
     */
    public synchronized TransactionDao getTransactionDao() {
        if (transactionDao == null) {
            transactionDao = new TransactionDao(dbHelper);
        }
        return transactionDao;
    }

    /**
     * Lấy BudgetDao instance.
     * Lazy initialization - chỉ tạo khi cần.
     * 
     * @return BudgetDao instance
     */
    public synchronized BudgetDao getBudgetDao() {
        if (budgetDao == null) {
            budgetDao = new BudgetDao(dbHelper);
        }
        return budgetDao;
    }

    // ==================== TRANSACTION SUPPORT ====================

    /**
     * Thực hiện nhiều thao tác trong một database transaction.
     * Đảm bảo tính atomic - tất cả hoặc không gì cả.
     * 
     * Ví dụ sử dụng:
     * <pre>
     * dbManager.executeInTransaction(() -> {
     *     transactionDao.insert(transaction1);
     *     transactionDao.insert(transaction2);
     *     walletDao.update(wallet);
     * });
     * </pre>
     * 
     * @param runnable Thao tác cần thực hiện trong transaction
     * @return true nếu thành công, false nếu có lỗi (đã rollback)
     */
    public boolean executeInTransaction(Runnable runnable) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            runnable.run();
            db.setTransactionSuccessful();
            Log.d(TAG, "Transaction completed successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Transaction failed, rolling back", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Thực hiện nhiều thao tác trong một database transaction với kết quả trả về.
     * Đảm bảo tính atomic - tất cả hoặc không gì cả.
     * 
     * Ví dụ sử dụng:
     * <pre>
     * String walletId = dbManager.executeInTransactionWithResult(() -> {
     *     String id = walletDao.insert(wallet);
     *     budgetDao.insert(budget);
     *     return id;
     * });
     * </pre>
     * 
     * @param callable Thao tác cần thực hiện trong transaction
     * @param <T> Kiểu dữ liệu trả về
     * @return Kết quả từ callable, hoặc null nếu có lỗi
     */
    public <T> T executeInTransactionWithResult(TransactionCallable<T> callable) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            T result = callable.call();
            db.setTransactionSuccessful();
            Log.d(TAG, "Transaction completed successfully");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Transaction failed, rolling back", e);
            return null;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Functional interface cho transaction với kết quả trả về.
     * 
     * @param <T> Kiểu dữ liệu trả về
     */
    public interface TransactionCallable<T> {
        T call() throws Exception;
    }

    // ==================== DATABASE MANAGEMENT ====================

    /**
     * Lấy BudgetDatabaseHelper instance.
     * Chỉ sử dụng khi cần truy cập trực tiếp SQLiteDatabase.
     * 
     * @return BudgetDatabaseHelper instance
     */
    public BudgetDatabaseHelper getDbHelper() {
        return dbHelper;
    }

    /**
     * Lấy SQLiteDatabase có thể ghi.
     * Chỉ sử dụng cho các trường hợp đặc biệt không thể dùng DAO.
     * 
     * @return SQLiteDatabase writable instance
     */
    public SQLiteDatabase getWritableDatabase() {
        return dbHelper.getWritableDatabase();
    }

    /**
     * Lấy SQLiteDatabase chỉ đọc.
     * Chỉ sử dụng cho các trường hợp đặc biệt không thể dùng DAO.
     * 
     * @return SQLiteDatabase readable instance
     */
    public SQLiteDatabase getReadableDatabase() {
        return dbHelper.getReadableDatabase();
    }

    /**
     * Đóng database connection.
     * Gọi khi không cần sử dụng database nữa (thường trong onDestroy của Application).
     * 
     * Lưu ý: Sau khi gọi close(), cần gọi getInstance() để lấy instance mới.
     */
    public void close() {
        Log.d(TAG, "Closing database connection");
        dbHelper.close();
        
        // Clear DAO instances
        userDao = null;
        walletDao = null;
        categoryDao = null;
        transactionDao = null;
        budgetDao = null;
    }

    /**
     * Reset Singleton instance.
     * Chỉ sử dụng trong testing hoặc khi cần recreate database.
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
