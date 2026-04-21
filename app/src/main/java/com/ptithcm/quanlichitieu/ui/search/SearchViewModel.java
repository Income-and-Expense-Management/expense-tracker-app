package com.ptithcm.quanlichitieu.ui.search;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ptithcm.quanlichitieu.data.model.TransactionGroup;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepository;
import com.ptithcm.quanlichitieu.data.repository.TransactionRepositoryImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SearchViewModel - ViewModel cho màn hình tìm kiếm giao dịch.
 *
 * <p>Tuân thủ MVVM pattern:</p>
 * <ul>
 *   <li>Expose state qua LiveData (read-only ra ngoài)</li>
 *   <li>Business logic chạy trên background thread (ExecutorService)</li>
 *   <li>Lịch sử tìm kiếm lưu trong SharedPreferences</li>
 * </ul>
 *
 * <p>Tuân thủ SOLID:</p>
 * <ul>
 *   <li>SRP: Chỉ quản lý state tìm kiếm</li>
 *   <li>DIP: Phụ thuộc vào TransactionRepository interface</li>
 * </ul>
 */
public class SearchViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "search_history";
    private static final String KEY_RECENT_SEARCHES = "recent_searches";
    private static final String SEPARATOR = "||";
    private static final int MAX_RECENT_SEARCHES = 10;

    private final TransactionRepository transactionRepository;
    private final SharedPreferences sharedPreferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<TransactionGroup>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> recentSearches = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isShowingResults = new MutableLiveData<>(false);

    public SearchViewModel(@NonNull Application application) {
        super(application);
        com.ptithcm.quanlichitieu.data.local.token.TokenStorage tokenStorage = com.ptithcm.quanlichitieu.data.local.token.EncryptedTokenStorage.getInstance(application);
        this.transactionRepository = new TransactionRepositoryImpl(application.getApplicationContext(), tokenStorage);
        this.sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadRecentSearches();
    }

    // ==================== LIVEDATA GETTERS ====================

    public LiveData<List<TransactionGroup>> getSearchResults() {
        return searchResults;
    }

    public LiveData<List<String>> getRecentSearches() {
        return recentSearches;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /** true khi đang hiển thị kết quả tìm kiếm, false khi hiển thị lịch sử */
    public LiveData<Boolean> getIsShowingResults() {
        return isShowingResults;
    }

    // ==================== PUBLIC METHODS ====================

    /**
     * Thực hiện tìm kiếm giao dịch theo từ khóa.
     * Chạy trên background thread, emit kết quả qua LiveData.
     *
     * @param keyword  Từ khóa tìm kiếm
     * @param walletId ID ví để lọc (null = tìm tất cả ví)
     */
    public void search(@NonNull String keyword, @Nullable String walletId) {
        if (keyword.trim().isEmpty()) {
            resetToRecentSearches();
            return;
        }

        isLoading.setValue(true);
        isShowingResults.setValue(true);

        executor.execute(() -> {
            List<TransactionGroup> results = transactionRepository.searchTransactions(keyword.trim(), walletId);
            searchResults.postValue(results);
            isLoading.postValue(false);
        });
    }

    /**
     * Lưu từ khóa vào lịch sử tìm kiếm.
     * Không lưu nếu từ khóa rỗng hoặc đã tồn tại (sẽ di chuyển lên đầu).
     *
     * @param keyword Từ khóa cần lưu
     */
    public void saveRecentSearch(@NonNull String keyword) {
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) return;

        LinkedList<String> current = new LinkedList<>(getRecentSearchesFromPrefs());
        // Xóa nếu đã tồn tại để di chuyển lên đầu
        current.remove(trimmed);
        current.addFirst(trimmed);

        // Giới hạn số lượng
        while (current.size() > MAX_RECENT_SEARCHES) {
            current.removeLast();
        }

        saveRecentSearchesToPrefs(current);
        recentSearches.setValue(new ArrayList<>(current));
    }

    /** Xóa toàn bộ lịch sử tìm kiếm */
    public void clearRecentSearches() {
        sharedPreferences.edit().remove(KEY_RECENT_SEARCHES).apply();
        recentSearches.setValue(new ArrayList<>());
    }

    /** Đặt lại UI về trạng thái hiển thị lịch sử (khi xóa hết text tìm kiếm) */
    public void resetToRecentSearches() {
        isShowingResults.setValue(false);
        searchResults.setValue(new ArrayList<>());
        isLoading.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    // ==================== PRIVATE HELPERS ====================

    private void loadRecentSearches() {
        recentSearches.setValue(getRecentSearchesFromPrefs());
    }

    private List<String> getRecentSearchesFromPrefs() {
        String raw = sharedPreferences.getString(KEY_RECENT_SEARCHES, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\\|\\|")));
    }

    private void saveRecentSearchesToPrefs(List<String> searches) {
        String raw = String.join(SEPARATOR, searches);
        sharedPreferences.edit().putString(KEY_RECENT_SEARCHES, raw).apply();
    }
}
