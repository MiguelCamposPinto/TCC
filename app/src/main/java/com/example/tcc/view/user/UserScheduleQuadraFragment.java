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
import com.example.tcc.view.adapter.HorarioAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserScheduleQuadraFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";
    private static final String ARG_QUADRA_ID = "quadrasId";

    private String buildingId, spaceId, quadraId, spaceType;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView textSelectedDate;
    private RecyclerView recyclerHorarios;
    private HorarioAdapter adapter;
    private LinearLayout datePickerContainer;

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
            quadraId = getArguments().getString(ARG_QUADRA_ID);
            spaceType = getArguments().getString("spaceType");
        }

        textSelectedDate = view.findViewById(R.id.textSelectedDate);
        datePickerContainer = view.findViewById(R.id.datePickerContainer);
        recyclerHorarios = view.findViewById(R.id.recyclerHorarios);
        recyclerHorarios.setLayoutManager(new GridLayoutManager(getContext(), 3));

        textSelectedDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            textSelectedDate.setText(date);
            carregarHorariosDisponiveis(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void carregarHorariosDisponiveis(String dataSelecionada) {
        List<String> todosSlots = gerarSlotsHoraEmHora();
        List<String> disponiveis = new ArrayList<>(todosSlots);

        db.collection("buildings").document(buildingId)
                .collection("spaces").document(spaceId)
                .collection("quadras").document(quadraId)
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
                            String fimSlot = calcularHoraFim(slot);
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
                        Toast.makeText(requireContext(), "Nenhum horário disponível.", Toast.LENGTH_LONG).show();
                    } else {
                        adapter = new HorarioAdapter(disponiveis, h -> fazerAgendamento(dataSelecionada, h));
                        recyclerHorarios.setAdapter(adapter);
                    }

                });
    }

    private List<String> gerarSlotsHoraEmHora() {
        List<String> slots = new ArrayList<>();
        for (int h = 7; h < 22; h++) {
            slots.add(String.format(Locale.getDefault(), "%02d:00", h));
        }
        return slots;
    }

    private void fazerAgendamento(String date, String startTime) {
        String userId = auth.getCurrentUser().getUid();
        String endTime = calcularHoraFim(startTime);

        verificarAgendamentoAtivo(userId, temAtivo -> {
            if (temAtivo) {
                Toast.makeText(getContext(), "Você já possui um agendamento ativo nesta máquina.", Toast.LENGTH_LONG).show();
                return;
            }

            db.collection("buildings").document(buildingId)
                    .collection("spaces").document(spaceId)
                    .collection("quadras").document(quadraId)
                    .collection("reservations")
                    .whereEqualTo("date", date)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String status = doc.getString("status");
                            String ini = doc.getString("startTime");
                            String fim = doc.getString("endTime");

                            if ("confirmado".equals(status) || "em_andamento".equals(status)) {
                                if (conflita(startTime, endTime, ini, fim)) {
                                    Toast.makeText(getContext(), "Horário em conflito", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }

                        salvarAgendamento(userId, date, startTime, endTime);
                    });
        });

    }

    private void verificarAgendamentoAtivo(String userId, UserScheduleQuadraFragment.OnAgendamentoCheckListener listener) {
        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("quadras")
                .document(quadraId)
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

    private void salvarAgendamento(String userId, String date, String startTime, String endTime) {
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");

            db.collection("buildings").document(buildingId)
                    .collection("spaces").document(spaceId)
                    .get().addOnSuccessListener(spaceDoc -> {
                        String espacoNome = spaceDoc.getString("name");

                        db.collection("buildings").document(buildingId)
                                .collection("spaces").document(spaceId)
                                .collection("quadras").document(quadraId)
                                .get().addOnSuccessListener(quadraDoc -> {
                                    String quadraNome = quadraDoc.getString("name");

                                    Agendamento ag = new Agendamento(userId, date, startTime, endTime, "confirmado", 60, spaceType);
                                    ag.setUserName(userName);
                                    ag.setSpaceName(espacoNome);
                                    ag.setMachineName(quadraNome); // mesmo campo pra reaproveitar
                                    ag.setBuildingID(buildingId);
                                    ag.setSpaceId(spaceId);
                                    ag.setMachineId(quadraId);

                                    db.collection("buildings").document(buildingId)
                                            .collection("spaces").document(spaceId)
                                            .collection("quadras").document(quadraId)
                                            .collection("reservations")
                                            .add(ag)
                                            .addOnSuccessListener(r -> {
                                                Toast.makeText(requireContext(), "Agendado com sucesso!", Toast.LENGTH_SHORT).show();
                                                requireActivity().getSupportFragmentManager().popBackStack();
                                            });
                                });
                    });
        });
    }

    private String horaAtual() {
        Calendar c = Calendar.getInstance();
        return String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private String hojeFormatado() {
        Calendar c = Calendar.getInstance();
        return String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private String calcularHoraFim(String startTime) {
        try {
            int h = Integer.parseInt(startTime.split(":")[0]) + 1;
            return String.format("%02d:00", h);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean conflita(String ini1, String fim1, String ini2, String fim2) {
        return ini1.compareTo(fim2) < 0 && fim1.compareTo(ini2) > 0;
    }

    private interface OnAgendamentoCheckListener {
        void onCheckFinished(boolean temAgendamentoAtivo);
    }
}
