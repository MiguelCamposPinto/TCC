package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.tcc.R;
import com.example.tcc.model.Building;
import com.example.tcc.view.adapter.BuildingAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class AdminMainFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private BuildingAdapter adapter;
    private List<Building> buildingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_main, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewBuildings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BuildingAdapter(buildingList, getActivity().getSupportFragmentManager());
        recyclerView.setAdapter(adapter);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadAdminBuildings();

        return view;
    }

    private void loadAdminBuildings() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("predios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        buildingList.clear();
                        for (DocumentSnapshot snapshot : queryDocumentSnapshots.getDocuments()) {
                            String name = snapshot.getString("name");
                            String address = snapshot.getString("address");
                            String buildingId = snapshot.getId();

                            buildingList.add(new Building(buildingId, name, address));
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao carregar prédios", Toast.LENGTH_SHORT).show();
                });
    }
}

