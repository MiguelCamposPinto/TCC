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

public class CreateSalaoFragment extends Fragment {

    private EditText edtNomeSalao, edtCapacidadeMax;
    private Button btnSalvarSalao;
    private FirebaseFirestore db;
    private String buildingId, spaceId, spaceType;

    public CreateSalaoFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_salao, container, false);

        edtNomeSalao = view.findViewById(R.id.edtNomeSalao);
        edtCapacidadeMax = view.findViewById(R.id.edtCapacidadeMax);
        btnSalvarSalao = view.findViewById(R.id.btnSalvarSalao);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
            spaceId = getArguments().getString("spaceId");
            spaceType = getArguments().getString("spaceType");
        }

        btnSalvarSalao.setOnClickListener(v -> salvarSalao(v));

        return view;
    }

    private void salvarSalao(View view) {
        String nome = edtNomeSalao.getText().toString().trim();
        String capacidadeStr = edtCapacidadeMax.getText().toString().trim();

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(capacidadeStr)) {
            Toast.makeText(getContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacidade;
        try {
            capacidade = Integer.parseInt(capacidadeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Capacidade deve ser um número válido", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> salaoData = new HashMap<>();
        salaoData.put("name", nome);
        salaoData.put("capacidadeMax", capacidade);
        salaoData.put("buildingId", buildingId);
        salaoData.put("spaceId", spaceId);
        salaoData.put("spaceType", spaceType);

        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("saloes")
                .add(salaoData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Salão criado com sucesso!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(view).popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
