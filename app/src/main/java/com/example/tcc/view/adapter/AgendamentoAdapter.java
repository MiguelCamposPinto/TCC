package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tcc.R;
import com.example.tcc.model.Agendamento;
import java.util.List;

public class AgendamentoAdapter extends RecyclerView.Adapter<AgendamentoAdapter.ViewHolder> {

    public interface OnStatusChangeListener {
        void onStatusChange(Agendamento agendamento, String novoStatus);
    }

    private List<Agendamento> agendamentos;
    private OnStatusChangeListener listener;

    public AgendamentoAdapter(List<Agendamento> agendamentos, OnStatusChangeListener listener) {
        this.agendamentos = agendamentos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_agendamento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Agendamento agendamento = agendamentos.get(position);

        holder.textInfo.setText("Usuário: " + agendamento.getUserId() +
                "\nData: " + agendamento.getData() +
                "\nHora: " + agendamento.getHoraInicio() + " - " + agendamento.getHoraFim() +
                "\nStatus: " + agendamento.getStatus());

        if (agendamento.getStatus().equals("cancelado") || agendamento.getStatus().equals("finalizado")) {
            holder.btnCancelar.setVisibility(View.GONE);
        } else {
            holder.btnCancelar.setVisibility(View.VISIBLE);
            holder.btnCancelar.setOnClickListener(v -> listener.onStatusChange(agendamento, "cancelado"));
        }

        holder.btnConfirmar.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return agendamentos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textInfo;
        Button btnConfirmar, btnCancelar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textInfo = itemView.findViewById(R.id.textAgendamentoInfo);
            btnConfirmar = itemView.findViewById(R.id.btnConfirmar);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
        }
    }
}
