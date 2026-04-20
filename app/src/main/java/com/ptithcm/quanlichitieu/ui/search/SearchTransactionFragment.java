package com.ptithcm.quanlichitieu.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;
import com.ptithcm.quanlichitieu.ui.main.MainActivity;
import com.ptithcm.quanlichitieu.ui.search.adapter.RecentSearchAdapter;
import com.ptithcm.quanlichitieu.ui.transaction.adapter.TransactionAdapter;

/**
 * SearchTransactionFragment - Màn hình tìm kiếm giao dịch dùng chung.
 *
 * <p>Được sử dụng bởi cả HomeFragment và TransactionFragment thông qua
 * static factory method {@link #newInstance(String)}.</p>
 *
 * <p>Tính năng:</p>
 * <ul>
 *   <li>Thanh tìm kiếm real-time với debounce 400ms</li>
 *   <li>Lịch sử tìm kiếm gần đây (lưu SharedPreferences)</li>
 *   <li>Kết quả hiển thị bằng TransactionAdapter tái dùng</li>
 *   <li>Empty state khi không tìm thấy kết quả</li>
 * </ul>
 *
 * <p>Navigation: mở bằng replace + addToBackStack. Nhấn X → popBackStack.</p>
 */
public class SearchTransactionFragment extends Fragment {

    private static final String ARG_WALLET_ID = "arg_wallet_id";
    private static final long DEBOUNCE_DELAY_MS = 400L;

    private SearchViewModel viewModel;

    // Views
    private EditText etSearchQuery;
    private ImageView btnSearchClose;
    private ProgressBar progressSearch;
    private LinearLayout layoutRecentSearches;
    private LinearLayout layoutSearchResults;
    private LinearLayout layoutNoResult;
    private RecyclerView rvRecentSearches;
    private RecyclerView rvSearchResults;
    private TextView tvClearHistory;
    private TextView tvEmptyRecent;

    // Adapters
    private RecentSearchAdapter recentSearchAdapter;
    private TransactionAdapter searchResultAdapter;

    // Debounce
    private final android.os.Handler debounceHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable debounceRunnable;

    /**
     * Tạo instance mới của SearchTransactionFragment.
     *
     * @param walletId ID ví để lọc kết quả (null = tìm toàn bộ ví, từ HomeFragment)
     * @return Instance mới của fragment
     */
    public static SearchTransactionFragment newInstance(@Nullable String walletId) {
        SearchTransactionFragment fragment = new SearchTransactionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WALLET_ID, walletId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        initViews(view);
        setupRecentSearchList();
        setupSearchResultList();
        setupSearchInput();
        setupCloseButton();
        setupClearHistory();
        observeViewModel();

        // Tự động mở bàn phím khi Fragment được mở
        showKeyboard();
    }

    // ==================== INIT ====================

    private void initViews(View view) {
        etSearchQuery = view.findViewById(R.id.etSearchQuery);
        btnSearchClose = view.findViewById(R.id.btnSearchClose);
        progressSearch = view.findViewById(R.id.progressSearch);
        layoutRecentSearches = view.findViewById(R.id.layoutRecentSearches);
        layoutSearchResults = view.findViewById(R.id.layoutSearchResults);
        layoutNoResult = view.findViewById(R.id.layoutNoResult);
        rvRecentSearches = view.findViewById(R.id.rvRecentSearches);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        tvClearHistory = view.findViewById(R.id.tvClearHistory);
        tvEmptyRecent = view.findViewById(R.id.tvEmptyRecent);
    }

    private void setupRecentSearchList() {
        recentSearchAdapter = new RecentSearchAdapter();
        recentSearchAdapter.setOnRecentSearchClickListener(new RecentSearchAdapter.OnRecentSearchClickListener() {
            @Override
            public void onSearchClick(String query) {
                // Điền vào ô tìm kiếm và search ngay
                etSearchQuery.setText(query);
                etSearchQuery.setSelection(query.length());
                triggerSearch(query);
            }

            @Override
            public void onFillClick(String query) {
                // Chỉ điền vào ô tìm kiếm, không search
                etSearchQuery.setText(query);
                etSearchQuery.setSelection(query.length());
            }
        });

        rvRecentSearches.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentSearches.setAdapter(recentSearchAdapter);
        rvRecentSearches.setNestedScrollingEnabled(false);
    }

    private void setupSearchResultList() {
        searchResultAdapter = new TransactionAdapter();
        searchResultAdapter.setOnTransactionClickListener(transaction -> {
            hideKeyboard();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openTransactionDetail(transaction.getId());
            }
        });
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(searchResultAdapter);
    }

    private void setupSearchInput() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hủy debounce cũ nếu người dùng vẫn đang gõ
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                final String query = s.toString().trim();

                if (query.isEmpty()) {
                    viewModel.resetToRecentSearches();
                    return;
                }

                // Đặt debounce mới: 400ms sau khi dừng gõ mới search
                debounceRunnable = () -> triggerSearch(query);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Nhấn Done/Search trên bàn phím → search ngay không cần chờ debounce
        etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchQuery.getText().toString().trim();
                if (!query.isEmpty()) {
                    if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                    triggerSearch(query);
                }
                return true;
            }
            return false;
        });
    }

    private void setupCloseButton() {
        btnSearchClose.setOnClickListener(v -> {
            hideKeyboard();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void setupClearHistory() {
        tvClearHistory.setOnClickListener(v -> viewModel.clearRecentSearches());
    }

    // ==================== OBSERVE ====================

    private void observeViewModel() {
        // Hiển thị/ẩn loading indicator
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading ->
                progressSearch.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        // Chuyển đổi giữa recent searches và search results
        viewModel.getIsShowingResults().observe(getViewLifecycleOwner(), isShowingResults -> {
            layoutRecentSearches.setVisibility(isShowingResults ? View.GONE : View.VISIBLE);
            layoutSearchResults.setVisibility(isShowingResults ? View.VISIBLE : View.GONE);
        });

        // Cập nhật danh sách lịch sử tìm kiếm
        viewModel.getRecentSearches().observe(getViewLifecycleOwner(), searches -> {
            recentSearchAdapter.setData(searches);
            // Ẩn hint khi có lịch sử, hiện khi chưa có lịch sử
            tvEmptyRecent.setVisibility(searches.isEmpty() ? View.VISIBLE : View.GONE);
            tvClearHistory.setVisibility(searches.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Cập nhật kết quả tìm kiếm
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), groups -> {
            searchResultAdapter.setGroups(groups);
            boolean hasResults = groups != null && !groups.isEmpty();
            layoutNoResult.setVisibility(hasResults ? View.GONE : View.VISIBLE);
            rvSearchResults.setVisibility(hasResults ? View.VISIBLE : View.GONE);
        });
    }

    // ==================== PRIVATE HELPERS ====================

    private void triggerSearch(String query) {
        String walletId = getArguments() != null ? getArguments().getString(ARG_WALLET_ID) : null;
        viewModel.search(query, walletId);
        viewModel.saveRecentSearch(query);
    }

    private void showKeyboard() {
        etSearchQuery.requestFocus();
        etSearchQuery.postDelayed(() -> {
            if (getContext() == null) return;
            InputMethodManager imm = (InputMethodManager)
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearchQuery, InputMethodManager.SHOW_IMPLICIT);
        }, 150);
    }

    private void hideKeyboard() {
        if (getView() == null) return;
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy debounce callback để tránh memory leak
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
    }
}
