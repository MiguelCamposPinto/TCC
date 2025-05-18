package com.example.tcc.view.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserSelectBuildingFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private List<Building> buildingList = new ArrayList<>();
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
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                    .update("predioID", building.getId())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Prédio associado com sucesso!", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack(); // volta pra tela anterior
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Erro ao associar prédio", Toast.LENGTH_SHORT).show());
        });

        recyclerView.setAdapter(adapter);
        loadBuildings();

        return view;
    }

    private void loadBuildings() {
        db.collection("predios")
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
                            b.setId(id);
                            buildingList.add(b);
                            idsAdicionados.add(id);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

}
