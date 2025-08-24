package com.example.tcc.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Quadra;

import java.util.List;

public class QuadraAdapter extends RecyclerView.Adapter<QuadraAdapter.ViewHolder> implements GenericAdapter{

    public interface OnMachineClickListener {
        void onClick(Quadra machine);
    }

    private List<Quadra> quadraList;
    private OnMachineClickListener listener;

    public QuadraAdapter(List<Quadra> quadraList, OnMachineClickListener listener) {
        this.quadraList = quadraList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quadra, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QuadraAdapter.ViewHolder holder, int position) {
        Quadra quadra = quadraList.get(position);

        holder.name.setText(quadra.getName());

        String status = quadra.getStatus();
        boolean emUso = status != null && status.equals("em_uso");

        holder.status.setText(emUso ? "Em uso" : "Livre");
        holder.status.setBackgroundColor(emUso ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(quadra);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quadraList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textQuadraName);
            status = itemView.findViewById(R.id.textQuadraStatus);
        }
    }
}
