package com.example.defty_movie_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.defty_movie_app.R;
import com.example.defty_movie_app.data.model.adapter.CastCrew;

import java.util.List;

public class CastCrewAdapter extends RecyclerView.Adapter<CastCrewAdapter.CastCrewViewHolder> {

    private final List<CastCrew> castList;

    public CastCrewAdapter(List<CastCrew> castList) {
        this.castList = castList;
    }

    @NonNull
    @Override
    public CastCrewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cast_crew, parent, false);
        return new CastCrewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CastCrewViewHolder holder, int position) {
        CastCrew person = castList.get(position);
        holder.bind(person);
    }

    @Override
    public int getItemCount() {
        return castList.size();
    }

    public static class CastCrewViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imagePerson;
        private final TextView textName;
        private final TextView textRole;

        public CastCrewViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePerson = itemView.findViewById(R.id.imagePerson);
            textName = itemView.findViewById(R.id.textName);
            textRole = itemView.findViewById(R.id.textRole);
        }

        public void bind(CastCrew person) {
            textName.setText(person.getName());
            textRole.setText(person.getRole());
            Glide.with(itemView.getContext())
                    .load(person.getImageUrl())
                    .placeholder(R.drawable.circle_background)
                    .into(imagePerson);
        }
    }
}
