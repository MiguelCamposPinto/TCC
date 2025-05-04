package com.example.tcc.view.admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.example.tcc.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BuildingDetailsFragment extends Fragment {
    private FirebaseFirestore db;
    private String buildingId;
    private TextView buildingName, buildingAddress;
    private Button addSpaceButton;

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

        return view;
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
