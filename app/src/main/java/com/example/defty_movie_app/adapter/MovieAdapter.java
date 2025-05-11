package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.Movie; // Đảm bảo model Movie của bạn ở đây

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private List<Movie> movies = new ArrayList<>();
    private final Context context;
    private final OnMovieClickListener onMovieClickListener;

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    public MovieAdapter(Context context, OnMovieClickListener listener) {
        this.context = context;
        this.onMovieClickListener = listener;
    }

    public void setMovies(List<Movie> newMovies) {
        this.movies = (newMovies != null) ? newMovies : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        if (movies != null && position < movies.size()) {
            Movie movie = movies.get(position);
            holder.bind(context, movie, onMovieClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        TextView titleView, labelType;
        ImageView imagePoster;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.titleView);
            imagePoster = itemView.findViewById(R.id.imagePoster);
            labelType = itemView.findViewById(R.id.labelType);
        }

        public void bind(Context context, final Movie movie, final OnMovieClickListener listener) {
            // Set title for the movie
            if (movie.getTitle() != null) {
                titleView.setText(movie.getTitle());
            } else {
                titleView.setText("");
            }

            if (movie.getImageUrl() != null && context != null) {
                Glide.with(context)
                        .load(movie.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(imagePoster);
            } else {
                if (context != null) {
                    imagePoster.setImageResource(R.drawable.placeholder_image);
                }
            }

            int membershipType = movie.isPremium();
            String membershipText = "";
            labelType.setTypeface(null, Typeface.BOLD);
            labelType.setTextSize(10);

            if (membershipType == 1) {
                membershipText = "Premium";
                labelType.setBackgroundResource(R.drawable.label_type_premium);
                assert context != null;
                labelType.setTextColor(context.getResources().getColor(R.color.premiumText, context.getTheme()));
            } else if (membershipType == 3) {
                membershipText = "Normal";
                labelType.setBackgroundResource(R.drawable.label_type_normal);
                assert context != null;
                labelType.setTextColor(context.getResources().getColor(R.color.normalText, context.getTheme()));
            } else {
                membershipText = "";
                labelType.setVisibility(View.GONE);
            }
            labelType.setText(membershipText);
            if (!membershipText.isEmpty()) {
                labelType.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onMovieClick(movie);
                        Toast.makeText(v.getContext(), "Choose Film!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}