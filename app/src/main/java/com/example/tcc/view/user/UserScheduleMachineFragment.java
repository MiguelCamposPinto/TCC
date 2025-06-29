package com.example.tcc.view.user;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private Spinner spinnerCiclo;

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

        spinnerCiclo = view.findViewById(R.id.spinnerCiclo);

        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document(machineId)
                .get()
                .addOnSuccessListener(doc -> {
                    String tipo = doc.getString("type");
                    if ("lavar".equalsIgnoreCase(tipo)) {
                        spinnerCiclo.setVisibility(View.VISIBLE);

                        List<String> nomesCiclos = new ArrayList<>();
                        List<Integer> duracoesCiclos = new ArrayList<>();

                        List<Map<String, Object>> ciclos = (List<Map<String, Object>>) doc.get("ciclos");

                        if (ciclos != null && !ciclos.isEmpty()) {
                            for (Map<String, Object> ciclo : ciclos) {
                                String nome = (String) ciclo.get("nome");
                                Long dur = (Long) ciclo.get("duracao");
                                if (nome != null && dur != null) {
                                    nomesCiclos.add(nome + " (" + dur + " min)");
                                    duracoesCiclos.add(dur.intValue());
                                }
                            }
                        }

                        if (nomesCiclos.isEmpty()) {
                            nomesCiclos.add("Padrão (60 min)");
                            duracoesCiclos.add(60);
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                nomesCiclos
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCiclo.setAdapter(adapter);

                        spinnerCiclo.setTag(duracoesCiclos);

                        spinnerCiclo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                                if (textSelectedDate.getText() != null && !textSelectedDate.getText().toString().isEmpty()) {
                                    carregarHorariosDisponiveis(textSelectedDate.getText().toString());
                                }
                            }
                            @Override public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            @SuppressLint("DefaultLocale") String data = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            textSelectedDate.setText(data);
            carregarHorariosDisponiveis(data);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();

    }

    private void carregarHorariosDisponiveis(String dataSelecionada) {
        int duracaoMinutos;
        if (spinnerCiclo.getTag() instanceof List) {
            List<Integer> duracoes = (List<Integer>) spinnerCiclo.getTag();
            int pos = spinnerCiclo.getSelectedItemPosition();
            if (pos >= 0 && pos < duracoes.size()) {
                duracaoMinutos = duracoes.get(pos);
            } else {
                duracaoMinutos = 60;
            }
        } else {
            duracaoMinutos = 60;
        }

        List<String> todosSlots = gerarSlotsDiarios(duracaoMinutos);
        List<String> disponiveis = new ArrayList<>(todosSlots);

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
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"confirmado".equals(status) && !"em_andamento".equals(status)) continue;

                        String ini = doc.getString("horaInicio");
                        String fim = doc.getString("horaFim");

                        List<String> remover = new ArrayList<>();
                        for (String slot : disponiveis) {
                            String fimSlot = calcularHoraFim(slot, duracaoMinutos);
                            if (fimSlot == null || conflita(slot, fimSlot, ini, fim)) {
                                remover.add(slot);
                            }
                        }
                        disponiveis.removeAll(remover);
                    }

                    if (dataSelecionada.equals(hojeFormatado())) {
                        String agora = horaAtual();
                        disponiveis.removeIf(hora -> hora.compareTo(agora) <= 0);
                    }

                    if (disponiveis.isEmpty()) {
                        Toast.makeText(requireContext(), "Nenhum horário disponível para essa data.", Toast.LENGTH_LONG).show();
                    } else {
                        adapter = new HorarioAdapter(disponiveis, h -> fazerAgendamento(dataSelecionada, h));
                        recyclerHorarios.setAdapter(adapter);
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Erro ao buscar horários", Toast.LENGTH_SHORT).show()
                );
    }

    private List<String> gerarSlotsDiarios(int duracaoMinutos) {
        List<String> slots = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 7);
            cal.set(Calendar.MINUTE, 0);

            Calendar limite = Calendar.getInstance();
            limite.set(Calendar.HOUR_OF_DAY, 22);
            limite.set(Calendar.MINUTE, 0);

            while (true) {
                Calendar fimSlot = (Calendar) cal.clone();
                fimSlot.add(Calendar.MINUTE, duracaoMinutos);
                if (fimSlot.after(limite)) break;

                slots.add(sdf.format(cal.getTime()));
                cal.add(Calendar.MINUTE, 15);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slots;
    }

    private void fazerAgendamento(String data, String horaInicio) {
        String userId = auth.getCurrentUser().getUid();

        int duracaoMinutos = 60;
        if (spinnerCiclo.getTag() instanceof List) {
            List<Integer> duracoes = (List<Integer>) spinnerCiclo.getTag();
            int pos = spinnerCiclo.getSelectedItemPosition();
            if (pos >= 0 && pos < duracoes.size()) {
                duracaoMinutos = duracoes.get(pos);
            }
        }

        String horaFim = calcularHoraFim(horaInicio, duracaoMinutos);

        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document(machineId)
                .collection("agendamentos")
                .whereEqualTo("data", data)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        String inicio = doc.getString("horaInicio");
                        String fim = doc.getString("horaFim");

                        if ("confirmado".equals(status) || "em_andamento".equals(status)) {
                            if (conflita(horaInicio, horaFim, inicio, fim)) {
                                Toast.makeText(getContext(), "Horário em conflito com outro agendamento.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                    salvarAgendamento(userId, data, horaInicio, horaFim);
                });
    }

    private boolean conflita(String ini1, String fim1, String ini2, String fim2) {
        return ini1.compareTo(fim2) < 0 && fim1.compareTo(ini2) > 0;
    }

    private String horaAtual() {
        Calendar c = Calendar.getInstance();
        return String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private String hojeFormatado() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private void salvarAgendamento(String userId, String data, String horaInicio, String horaFim) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");

            db.collection("predios").document(buildingId)
                    .collection("spaces").document(spaceId)
                    .get().addOnSuccessListener(spaceDoc -> {
                        String espacoNome = spaceDoc.getString("name");

                        db.collection("predios").document(buildingId)
                                .collection("spaces").document(spaceId)
                                .collection("maquinas").document(machineId)
                                .get().addOnSuccessListener(machineDoc -> {
                                    String machineName = machineDoc.getString("name");

                                    Agendamento ag = new Agendamento(userId, data, horaInicio, horaFim, "confirmado");
                                    ag.setUserName(userName);
                                    ag.setEspacoNome(espacoNome);
                                    ag.setMachineName(machineName);

                                    db.collection("predios").document(buildingId)
                                            .collection("spaces").document(spaceId)
                                            .collection("maquinas").document(machineId)
                                            .collection("agendamentos")
                                            .add(ag)
                                            .addOnSuccessListener(r -> {
                                                Toast.makeText(requireContext(),
                                                        "Agendado de " + horaInicio + " às " + horaFim,
                                                        Toast.LENGTH_SHORT).show();
                                                requireActivity().getSupportFragmentManager().popBackStack();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "Erro ao agendar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                            );
                                });
                    });
        });
    }


    private String calcularHoraFim(String horaInicio, int durMin) {
        try {
            String[] p = horaInicio.split(":");
            int h = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]) + durMin;
            h += m / 60;
            m = m % 60;

            if (h > 22 || (h == 22 && m > 0)) return null;

            return String.format("%02d:%02d", h, m);
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
