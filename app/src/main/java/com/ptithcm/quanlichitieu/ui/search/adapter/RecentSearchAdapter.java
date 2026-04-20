package com.ptithcm.quanlichitieu.ui.search.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecentSearchAdapter - Adapter hiển thị danh sách từ khóa tìm kiếm gần đây.
 *
 * <p>Tuân thủ SOLID:</p>
 * <ul>
 *   <li>SRP: Chỉ render danh sách lịch sử tìm kiếm</li>
 *   <li>OCP: Callback interface cho phép mở rộng hành vi mà không sửa class này</li>
 * </ul>
 */
public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder> {

    /**
     * Callback xử lý sự kiện click trên item lịch sử tìm kiếm.
     */
    public interface OnRecentSearchClickListener {
        /** Nhấn vào item → thực hiện tìm kiếm với từ khóa đó */
        void onSearchClick(String query);

        /** Nhấn icon mũi tên → điền từ khóa vào ô tìm kiếm (không search ngay) */
        void onFillClick(String query);
    }

    private List<String> queries = new ArrayList<>();
    private OnRecentSearchClickListener listener;

    public void setData(@NonNull List<String> queries) {
        this.queries = new ArrayList<>(queries);
        notifyDataSetChanged();
    }

    public void setOnRecentSearchClickListener(OnRecentSearchClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecentSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_search, parent, false);
        return new RecentSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentSearchViewHolder holder, int position) {
        String query = queries.get(position);
        holder.bind(query, listener);
    }

    @Override
    public int getItemCount() {
        return queries.size();
    }

    static class RecentSearchViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRecentSearchQuery;
        private final ImageView btnFillSearch;

        RecentSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecentSearchQuery = itemView.findViewById(R.id.tvRecentSearchQuery);
            btnFillSearch = itemView.findViewById(R.id.btnFillSearch);
        }

        void bind(String query, OnRecentSearchClickListener listener) {
            tvRecentSearchQuery.setText(query);

            // Nhấn toàn bộ item → search ngay
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSearchClick(query);
            });

            // Nhấn icon mũi tên → chỉ điền vào ô tìm kiếm
            btnFillSearch.setOnClickListener(v -> {
                if (listener != null) listener.onFillClick(query);
            });
        }
    }
}
