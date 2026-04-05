package com.ptithcm.quanlichitieu.ui.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.quanlichitieu.R;

import java.util.List;

public class IconPickerAdapter extends RecyclerView.Adapter<IconPickerAdapter.IconViewHolder> {

    private final List<String> iconNames;
    private final OnIconClickListener listener;

    public interface OnIconClickListener {
        void onIconClick(String iconName);
    }

    public IconPickerAdapter(List<String> iconNames, OnIconClickListener listener) {
        this.iconNames = iconNames;
        this.listener = listener;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_icon_picker, parent, false);
        return new IconViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        holder.bind(iconNames.get(position));
    }

    @Override
    public int getItemCount() {
        return iconNames.size();
    }

    static class IconViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final OnIconClickListener listener;

        public IconViewHolder(@NonNull View itemView, OnIconClickListener listener) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            this.listener = listener;
        }

        public void bind(String iconName) {
            int resId = itemView.getContext().getResources().getIdentifier(
                    iconName, "drawable", itemView.getContext().getPackageName());
            if (resId != 0) {
                ivIcon.setImageResource(resId);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIconClick(iconName);
                }
            });
        }
    }
}
