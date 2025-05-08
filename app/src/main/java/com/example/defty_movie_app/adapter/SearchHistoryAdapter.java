package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.defty_movie_app.R;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {

    private List<String> historyList;
    private OnHistoryItemClickListener listener;
    private Context context;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(String query);
    }

    public SearchHistoryAdapter(Context context, OnHistoryItemClickListener listener) {
        this.context = context;
        this.historyList = new ArrayList<>();
        this.listener = listener;
    }

    public void updateHistory(List<String> newHistory) {
        this.historyList.clear();
        if (newHistory != null) {
            this.historyList.addAll(newHistory);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = historyList.get(position);
        holder.historyQueryTextView.setText(query);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryItemClick(query);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView historyQueryTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            historyQueryTextView = itemView.findViewById(R.id.historyQueryTextView);
        }
    }
}
