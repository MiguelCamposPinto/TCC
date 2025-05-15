package com.example.tcc.model;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.tcc.MainActivity;
import com.example.tcc.view.admin.AdminMainActivity;
import com.example.tcc.view.auth.LoginActivity;
import com.example.tcc.view.user.UserMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void registerUser(String email, String name, String password, String userType, Activity activity) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                Map<String, Object> data = new HashMap<>();
                data.put("name", name);
                data.put("email", user.getEmail());
                data.put("type", userType);

                db.collection("users").document(user.getUid()).set(data).addOnSuccessListener(unused -> {
                    Toast.makeText(activity, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show();
                    redirectUser(activity, userType);
                });
            } else {
                Toast.makeText(activity, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    public void loginUser(String email, String password, Activity activity) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                db.collection("users").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                    String userType = documentSnapshot.getString("type");
                    redirectUser(activity, userType);
                });
            } else {
                Toast.makeText(activity, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void logout(Activity activity) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    private void redirectUser(Activity activity, String userType) {
        Intent intent;
        if ("admin".equals(userType)) {
            Toast.makeText(activity, "ADMIN", Toast.LENGTH_LONG).show();
            intent = new Intent(activity, AdminMainActivity.class);
        } else {
            Toast.makeText(activity, "USER", Toast.LENGTH_LONG).show();
            intent = new Intent(activity, UserMainActivity.class);
        }
        activity.startActivity(intent);
        activity.finish();
    }
}

