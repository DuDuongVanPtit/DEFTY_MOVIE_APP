package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable; // Import Drawable
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Import Nullable
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.Movie;

import java.util.ArrayList;
import java.util.List;

public class MovieHomeAdapter extends RecyclerView.Adapter<MovieHomeAdapter.MovieViewHolder> {
    private List<Movie> movies;
    private final Context context; // Make context final if initialized in constructor

    public MovieHomeAdapter(@NonNull Context context) { // Add NonNull annotation
        this.context = context;
        this.movies = new ArrayList<>();
    }

    // Method to update the list of movies
    public void setMovies(List<Movie> movies) {
        if (movies != null) {
            this.movies = new ArrayList<>(movies); // Create a new list to avoid reference issues
        } else {
            this.movies = new ArrayList<>(); // Initialize with empty list if null
        }
        notifyDataSetChanged(); // Notify adapter about data change
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each movie item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.movie_item, parent, false); // Use your item layout file name
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        // Get the movie at the current position
        Movie movie = movies.get(position);
        // Bind the movie data to the ViewHolder
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        // Return the total number of movies
        return movies != null ? movies.size() : 0;
    }

    // ViewHolder class to hold the views for each movie item
    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView posterImageView;
        private final TextView titleTextView;
        private final TextView infoTextView; // Keep infoTextView if needed for future use or hide it

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views from the layout
            posterImageView = itemView.findViewById(R.id.posterImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            infoTextView = itemView.findViewById(R.id.infoTextView); // Make sure this ID exists in item_movie_home.xml

            // Set click listener for the item view
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition(); // Use getBindingAdapterPosition()
                if (position != RecyclerView.NO_POSITION) {
                    Movie clickedMovie = movies.get(position);
                    Toast.makeText(context, "Clicked: " + clickedMovie.getTitle(), Toast.LENGTH_SHORT).show();
                    // TODO: Implement navigation to movie details screen
                    // Intent intent = new Intent(context, MovieDetailActivity.class);
                    // intent.putExtra("MOVIE_SLUG", clickedMovie.getSlug());
                    // context.startActivity(intent);
                }
            });
        }

        // Method to bind movie data to the views
        public void bind(Movie movie) {
            // Set movie title
            titleTextView.setText(movie.getTitle());

            // Set info text (using slug as an example, or leave empty/hide)
            // infoTextView.setText(movie.getSlug()); // Example: Show slug
            infoTextView.setText(""); // Or set to empty
            // infoTextView.setVisibility(View.GONE); // Or hide it completely if not needed

            // Load poster image using Glide
            if (movie.getImageUrl() != null && !movie.getImageUrl().isEmpty()) {
                // Define a placeholder drawable
                Drawable placeholder = ContextCompat.getDrawable(context, R.drawable.ic_movie_placeholder); // Create this placeholder drawable

                Glide.with(context)
                        .load(movie.getImageUrl())
                        .placeholder(placeholder) // Show placeholder while loading
                        .error(placeholder) // Show placeholder if loading fails
                        .listener(new RequestListener<Drawable>() { // Optional: Listener for success/failure
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                // Handle error (e.g., log it)
                                android.util.Log.e("Glide", "Image load failed: " + (e != null ? e.getMessage() : "Unknown error") + " for URL: " + movie.getImageUrl());
                                return false; // Return false to allow error drawable to be set
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                // Image loaded successfully
                                return false;
                            }
                        })
                        .into(posterImageView);
            } else {
                // Set placeholder if imageUrl is null or empty
                posterImageView.setImageResource(R.drawable.ic_movie_placeholder);
            }

            // --- REMOVE logic based on genre and rating ---
            // Remove the switch statement based on movie.getGenre()
            // Remove the background setting based on movie.getRating()
            posterImageView.setBackground(null); // Remove any previously set background color
        }
    }
}
    