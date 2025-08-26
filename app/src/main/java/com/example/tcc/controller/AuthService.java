package com.example.tcc.controller;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.tcc.view.admin.AdminMainActivity;
import com.example.tcc.view.auth.LoginActivity;
import com.example.tcc.view.auth.LoginCallback;
import com.example.tcc.view.user.UserMainActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public AuthService() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    // CADASTRO
    public void registerUser(String email, String name, String password, String userType, Activity activity) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    // Estrutura inicial do doc users/{uid}
                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("uid", uid);
                    userDoc.put("name", name);
                    userDoc.put("email", email);
                    userDoc.put("type", userType);

                    if ("admin".equals(userType)) {
                        userDoc.put("buildings", Collections.emptyList()); // admin gerencia vários
                    } else {
                        userDoc.put("buildingId", ""); // user tem 1 prédio atual
                    }

                    db.collection("users").document(uid)
                            .set(userDoc, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(activity, "Cadastro realizado com sucesso", Toast.LENGTH_SHORT).show();
                                // Redireciona conforme o tipo (se preferir, pode voltar para LoginActivity)
                                redirectUser(activity, userType);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(activity, "Erro ao salvar dados: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(activity, "Erro ao registrar: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // LOGIN
    public void loginUser(String email, String password, Activity activity, LoginCallback callback) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Toast.makeText(activity, "Erro: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                callback.onLoginFailure();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(activity, "Erro: usuário nulo após login", Toast.LENGTH_LONG).show();
                callback.onLoginFailure();
                return;
            }

            final String uid = user.getUid();

            // Garante que o campo 'uid' existe no doc
            db.collection("users").document(uid)
                    .set(Collections.singletonMap("uid", uid), SetOptions.merge());

            // Anexa token ao usuário atual e remove de outros docs (chama a callable)
            attachTokenToCurrentUser(uid)
                    .addOnCompleteListener(__ -> {
                        // Após sincronizar token, descobre o tipo e redireciona
                        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                            String userType = documentSnapshot.getString("type");
                            redirectUser(activity, userType);
                        });
                    });
        });
    }

    // LOGOUT
    public void logout(Activity activity) {
        FirebaseUser current = auth.getCurrentUser();
        if (current == null) {
            goToLogin(activity);
            return;
        }
        String uid = current.getUid();

        // 1) Remove fcmToken do doc do usuário
        db.collection("users").document(uid)
                .update("fcmToken", FieldValue.delete())
                .addOnCompleteListener(t1 -> {
                    // 2) Invalida o token no device (importante ao trocar de conta)
                    FirebaseMessaging.getInstance().deleteToken()
                            .addOnCompleteListener(t2 -> {
                                auth.signOut();
                                goToLogin(activity);
                            });
                });
    }

    // ===== Helpers =====

    private void redirectUser(Activity activity, String userType) {
        Intent intent;
        if ("admin".equals(userType)) {
            Toast.makeText(activity, "ADMIN", Toast.LENGTH_SHORT).show();
            intent = new Intent(activity, AdminMainActivity.class);
        } else {
            Toast.makeText(activity, "USER", Toast.LENGTH_SHORT).show();
            intent = new Intent(activity, UserMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void goToLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private Task<?> attachTokenToCurrentUser(String uid) {
        return FirebaseMessaging.getInstance().getToken().onSuccessTask(token -> {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("token", token);

            return FirebaseFunctions.getInstance("southamerica-east1")
                    .getHttpsCallable("attachTokenToUser")
                    .call(data);
        });
    }
}
