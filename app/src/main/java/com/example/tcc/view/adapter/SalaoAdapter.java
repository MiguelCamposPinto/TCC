package com.example.tcc.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Salao;

import java.util.List;

public class SalaoAdapter extends RecyclerView.Adapter<SalaoAdapter.ViewHolder> implements GenericAdapter{
    public interface OnMachineClickListener {
        void onClick(Salao salao);
    }

    private List<Salao> salaoList;
    private OnMachineClickListener listener;

    public SalaoAdapter(List<Salao> salaoList, OnMachineClickListener listener) {
        this.salaoList = salaoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_salao, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SalaoAdapter.ViewHolder holder, int position) {
        Salao salao = salaoList.get(position);

        holder.name.setText(salao.getName());
        holder.capacidade.setText("Capacidade: " + salao.getCapacidadeMax());

        String status = salao.getStatus();
        boolean emUso = status != null && status.equals("em_uso");

        holder.status.setText(emUso ? "Em uso" : "Livre");
        holder.status.setBackgroundColor(emUso ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(salao);
            }
        });
    }

    @Override
    public int getItemCount() {
        return salaoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, status, capacidade;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textSalaoName);
            status = itemView.findViewById(R.id.textSalaoStatus);
            capacidade = itemView.findViewById(R.id.textSalaoCapacidade);
        }
    }
}
