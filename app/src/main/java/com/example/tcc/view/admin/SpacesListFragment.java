package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Space;
import android.widget.TextView;

import com.example.tcc.R;
import com.example.tcc.model.Machine;
import com.example.tcc.model.Spaces;
import com.example.tcc.view.adapter.MachineAdapter;
import com.example.tcc.view.adapter.SpacesAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SpacesListFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId;
    private TextView spaceName, spaceType;
    private Button buttonAddMachine;
    private RecyclerView recyclerMachines;

    private List<Machine> machineList = new ArrayList<>();
    private MachineAdapter machineAdapter;

    private FirebaseFirestore db;

    public static SpacesListFragment newInstance(String buildingId, String spaceId) {
        SpacesListFragment fragment = new SpacesListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUILDING_ID, buildingId);
        args.putString(ARG_SPACE_ID, spaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_space_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        spaceName = view.findViewById(R.id.textSpaceName);
        spaceType = view.findViewById(R.id.textSpaceType);
        buttonAddMachine = view.findViewById(R.id.buttonAddMachine);
        recyclerMachines = view.findViewById(R.id.recyclerMachines);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
        }

        machineAdapter = new MachineAdapter(machineList, machine -> {
            Fragment frag = AdminAgendamentosFragment.newInstance(buildingId, spaceId, machine.getId());
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, frag)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerMachines.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMachines.setAdapter(machineAdapter);

        buttonAddMachine.setOnClickListener(v -> {
            CreateMachineFragment frag = CreateMachineFragment.newInstance(buildingId, spaceId);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, frag)
                    .addToBackStack(null)
                    .commit();
        });

        loadSpaceInfo();
        loadMachines();
    }

    private void loadSpaceInfo() {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        spaceName.setText(doc.getString("name"));
                        spaceType.setText(doc.getString("type"));
                    }
                });
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
                                    machineAdapter.notifyDataSetChanged();
                                });
                    }
                });
    }

}

