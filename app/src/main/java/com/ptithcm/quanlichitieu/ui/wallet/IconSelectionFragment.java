package com.ptithcm.quanlichitieu.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;

import java.util.Arrays;
import java.util.List;

public class IconSelectionFragment extends Fragment {

    public static final String REQUEST_KEY_ICON = "request_key_icon";
    public static final String RESULT_ICON_ID = "result_icon_id";

    // Danh sách placeholder icon (nếu db lưu iconId là String có thể dùng format resource name)
    // Để cho tiện mình sẽ dùng mảng resource int ID trước.
    private final List<String> iconNames = Arrays.asList(
            "ic_bills", "ic_bonus", "ic_education", "ic_food",
            "ic_shopping", "ic_wallet", "ic_health", "ic_investment",
            "ic_entertainment", "ic_gift", "ic_salary", "ic_transport", "ic_other"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_icon_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnClose).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        RecyclerView rvIcons = view.findViewById(R.id.rvIcons);
        rvIcons.setLayoutManager(new GridLayoutManager(getContext(), 5));

        IconAdapter adapter = new IconAdapter(iconNames, iconName -> {
            Bundle bundle = new Bundle();
            bundle.putString(RESULT_ICON_ID, iconName);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY_ICON, bundle);
            requireActivity().getSupportFragmentManager().popBackStack();
        });
        rvIcons.setAdapter(adapter);
    }

    private static class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private final List<String> items;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(String iconName);
        }

        public IconAdapter(List<String> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_icon, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String iconName = items.get(position);
            int resId = holder.itemView.getContext().getResources().getIdentifier(
                    iconName, "drawable", holder.itemView.getContext().getPackageName()
            );
            if (resId != 0) {
                holder.ivIcon.setImageResource(resId);
            }
            holder.itemView.setOnClickListener(v -> listener.onItemClick(iconName));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            ViewHolder(View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivIconContainer);
            }
        }
    }
}

