package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // Import ImageButton
import android.widget.ImageView;
import android.widget.TextView; // Import TextView
// Removed Toast import as Toast can be shown in Fragment/Activity
// import android.widget.Toast;

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
    // Define the listener interface within the adapter
    public interface OnBannerClickListener {
        void onBannerClick(Banner banner); // Pass the Banner object
    }
    private final OnBannerClickListener onBannerClickListener; // Declare the listener


    // Constructor now accepts Context and the listener
    public BannerAdapter(Context context, OnBannerClickListener listener) {
        this.context = context;
        this.bannerList = new ArrayList<>();
        this.onBannerClickListener = listener; // Initialize the listener
    }

    // Cập nhật để nhận List<Banner>
    public void setBanners(List<Banner> banners) {
        this.bannerList = banners == null ? new ArrayList<>() : banners;
        // Cân nhắc dùng DiffUtil cho hiệu suất tốt hơn
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_banner.xml
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, parent, false);
        // Pass the listener to the ViewHolder
        return new BannerViewHolder(view, onBannerClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        if (bannerList != null && position < bannerList.size()) {
            Banner banner = bannerList.get(position);
            // Pass context, banner, and listener to the bind method
            holder.bind(context, banner, onBannerClickListener);
        }
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
        // Declare listener in ViewHolder
        private final OnBannerClickListener listener;


        // Constructor accepts the listener
        public BannerViewHolder(@NonNull View itemView, OnBannerClickListener listener) {
            super(itemView);
            // Ánh xạ các view mới từ item_banner.xml
            bannerImageView = itemView.findViewById(R.id.bannerImageView);
            bannerTitleTextView = itemView.findViewById(R.id.bannerTitleTextView);
            bannerPlayButton = itemView.findViewById(R.id.bannerPlayButton);
            this.listener = listener; // Initialize listener


            // Xử lý sự kiện click vào toàn bộ item (nếu cần) - ưu tiên click vào item hơn nút Play
            itemView.setOnClickListener(v -> {
                // Sử dụng getBindingAdapterPosition() an toàn hơn
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    // Retrieve the banner object from the adapter's list
                    // Note: Accessing adapter's list directly from ViewHolder is generally discouraged.
                    // A better approach is to pass the banner object to the bind method
                    // and then use that object here. Let's adjust bind method signature.

                    // --- CORRECTED: Get banner from bind method ---
                    // The banner object should be passed to the bind method and stored if needed,
                    // or accessed from the adapter using getBindingAdapterPosition().
                    // Assuming the banner object is available via the bind method or can be
                    // accessed safely using the position:
                    RecyclerView.Adapter<?> adapter = getBindingAdapter();
                    if (adapter instanceof BannerAdapter) {
                        Banner clickedBanner = ((BannerAdapter) adapter).bannerList.get(position);
                        listener.onBannerClick(clickedBanner); // Call the listener method
                    }
                    // --- END CORRECTED ---

                    // Removed Toast message from adapter
                    // Toast.makeText(context, "Item clicked: " + clickedBanner.getTitle(), Toast.LENGTH_SHORT).show();
                    // openDetailPage(clickedBanner.getContentId()); // Ví dụ
                }
            });

            // Xử lý sự kiện click vào nút Play (có thể xử lý khác hoặc gọi listener khác)
            // Hiện tại, logic này bị ghi đè bởi click listener của itemView.
            // Nếu bạn muốn nút Play có hành vi riêng, bạn cần xử lý nó ở đây
            // và có thể cần một listener riêng cho nút Play.
            // Để đơn giản, chúng ta sẽ chỉ sử dụng click listener của itemView.
            bannerPlayButton.setOnClickListener(v -> {
                // Logic cho nút Play nếu cần khác với click item
                // Ví dụ: Toast.makeText(context, "Play button clicked!", Toast.LENGTH_SHORT).show();
                // Hoặc gọi listener.onBannerPlayClick(clickedBanner); nếu có listener riêng
            });
        }

        /**
         * Binds the banner data to the views in the ViewHolder.
         * @param context The context.
         * @param banner The banner data object.
         * @param listener The click listener.
         */
        void bind(Context context, final Banner banner, final OnBannerClickListener listener) {
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
            // Bạn có thể hiển thị subtitle này trong một TextView khác nếu có trong layout item_banner.xml


            // Tải ảnh nền banner
            if (banner.getThumbnail() != null && !banner.getThumbnail().isEmpty()) {
                Glide.with(context)
                        .load(banner.getThumbnail())
                        .placeholder(R.drawable.placeholder_image) // Use your placeholder drawable
                        .error(R.drawable.error_image) // Use your error drawable
                        .centerCrop()
                        .into(bannerImageView);
            } else {
                bannerImageView.setImageResource(R.drawable.placeholder_image);
            }

            // Click listener được set trong constructor, không cần set lại ở đây.
            // Dữ liệu banner được sử dụng trực tiếp trong click listener từ adapter's list.
        }
    }
}
