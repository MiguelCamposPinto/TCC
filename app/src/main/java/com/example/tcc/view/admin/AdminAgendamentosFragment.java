package com.example.tcc.view.admin;

import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminAgendamentosFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";
    private static final String ARG_MACHINE_ID = "machineId";

    private String buildingId, spaceId, machineId;
    private FirebaseFirestore db;
    private RecyclerView recycler;
    private final List<Agendamento> agendamentos = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private AgendamentoAdapter adapter;

    public static AdminAgendamentosFragment newInstance(String buildingId, String spaceId, String machineId) {
        AdminAgendamentosFragment fragment = new AdminAgendamentosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUILDING_ID, buildingId);
        args.putString(ARG_SPACE_ID, spaceId);
        args.putString(ARG_MACHINE_ID, machineId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_agendamentos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
            machineId = getArguments().getString(ARG_MACHINE_ID);
        }

        db = FirebaseFirestore.getInstance();
        recycler = view.findViewById(R.id.recyclerAgendamentos);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AgendamentoAdapter(agendamentos, (agendamento, novoStatus) ->
                atualizarStatusAgendamento(agendamento.getFirestorePath(), novoStatus), true);
        recycler.setAdapter(adapter);

        carregarAgendamentos();
    }

    private void carregarAgendamentos() {
        ListenerRegistration reg = db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document(machineId)
                .collection("agendamentos")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Erro ao escutar agendamentos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    agendamentos.clear();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Agendamento agendamento = doc.toObject(Agendamento.class);
                            if (agendamento != null) {
                                agendamento.setUserName(doc.getString("userName"));
                                agendamento.setEspacoNome(doc.getString("espacoNome"));
                                agendamento.setFirestorePath(doc.getReference().getPath());
                                agendamentos.add(agendamento);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        listeners.add(reg);
    }

    private void atualizarStatusAgendamento(String firestorePath, String status) {
        if (status.equals("cancelado")) {
            db.document(firestorePath)
                    .delete()
                    .addOnSuccessListener(unused -> Toast.makeText(getContext(), "Reserva removida com sucesso!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao remover: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.document(firestorePath)
                    .update("status", status)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao atualizar", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration reg : listeners) {
            reg.remove();
        }
        listeners.clear();
    }
}
