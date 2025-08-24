package com.example.tcc.view.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.tcc.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateQuadraFragment extends Fragment {

    private EditText edtNomeQuadra;
    private Button btnSalvarQuadra;
    private FirebaseFirestore db;
    private String buildingId, spaceId, spaceType;

    public CreateQuadraFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_quadra, container, false);

        edtNomeQuadra = view.findViewById(R.id.edtNomeQuadra);
        btnSalvarQuadra = view.findViewById(R.id.btnSalvarQuadra);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
            spaceId = getArguments().getString("spaceId");
            spaceType = getArguments().getString("spaceType");
        }

        btnSalvarQuadra.setOnClickListener(v -> salvarQuadra(v));

        return view;
    }

    private void salvarQuadra(View view) {
        String nome = edtNomeQuadra.getText().toString().trim();

        if (TextUtils.isEmpty(nome)) {
            Toast.makeText(getContext(), "Digite o nome da quadra", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> quadraData = new HashMap<>();
        quadraData.put("name", nome);
        quadraData.put("buildingId", buildingId);
        quadraData.put("spaceId", spaceId);
        quadraData.put("spaceType", spaceType);

        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("quadras")
                .add(quadraData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Quadra criada com sucesso!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
