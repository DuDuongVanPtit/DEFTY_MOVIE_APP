package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // THÊM IMPORT NÀY
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
// import com.bumptech.glide.load.resource.bitmap.RoundedCorners; // KHÔNG CẦN NỮA
// import com.bumptech.glide.request.RequestOptions; // KHÔNG CẦN NỮA
import com.example.defty_movie_app.R;
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
        void onWatchNowClicked(DownloadedMovie movie);
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
        Button watchNowButton;
        TextView membershipLabelTextView; // THÊM TEXTVIEW CHO NHÃN

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.downloadedMovieThumbnailImageView);
            titleTextView = itemView.findViewById(R.id.downloadedMovieTitleTextView);
            infoTextView = itemView.findViewById(R.id.downloadedMovieInfoTextView);
            deleteButton = itemView.findViewById(R.id.deleteDownloadedMovieButton);
            watchNowButton = itemView.findViewById(R.id.watchNowDownloadedMovieButton);
            membershipLabelTextView = itemView.findViewById(R.id.downloadedMovieMembershipLabel); // LẤY THAM CHIẾU
        }

        void bind(final DownloadedMovie movie, Context context, final OnDownloadedMovieClickListener listener, final int position) {
            titleTextView.setText(movie.getTitle());

            String statusText = "";
            int statusColor = Color.parseColor("#AAAAAA");
            boolean isCompleted = false;
            String currentStatus = movie.getDownloadStatus();

            if (currentStatus == null) {
                currentStatus = DownloadedMovie.STATUS_PENDING;
            }

            switch (currentStatus) {
                case DownloadedMovie.STATUS_COMPLETED:
                    statusText = "Đã tải xuống";
                    statusColor = Color.parseColor("#4CAF50");
                    isCompleted = true;
                    break;
                case DownloadedMovie.STATUS_DOWNLOADING:
                    statusText = "Đang tải...";
                    statusColor = Color.parseColor("#2196F3");
                    break;
                case DownloadedMovie.STATUS_PENDING:
                    statusText = "Đang chờ tải...";
                    statusColor = Color.parseColor("#FFC107");
                    break;
                case DownloadedMovie.STATUS_FAILED:
                    statusText = "Tải lỗi";
                    statusColor = Color.parseColor("#F44336");
                    break;
                case DownloadedMovie.STATUS_PAUSED:
                    statusText = "Đã tạm dừng";
                    statusColor = Color.parseColor("#FF9800");
                    break;
                case DownloadedMovie.STATUS_CANCELLED:
                    statusText = "Đã hủy tải";
                    statusColor = Color.parseColor("#9E9E9E");
                    break;
                default:
                    if (DownloadedMovie.STATUS_PENDING.equals(currentStatus) && statusText.isEmpty()) {
                        statusText = "Đang chờ tải...";
                        statusColor = Color.parseColor("#FFC107");
                    } else if (statusText.isEmpty()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        String dateString = sdf.format(new Date(movie.getDownloadDate()));
                        statusText = "Đã thêm: " + dateString;
                    }
                    break;
            }
            infoTextView.setText(statusText);
            infoTextView.setTextColor(statusColor);

            Glide.with(context)
                    .load(movie.getThumbnail())
                    // Dòng .apply() với RoundedCorners đã được xóa để CardView xử lý bo góc
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(thumbnailImageView);

            if (isCompleted) {
                watchNowButton.setVisibility(View.VISIBLE);
                watchNowButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onWatchNowClicked(movie);
                    }
                });
            } else {
                watchNowButton.setVisibility(View.GONE);
            }

            // ---- XỬ LÝ HIỂN THỊ NHÃN LOẠI THÀNH VIÊN ----
            Integer membershipType = movie.getMembershipType(); // Giả sử bạn đã có getter này trong DownloadedMovie
            if (membershipType != null) {
                String membershipText = "";
                // Bạn cần đảm bảo các drawable và color này tồn tại trong project
                // Ví dụ: R.drawable.label_type_premium, R.color.premium_text_color
                // Giả sử: 1 là Premium, 3 là Normal (tương tự MovieAdapter trước)
                // Bạn hãy điều chỉnh các giá trị và resource cho phù hợp
                if (membershipType == 1) { // PREMIUM
                    membershipText = "Premium"; // Hoặc tên bạn muốn hiển thị
                    // Ví dụ sử dụng màu và background (bạn cần tạo các resource này)
                    membershipLabelTextView.setBackgroundResource(R.drawable.label_type_premium); // Ví dụ: res/drawable/label_type_premium.xml
                    membershipLabelTextView.setTextColor(ContextCompat.getColor(context, R.color.premiumText)); // Ví dụ: res/values/colors.xml -> premium_text_color
                } else if (membershipType == 3) { // NORMAL / FREE
                    membershipText = "Normal"; // Hoặc "Free"
                    membershipLabelTextView.setBackgroundResource(R.drawable.label_type_normal); // Ví dụ: res/drawable/label_type_normal.xml
                    membershipLabelTextView.setTextColor(ContextCompat.getColor(context,R.color.normalText)); // Ví dụ: res/values/colors.xml -> normal_text_color
                }
                // Thêm các else if cho các loại membership khác nếu có

                if (!membershipText.isEmpty()) {
                    membershipLabelTextView.setText(membershipText);
                    membershipLabelTextView.setVisibility(View.VISIBLE);
                } else {
                    membershipLabelTextView.setVisibility(View.GONE); // Ẩn nếu không có text (ví dụ: loại membership không xác định)
                }
            } else {
                membershipLabelTextView.setVisibility(View.GONE); // Ẩn nếu membershipType là null
            }
            // ---- KẾT THÚC XỬ LÝ NHÃN ----


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