package com.example.tcc.view.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.Quadra;
import com.example.tcc.view.adapter.QuadraAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class UserQuadrasFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId, spaceType;
    private FirebaseFirestore db;
    private final List<Quadra> quadraList = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    private RecyclerView recyclerView;
    private QuadraAdapter adapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
            spaceType = getArguments().getString("spaceType");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_quadras, container, false);

        recyclerView = view.findViewById(R.id.recyclerUserQuadras);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        db = FirebaseFirestore.getInstance();

        adapter = new QuadraAdapter(quadraList, quadra -> {
            NavController nav = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", spaceId);
            args.putString("quadrasId", quadra.getId());
            args.putString("spaceType", spaceType);
            nav.navigate(R.id.action_userQuadrasFragment_to_userScheduleQuadraFragment, args);
        });

        recyclerView.setAdapter(adapter);

        loadQuadras();

        return view;
    }

    private void loadQuadras() {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("quadras")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Erro ao carregar quadras", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    quadraList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Quadra q = doc.toObject(Quadra.class);
                        if (q != null) {
                            q.setId(doc.getId());
                            escutarStatusEmUso(q);
                        }
                    }
                });

        listeners.add(reg);
    }

    private void escutarStatusEmUso(Quadra quadra) {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("quadras")
                .document(quadra.getId())
                .collection("reservations")
                .whereEqualTo("status", "em_andamento")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;

                    boolean emUso = snapshot != null && !snapshot.isEmpty();
                    quadra.setStatus(emUso ? "em_uso" : "livre");

                    boolean jaExiste = false;
                    for (Quadra q : quadraList) {
                        if (q.getId().equals(quadra.getId())) {
                            q.setStatus(quadra.getStatus());
                            jaExiste = true;
                            break;
                        }
                    }
                    if (!jaExiste) quadraList.add(quadra);

                    adapter.notifyDataSetChanged();
                });

        listeners.add(reg);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration reg : listeners) reg.remove();
        listeners.clear();
    }
}
