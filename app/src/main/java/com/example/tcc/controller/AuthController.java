package com.example.tcc.controller;

import android.app.Activity;
import android.content.Intent;

import com.example.tcc.MainActivity;
import com.example.tcc.view.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AuthController {

    private final Activity activity;

    public AuthController(Activity activity) {
        this.activity = activity;
    }

    public void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
