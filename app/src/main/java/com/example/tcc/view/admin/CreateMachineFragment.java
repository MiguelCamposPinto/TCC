package com.example.tcc.view.admin;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tcc.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateMachineFragment extends Fragment {

    private EditText machineNameInput;
    private Spinner spinnerMachineType;
    private Button createMachineButton, addCycleButton;
    private LinearLayout cycleContainer;

    private FirebaseFirestore db;
    private String buildingId, spaceId;

    private final List<View> cycleViews = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_machine, container, false);

        machineNameInput = view.findViewById(R.id.editTextMachineName);
        spinnerMachineType = view.findViewById(R.id.spinnerMachineType);
        createMachineButton = view.findViewById(R.id.buttonCreateMachine);
        addCycleButton = view.findViewById(R.id.buttonAddCycle);
        cycleContainer = view.findViewById(R.id.cycleContainer);

        db = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
            spaceId    = getArguments().getString("spaceId");
        }

        addCycleButton.setOnClickListener(v -> addCycleView());
        createMachineButton.setOnClickListener(v -> createMachine());
        spinnerMachineType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = spinnerMachineType.getSelectedItem().toString();
                if (selected.equals("Máquina de lavar")) {
                    cycleContainer.setVisibility(View.VISIBLE);
                    addCycleButton.setVisibility(View.VISIBLE);
                } else {
                    cycleContainer.setVisibility(View.GONE);
                    addCycleButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    private void addCycleView() {
        View cycleView = LayoutInflater.from(getContext()).inflate(R.layout.item_cycle_input, cycleContainer, false);
        cycleContainer.addView(cycleView);
        cycleViews.add(cycleView);
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

        Map<String, Object> machineData = new HashMap<>();
        machineData.put("name", name);
        machineData.put("type", type);
        machineData.put("buildingId", buildingId);
        machineData.put("spaceId", spaceId);

        if (type.equals("lavar")) {
            List<Map<String, Object>> ciclos = new ArrayList<>();
            for (View view : cycleViews) {
                EditText nomeInput = view.findViewById(R.id.editCycleName);
                EditText duracaoInput = view.findViewById(R.id.editCycleDuration);

                String nome = nomeInput.getText().toString().trim();
                String duracaoStr = duracaoInput.getText().toString().trim();

                if (!nome.isEmpty() && !duracaoStr.isEmpty()) {
                    try {
                        int duracao = Integer.parseInt(duracaoStr);
                        Map<String, Object> ciclo = new HashMap<>();
                        ciclo.put("nome", nome);
                        ciclo.put("duracao", duracao);
                        ciclos.add(ciclo);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Duração inválida para ciclo: " + nome, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            if (ciclos.isEmpty()) {
                Map<String, Object> cicloPadrao = new HashMap<>();
                cicloPadrao.put("nome", "Normal");
                cicloPadrao.put("duracao", 60);
                ciclos.add(cicloPadrao);
            }

            machineData.put("ciclos", ciclos);
        }

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
