package com.example.tcc.view.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.tcc.R;
import com.example.tcc.controller.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminContaFragment extends Fragment {

    private Button btnLogout;
    private AuthService authService;
    private LinearLayout buildingInfoContainer;

    public AdminContaFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_conta, container, false);

        authService = new AuthService();
        btnLogout = view.findViewById(R.id.btnLogout);
        buildingInfoContainer = view.findViewById(R.id.buildingInfoContainer);

        btnLogout.setOnClickListener(v -> authService.logout(requireActivity()));

        String adminId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadAdminBuildings(adminId);

        return view;
    }

    private void loadAdminBuildings(String adminId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("buildings")
                .whereEqualTo("adminId", adminId)
                .get()
                .addOnSuccessListener(buildingSnapshots -> {
                    for (DocumentSnapshot buildingDoc : buildingSnapshots) {
                        String name = buildingDoc.getString("name");
                        String address = buildingDoc.getString("address");
                        String buildingId = buildingDoc.getId();

                        countUsersInBuilding(buildingId, userCount -> {
                            countSpacesInBuilding(buildingId, spaceCount -> {
                                TextView info = new TextView(requireContext());
                                info.setText(
                                        "🏢 Prédio: " + name +
                                                "\n📍 Endereço: " + address +
                                                "\n👥 Moradores: " + userCount +
                                                "\n📌 Espaços: " + spaceCount
                                );
                                info.setPadding(0, 32, 0, 32);
                                info.setTextSize(16);
                                buildingInfoContainer.addView(info);
                            });
                        });
                    }
                });
    }

    private void countUsersInBuilding(String buildingId, OnUserCountReady callback) {
        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("buildingId", buildingId)
                .whereEqualTo("type", "user")
                .get()
                .addOnSuccessListener(snapshot -> callback.onReady(snapshot.size()));
    }

    private void countSpacesInBuilding(String buildingId, OnSpaceCountReady callback) {
        FirebaseFirestore.getInstance()
                .collection("buildings")
                .document(buildingId)
                .collection("spaces")
                .get()
                .addOnSuccessListener(snapshot -> callback.onReady(snapshot.size()));
    }

    interface OnUserCountReady {
        void onReady(int count);
    }

    interface OnSpaceCountReady {
        void onReady(int count);
    }

}
