package com.example.tcc.view.admin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.tcc.R;
import com.example.tcc.model.Machine;
import com.example.tcc.model.Quadra;
import com.example.tcc.model.Resource;
import com.example.tcc.model.Salao;
import com.example.tcc.view.adapter.GenericAdapter;
import com.example.tcc.view.adapter.MachineAdapter;
import com.example.tcc.view.adapter.QuadraAdapter;
import com.example.tcc.view.adapter.SalaoAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SpacesListFragment extends Fragment {

    private static final String ARG_BUILDING_ID = "buildingId";
    private static final String ARG_SPACE_ID = "spaceId";
    private static final String ARG_SPACE_TYPE = "type";

    private String buildingId, spaceId, spaceType, place;
    private TextView spaceName;
    private Button buttonAddMachine;
    private RecyclerView recyclerMachines;

    private List<Resource> resourceList = new ArrayList<>();
    private GenericAdapter resourceAdapter;


    private FirebaseFirestore db;
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_space_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        spaceName = view.findViewById(R.id.textSpaceName);
        buttonAddMachine = view.findViewById(R.id.buttonAddMachine);
        recyclerMachines = view.findViewById(R.id.recyclerMachines);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            buildingId = getArguments().getString(ARG_BUILDING_ID);
            spaceId = getArguments().getString(ARG_SPACE_ID);
            spaceType = getArguments().getString(ARG_SPACE_TYPE);
        }

        recyclerMachines.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerMachines.setAdapter((RecyclerView.Adapter) resourceAdapter);

        buttonAddMachine.setOnClickListener(v -> {
            if (spaceType == null) {
                return;
            }
            NavController navController = Navigation.findNavController(requireView());
            Bundle args = new Bundle();
            args.putString("buildingId", buildingId);
            args.putString("spaceId", spaceId);
            args.putString("spaceType", spaceType);
            switch (spaceType) {
                case "lavanderias":
                    navController.navigate(R.id.action_spacesListFragment_to_createMachineFragment, args);
                    break;
                case "quadras":
                    navController.navigate(R.id.action_spacesListFragment_to_createQuadraFragment, args);
                    break;
                case "saloes":
                    navController.navigate(R.id.action_spacesListFragment_to_createSalaoFragment, args);
                    break;
            }
        });

        loadSpaceInfo();
    }

    private void loadSpaceInfo() {
        db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        spaceName.setText(doc.getString("name"));
                        switch (Objects.requireNonNull(spaceType)) {
                            case "lavanderias":
                                place = "machines";
                                break;
                            case "quadras":
                                place = "quadras";
                                break;
                            case "saloes":
                                place = "saloes";
                                break;
                        }
                        loadMachines();
                    }
                });
    }

    private void loadMachines() {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection(place)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;

                    List<Resource> updatedList = new ArrayList<>();
                    Set<String> idsAdicionados = new HashSet<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Resource resource = parseResource(doc);
                        if (resource == null) continue;

                        if (!idsAdicionados.contains(resource.getId())) {
                            idsAdicionados.add(resource.getId());
                            updatedList.add(resource);
                            verificarStatusEmTempoReal(resource);
                        }
                    }

                    resourceList = updatedList;
                    atualizarAdapter(resourceList);
                });

        listeners.add(reg);

    }

    private void verificarStatusEmTempoReal(Resource resource) {
        ListenerRegistration reg = db.collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .document(spaceId)
                .collection(place)
                .document(resource.getId())
                .collection("reservations")
                .whereEqualTo("status", "em_andamento")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;

                    boolean emUso = snapshot != null && !snapshot.isEmpty();
                    resource.setStatus(emUso ? "em_uso" : "livre");

                    for (Resource r : resourceList) {
                        if (r.getId().equals(resource.getId())) {
                            r.setStatus(resource.getStatus());
                            break;
                        }
                    }

                    if (resourceAdapter != null)
                        resourceAdapter.notifyDataSetChanged();
                });

        listeners.add(reg);
    }


    private void atualizarAdapter(List<? extends Resource> list) {
        switch (spaceType) {
            case "lavanderias":
                List<Machine> mList = castList(list, Machine.class);
                resourceAdapter = new MachineAdapter(mList, machine -> {
                    callAgendamentos(machine.getId());
                });
                break;
            case "quadras":
                List<Quadra> qList = castList(list, Quadra.class);
                resourceAdapter = new QuadraAdapter(qList, quadra -> {
                    callAgendamentos(quadra.getId());
                });
                break;
            case "saloes":
                List<Salao> sList = castList(list, Salao.class);
                resourceAdapter = new SalaoAdapter(sList, salao -> {
                    callAgendamentos(salao.getId());
                });
                break;
        }

        recyclerMachines.setAdapter((RecyclerView.Adapter<?>) resourceAdapter);
    }

    private void callAgendamentos(String id) {
        Fragment frag = AdminAgendamentosFragment.newInstance(buildingId, spaceId, id, place);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.admin_fragment_container, frag)
                .addToBackStack(null)
                .commit();
    }


    private Resource parseResource(DocumentSnapshot doc) {
        Resource resource = null;

        switch (spaceType) {
            case "lavanderias":
                resource = doc.toObject(Machine.class);
                break;
            case "quadras":
                resource = doc.toObject(Quadra.class);
                break;
            case "saloes":
                resource = doc.toObject(Salao.class);
                break;
        }

        if (resource != null) {
            resource.setId(doc.getId());
            resource.setStatus("livre");
        }

        return resource;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> castList(List<?> list, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (Object o : list) {
            if (clazz.isInstance(o)) {
                result.add((T) o);
            }
        }
        return result;
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

