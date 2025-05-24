package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpacesListFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId;
    private TextView spaceName;
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
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", spaceId);
            navController.navigate(R.id.action_spacesListFragment_to_createMachineFragment, args);
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
                    }
                });
    }

    private void loadMachines() {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;

                    machineList.clear();
                    Set<String> idsAdicionados = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Machine machine = doc.toObject(Machine.class);
                        machine.setId(doc.getId());

                        if (!idsAdicionados.contains(machine.getId())) {
                            idsAdicionados.add(machine.getId());
                            verificarStatusMaquinaEmTempoReal(machine);
                        }
                    }
                });
    }

    private void verificarStatusMaquinaEmTempoReal(Machine machine) {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document(machine.getId())
                .collection("agendamentos")
                .whereEqualTo("status", "em_andamento")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;

                    boolean emUso = snapshot != null && !snapshot.isEmpty();
                    machine.setStatus(emUso ? "em_uso" : "livre");

                    if (!machineList.contains(machine)) {
                        machineList.add(machine);
                    }

                    machineAdapter.notifyDataSetChanged();
                });
    }


}

