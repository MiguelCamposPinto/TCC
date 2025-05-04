package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Spaces;

import java.util.List;

public class SpacesAdapter extends RecyclerView.Adapter<SpacesAdapter.SpaceViewHolder> {
    private List<Spaces> spaceList;

    public SpacesAdapter(List<Spaces> spaceList) {
        this.spaceList = spaceList;
    }

    @Override
    public SpaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_space, parent, false);
        return new SpaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SpaceViewHolder holder, int position) {
        Spaces space = spaceList.get(position);
        holder.tvName.setText(space.getName());
        holder.tvType.setText(space.getType());
    }

    @Override
    public int getItemCount() {
        return spaceList.size();
    }

    static class SpaceViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;

        SpaceViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSpaceName);
            tvType = itemView.findViewById(R.id.tvSpaceType);
        }
    }
}
