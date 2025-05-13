package com.example.defty_movie_app.adapter;

import android.content.Context;
import android.graphics.Color; // Import Color
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R; // Thay bằng package R của bạn
import com.example.defty_movie_app.data.model.response.MovieDetailResponse; // Model Episode

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; // Import Locale

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    private List<MovieDetailResponse.Episode> episodeList;
    private Context context;
    private OnEpisodeClickListener listener;
    private String currentPlayingEpisodeSlug; // Slug của tập đang phát

    public interface OnEpisodeClickListener {
        void onEpisodeClick(MovieDetailResponse.Episode episode);
    }

    public EpisodeAdapter(Context context, OnEpisodeClickListener listener) {
        this.context = context;
        this.episodeList = new ArrayList<>();
        this.listener = listener;
    }

    public void updateEpisodes(List<MovieDetailResponse.Episode> newEpisodes) {
        this.episodeList.clear();
        if (newEpisodes != null) {
            this.episodeList.addAll(newEpisodes);
        }
        notifyDataSetChanged();
    }

    public void setCurrentPlayingEpisode(String episodeSlug) {
        this.currentPlayingEpisodeSlug = episodeSlug;
        notifyDataSetChanged(); // Cập nhật lại list để highlight
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_episode, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        MovieDetailResponse.Episode episode = episodeList.get(position);
        holder.bind(episode, listener, currentPlayingEpisodeSlug);
    }

    @Override
    public int getItemCount() {
        return episodeList.size();
    }

    static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        ImageView imageEpisodeThumbnail;
        TextView textEpisodeNumber;
        TextView textEpisodeName;
        View itemViewRoot; // Để thay đổi background hoặc style cho cả item

        public EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            itemViewRoot = itemView;
            imageEpisodeThumbnail = itemView.findViewById(R.id.image_episode_thumbnail);
            textEpisodeNumber = itemView.findViewById(R.id.text_episode_number);
            textEpisodeName = itemView.findViewById(R.id.text_episode_name);
        }

        public void bind(final MovieDetailResponse.Episode episode,
                         final OnEpisodeClickListener listener,
                         final String currentPlayingEpisodeSlug) {

            textEpisodeNumber.setText(String.format(Locale.getDefault(), "Tập %d", episode.getNumber()));
            textEpisodeName.setText(episode.getDescription()); // Giả sử description là tên tập

            Glide.with(itemView.getContext())
                    .load(episode.getThumbnail()) // Model Episode cần có getThumbnail()
                    .placeholder(R.drawable.default_cover_image)
                    .error(R.drawable.default_cover_image)
                    .into(imageEpisodeThumbnail);

            itemView.setOnClickListener(v -> listener.onEpisodeClick(episode));

            // Highlight tập đang phát
            if (episode.getSlug() != null && episode.getSlug().equals(currentPlayingEpisodeSlug)) {
                // Ví dụ: thay đổi màu chữ của số tập
                textEpisodeNumber.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green)); // Định nghĩa màu này trong colors.xml
                // Hoặc thay đổi background của item
                // itemViewRoot.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.your_highlight_background_color));
            } else {
                // Đặt lại màu mặc định
                textEpisodeNumber.setTextColor(Color.WHITE); // Hoặc màu mặc định của bạn
                // itemViewRoot.setBackgroundResource(R.drawable.default_item_background); // Nếu bạn dùng background drawable
            }
        }
    }
}