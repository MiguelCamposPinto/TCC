package com.example.tcc.view.user;

import android.os.Bundle;
import android.util.Log;
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
import com.example.tcc.model.Spaces;
import com.example.tcc.view.adapter.SpacesAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class UserSpacesFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";

    private String buildingId;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private List<Spaces> spaceList = new ArrayList<>();
    private SpacesAdapter adapter;

    public static UserSpacesFragment newInstance(String buildingId) {
        UserSpacesFragment fragment = new UserSpacesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BUILDING_ID, buildingId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_spaces, container, false);
        recyclerView = view.findViewById(R.id.recyclerUserSpaces);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new SpacesAdapter(spaceList, space -> {
            Fragment frag = UserMachinesFragment.newInstance(buildingId, space.getId());
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.user_fragment_container, frag)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);
        Log.d("DEBUG", "Buscando espaços do prédio ID: " + buildingId);

        loadSpaces();

        return view;
    }

    private void loadSpaces() {
        db.collection("predios")
                .document(buildingId)
                .collection("spaces")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    spaceList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Spaces space = doc.toObject(Spaces.class);
                        space.setId(doc.getId());
                        space.setBuildingId(buildingId);
                        spaceList.add(space);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erro ao carregar espaços", Toast.LENGTH_SHORT).show();
                });
    }
}
