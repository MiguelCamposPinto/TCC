package com.example.tcc.view.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.tcc.R;
import com.example.tcc.view.admin.BuildingDetailsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserMainFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView textInfo;
    private Button btnChoose, btnEnter;
    private String currentBuildingId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_main, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        textInfo = view.findViewById(R.id.textUserBuildingInfo);
        btnChoose = view.findViewById(R.id.buttonChooseBuilding);
        btnEnter = view.findViewById(R.id.buttonEnterBuilding);

        btnChoose.setOnClickListener(v -> {
            Fragment frag = new UserSelectBuildingFragment();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.user_fragment_container, frag)
                    .addToBackStack(null)
                    .commit();
        });

        btnEnter.setOnClickListener(v -> {
            if (currentBuildingId != null) {
                Fragment frag = BuildingDetailsFragment.newInstance(currentBuildingId);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.user_fragment_container, frag)
                        .addToBackStack(null)
                        .commit();
            }
        });

        loadUserBuilding();
        return view;
    }

    private void loadUserBuilding() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.contains("predioID")) {
                String predioID = snapshot.getString("predioID");
                if (predioID != null && !predioID.isEmpty()) {
                    db.collection("predios").document(predioID).get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String address = doc.getString("address");
                            textInfo.setText("Você está associado ao prédio:\n" + name + "\n" + address);
                            currentBuildingId = predioID;
                            btnEnter.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    textInfo.setText("Você ainda não está associado a nenhum prédio.");
                }
            }
        });
    }
}

