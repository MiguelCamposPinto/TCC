package com.example.tcc.view.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tcc.R;
import com.example.tcc.model.Machine;
import java.util.List;

public class MachineAdapter extends RecyclerView.Adapter<MachineAdapter.ViewHolder> {

    public interface OnMachineClickListener {
        void onClick(Machine machine);
    }

    private List<Machine> machineList;
    private OnMachineClickListener listener;

    public MachineAdapter(List<Machine> machineList, OnMachineClickListener listener) {
        this.machineList = machineList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_machine, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Machine machine = machineList.get(position);

        holder.name.setText(machine.getName());

        String status = machine.getStatus();
        boolean emUso = status != null && status.equals("em_uso");

        holder.status.setText(emUso ? "Em uso" : "Livre");
        holder.status.setBackgroundColor(emUso ? Color.parseColor("#F44336") : Color.parseColor("#4CAF50"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(machine);
            }
        });
    }

    @Override
    public int getItemCount() {
        return machineList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textMachineName);
            status = itemView.findViewById(R.id.textMachineStatus);
        }
    }
}
