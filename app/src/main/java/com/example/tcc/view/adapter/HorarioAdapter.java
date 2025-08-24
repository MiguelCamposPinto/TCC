package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.view.user.UserScheduleMachineFragment;

import java.util.List;

public class HorarioAdapter extends RecyclerView.Adapter<HorarioAdapter.ViewHolder> {
    private final List<String> horarios;
    private final OnHorarioClickListener listener;

    public HorarioAdapter(List<String> horarios, OnHorarioClickListener listener) {
        this.horarios = horarios;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horario_slot, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String hora = horarios.get(position);
        holder.textHorario.setText(hora);
        holder.textHorario.setOnClickListener(v -> listener.onClick(hora));
    }

    @Override
    public int getItemCount() {
        return horarios.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textHorario;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textHorario = itemView.findViewById(R.id.textHorarioSlot);
        }
    }

    public interface OnHorarioClickListener {
        void onClick(String hora);
    }
}