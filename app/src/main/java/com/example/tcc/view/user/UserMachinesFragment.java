package com.example.tcc.view.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Machine;
import com.example.tcc.view.adapter.MachineAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class UserMachinesFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId;
    private FirebaseFirestore db;
    private final List<Machine> machineList = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    private RecyclerView recyclerView;
    private MachineAdapter adapter;

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
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", spaceId);
            args.putString("machineId", machine.getId());
            navController.navigate(R.id.action_userMachinesFragment_to_userScheduleMachineFragment, args);
        });

        recyclerView.setAdapter(adapter);

        loadMachines();

        return view;
    }

    private void loadMachines() {
        ListenerRegistration reg = db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Erro ao escutar máquinas", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    machineList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Machine machine = doc.toObject(Machine.class);
                        if (machine != null) {
                            machine.setId(doc.getId());
                            verificarStatusMaquinaEmTempoReal(machine);
                        }
                    }
                });

        listeners.add(reg);
    }

    private void verificarStatusMaquinaEmTempoReal(Machine machine) {
        ListenerRegistration reg = db.collection("predios")
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

                    boolean jaExiste = false;
                    for (Machine m : machineList) {
                        if (m.getId().equals(machine.getId())) {
                            m.setStatus(machine.getStatus());
                            jaExiste = true;
                            break;
                        }
                    }
                    if (!jaExiste) {
                        machineList.add(machine);
                    }

                    adapter.notifyDataSetChanged();
                });

        listeners.add(reg);
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
