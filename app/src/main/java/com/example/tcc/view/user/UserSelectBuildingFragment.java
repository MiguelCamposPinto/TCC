package com.example.tcc.view.user;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tcc.R;
import com.example.tcc.model.Building;
import com.example.tcc.view.adapter.UserBuildingAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserSelectBuildingFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private final List<Building> buildingList = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private UserBuildingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_select_building, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recyclerSelectBuildings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new UserBuildingAdapter(buildingList, building -> {
            abrirDialogSenha(building.getId());
        });

        recyclerView.setAdapter(adapter);
        loadBuildings();

        return view;
    }

    private void loadBuildings() {
        ListenerRegistration reg = db.collection("buildings")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Toast.makeText(getContext(), "Erro ao escutar prédios", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    buildingList.clear();
                    Set<String> idsAdicionados = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getId();
                        if (!idsAdicionados.contains(id)) {
                            Building b = doc.toObject(Building.class);
                            if (b != null) {
                                b.setId(id);
                                buildingList.add(b);
                                idsAdicionados.add(id);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });

        listeners.add(reg);
    }

    private void abrirDialogSenha(String buildingId) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_enter_password, null);

        EditText editSenha = dialogView.findViewById(R.id.editSenha);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();
        dialog.show();

        Button buttonConfirmar = dialogView.findViewById(R.id.buttonConfirmarSenha);
        Button buttonCancelar = dialogView.findViewById(R.id.buttonCancelarSenha);

        final int[] tentativas = {0};

        buttonConfirmar.setOnClickListener(v -> {
            String senhaDigitada = editSenha.getText().toString();
            db.collection("buildings").document(buildingId).get().addOnSuccessListener(doc -> {
                String senhaCorreta = doc.getString("password");

                if (senhaDigitada.equals(senhaCorreta)) {
                    String userId = auth.getCurrentUser().getUid();
                    db.collection("users").document(userId)
                            .update("buildingId", buildingId)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Prédio associado com sucesso!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao associar prédio", Toast.LENGTH_SHORT).show());
                } else {
                    tentativas[0]++;
                    if (tentativas[0] >= 3) {
                        Toast.makeText(getContext(), "Número de tentativas excedido", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Senha incorreta. Tentativa " + tentativas[0] + "/3", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        buttonCancelar.setOnClickListener(v -> dialog.dismiss());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration reg : listeners) {
            reg.remove();
        }
        listeners.clear();
    }
}
