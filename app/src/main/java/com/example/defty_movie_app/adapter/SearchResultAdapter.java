package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.response.MovieAppSearchResultResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<MovieAppSearchResultResponse> movies;
    private Context context;
    private OnMovieClickListener movieClickListener;

    public interface OnMovieClickListener {
        void onMovieClick(MovieAppSearchResultResponse movie);
        void onPlayNowClick(MovieAppSearchResultResponse movie);
    }

    public SearchResultAdapter(Context context, OnMovieClickListener listener) {
        this.context = context;
        this.movies = new ArrayList<>();
        this.movieClickListener = listener;
    }

    public void setMovies(List<MovieAppSearchResultResponse> newMovies) {
        if (newMovies == null) {
            this.movies.clear();
        } else {
            this.movies = new ArrayList<>(newMovies); // Create a new list to avoid reference issues
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieAppSearchResultResponse movie = movies.get(position);
        holder.bind(movie, context, movieClickListener);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView movieThumbnailImageView;
        TextView movieTitleTextView;
        TextView movieRatingTextView;
        TextView movieYearTextView;
        TextView movieCategory1TextView;
        TextView movieCategory2TextView;
        TextView categorySeparator1TextView;
        TextView categorySeparator2TextView;
        TextView movieActorsTextView;
        TextView movieDescriptionTextView;
        Button playNowButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieThumbnailImageView = itemView.findViewById(R.id.movieThumbnailImageView);
            movieTitleTextView = itemView.findViewById(R.id.movieTitleTextView);
            movieRatingTextView = itemView.findViewById(R.id.movieRatingTextView); // Hardcoded text in XML
            movieYearTextView = itemView.findViewById(R.id.movieYearTextView);
            movieCategory1TextView = itemView.findViewById(R.id.movieCategory1TextView);
            movieCategory2TextView = itemView.findViewById(R.id.movieCategory2TextView);
            categorySeparator1TextView = itemView.findViewById(R.id.categorySeparator1TextView);
            categorySeparator2TextView = itemView.findViewById(R.id.categorySeparator2TextView);
            movieActorsTextView = itemView.findViewById(R.id.movieActorsTextView);
            movieDescriptionTextView = itemView.findViewById(R.id.movieDescriptionTextView);
            playNowButton = itemView.findViewById(R.id.playNowButton);
        }

        void bind(final MovieAppSearchResultResponse movie, Context context, final OnMovieClickListener listener) {
            movieTitleTextView.setText(movie.getTitle());
            movieDescriptionTextView.setText(movie.getDescription());

            // Load image with Glide, maintaining aspect ratio and adding rounded corners
            Glide.with(context)
                    .load(movie.getThumbnail())
                    .apply(new RequestOptions().transform(new RoundedCorners(16))) // 16px rounded corners
                    .placeholder(R.drawable.placeholder_image) // Make sure you have this drawable
                    .error(R.drawable.error_image) // Make sure you have this drawable
                    .into(movieThumbnailImageView);

            // Format release year
            if (movie.getReleaseDate() != null) {
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
                movieYearTextView.setText(yearFormat.format(movie.getReleaseDate()));
                movieYearTextView.setVisibility(View.VISIBLE);
            } else {
                movieYearTextView.setVisibility(View.GONE);
            }

            // Format categories
            List<String> categories = movie.getCategories();
            movieCategory1TextView.setVisibility(View.GONE);
            movieCategory2TextView.setVisibility(View.GONE);
            categorySeparator1TextView.setVisibility(View.GONE);
            categorySeparator2TextView.setVisibility(View.GONE);

            if (categories != null && !categories.isEmpty()) {
                movieCategory1TextView.setText(categories.get(0));
                movieCategory1TextView.setVisibility(View.VISIBLE);
                if (movieYearTextView.getVisibility() == View.VISIBLE) {
                    categorySeparator1TextView.setVisibility(View.VISIBLE);
                }

                if (categories.size() > 1) {
                    movieCategory2TextView.setText(categories.get(1));
                    movieCategory2TextView.setVisibility(View.VISIBLE);
                    categorySeparator2TextView.setVisibility(View.VISIBLE);
                }
            }


            // Format actors
            if (movie.getActors() != null && !movie.getActors().isEmpty()) {
                String actorsString = String.join(", ", movie.getActors());
                movieActorsTextView.setText(actorsString);
                movieActorsTextView.setVisibility(View.VISIBLE);
            } else {
                movieActorsTextView.setVisibility(View.GONE);
            }

            // Rating is hardcoded in XML for now: android:text="★ 9.8"

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovieClick(movie);
                }
            });

            playNowButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPlayNowClick(movie);
                }
                // Example action for play button
                Toast.makeText(context, "Chiếu ngay: " + movie.getTitle(), Toast.LENGTH_SHORT).show();
            });
        }
    }
}
