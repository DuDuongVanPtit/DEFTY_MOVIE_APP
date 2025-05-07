package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Ví dụ cho click listener

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
// import com.example.defty_movie_app.data.dto.Movie; // Không cần nữa
import com.example.defty_movie_app.data.model.request.MovieNameResponse; // IMPORT THAY THẾ

import java.util.ArrayList;
import java.util.List;

public class HotSearchMovieAdapter extends RecyclerView.Adapter<HotSearchMovieAdapter.ViewHolder> {
    // THAY ĐỔI KIỂU DỮ LIỆU TẠI ĐÂY
    private List<MovieNameResponse> movies;
    private Context context;

    public HotSearchMovieAdapter(Context context) {
        this.context = context;
        this.movies = new ArrayList<>(); // Khởi tạo là List rỗng của MovieNameResponse
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hot_search_movie, parent, false); // Đảm bảo tên layout này đúng
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // THAY ĐỔI KIỂU DỮ LIỆU TẠI ĐÂY
        MovieNameResponse movieData = movies.get(position);

        if (movieData == null) {
            // Trường hợp movieData là null (dù đã kiểm tra trong ViewModel, cẩn thận không thừa)
            Log.e("HotSearchAdapter", "MovieNameResponse object is null at position " + position);
            holder.movieTitle.setText("Lỗi dữ liệu");
            holder.movieImage.setImageResource(R.drawable.error_image); // Hoặc placeholder
            return;
        }

        // Sử dụng getName() từ MovieNameResponse
        holder.movieTitle.setText(movieData.getName());

        // Log để kiểm tra
        Log.d("HotSearchAdapter", "Binding item: " + movieData.getName() + ", Thumbnail: " + movieData.getThumbnail());


        // Sử dụng getThumbnail() từ MovieNameResponse
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

        holder.itemView.setOnClickListener(v -> {
            // Xử lý khi click vào một item, ví dụ:
            Toast.makeText(context, "Clicked: " + movieData.getName(), Toast.LENGTH_SHORT).show();
            // Mở chi tiết phim với movieData.getSlug() chẳng hạn
        });

        holder.playButton.setOnClickListener(v -> {
            // Xử lý click vào nút play
            Toast.makeText(context, "Play: " + movieData.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return movies != null ? movies.size() : 0;
    }

    // THAY ĐỔI KIỂU DỮ LIỆU CỦA PARAMETER TẠI ĐÂY
    public void setMovies(List<MovieNameResponse> newMovies) {
        if (newMovies == null) {
            this.movies.clear();
            Log.d("HotSearchAdapter", "setMovies called with null list, clearing adapter.");
        } else {
            this.movies = newMovies; // Gán trực tiếp hoặc clear().addAll()
            Log.d("HotSearchAdapter", "setMovies called with list size: " + newMovies.size());
        }
        notifyDataSetChanged(); // Thông báo cho RecyclerView cập nhật
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