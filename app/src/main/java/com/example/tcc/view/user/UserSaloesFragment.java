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
import com.example.tcc.model.Salao;
import com.example.tcc.view.adapter.MachineAdapter;
import com.example.tcc.view.adapter.SalaoAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class UserSaloesFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";

    private String buildingId, spaceId, spaceType;
    private FirebaseFirestore db;
    private final List<Salao> saloesList = new ArrayList<>();
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    private RecyclerView recyclerView;
    private SalaoAdapter adapter;

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
        View view = inflater.inflate(R.layout.fragment_user_saloes, container, false);

        recyclerView = view.findViewById(R.id.recyclerUserSaloes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        adapter = new SalaoAdapter(saloesList, salao -> {
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", spaceId);
            args.putString("saloesId", salao.getId());
            args.putString("spaceType", spaceType);
            navController.navigate(R.id.action_userSaloesFragment_to_userScheduleSalaoFragment, args);
        });

        recyclerView.setAdapter(adapter);

        loadSaloes();

        return view;
    }

    private void loadSaloes() {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("saloes")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Erro ao escutar salões", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saloesList.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Salao salao = doc.toObject(Salao.class); // Mesmo model
                        if (salao != null) {
                            salao.setId(doc.getId());
                            verificarDisponibilidadeSalao(salao);
                        }
                    }
                });

        listeners.add(reg);
    }

    private void verificarDisponibilidadeSalao(Salao salao) {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection("saloes")
                .document(salao.getId())
                .collection("reservations")
                .addSnapshotListener((snapshot, error) -> {
                    boolean emUso = snapshot != null && snapshot.getDocuments().stream().anyMatch(doc ->
                            "em_andamento".equals(doc.getString("status"))
                    );

                    salao.setStatus(emUso ? "em_uso" : "livre");

                    boolean jaExiste = false;
                    for (Salao s : saloesList) {
                        if (s.getId().equals(salao.getId())) {
                            s.setStatus(salao.getStatus());
                            jaExiste = true;
                            break;
                        }
                    }
                    if (!jaExiste) {
                        saloesList.add(salao);
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
