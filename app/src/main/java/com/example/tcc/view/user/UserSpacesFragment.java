package com.example.tcc.view.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tcc.R;
import com.example.tcc.model.Spaces;
import com.example.tcc.view.adapter.SpacesAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UserSpacesFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";

    private String buildingId;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private Button buttonChat;
    private final List<Spaces> spaceList = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private SpacesAdapter adapter;

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

        buttonChat = view.findViewById(R.id.buttonChat);

        db = FirebaseFirestore.getInstance();

        adapter = new SpacesAdapter(spaceList, space -> {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", space.getId());
            args.putString("spaceType", space.getType());
            if (Objects.equals(space.getType(), "lavanderias")){
                navController.navigate(R.id.action_userSpacesFragment_to_userMachinesFragment, args);
            } else if(Objects.equals(space.getType(), "quadras")) {
                navController.navigate(R.id.action_userSpacesFragment_to_userQuadrasFragment, args);
            } else if(Objects.equals(space.getType(), "saloes")) {
                navController.navigate(R.id.action_userSpacesFragment_to_userSaloesFragment, args);
            }
        });

        buttonChat.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            Navigation.findNavController(requireView()).navigate(R.id.action_userSpacesFragment_to_chatFragment, args);
        });

        recyclerView.setAdapter(adapter);
        Log.d("DEBUG", "Buscando espaços do prédio ID: " + buildingId);

        loadSpaces();

        return view;
    }

    private void loadSpaces() {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        Toast.makeText(getContext(), "Erro ao escutar espaços", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    spaceList.clear();
                    Set<String> idsAdicionados = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!idsAdicionados.contains(doc.getId())) {
                            Spaces space = doc.toObject(Spaces.class);
                            if (space != null) {
                                space.setId(doc.getId());
                                space.setBuildingId(buildingId);
                                spaceList.add(space);
                                idsAdicionados.add(doc.getId());
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });

        listeners.add(reg);
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
