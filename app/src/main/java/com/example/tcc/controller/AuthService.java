package com.example.tcc.controller;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.tcc.model.User;
import com.example.tcc.view.admin.AdminMainActivity;
import com.example.tcc.view.auth.LoginActivity;
import com.example.tcc.view.auth.LoginCallback;
import com.example.tcc.view.user.UserMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void registerUser(String email, String name, String password, String userType, Activity activity) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    User user = new User(uid, name, email, "", userType); // buildingId vazio

                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(activity, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(activity, LoginActivity.class);
                                redirectUser(activity, userType);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(activity, "Erro ao salvar dados: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(activity, "Erro ao registrar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }



    public void loginUser(String email, String password, Activity activity, LoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    db.collection("users").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                        String userType = documentSnapshot.getString("type");
                        redirectUser(activity, userType);
                    });
                }
            } else {
                Toast.makeText(activity, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                callback.onLoginFailure();
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

