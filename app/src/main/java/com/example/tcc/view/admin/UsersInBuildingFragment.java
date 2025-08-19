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

import com.example.tcc.R;
import com.example.tcc.model.User;
import com.example.tcc.view.adapter.UsersAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UsersInBuildingFragment extends Fragment {
    private RecyclerView recyclerView;
    private UsersAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String buildingId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users_in_building, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UsersAdapter(userList);
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            buildingId = getArguments().getString("buildingId");
            loadUsers();
        }

        return view;
    }

    private void loadUsers() {
        db.collection("users")
                .whereEqualTo("buildingId", buildingId)
                .whereEqualTo("type", "user")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        user.setUid(doc.getId());
                        userList.add(user);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
