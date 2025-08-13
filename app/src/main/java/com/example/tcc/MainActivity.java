package com.example.tcc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tcc.view.admin.AdminMainActivity;
import com.example.tcc.view.auth.LoginActivity;
import com.example.tcc.view.auth.RegisterActivity;
import com.example.tcc.view.user.UserMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String tipo = documentSnapshot.getString("type");
                            if ("admin".equals(tipo)) {
                                startActivity(new Intent(this, AdminMainActivity.class));
                            } else {
                                startActivity(new Intent(this, UserMainActivity.class));
                            }
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Erro ao buscar usuário", e);
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
        }
    }

    public void register(View view) {
        startActivity(new Intent(MainActivity.this, RegisterActivity.class));

    }

    public void login(View view) {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }
}