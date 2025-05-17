package com.example.tcc.view.user;

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
import com.example.tcc.model.Machine;
import com.example.tcc.view.adapter.MachineAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserMachinesFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId;
    private FirebaseFirestore db;
    private List<Machine> machineList = new ArrayList<>();
    private RecyclerView recyclerView;
    private MachineAdapter adapter;

    public static UserMachinesFragment newInstance(String buildingId, String spaceId) {
        UserMachinesFragment fragment = new UserMachinesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUILDING_ID, buildingId);
        args.putString(ARG_SPACE_ID, spaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_machines, container, false);

        recyclerView = view.findViewById(R.id.recyclerUserMachines);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new MachineAdapter(machineList, machine -> {
            // Substitua pelos valores reais
            Fragment frag = UserScheduleMachineFragment.newInstance(buildingId, spaceId, machine.getId());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.user_fragment_container, frag)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        loadMachines();

        return view;
    }

    private void loadMachines() {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .get()
                .addOnSuccessListener(snapshot -> {
                    machineList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Machine machine = doc.toObject(Machine.class);
                        if (machine == null) continue;

                        String machineId = doc.getId();
                        machine.setId(machineId);

                        // Verifica se a máquina está em uso
                        db.collection("predios")
                                .document(buildingId)
                                .collection("spaces")
                                .document(spaceId)
                                .collection("maquinas")
                                .document(machineId)
                                .collection("agendamentos")
                                .whereIn("status", List.of("confirmado", "em_andamento"))
                                .get()
                                .addOnSuccessListener(agendamentos -> {
                                    boolean emUso = false;
                                    String hoje = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
                                    String agora = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date());

                                    for (DocumentSnapshot ag : agendamentos.getDocuments()) {
                                        String data = ag.getString("data");
                                        String inicio = ag.getString("horaInicio");
                                        String fim = ag.getString("horaFim");

                                        if (hoje.equals(data) && agora.compareTo(inicio) >= 0 && agora.compareTo(fim) < 0) {
                                            emUso = true;
                                            break;
                                        }
                                    }

                                    machine.setStatus(emUso ? "em uso" : "livre");
                                    machineList.add(machine);
                                    adapter.notifyDataSetChanged();
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erro ao carregar máquinas: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


}
