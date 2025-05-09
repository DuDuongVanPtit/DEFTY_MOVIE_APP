package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.Movie;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private List<Movie> movies = new ArrayList<>();
    private Context context;

    public MovieAdapter(Context context) {
        this.context = context;
    }

    public void setMovies(List<Movie> newMovies) {
        this.movies = newMovies;
        notifyDataSetChanged();
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

        public void bind(Context context, Movie movie) {
            // Set title for the movie
            titleView.setText(movie.getTitle());

            // Load the movie poster using Glide
            Glide.with(context)
                    .load(movie.getImageUrl())
                    .into(imagePoster);

            int membershipType = movie.isPremium();
            String membershipText = "";
            labelType.setTypeface(null, Typeface.BOLD);
            labelType.setTextSize(10);

            if (membershipType == 1) {
                membershipText = "Premium";
                labelType.setBackgroundResource(R.drawable.label_type_premium); // Drawable for Premium
                labelType.setTextColor(context.getResources().getColor(R.color.premiumText));
            } else if (membershipType == 3) {
                membershipText = "Normal";
                labelType.setBackgroundResource(R.drawable.label_type_normal); // Drawable for Normal
                labelType.setTextColor(context.getResources().getColor(R.color.normalText));
            }
            labelType.setText(membershipText);
        }

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
        holder.bind(context, movies.get(position));
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }
}
