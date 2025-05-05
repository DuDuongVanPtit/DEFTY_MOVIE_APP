package com.example.defty_movie_app.adapter;

import android.content.Context;
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

public class HotSearchMovieAdapter extends RecyclerView.Adapter<HotSearchMovieAdapter.ViewHolder> {
    private List<Movie> movies;
    private Context context;

    public HotSearchMovieAdapter(Context context) {
        this.context = context;
        this.movies = new ArrayList<>();
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
        Movie movie = movies.get(position);
        holder.movieTitle.setText(movie.getTitle()); // Giả sử Movie có phương thức getTitle()

        // Tải ảnh bằng Glide
        if (movie.getImageUrl() != null && !movie.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(movie.getImageUrl())
                    .placeholder(R.drawable.placeholder_image) // Hình placeholder nếu ảnh chưa tải
                    .error(R.drawable.error_image) // Hình thay thế nếu tải lỗi
                    .into(holder.movieImage);
        } else {
            holder.movieImage.setImageResource(R.drawable.placeholder_image); // Hình mặc định nếu không có URL
        }

        // Xử lý click vào nút play (nếu cần)
        holder.playButton.setOnClickListener(v -> {
            // Thêm logic để phát phim tại đây
        });
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView movieImage;
        TextView movieTitle;
        ImageView playButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            movieImage = itemView.findViewById(R.id.movieImage);
            movieTitle = itemView.findViewById(R.id.movieTitle);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }
}