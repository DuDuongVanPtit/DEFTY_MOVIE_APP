package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
// import android.widget.Toast; // Can remove if not using for temporary toasts

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.request.MovieNameResponse;

import java.util.ArrayList;
import java.util.List;

public class HotSearchMovieAdapter extends RecyclerView.Adapter<HotSearchMovieAdapter.ViewHolder> {
    private List<MovieNameResponse> movies;
    private Context context;
    private OnHotMovieClickListener onHotMovieClickListener; // Listener instance

    // Interface for click events
    public interface OnHotMovieClickListener {
        void onHotMovieClick(MovieNameResponse movie);
    }

    // Update constructor to accept the listener
    public HotSearchMovieAdapter(Context context, OnHotMovieClickListener listener) {
        this.context = context;
        this.movies = new ArrayList<>();
        this.onHotMovieClickListener = listener; // Initialize the listener
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hot_search_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieNameResponse movieData = movies.get(position);

        if (movieData == null) {
            Log.e("HotSearchAdapter", "MovieNameResponse object is null at position " + position);
            holder.movieTitle.setText("Lỗi dữ liệu");
            holder.movieImage.setImageResource(R.drawable.error_image);
            return;
        }

        holder.movieTitle.setText(movieData.getName());
        Log.d("HotSearchAdapter", "Binding item: " + movieData.getName() + ", Thumbnail: " + movieData.getThumbnail());

        if (movieData.getThumbnail() != null && !movieData.getThumbnail().isEmpty()) {
            Glide.with(context)
                    .load(movieData.getThumbnail())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(holder.movieImage);
        } else {
            Log.w("HotSearchAdapter", "Thumbnail URL is null or empty for: " + movieData.getName());
            holder.movieImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set click listener on the item view
        holder.itemView.setOnClickListener(v -> {
            if (onHotMovieClickListener != null) {
                onHotMovieClickListener.onHotMovieClick(movieData);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    public void setMovies(List<MovieNameResponse> newMovies) {
        if (newMovies == null) {
            this.movies.clear();
            Log.d("HotSearchAdapter", "setMovies called with null list, clearing adapter.");
        } else {
            this.movies = newMovies;
            Log.d("HotSearchAdapter", "setMovies called with list size: " + newMovies.size());
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView movieImage;
        TextView movieTitle;
        ImageView playButton; // Assuming this might be used or styled, otherwise can be removed if not interactive

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImage = itemView.findViewById(R.id.movieImage);
            movieTitle = itemView.findViewById(R.id.movieTitle);
            playButton = itemView.findViewById(R.id.playButton); // Make sure this ID exists in item_hot_search_movie.xml
        }
    }
}