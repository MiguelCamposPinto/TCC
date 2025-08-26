package com.example.tcc.view.auth;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcc.R;
import com.example.tcc.controller.AuthService;


public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextSenha, editTextNome;
    private RadioGroup radioGroupTipo;
    private Button buttonCadastrar, buttonGoToLogin;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextSenha = findViewById(R.id.editTextSenha);
        editTextNome = findViewById(R.id.editTextNome);
        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        radioGroupTipo = findViewById(R.id.radioGroupTipo);
        buttonGoToLogin = findViewById(R.id.buttonGoToLogin);

        authService = new AuthService();

        radioGroupTipo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioButtonAdmin) {
                editTextNome.setText("Admin");
                editTextNome.setEnabled(false);
            } else {
                editTextNome.setText("");
                editTextNome.setEnabled(true);
            }
        });


        buttonCadastrar.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString();
            String password = editTextSenha.getText().toString();
            String name =  editTextNome.getText().toString();
            int selectedId = radioGroupTipo.getCheckedRadioButtonId();
            String userType = (selectedId == R.id.radioButtonAdmin) ? "admin" : "user";

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
            } else {
                authService.registerUser(email, name, password, userType, RegisterActivity.this);
            }
        });
        buttonGoToLogin.setOnClickListener(view -> startActivity(new Intent(this, LoginActivity.class)));
    }
}
