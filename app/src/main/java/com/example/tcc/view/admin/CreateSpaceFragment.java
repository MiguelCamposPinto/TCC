package com.example.tcc.view.admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Button;

import com.example.tcc.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateSpaceFragment extends Fragment {

    private EditText spaceNameInput, spaceTypeInput;
    private Button createSpaceButton;
    private FirebaseFirestore db;
    private String buildingId;
    private Spinner spinnerSpaceType;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_space, container, false);

        spaceNameInput = view.findViewById(R.id.editTextSpaceName);
        createSpaceButton = view.findViewById(R.id.buttonCreateSpace);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
        }

        spinnerSpaceType = view.findViewById(R.id.spinnerSpaceType);

        String[] spaceTypes = {"lavanderia", "quadra", "salao"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, spaceTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSpaceType.setAdapter(adapter);

        createSpaceButton.setOnClickListener(v -> createSpace());

        return view;
    }

    private void createSpace() {
        String name = spaceNameInput.getText().toString().trim();
        String type = spinnerSpaceType.getSelectedItem().toString().toLowerCase();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buildingId == null) {
            Toast.makeText(getContext(), "Erro: ID do prédio não encontrado", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> spaceData = new HashMap<>();
        spaceData.put("name", name);
        spaceData.put("buildingId", buildingId);
        spaceData.put("type", type);

        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .add(spaceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Espaço criado com sucesso!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao criar espaço: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}