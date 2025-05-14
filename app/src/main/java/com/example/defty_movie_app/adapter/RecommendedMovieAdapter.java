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

import java.util.ArrayList; // Thêm import này nếu bạn khởi tạo movieList là ArrayList
import java.util.List;

public class RecommendedMovieAdapter extends RecyclerView.Adapter<RecommendedMovieAdapter.MovieViewHolder> {

    private List<RecommendedMovieResponse.RecommendedMovie> movieList;
    private final OnMovieClickListener onMovieClickListener; // Biến listener

    // 1. Định nghĩa Interface OnMovieClickListener
    public interface OnMovieClickListener {
        void onMovieClick(RecommendedMovieResponse.RecommendedMovie movie);
    }

    public void updateMovies(List<RecommendedMovieResponse.RecommendedMovie> newMovies) {
        // Nên kiểm tra newMovies có null không trước khi clear và addAll
        if (newMovies != null) {
            if (this.movieList == null) {
                this.movieList = new ArrayList<>();
            }
            this.movieList.clear();
            this.movieList.addAll(newMovies);
            notifyDataSetChanged();
        }
    }

    // 2. Cập nhật Constructor để nhận listener
    public RecommendedMovieAdapter(List<RecommendedMovieResponse.RecommendedMovie> movieList, OnMovieClickListener listener) {
        this.movieList = (movieList == null) ? new ArrayList<>() : movieList; // Khởi tạo an toàn
        this.onMovieClickListener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.movie_item, parent, false);
        // 3. Truyền listener cho ViewHolder
        return new MovieViewHolder(view, onMovieClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        // Kiểm tra movieList có null và position có hợp lệ không
        if (movieList != null && position >= 0 && position < movieList.size()) {
            RecommendedMovieResponse.RecommendedMovie movie = movieList.get(position);
            if (movie != null) {
                holder.bind(movie);
            }
        }
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    // Cập nhật MovieViewHolder
    public class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageMovie;
        private final TextView textMovieName;
        // Không cần khai báo lại listener ở đây nếu bạn truyền trực tiếp vào setOnClickListener

        public MovieViewHolder(@NonNull View itemView, final OnMovieClickListener listener) {
            super(itemView);
            imageMovie = itemView.findViewById(R.id.posterImageView); // Đảm bảo ID này khớp với R.layout.movie_item
            textMovieName = itemView.findViewById(R.id.titleTextView); // Đảm bảo ID này khớp với R.layout.movie_item

            // 4. Set OnClickListener cho itemView
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && movieList != null && position < movieList.size()) {
                    RecommendedMovieResponse.RecommendedMovie clickedMovie = movieList.get(position);
                    if (clickedMovie != null) {
                        listener.onMovieClick(clickedMovie);
                    }
                }
            });
        }

        public void bind(RecommendedMovieResponse.RecommendedMovie movie) {
            textMovieName.setText(movie.title);
            Glide.with(itemView.getContext())
                    .load(movie.thubnail != null ? movie.thubnail : "") // Sử dụng movie.thubnail thay vì movie.coverImage
                    // .thumbnail(0.25f) // Cân nhắc bỏ nếu gây mờ
                    // .override(120, 80) // Cân nhắc kích thước này
                    .placeholder(R.drawable.default_cover_image)
                    .error(R.drawable.default_cover_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageMovie);
        }
    }
}