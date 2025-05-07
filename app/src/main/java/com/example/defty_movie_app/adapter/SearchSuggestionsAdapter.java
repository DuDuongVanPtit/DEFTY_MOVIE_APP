package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Color; // THÊM import này
import android.text.Spannable; // THÊM import này
import android.text.SpannableString; // THÊM import này
import android.text.style.ForegroundColorSpan; // THÊM import này
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // THÊM import này cho toLowerCase

public class SearchSuggestionsAdapter extends RecyclerView.Adapter<SearchSuggestionsAdapter.ViewHolder> {

    private List<MovieNameResponse> suggestionsList;
    private Context context;
    private OnSuggestionClickListener listener;
    private String currentQuery = ""; // THÊM: Lưu trữ query hiện tại

    public interface OnSuggestionClickListener {
        void onSuggestionClick(MovieNameResponse movieNameResponse);
    }

    public SearchSuggestionsAdapter(Context context, OnSuggestionClickListener listener) {
        this.context = context;
        this.suggestionsList = new ArrayList<>();
        this.listener = listener;
    }

    // THAY ĐỔI: Thêm tham số query
    public void updateSuggestions(List<MovieNameResponse> newSuggestions, String query) {
        if (newSuggestions == null) {
            this.suggestionsList.clear();
        } else {
            this.suggestionsList = newSuggestions;
        }
        this.currentQuery = (query != null) ? query.toLowerCase(Locale.getDefault()) : ""; // Lưu query dưới dạng chữ thường
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieNameResponse movieNameResponse = suggestionsList.get(position);
        // TRUYỀN currentQuery vào bind
        holder.bind(movieNameResponse, listener, currentQuery);
    }

    @Override
    public int getItemCount() {
        return suggestionsList != null ? suggestionsList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView movieTitleOnly;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieTitleOnly = itemView.findViewById(R.id.suggestionMovieTitleOnly);
            if (movieTitleOnly == null) {
                Log.e("AdapterSuggestVH", "movieTitleOnly TextView not found!");
            }
        }

        // THAY ĐỔI: Thêm tham số query
        void bind(final MovieNameResponse movieNameResponse, final OnSuggestionClickListener listener, String query) {
            if (movieTitleOnly == null) return;

            String originalName = movieNameResponse.getName();

            if (movieNameResponse != null && originalName != null) {
                if (query != null && !query.isEmpty() && originalName.toLowerCase(Locale.getDefault()).contains(query)) {
                    SpannableString spannableName = new SpannableString(originalName);
                    String lowerCaseOriginalName = originalName.toLowerCase(Locale.getDefault());
                    int startIndex = 0;
                    while (startIndex < lowerCaseOriginalName.length()) {
                        int queryIndex = lowerCaseOriginalName.indexOf(query, startIndex);
                        if (queryIndex == -1) {
                            break; // Không tìm thấy nữa
                        }
                        // Áp dụng màu xanh lá cây (Color.GREEN)
                        spannableName.setSpan(new ForegroundColorSpan(Color.GREEN),
                                queryIndex,
                                queryIndex + query.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        startIndex = queryIndex + query.length(); // Tiếp tục tìm kiếm từ sau vị trí vừa tìm thấy
                    }
                    movieTitleOnly.setText(spannableName);
                    Log.d("AdapterSuggestBind", "Binding spannable name: '" + originalName + "' with query '" + query + "'");
                } else {
                    // Nếu không có query hoặc không khớp, hiển thị tên gốc
                    movieTitleOnly.setText(originalName);
                    Log.d("AdapterSuggestBind", "Binding original name: '" + originalName + "'");
                }
            } else {
                Log.w("AdapterSuggestBind", "MovieNameResponse or name is null at position " + getAdapterPosition());
                movieTitleOnly.setText("");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null && movieNameResponse != null) {
                    listener.onSuggestionClick(movieNameResponse);
                }
            });
        }
    }
}