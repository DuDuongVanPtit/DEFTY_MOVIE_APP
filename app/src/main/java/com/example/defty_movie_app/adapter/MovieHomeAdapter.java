package com.example.defty_movie_app.adapter;

import android.content.Context;
// Removed Intent as we won't start activity directly
// import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
// Removed Toast as it should be handled by the listener's receiver
// import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.Movie;
// Removed WatchActivity import
// import com.example.defty_movie_app.view.WatchActivity;

import java.util.ArrayList;
import java.util.List;

public class MovieHomeAdapter extends RecyclerView.Adapter<MovieHomeAdapter.MovieViewHolder> {

    private List<Movie> movies;
    private final Context context;
    // Define the listener interface within the adapter
    public interface OnMovieClickListener {
        void onMovieClick(Movie movie); // Method to be called when a movie item is clicked
    }
    // Declare the listener field
    private final OnMovieClickListener onMovieClickListener;

    // Constructor now accepts Context and the listener
    public MovieHomeAdapter(@NonNull Context context, @NonNull OnMovieClickListener listener) {
        this.context = context;
        this.movies = new ArrayList<>(); // Initialize with an empty list
        this.onMovieClickListener = listener; // Initialize the listener
    }

    /**
     * Updates the list of movies in the adapter.
     * @param movies The new list of movies. Can be null, which clears the list.
     */
    public void setMovies(@Nullable List<Movie> movies) {
        // Create a new list to avoid external modifications and handle null input
        this.movies = movies == null ? new ArrayList<>() : new ArrayList<>(movies);
        // Notify adapter about data change. Consider DiffUtil for larger lists.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each movie item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.movie_item, parent, false); // Use your item layout file name
        // Pass the listener to the ViewHolder
        return new MovieViewHolder(view, onMovieClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        // Check if the list is valid and position is within bounds
        if (movies != null && position < movies.size()) {
            // Get the movie at the current position
            Movie movie = movies.get(position);
            // Bind the movie data to the ViewHolder
            holder.bind(context, movie); // Pass context and movie to bind
            // Click listener is set in ViewHolder constructor, no need to set here
        }
    }

    @Override
    public int getItemCount() {
        // Return the total number of movies. Handle null list case.
        return movies != null ? movies.size() : 0;
    }

    // ViewHolder class to hold the views for each movie item
    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView posterImageView;
        private final TextView titleTextView;
        // Keep infoTextView if needed for future use or hide it as shown below
        private final TextView infoTextView;
        private final TextView premiumTextView;
        // Declare the listener field in the ViewHolder
        private final OnMovieClickListener listener;

        // Constructor receives itemView and the listener
        public MovieViewHolder(@NonNull View itemView, @NonNull OnMovieClickListener listener) {
            super(itemView);
            // Initialize views from the layout
            posterImageView = itemView.findViewById(R.id.posterImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            infoTextView = itemView.findViewById(R.id.infoTextView); // Ensure this ID exists
            premiumTextView = itemView.findViewById(R.id.qualityTagTextView); // Ensure this ID exists

            // Initialize the listener
            this.listener = listener;

            // Set click listener for the entire item view
            itemView.setOnClickListener(v -> {
                // Use getBindingAdapterPosition() as it's safer and recommended
                int position = getBindingAdapterPosition();
                // Check if position is valid and listener is set
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    // Safely access the adapter
                    RecyclerView.Adapter<?> adapter = getBindingAdapter();
                    if (adapter instanceof MovieHomeAdapter) {
                        MovieHomeAdapter movieHomeAdapter = (MovieHomeAdapter) adapter;
                        // Access the clicked movie object from the adapter's list
                        Movie clickedMovie = movieHomeAdapter.movies.get(position);

                        // --- Call the listener method instead of starting an Intent ---
                        listener.onMovieClick(clickedMovie);
                        // --- End Call Listener ---

                        // Removed Toast message from adapter
                        // Removed direct Intent logic from adapter
                    }
                }
            });
        }

        /**
         * Binds the movie data to the views in the ViewHolder.
         * @param context The context.
         * @param movie The movie data object.
         */
        public void bind(Context context, Movie movie) {
            // Set movie title, handle potential null title
            titleTextView.setText(movie.getTitle() != null ? movie.getTitle() : "N/A");

            // Set info text (e.g., use slug or leave empty/hide)
            // infoTextView.setText(movie.getSlug()); // Example: Show slug
            infoTextView.setText(""); // Set to empty if not needed
            // Or hide the TextView completely if it's not used in your layout:
            // infoTextView.setVisibility(View.GONE);

            premiumTextView.setText(movie.isPremium() == 1 ? "Premium" : (movie.isPremium() == 3 ? "Normal" : ""));

            // Load poster image using Glide
            // Define a placeholder drawable (ensure you have R.drawable.ic_movie_placeholder)
            Drawable placeholder = ContextCompat.getDrawable(context, R.drawable.ic_movie_placeholder);

            if (movie.getImageUrl() != null && !movie.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(movie.getImageUrl())
                        .placeholder(placeholder) // Show placeholder while loading
                        .error(placeholder) // Show placeholder if loading fails
                        // Optional: Add a listener for debugging/handling specific outcomes
                        // .listener(new RequestListener<Drawable>() { ... })
                        .into(posterImageView);
            } else {
                // Set placeholder if imageUrl is null or empty
                posterImageView.setImageDrawable(placeholder);
            }

            // --- Removed old logic ---
            // Remove the switch statement based on movie.getGenre()
            // Remove the background setting based on movie.getRating()
            // Clear any residual background
            posterImageView.setBackground(null);
            itemView.setBackground(null);
        }
    }
}