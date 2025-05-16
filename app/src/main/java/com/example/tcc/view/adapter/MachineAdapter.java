package com.example.tcc.view.adapter;

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
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Machine machine = machineList.get(position);
        holder.name.setText(machine.getName());
        holder.status.setText("Status: " + machine.getStatus());

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
            name = itemView.findViewById(android.R.id.text1);
            status = itemView.findViewById(android.R.id.text2);
        }
    }
}
