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
                .addOnSuccessListener(querySnapshot -> {
                    machineList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Machine m = doc.toObject(Machine.class);
                        m.setId(doc.getId());
                        machineList.add(m);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao carregar máquinas: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
