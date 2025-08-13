package com.example.tcc.view.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcc.R;
import com.example.tcc.model.AuthService;


import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity implements LoginCallback {

    private EditText emailInput, passwordInput;
    private Button loginButton, buttonGoToRegistry;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.editTextEmail);
        passwordInput = findViewById(R.id.editTextSenha);
        loginButton = findViewById(R.id.buttonLogin);
        buttonGoToRegistry = findViewById(R.id.buttonGoToRegistry);

        authService = new AuthService();

        loginButton.setOnClickListener(view -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();

            } else {
                loginButton.setEnabled(false);
                authService.loginUser(email, password, LoginActivity.this,this);
            }
        });
        buttonGoToRegistry.setOnClickListener(view -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    public void onLoginFailure() {
        loginButton.setEnabled(true);
    }
}
