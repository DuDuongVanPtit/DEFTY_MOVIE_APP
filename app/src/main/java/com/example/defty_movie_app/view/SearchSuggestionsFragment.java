package com.example.defty_movie_app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.defty_movie_app.R;
import com.example.defty_movie_app.adapter.SearchSuggestionsAdapter;

import java.util.List;

public class SearchSuggestionsFragment extends Fragment implements SearchSuggestionsAdapter.OnSuggestionClickListener {
    private RecyclerView suggestionsRecyclerView;
    private SearchSuggestionsAdapter suggestionsAdapter;
    private OnSuggestionSelectedListener listener;

    public interface OnSuggestionSelectedListener {
        void onSuggestionSelected(String suggestion);
    }

    public static SearchSuggestionsFragment newInstance() {
        return new SearchSuggestionsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.suggest_search_fragment, container, false);

        suggestionsRecyclerView = view.findViewById(R.id.suggestionsRecyclerView);
        suggestionsAdapter = new SearchSuggestionsAdapter(getContext(), this);
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    public void setSuggestions(List<String> suggestions) {
        if (suggestionsAdapter != null) {
            suggestionsAdapter.setSuggestions(suggestions);
        }
    }

    public void setOnSuggestionSelectedListener(OnSuggestionSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSuggestionClick(String suggestion) {
        if (listener != null) {
            listener.onSuggestionSelected(suggestion);
        }
    }
}