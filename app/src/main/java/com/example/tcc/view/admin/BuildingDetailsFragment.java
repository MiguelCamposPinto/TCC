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
import android.widget.TextView;
import android.widget.Toast;
import com.example.tcc.R;
import com.example.tcc.model.Spaces;
import com.example.tcc.view.adapter.SpacesAdapter;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BuildingDetailsFragment extends Fragment {
    private FirebaseFirestore db;
    private String buildingId;
    private TextView buildingName, buildingAddress;
    private Button addSpaceButton;
    private RecyclerView recyclerSpaces;
    private List<Spaces> spaceList = new ArrayList<>();
    private SpacesAdapter spacesAdapter;


    public static BuildingDetailsFragment newInstance(String buildingId) {
        BuildingDetailsFragment fragment = new BuildingDetailsFragment();
        Bundle args = new Bundle();
        args.putString("buildingId", buildingId);
        fragment.setArguments(args);
        return fragment;
    }

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

        loadBuildingDetails();

        addSpaceButton.setOnClickListener(v -> {
            CreateSpaceFragment createSpaceFragment = CreateSpaceFragment.newInstance(buildingId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, createSpaceFragment)
                    .addToBackStack(null)
                    .commit();

        });

        recyclerSpaces = view.findViewById(R.id.recyclerSpaces);
        recyclerSpaces.setLayoutManager(new LinearLayoutManager(getContext()));

        spacesAdapter = new SpacesAdapter(spaceList, space -> {
            Fragment frag = SpacesListFragment.newInstance(buildingId, space.getId());
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, frag)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerSpaces.setAdapter(spacesAdapter);
        loadSpaces();

        return view;
    }

    private void loadSpaces() {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    spaceList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Spaces space = doc.toObject(Spaces.class);
                        space.setId(doc.getId());
                        space.setBuildingId(buildingId);
                        spaceList.add(space);
                    }
                    spacesAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao carregar espaços", Toast.LENGTH_SHORT).show();
                });
    }


    private void loadBuildingDetails() {
        DocumentReference buildingRef = db.collection("predios").document(buildingId);
        buildingRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String address = documentSnapshot.getString("address");
                buildingName.setText(name);
                buildingAddress.setText(address);
            } else {
                Toast.makeText(getContext(), "Erro ao carregar detalhes do prédio", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Erro ao carregar detalhes do prédio", Toast.LENGTH_SHORT).show();
        });
    }
}
