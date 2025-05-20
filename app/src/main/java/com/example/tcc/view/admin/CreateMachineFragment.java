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
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import com.example.tcc.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateMachineFragment extends Fragment {

    private EditText machineNameInput;
    private Spinner spinnerMachineType;
    private Button createMachineButton;
    private FirebaseFirestore db;
    private String buildingId, spaceId;

    public static CreateMachineFragment newInstance(String buildingId, String spaceId) {
        CreateMachineFragment fragment = new CreateMachineFragment();
        Bundle args = new Bundle();
        args.putString("buildingId", buildingId);
        args.putString("spaceId", spaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_machine, container, false);

        machineNameInput    = view.findViewById(R.id.editTextMachineName);
        spinnerMachineType  = view.findViewById(R.id.spinnerMachineType);
        createMachineButton = view.findViewById(R.id.buttonCreateMachine);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
            spaceId    = getArguments().getString("spaceId");
        }

        // Configura spinner (caso queira custom adapter; opcional pois usamos android:entries)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.machine_types,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMachineType.setAdapter(adapter);

        createMachineButton.setOnClickListener(v -> createMachine());

        return view;
    }

    private void createMachine() {
        String name = machineNameInput.getText().toString().trim();
        String type = spinnerMachineType.getSelectedItem().toString();
        if (type.equals("Máquina de lavar")) {
            type = "lavar";
        } else if (type.equals("Máquina de secar")) {
            type = "secar";
        }
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Informe o nome da máquina", Toast.LENGTH_SHORT).show();
            return;
        }

        if (buildingId == null || spaceId == null) {
            Toast.makeText(getContext(), "Erro: dados de prédio/espaço faltando", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> machineData = new HashMap<>();
        machineData.put("name", name);
        machineData.put("type", type);
        machineData.put("buildingId", buildingId);
        machineData.put("spaceId", spaceId);

        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .add(machineData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Máquina criada com sucesso!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao criar máquina: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
