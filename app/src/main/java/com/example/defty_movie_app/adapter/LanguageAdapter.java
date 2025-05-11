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
    private String selectedLanguageCode = "";

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

        if (language.getCode().equals(selectedLanguageCode)) {
            holder.languageSelected.setVisibility(View.VISIBLE);
        } else {
            holder.languageSelected.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedLanguageCode = language.getCode();
            viewModel.setLanguage(language.getCode());
            notifyDataSetChanged();
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
            languageSelected = itemView.findViewById(R.id.language_selected);
        }
    }
}
