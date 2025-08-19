package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tcc.R;
import com.example.tcc.model.Building;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateBuildingFragment extends Fragment {
    private EditText nameInput, addressInput, passwordInput;
    private Button createButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_building, container, false);

        nameInput = view.findViewById(R.id.editTextBuildingName);
        addressInput = view.findViewById(R.id.editTextBuildingAddress);
        createButton = view.findViewById(R.id.buttonCreateBuilding);
        passwordInput = view.findViewById(R.id.editTextBuildingPasswod);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        createButton.setOnClickListener(v -> createBuilding());

        return view;
    }

    private void createBuilding() {
        String name = nameInput.getText().toString().trim();
        String address = addressInput.getText().toString().trim();
        String buildingPassword = passwordInput.getText().toString().trim();

        if (name.isEmpty() || address.isEmpty() || buildingPassword.isEmpty()) {
            Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        String adminId = auth.getCurrentUser().getUid();
        Building building = new Building(name, address, buildingPassword, adminId);

        db.collection("buildings")
                .add(building.toMap())
                .addOnSuccessListener(documentReference -> {
                    String buildingId = documentReference.getId();

                    db.collection("users").document(adminId)
                            .update("buildings", FieldValue.arrayUnion(buildingId))
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Prédio criado!", Toast.LENGTH_SHORT).show();
                                nameInput.setText("");
                                addressInput.setText("");
                                passwordInput.setText("");
                            });

                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao criar prédio: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
