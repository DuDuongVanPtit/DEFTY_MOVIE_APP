package com.example.defty_movie_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.Language;
import com.example.defty_movie_app.viewmodel.LanguageViewModel;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    private final List<Language> languageList;
    private final LanguageViewModel viewModel;
    private String selectedLanguageCode = ""; // Lưu ngôn ngữ đã chọn

    public LanguageAdapter(List<Language> languageList, LanguageViewModel viewModel) {
        this.languageList = languageList;
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        Language language = languageList.get(position);
        holder.flagImage.setImageResource(language.getFlagResId());
        holder.languageText.setText(language.getDisplayName());

        // Hiển thị dấu tích nếu ngôn ngữ này được chọn
        if (language.getCode().equals(selectedLanguageCode)) {
            holder.languageSelected.setVisibility(View.VISIBLE); // Hiển thị dấu tích
        } else {
            holder.languageSelected.setVisibility(View.GONE); // Ẩn dấu tích
        }

        // Xử lý sự kiện click
        holder.itemView.setOnClickListener(v -> {
            selectedLanguageCode = language.getCode(); // Cập nhật ngôn ngữ được chọn
            viewModel.setLanguage(language.getCode()); // Gọi ViewModel để thay đổi ngôn ngữ
            notifyDataSetChanged(); // Cập nhật lại RecyclerView để hiển thị dấu tích mới
        });
    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        ImageView flagImage, languageSelected;
        TextView languageText;

        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            flagImage = itemView.findViewById(R.id.language_flag);
            languageText = itemView.findViewById(R.id.language_name);
            languageSelected = itemView.findViewById(R.id.language_selected); // Dấu tích
        }
    }
}
