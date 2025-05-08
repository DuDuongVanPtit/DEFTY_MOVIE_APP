package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.defty_movie_app.R;
// Đảm bảo bạn đang import đúng package cho DownloadedMovie
// Nếu bạn đặt nó trong data.model thì sửa thành:
// import com.example.defty_movie_app.data.model.DownloadedMovie;
import com.example.defty_movie_app.data.dto.DownloadedMovie;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DownloadedMovieAdapter extends RecyclerView.Adapter<DownloadedMovieAdapter.ViewHolder> {

    private List<DownloadedMovie> downloadedMovies;
    private Context context;
    private OnDownloadedMovieClickListener listener;

    public interface OnDownloadedMovieClickListener {
        void onMovieClicked(DownloadedMovie movie);
        void onDeleteClicked(DownloadedMovie movie, int position);
    }

    public DownloadedMovieAdapter(Context context, OnDownloadedMovieClickListener listener) {
        this.context = context;
        this.downloadedMovies = new ArrayList<>();
        this.listener = listener;
    }

    public void setDownloadedMovies(List<DownloadedMovie> movies) {
        this.downloadedMovies.clear();
        if (movies != null) {
            this.downloadedMovies.addAll(movies);
        }
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < downloadedMovies.size()) {
            downloadedMovies.remove(position);
            notifyItemRemoved(position);
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_downloaded_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadedMovie movie = downloadedMovies.get(position);
        holder.bind(movie, context, listener, position);
    }

    @Override
    public int getItemCount() {
        return downloadedMovies.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;
        TextView titleTextView;
        TextView infoTextView;
        ImageView deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.downloadedMovieThumbnailImageView);
            titleTextView = itemView.findViewById(R.id.downloadedMovieTitleTextView);
            infoTextView = itemView.findViewById(R.id.downloadedMovieInfoTextView);
            deleteButton = itemView.findViewById(R.id.deleteDownloadedMovieButton);
        }

        void bind(final DownloadedMovie movie, Context context, final OnDownloadedMovieClickListener listener, final int position) {
            titleTextView.setText(movie.getTitle());

            String statusText = "";
            int statusColor = Color.parseColor("#AAAAAA"); // Màu mặc định

            String currentStatus = movie.getDownloadStatus(); // Lấy trạng thái

            // THÊM KIỂM TRA NULL Ở ĐÂY
            if (currentStatus == null) {
                currentStatus = DownloadedMovie.STATUS_PENDING; // Hoặc một trạng thái mặc định khác nếu muốn
                // Hoặc bạn có thể hiển thị một thông báo lỗi/không xác định riêng
                // statusText = "Trạng thái không xác định";
            }

            switch (currentStatus) {
                case DownloadedMovie.STATUS_COMPLETED:
                    statusText = "Đã tải xong";
                    statusColor = Color.parseColor("#4CAF50"); // Xanh lá
                    break;
                case DownloadedMovie.STATUS_DOWNLOADING:
                    statusText = "Đang tải...";
                    statusColor = Color.parseColor("#2196F3"); // Xanh dương
                    break;
                case DownloadedMovie.STATUS_PENDING:
                    statusText = "Đang chờ tải...";
                    statusColor = Color.parseColor("#FFC107"); // Vàng
                    break;
                case DownloadedMovie.STATUS_FAILED:
                    statusText = "Tải lỗi";
                    statusColor = Color.parseColor("#F44336"); // Đỏ
                    break;
                case DownloadedMovie.STATUS_PAUSED:
                    statusText = "Đã tạm dừng";
                    statusColor = Color.parseColor("#FF9800"); // Cam
                    break;
                default: // Bao gồm cả trường hợp currentStatus là PENDING sau khi kiểm tra null
                    if (DownloadedMovie.STATUS_PENDING.equals(currentStatus) && statusText.isEmpty()) {
                        statusText = "Đang chờ tải..."; // Đảm bảo PENDING có text
                        statusColor = Color.parseColor("#FFC107");
                    } else if (statusText.isEmpty()) { // Nếu vẫn rỗng (trường hợp không mong muốn)
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String dateString = sdf.format(new Date(movie.getDownloadDate()));
                        statusText = "Đã thêm: " + dateString;
                    }
                    break;
            }
            infoTextView.setText(statusText);
            infoTextView.setTextColor(statusColor);


            Glide.with(context)
                    .load(movie.getThumbnail())
                    .apply(new RequestOptions().transform(new RoundedCorners(8)))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(thumbnailImageView);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovieClicked(movie);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(movie, position);
                }
            });
        }
    }
}
