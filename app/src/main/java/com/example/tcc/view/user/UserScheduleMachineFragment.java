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
import com.example.tcc.view.adapter.HorarioAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserScheduleMachineFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";
    private static final String ARG_MACHINE_ID = "machineId";

    private String buildingId, spaceId, machineId, spaceType;
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
            spaceType = getArguments().getString("spaceType");
        }

        textSelectedDate = view.findViewById(R.id.textSelectedDate);
        datePickerContainer = view.findViewById(R.id.datePickerContainer);
        recyclerHorarios = view.findViewById(R.id.recyclerHorarios);
        recyclerHorarios.setLayoutManager(new GridLayoutManager(getContext(), 3));

        textSelectedDate.setOnClickListener(v -> showDatePicker());

        spinnerCiclo = view.findViewById(R.id.spinnerCiclo);

        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("machines")
                .document(machineId)
                .get()
                .addOnSuccessListener(doc -> {
                    String tipo = doc.getString("type");
                    if ("wash".equalsIgnoreCase(tipo)) {
                        spinnerCiclo.setVisibility(View.VISIBLE);

                        List<String> nomesCiclos = new ArrayList<>();
                        List<Integer> duracoesCiclos = new ArrayList<>();

                        List<Map<String, Object>> ciclos = (List<Map<String, Object>>) doc.get("cycles");

                        if (ciclos != null && !ciclos.isEmpty()) {
                            for (Map<String, Object> ciclo : ciclos) {
                                String nome = (String) ciclo.get("name");
                                Long dur = (Long) ciclo.get("duration");
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
            @SuppressLint("DefaultLocale") String date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            textSelectedDate.setText(date);
            carregarHorariosDisponiveis(date);
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

        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("machines")
                .document(machineId)
                .collection("reservations")
                .whereEqualTo("date", dataSelecionada)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"confirmado".equals(status) && !"em_andamento".equals(status)) continue;

                        String ini = doc.getString("startTime");
                        String fim = doc.getString("endTime");

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

    private void fazerAgendamento(String date, String startTime) {
        String userId = auth.getCurrentUser().getUid();

        int durationMin;
        if (spinnerCiclo.getTag() instanceof List) {
            List<Integer> duracoes = (List<Integer>) spinnerCiclo.getTag();
            int pos = spinnerCiclo.getSelectedItemPosition();
            if (pos >= 0 && pos < duracoes.size()) {
                durationMin = duracoes.get(pos);
            } else {
                durationMin = 60;
            }
        } else {
            durationMin = 60;
        }

        String endTime = calcularHoraFim(startTime, durationMin);
        verificarAgendamentoAtivo(userId, temAtivo -> {
            if (temAtivo) {
                Toast.makeText(getContext(), "Você já possui um agendamento ativo nesta máquina.", Toast.LENGTH_LONG).show();
                return;
            }

            db.collection("buildings")
                    .document(buildingId)
                    .collection("spaces")
                    .document(spaceId)
                    .collection("machines")
                    .document(machineId)
                    .collection("reservations")
                    .whereEqualTo("date", date)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String status = doc.getString("status");
                            String inicio = doc.getString("startTime");
                            String fim = doc.getString("endTime");

                            if ("confirmado".equals(status) || "em_andamento".equals(status)) {
                                if (conflita(startTime, endTime, inicio, fim)) {
                                    Toast.makeText(getContext(), "Horário em conflito com outro agendamento.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }

                        salvarAgendamento(userId, date, startTime, endTime,durationMin);
                    });
        });
    }

    private void verificarAgendamentoAtivo(String userId, OnAgendamentoCheckListener listener) {
        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("machines")
                .document(machineId)
                .collection("reservations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean temAgendamentoAtivo = false;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if ("confirmado".equals(status) || "em_andamento".equals(status)) {
                            temAgendamentoAtivo = true;
                            break;
                        }
                    }
                    listener.onCheckFinished(temAgendamentoAtivo);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao verificar agendamentos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    listener.onCheckFinished(false);
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

    private void salvarAgendamento(String userId, String date, String startTime, String endTime, int durationMin) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");

            db.collection("buildings").document(buildingId)
                    .collection("spaces").document(spaceId)
                    .get().addOnSuccessListener(spaceDoc -> {
                        String espacoNome = spaceDoc.getString("name");

                        db.collection("buildings").document(buildingId)
                                .collection("spaces").document(spaceId)
                                .collection("machines").document(machineId)
                                .get().addOnSuccessListener(machineDoc -> {
                                    String machineName = machineDoc.getString("name");

                                    Agendamento ag = new Agendamento(userId, date, startTime, endTime, "confirmado", durationMin, spaceType);
                                    ag.setUserName(userName);
                                    ag.setSpaceName(espacoNome);
                                    ag.setMachineName(machineName);
                                    ag.setBuildingID(buildingId);
                                    ag.setSpaceId(spaceId);
                                    ag.setMachineId(machineId);

                                    db.collection("buildings").document(buildingId)
                                            .collection("spaces").document(spaceId)
                                            .collection("machines").document(machineId)
                                            .collection("reservations")
                                            .add(ag)
                                            .addOnSuccessListener(r -> {
                                                Toast.makeText(requireContext(),
                                                        "Agendado de " + startTime + " às " + endTime,
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

    private String calcularHoraFim(String startTime, int durMin) {
        try {
            String[] p = startTime.split(":");
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

    private interface OnAgendamentoCheckListener {
        void onCheckFinished(boolean temAgendamentoAtivo);
    }
}
