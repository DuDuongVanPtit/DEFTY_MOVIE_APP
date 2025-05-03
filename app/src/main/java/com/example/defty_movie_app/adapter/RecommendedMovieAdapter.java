package com.example.defty_movie_app.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.response.RecommendedMovieResponse;

import java.util.List;

public class RecommendedMovieAdapter extends RecyclerView.Adapter<RecommendedMovieAdapter.MovieViewHolder> {

    private final List<RecommendedMovieResponse.RecommendedMovie> movieList;

    public void updateMovies(List<RecommendedMovieResponse.RecommendedMovie> newMovies) {
        movieList.clear();
        movieList.addAll(newMovies);
        notifyDataSetChanged();
    }

    public RecommendedMovieAdapter(List<RecommendedMovieResponse.RecommendedMovie> movieList) {
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        RecommendedMovieResponse.RecommendedMovie movie = movieList.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageMovie;
        private final TextView textMovieName;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imageMovie = itemView.findViewById(R.id.imageMovie);
            textMovieName = itemView.findViewById(R.id.textMovieName);
        }

        public void bind(RecommendedMovieResponse.RecommendedMovie movie) {
            textMovieName.setText(movie.title);
            Glide.with(itemView.getContext())
                    .load(movie.coverImage != null ? movie.coverImage : "")
                    .thumbnail(0.25f)
                    .override(120, 80) // Kích thước mới của ImageView
                    .placeholder(R.drawable.default_cover_image)
                    .error(R.drawable.default_cover_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageMovie);
//            Glide.with(itemView.getContext())
//                    .load(movie.coverImage)
//                    .placeholder(R.drawable.default_cover_image)
//                    .into(imageMovie);

//            itemView.setOnClickListener(v -> {
//                // Xử lý click, ví dụ: mở Activity chi tiết phim
//                Intent intent = new Intent(itemView.getContext(), MovieDetailActivity.class);
//                intent.putExtra("movie_slug", movie.slug);
//                itemView.getContext().startActivity(intent);
//            });
        }


    }
}
