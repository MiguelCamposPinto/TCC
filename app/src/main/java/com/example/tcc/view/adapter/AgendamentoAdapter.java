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
    private boolean isAdminView;

    public AgendamentoAdapter(List<Agendamento> agendamentos, OnStatusChangeListener listener) {
        this(agendamentos, listener, false);
    }

    public AgendamentoAdapter(List<Agendamento> agendamentos, OnStatusChangeListener listener, boolean isAdminView) {
        this.agendamentos = agendamentos;
        this.listener = listener;
        this.isAdminView = isAdminView;
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

        StringBuilder info = new StringBuilder();

        if (isAdminView) {
            info.append("Usuário: ")
                    .append(agendamento.getUserName())
                    .append("\n");
        }

        info.append("Espaço: ")
                .append(agendamento.getSpaceName() != null ? agendamento.getSpaceName() : "-")
                .append("\n");

        info.append("Recurso: ")
                .append(agendamento.getMachineName() != null ? agendamento.getMachineName() : "-")
                .append("\n");

        info.append("Data: ").append(agendamento.getDate()).append("\n");

        if (!agendamento.getSpaceType().equals("saloes")){
            info.append("Hora: ").append(agendamento.getStartTime()).append(" - ").append(agendamento.getEndTime()).append("\n");
        }

        info.append("Status: ").append(agendamento.getStatus());

        holder.textInfo.setText(info.toString());

        boolean isFinalizadoOuCancelado = agendamento.getStatus().equals("cancelado") || agendamento.getStatus().equals("finalizado");

        holder.btnCancelar.setVisibility(isFinalizadoOuCancelado ? View.GONE : View.VISIBLE);
        holder.btnCancelar.setOnClickListener(v -> listener.onStatusChange(agendamento, "cancelado"));
    }

    @Override
    public int getItemCount() {
        return agendamentos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textInfo;
        Button btnCancelar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textInfo = itemView.findViewById(R.id.textAgendamentoInfo);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
        }
    }
}
