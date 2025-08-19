package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Spaces;

import java.util.List;

public class SpacesAdapter extends RecyclerView.Adapter<SpacesAdapter.ViewHolder> {

    public interface OnSpaceClickListener {
        void onClick(Spaces space);
    }

    private List<Spaces> spaceList;
    private OnSpaceClickListener listener;

    public SpacesAdapter(List<Spaces> spaceList, OnSpaceClickListener listener) {
        this.spaceList = spaceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Spaces space = spaceList.get(position);
        holder.name.setText(space.getName());
        holder.type.setText(space.getType());
        holder.itemView.setOnClickListener(v -> listener.onClick(space));
    }

    @Override
    public int getItemCount() {
        return spaceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, type;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            type = itemView.findViewById(android.R.id.text2);

        }
    }
}

