package com.example.tcc.view.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Building;

import java.util.List;

public class BuildingAdapter extends RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder> {
    private final List<Building> buildingList;
    private final FragmentManager fragmentManager;

    // Construtor modificado para receber o FragmentManager
    public BuildingAdapter(List<Building> buildingList, FragmentManager fragmentManager) {
        this.buildingList = buildingList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public BuildingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new BuildingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BuildingViewHolder holder, int position) {
        Building building = buildingList.get(position);
        holder.name.setText(building.getName());
        holder.address.setText(building.getAddress());

        holder.itemView.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(holder.itemView);
            Bundle args = new Bundle();
            args.putString("buildingId", building.getId());
            navController.navigate(R.id.action_nav_main_to_buildingDetailsFragment, args);
        });
    }

    @Override
    public int getItemCount() {
        return buildingList.size();
    }

    static class BuildingViewHolder extends RecyclerView.ViewHolder {
        TextView name, address;

        public BuildingViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            address = itemView.findViewById(android.R.id.text2);
        }
    }
}

