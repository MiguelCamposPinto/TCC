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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tcc.R;
import com.example.tcc.model.Machine;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateMachineFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId;
    private EditText machineNameInput;
    private Button createButton;
    private FirebaseFirestore db;

    public static CreateMachineFragment newInstance(String buildingId, String spaceId) {
        CreateMachineFragment fragment = new CreateMachineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUILDING_ID, buildingId);
        args.putString(ARG_SPACE_ID, spaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_machine, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        machineNameInput = view.findViewById(R.id.editTextMachineName);
        createButton = view.findViewById(R.id.buttonCreateMachine);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
        }

        createButton.setOnClickListener(v -> createMachine());
    }

    private void createMachine() {
        String name = machineNameInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Informe o nome da máquina", Toast.LENGTH_SHORT).show();
            return;
        }

        // Criar ID automático
        String machineId = db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document()
                .getId();

        Machine machine = new Machine(machineId, name, "livre", spaceId);

        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("maquinas")
                .document(machineId)
                .set(machine)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "Máquina criada com sucesso!", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
