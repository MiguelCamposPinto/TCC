package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Building;
import com.example.tcc.view.admin.BuildingDetailsFragment;

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
            // Passar o ID do prédio para a tela de detalhes
            Fragment buildingDetailsFragment = BuildingDetailsFragment.newInstance(building.getId());
            fragmentManager.beginTransaction()
                    .replace(R.id.admin_fragment_container, buildingDetailsFragment)
                    .addToBackStack(null)
                    .commit();
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

