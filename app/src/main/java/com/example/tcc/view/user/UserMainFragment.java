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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.tcc.R;
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
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_nav_user_main_to_userSelectBuildingFragment);
        });

        btnEnter.setOnClickListener(v -> {
            if (currentBuildingId != null) {
                NavController navController = Navigation.findNavController(requireView());
                Bundle args = new Bundle();
                args.putString("buildingId", currentBuildingId);
                navController.navigate(R.id.action_nav_user_main_to_userSpacesFragment, args);
            }
        });
        loadUserBuilding();
        return view;
    }

    private void loadUserBuilding() {
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && snapshot.contains("buildingId")) {
                String buildingId = snapshot.getString("buildingId");
                if (buildingId != null && !buildingId.isEmpty()) {
                    db.collection("buildings").document(buildingId).get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String address = doc.getString("address");
                            textInfo.setText("Você está associado ao prédio:\n" + name + "\n" + address);
                            currentBuildingId = buildingId;
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

