package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Import ImageButton
import android.widget.ImageView;
import android.widget.TextView; // Import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.dto.Banner; // Sử dụng Banner DTO

import java.util.ArrayList;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Banner> bannerList;
    private final Context context;

    public BannerAdapter(Context context) {
        this.context = context;
        this.bannerList = new ArrayList<>();
    }

    // Cập nhật để nhận List<Banner>
    public void setBanners(List<Banner> banners) {
        this.bannerList = banners == null ? new ArrayList<>() : banners;
        notifyDataSetChanged(); // Cân nhắc dùng DiffUtil
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_banner.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        Banner banner = bannerList.get(position);
        holder.bind(banner);
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    // ViewHolder cập nhật
    class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImageView;
        TextView bannerTitleTextView;
        ImageButton bannerPlayButton;    // Nút Play

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các view mới từ item_banner.xml
            bannerImageView = itemView.findViewById(R.id.bannerImageView);
            bannerTitleTextView = itemView.findViewById(R.id.bannerTitleTextView);
            bannerPlayButton = itemView.findViewById(R.id.bannerPlayButton);

            // Xử lý sự kiện click vào nút Play
            bannerPlayButton.setOnClickListener(v -> {
                // Sử dụng getAdapterPosition() (phiên bản cũ)
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Banner clickedBanner = bannerList.get(position);
                    // Xử lý khi nhấn nút play (ví dụ: mở video)
                    Toast.makeText(context, "Play clicked: " + clickedBanner.getTitle(), Toast.LENGTH_SHORT).show();
                    // openVideoPlayer(clickedBanner.getContentId()); // Ví dụ
                }
            });

            // Xử lý sự kiện click vào toàn bộ item (nếu cần)
            itemView.setOnClickListener(v -> {
                // Sử dụng getAdapterPosition() (phiên bản cũ)
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Banner clickedBanner = bannerList.get(position);
                    // Xử lý khi click vào item (ví dụ: mở trang chi tiết)
                    Toast.makeText(context, "Item clicked: " + clickedBanner.getTitle(), Toast.LENGTH_SHORT).show();
                    // openDetailPage(clickedBanner.getContentId()); // Ví dụ
                }
            });
        }

        /**
         * Binds the banner data to the views in the ViewHolder.
         * @param banner The banner data object.
         */
        void bind(Banner banner) {
            // Hiển thị tiêu đề
            if (banner.getTitle() != null) {
                bannerTitleTextView.setText(banner.getTitle());
            } else {
                bannerTitleTextView.setText(""); // Hoặc ẩn đi nếu không có title
            }

            // Hiển thị phụ đề (ví dụ: dùng contentName hoặc description từ subBannerResponse)
            String subtitle = "";
            if (banner.getContentName() != null && !banner.getContentName().isEmpty()) {
                subtitle = banner.getContentName();
            } else if (banner.getSubBannerResponse() != null && banner.getSubBannerResponse().getDescription() != null) {
                subtitle = banner.getSubBannerResponse().getDescription();
            }


            // Tải ảnh nền banner
            if (banner.getThumbnail() != null && !banner.getThumbnail().isEmpty()) {
                Glide.with(context)
                        .load(banner.getThumbnail())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .centerCrop()
                        .into(bannerImageView);
            } else {
                bannerImageView.setImageResource(R.drawable.placeholder_image);
            }
        }
    }
}
