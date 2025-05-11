package com.example.defty_movie_app.adapter;

// SettingsAdapter.java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.SettingItem;


public class SettingsAdapter extends ListAdapter<SettingItem, SettingsAdapter.SettingViewHolder> {

    private final OnSettingItemClickListener clickListener;

    public interface OnSettingItemClickListener {
        void onSettingItemClicked(SettingItem item);
    }

    public SettingsAdapter(@NonNull DiffUtil.ItemCallback<SettingItem> diffCallback, OnSettingItemClickListener listener) {
        super(diffCallback);
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public SettingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_setting, parent, false);
        return new SettingViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingViewHolder holder, int position) {
        SettingItem currentItem = getItem(position);
        holder.bind(currentItem, clickListener);
    }

    static class SettingViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView summaryView;
        TextView currentValueView;
        ImageView arrowView;

        public SettingViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.setting_title);
            summaryView = itemView.findViewById(R.id.setting_summary);
            currentValueView = itemView.findViewById(R.id.setting_current_value);
            arrowView = itemView.findViewById(R.id.setting_arrow);
        }

        public void bind(final SettingItem item, final OnSettingItemClickListener listener) {
            titleView.setText(item.getTitle());

            if (item.getSummary() != null && !item.getSummary().isEmpty()) {
                summaryView.setText(item.getSummary());
                summaryView.setVisibility(View.VISIBLE);
            } else {
                summaryView.setVisibility(View.GONE);
            }

            if (item.getCurrentValue() != null && !item.getCurrentValue().isEmpty()) {
                currentValueView.setText(item.getCurrentValue());
                currentValueView.setVisibility(View.VISIBLE);
            } else {
                currentValueView.setVisibility(View.GONE);
            }

            if (item.isSelectable()) {
                arrowView.setVisibility(View.VISIBLE);
                itemView.setClickable(true);
                itemView.setFocusable(true);
                itemView.setOnClickListener(v -> listener.onSettingItemClicked(item));
            } else {
                arrowView.setVisibility(View.GONE);
                itemView.setClickable(false);
                itemView.setFocusable(false);
                itemView.setOnClickListener(null);
            }
        }
    }

    public static class SettingItemDiffCallback extends DiffUtil.ItemCallback<SettingItem> {
        @Override
        public boolean areItemsTheSame(@NonNull SettingItem oldItem, @NonNull SettingItem newItem) {
            return oldItem.getKey().equals(newItem.getKey());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SettingItem oldItem, @NonNull SettingItem newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                    (oldItem.getSummary() != null ? oldItem.getSummary().equals(newItem.getSummary()) : newItem.getSummary() == null) &&
                    (oldItem.getCurrentValue() != null ? oldItem.getCurrentValue().equals(newItem.getCurrentValue()) : newItem.getCurrentValue() == null) &&
                    oldItem.isSelectable() == newItem.isSelectable();
        }
    }
}