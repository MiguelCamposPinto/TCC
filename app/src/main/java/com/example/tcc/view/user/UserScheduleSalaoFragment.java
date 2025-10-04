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

import com.example.tcc.R;
import com.example.tcc.model.Agendamento;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class UserScheduleSalaoFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";
    private static final String ARG_SALAO_ID = "saloesId";

    private String buildingId, spaceId, salaoId, spaceType;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView textSelectedDate;
    private LinearLayout container;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_schedule_salao, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
            salaoId = getArguments().getString(ARG_SALAO_ID);
            spaceType = getArguments().getString("spaceType");
        }

        textSelectedDate = view.findViewById(R.id.textSelectedDateSalao);
        container = view.findViewById(R.id.containerSalaoAgendamento);

        textSelectedDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            textSelectedDate.setText(date);
            verificarDisponibilidade(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void verificarDisponibilidade(String date) {
        db.collection("buildings").document(buildingId)
                .collection("spaces").document(spaceId)
                .collection("saloes").document(salaoId)
                .collection("reservations")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean ocupado = snapshot.getDocuments().stream()
                            .anyMatch(doc -> {
                                String status = doc.getString("status");
                                return "confirmado".equals(status) || "em_andamento".equals(status);
                            });

                    if (ocupado) {
                        Toast.makeText(getContext(), "Salão já reservado para essa data", Toast.LENGTH_LONG).show();
                    } else {
                        String userId = auth.getCurrentUser().getUid();
                        verificarAgendamentoAtivoSalao(userId, date);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erro ao verificar disponibilidade: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Verifica se o usuário já possui um agendamento ativo neste salão (em qualquer data)
     */
    private void verificarAgendamentoAtivoSalao(String userId, String date) {
        db.collection("buildings").document(buildingId)
                .collection("spaces").document(spaceId)
                .collection("saloes").document(salaoId)
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

                    if (temAgendamentoAtivo) {
                        Toast.makeText(getContext(),
                                "Você já possui um agendamento ativo neste salão.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        fazerAgendamento(date);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erro ao verificar agendamento ativo: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void fazerAgendamento(String date) {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String userName = userDoc.getString("name");

            db.collection("buildings").document(buildingId)
                    .collection("spaces").document(spaceId)
                    .get().addOnSuccessListener(spaceDoc -> {
                        String espacoNome = spaceDoc.getString("name");

                        db.collection("buildings").document(buildingId)
                                .collection("spaces").document(spaceId)
                                .collection("saloes").document(salaoId)
                                .get().addOnSuccessListener(salaoDoc -> {
                                    String salaoNome = salaoDoc.getString("name");

                                    Agendamento ag = new Agendamento(userId, date, "00:00", "23:59",
                                            "confirmado", 1440, spaceType);
                                    ag.setUserName(userName);
                                    ag.setSpaceName(espacoNome);
                                    ag.setMachineName(salaoNome); // reaproveitando campo
                                    ag.setBuildingID(buildingId);
                                    ag.setSpaceId(spaceId);
                                    ag.setMachineId(salaoId);

                                    db.collection("buildings").document(buildingId)
                                            .collection("spaces").document(spaceId)
                                            .collection("saloes").document(salaoId)
                                            .collection("reservations")
                                            .add(ag)
                                            .addOnSuccessListener(r -> {
                                                Toast.makeText(requireContext(),
                                                        "Agendado com sucesso!",
                                                        Toast.LENGTH_SHORT).show();
                                                requireActivity().getSupportFragmentManager().popBackStack();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(),
                                                            "Erro ao salvar agendamento: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                });
                    });
        });
    }
}
