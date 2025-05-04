package com.example.tcc.view.admin;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

    public static CreateSpaceFragment newInstance(String buildingId) {
        CreateSpaceFragment fragment = new CreateSpaceFragment();
        Bundle args = new Bundle();
        args.putString("buildingId", buildingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_space, container, false);

        spaceNameInput = view.findViewById(R.id.editTextSpaceName);
        spaceTypeInput = view.findViewById(R.id.editTextSpaceType);
        createSpaceButton = view.findViewById(R.id.buttonCreateSpace);

        db = FirebaseFirestore.getInstance();

        // PEGAR O buildingId DO BUNDLE
        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
        }

        createSpaceButton.setOnClickListener(v -> createSpace());

        return view;
    }

    private void createSpace() {
        String name = spaceNameInput.getText().toString().trim();
        String type = spaceTypeInput.getText().toString().trim();

        if (name.isEmpty() || type.isEmpty()) {
            Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buildingId == null) {
            Toast.makeText(getContext(), "Erro: ID do prédio não encontrado", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> spaceData = new HashMap<>();
        spaceData.put("name", name);
        spaceData.put("type", type);
        spaceData.put("buildingId", buildingId);

        db.collection("spaces").add(spaceData).addOnSuccessListener(documentReference -> {
            Toast.makeText(getContext(), "Espaço criado com sucesso!", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Erro ao criar espaço: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}