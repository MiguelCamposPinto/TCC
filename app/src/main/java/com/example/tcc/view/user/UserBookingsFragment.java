package com.example.tcc.view.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Agendamento;
import com.example.tcc.view.adapter.AgendamentoAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserBookingsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recycler;
    private List<Agendamento> reservas = new ArrayList<>();
    private AgendamentoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recycler = view.findViewById(R.id.recyclerUserBookings);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AgendamentoAdapter(reservas, (agendamento, novoStatus) -> {
            cancelarReserva(agendamento);
        });
        recycler.setAdapter(adapter);

        carregarReservas();
    }

    private void carregarReservas() {
        String userId = auth.getCurrentUser().getUid();

        db.collectionGroup("agendamentos")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Erro ao escutar reservas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reservas.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Agendamento ag = doc.toObject(Agendamento.class);
                        ag.setId(doc.getId());
                        ag.setFirestorePath(doc.getReference().getPath());

                        if ("confirmado".equals(ag.getStatus())) {
                            String novoStatus = verificarStatusAutomatico(ag);
                            if (!"confirmado".equals(novoStatus)) {
                                doc.getReference().update("status", novoStatus);
                                ag.setStatus(novoStatus);
                            }
                        }

                        reservas.add(ag);
                    }
                    adapter.notifyDataSetChanged();
                });
    }


    private void cancelarReserva(Agendamento agendamento) {
        String path = agendamento.getFirestorePath();
        if (path == null || path.isEmpty()) {
            Toast.makeText(getContext(), "Caminho da reserva inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        db.document(path)
                .update("status", "cancelado")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Reserva cancelada com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarReservas();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao cancelar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.d("cancelarReserva", e.getMessage());
                });
    }

    private String verificarStatusAutomatico(Agendamento ag) {
        try {
            SimpleDateFormat sdfData = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", Locale.getDefault());

            Date agora = new Date();
            Date dataAtual = sdfData.parse(sdfData.format(agora));
            Date horaAtual = sdfHora.parse(sdfHora.format(agora));

            Date dataReserva = sdfData.parse(ag.getData());
            Date horaInicio = sdfHora.parse(ag.getHoraInicio());
            Date horaFim = sdfHora.parse(ag.getHoraFim());

            if (dataAtual.before(dataReserva)) {
                return "confirmado";
            } else if (dataAtual.equals(dataReserva)) {
                if (horaAtual.before(horaInicio)) {
                    return "confirmado";
                } else if (!horaAtual.before(horaInicio) && horaAtual.before(horaFim)) {
                    return "em_andamento";
                } else {
                    return "finalizado";
                }
            } else {
                return "finalizado";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ag.getStatus(); // fallback se der erro
        }
    }
}
