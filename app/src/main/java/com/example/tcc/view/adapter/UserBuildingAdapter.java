package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tcc.R;
import com.example.tcc.model.Building;
import java.util.List;

public class UserBuildingAdapter extends RecyclerView.Adapter<UserBuildingAdapter.ViewHolder> {

    public interface OnBuildingSelectedListener {
        void onSelect(Building building);
    }

    private final List<Building> buildingList;
    private final OnBuildingSelectedListener listener;

    public UserBuildingAdapter(List<Building> list, OnBuildingSelectedListener listener) {
        this.buildingList = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_building, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Building building = buildingList.get(position);
        holder.name.setText(building.getName());
        holder.address.setText(building.getAddress());
        holder.itemView.setOnClickListener(v -> listener.onSelect(building));
    }

    @Override
    public int getItemCount() {
        return buildingList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textBuildingName);
            address = itemView.findViewById(R.id.textBuildingAddress);
        }
    }
}
