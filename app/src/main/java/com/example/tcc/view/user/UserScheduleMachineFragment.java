package com.example.tcc.view.user;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Agendamento;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class UserScheduleMachineFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";
    private static final String ARG_MACHINE_ID = "machineId";

    private String buildingId, spaceId, machineId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView textSelectedDate;
    private RecyclerView recyclerHorarios;
    private HorarioAdapter adapter;
    private LinearLayout datePickerContainer;

    private List<String> todosHorarios = Arrays.asList(
            "08:00", "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00", "17:00",
            "18:00", "19:00", "20:00", "21:00"
    );

    public static UserScheduleMachineFragment newInstance(String buildingId, String spaceId, String machineId) {
        UserScheduleMachineFragment fragment = new UserScheduleMachineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUILDING_ID, buildingId);
        args.putString(ARG_SPACE_ID, spaceId);
        args.putString(ARG_MACHINE_ID, machineId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_schedule_machine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
            machineId = getArguments().getString(ARG_MACHINE_ID);
        }

        textSelectedDate = view.findViewById(R.id.textSelectedDate);
        datePickerContainer = view.findViewById(R.id.datePickerContainer);
        recyclerHorarios = view.findViewById(R.id.recyclerHorarios);
        recyclerHorarios.setLayoutManager(new GridLayoutManager(getContext(), 3));

        textSelectedDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            String data = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            textSelectedDate.setText(data);
            carregarHorariosDisponiveis(data);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void carregarHorariosDisponiveis(String dataSelecionada) {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document(machineId)
                .collection("agendamentos")
                .whereEqualTo("data", dataSelecionada)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> disponiveis = new ArrayList<>(todosHorarios);

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        String ocupado = doc.getString("horaInicio");

                        if ("confirmado".equals(status) || "em_andamento".equals(status)) {
                            disponiveis.remove(ocupado);
                        }
                    }

                    adapter = new HorarioAdapter(disponiveis, horaSelecionada -> fazerAgendamento(dataSelecionada, horaSelecionada));
                    recyclerHorarios.setAdapter(adapter);
                });

    }

    private void fazerAgendamento(String data, String horaInicio) {
        String userId = auth.getCurrentUser().getUid();

        // Pegar nome do usuário
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");

            // Pegar nome do espaço
            db.collection("predios")
                    .document(buildingId)
                    .collection("spaces")
                    .document(spaceId)
                    .get()
                    .addOnSuccessListener(spaceDoc -> {
                        String espacoNome = spaceDoc.getString("name");

                        // Pegar nome da máquina
                        db.collection("predios")
                                .document(buildingId)
                                .collection("spaces")
                                .document(spaceId)
                                .collection("maquinas")
                                .document(machineId)
                                .get()
                                .addOnSuccessListener(machineDoc -> {
                                    String machineName = machineDoc.getString("name");

                                    String horaFim = calcularHoraFim(horaInicio);
                                    if (horaFim == null) {
                                        Toast.makeText(getContext(), "Horário final inválido.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    Agendamento agendamento = new Agendamento(userId, data, horaInicio, horaFim, "confirmado");
                                    agendamento.setUserName(userName);
                                    agendamento.setEspacoNome(espacoNome);
                                    agendamento.setMachineName(machineName);

                                    db.collection("predios")
                                            .document(buildingId)
                                            .collection("spaces")
                                            .document(spaceId)
                                            .collection("maquinas")
                                            .document(machineId)
                                            .collection("agendamentos")
                                            .add(agendamento)
                                            .addOnSuccessListener(ref -> {
                                                Toast.makeText(getContext(), "Reserva confirmada!", Toast.LENGTH_SHORT).show();
                                                requireActivity().getSupportFragmentManager().popBackStack();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Erro ao agendar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                });
                    });
        });
    }



    private String calcularHoraFim(String horaInicio) {
        try {
            String[] partes = horaInicio.split(":");
            int h = Integer.parseInt(partes[0]);
            if (h >= 21) return null;
            return String.format("%02d:%02d", h + 1, 0);
        } catch (Exception e) {
            return null;
        }
    }

    public interface OnHorarioClickListener {
        void onClick(String hora);
    }

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
    }
}
