package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Space;

import com.example.tcc.R;
import com.example.tcc.model.Spaces;
import com.example.tcc.view.adapter.SpacesAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SpacesListFragment extends Fragment {

    private RecyclerView recyclerView;
    private SpacesAdapter adapter;
    private List<Spaces> spaceList = new ArrayList<>();
    private FirebaseFirestore db;
    private String buildingId;

    public SpacesListFragment(String buildingId) {
        this.buildingId = buildingId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_space_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerSpaces);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SpacesAdapter(spaceList);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        loadSpaces();
        return view;
    }

    private void loadSpaces() {
        db.collection("spaces").whereEqualTo("buildingId", buildingId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    spaceList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        spaceList.add(doc.toObject(Spaces.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
