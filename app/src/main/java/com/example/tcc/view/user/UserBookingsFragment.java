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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class UserBookingsFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recycler;
    private List<Agendamento> reservas = new ArrayList<>();
    private AgendamentoAdapter adapter;
    private ListenerRegistration listener;

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

        adapter = new AgendamentoAdapter(reservas, (agendamento, novoStatus) -> cancelarReserva(agendamento));
        recycler.setAdapter(adapter);

        carregarReservas();
    }

    private void carregarReservas() {
        String userId = auth.getCurrentUser().getUid();

        listener = db.collectionGroup("reservations")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Erro ao escutar reservas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    reservas.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot) {
                            Agendamento ag = doc.toObject(Agendamento.class);
                            if (ag != null) {
                                ag.setFirestorePath(doc.getReference().getPath());
                            }
                            reservas.add(ag);
                        }
                    }

                    reservas.sort((a1, a2) -> {
                        String d1 = a1.getDate() + " " + a1.getStartTime();
                        String d2 = a2.getDate() + " " + a2.getStartTime();
                        return d2.compareTo(d1);
                    });

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
                .delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Reserva cancelada e removida com sucesso!", Toast.LENGTH_SHORT).show();
                    carregarReservas();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao cancelar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }
}
