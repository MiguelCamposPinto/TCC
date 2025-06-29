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
import android.widget.TextView;
import android.widget.Toast;

import com.example.tcc.R;
import com.example.tcc.model.Spaces;
import com.example.tcc.view.adapter.SpacesAdapter;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildingDetailsFragment extends Fragment {

    private FirebaseFirestore db;
    private String buildingId;
    private TextView buildingName, buildingAddress;
    private Button addSpaceButton;
    private RecyclerView recyclerSpaces;
    private final List<Spaces> spaceList = new ArrayList<>();
    private SpacesAdapter spacesAdapter;
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_build_details, container, false);
        db = FirebaseFirestore.getInstance();

        buildingName = view.findViewById(R.id.textBuildingName);
        buildingAddress = view.findViewById(R.id.textBuildingAddress);
        addSpaceButton = view.findViewById(R.id.buttonAddSpace);
        recyclerSpaces = view.findViewById(R.id.recyclerSpaces);

        recyclerSpaces.setLayoutManager(new LinearLayoutManager(getContext()));
        spacesAdapter = new SpacesAdapter(spaceList, space -> {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", space.getId());
            navController.navigate(R.id.action_buildingDetailsFragment_to_spacesListFragment, args);
        });
        recyclerSpaces.setAdapter(spacesAdapter);

        addSpaceButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            navController.navigate(R.id.action_buildingDetailsFragment_to_createSpaceFragment, args);
        });

        loadBuildingDetails();
        loadSpaces();

        return view;
    }

    private void loadSpaces() {
        ListenerRegistration reg = db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Toast.makeText(getContext(), "Erro ao escutar espaços", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    spaceList.clear();
                    Set<String> idsAdicionados = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!idsAdicionados.contains(doc.getId())) {
                            Spaces space = doc.toObject(Spaces.class);
                            if (space != null) {
                                space.setId(doc.getId());
                                space.setBuildingId(buildingId);
                                spaceList.add(space);
                                idsAdicionados.add(doc.getId());
                            }
                        }
                    }

                    spacesAdapter.notifyDataSetChanged();
                });

        listeners.add(reg);
    }

    private void loadBuildingDetails() {
        DocumentReference buildingRef = db.collection("predios").document(buildingId);
        buildingRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String address = documentSnapshot.getString("address");
                buildingName.setText(name);
                buildingAddress.setText(address);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Erro ao carregar detalhes do prédio", Toast.LENGTH_SHORT).show());
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
