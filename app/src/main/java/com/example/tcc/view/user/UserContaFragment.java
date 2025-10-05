package com.example.tcc.view.user;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.tcc.R;
import com.example.tcc.controller.AuthService;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class UserContaFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ImageView imageProfile;
    private EditText editName;
    private TextView textEmail;
    private Button buttonChangePhoto, buttonSave, btnLogout, btnExcluirConta;
    private Uri selectedImageUri;
    private final int PICK_IMAGE_REQUEST = 1;

    private final String defaultImageUrl = "https://firebasestorage.googleapis.com/profile_pictures/default_user.png";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_conta, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        imageProfile = view.findViewById(R.id.imageProfile);
        editName = view.findViewById(R.id.editName);
        textEmail = view.findViewById(R.id.textEmail);
        buttonChangePhoto = view.findViewById(R.id.buttonChangePhoto);
        buttonSave = view.findViewById(R.id.buttonSave);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnExcluirConta = view.findViewById(R.id.btnExcluirConta);

        loadUserInfo();

        buttonChangePhoto.setOnClickListener(v -> openGallery());
        buttonSave.setOnClickListener(v -> saveChanges());
        btnLogout.setOnClickListener(v -> new AuthService().logout(requireActivity()));
        btnExcluirConta.setOnClickListener(v -> showDeleteAccountDialog());


        return view;
    }

    private void loadUserInfo() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String photoUrl = doc.getString("photoUrl");

                editName.setText(name);
                textEmail.setText(email);

                Glide.with(this)
                        .load(photoUrl != null ? photoUrl : defaultImageUrl)
                        .placeholder(R.drawable.default_user)
                        .into(imageProfile);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageProfile.setImageURI(selectedImageUri);
        }
    }

    private void saveChanges() {
        String userId = auth.getCurrentUser().getUid();
        String name = editName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Nome não pode ser vazio", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference("profile_pictures/" + userId + ".jpg");

            storageRef.putFile(selectedImageUri)
                    .continueWithTask(task -> storageRef.getDownloadUrl())
                    .addOnSuccessListener(uri -> updateUser(userId, name, uri.toString()));

        } else {
            updateUser(userId, name, null);
        }
    }

    private void updateUser(String userId, String name, @Nullable String photoUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);

        if (photoUrl != null) {
            updates.put("photoUrl", photoUrl);
        }

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), "Atualizado com sucesso", Toast.LENGTH_SHORT).show();
                    }})
                .addOnFailureListener(e -> {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), "Erro ao atualizar", Toast.LENGTH_SHORT).show();
                }});
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Excluir Conta");

        final EditText input = new EditText(requireContext());
        input.setHint("Digite sua senha");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Excluir", (dialog, which) -> {
            String senha = input.getText().toString().trim();
            if (!senha.isEmpty()) {
                deletarContaComSenha(senha);
            } else {
                Toast.makeText(getContext(), "Digite sua senha", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            // Fundo azul e texto branco nos dois botões
            positiveButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
            negativeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
            positiveButton.setTextColor(Color.WHITE);
            negativeButton.setTextColor(Color.WHITE);
        });

        dialog.show();
    }


    private void deletarContaComSenha(String senha) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String email = user.getEmail();
        AuthCredential credential = EmailAuthProvider.getCredential(email, senha);

        user.reauthenticate(credential).addOnSuccessListener(unused -> {
            String uid = user.getUid();

                FirebaseStorage.getInstance().getReference("profile_pictures/" + uid + ".jpg").delete();

            FirebaseFirestore.getInstance().collection("users").document(uid).delete();

            user.delete().addOnSuccessListener(unused2 -> {
                Toast.makeText(getContext(), "Conta excluída com sucesso", Toast.LENGTH_LONG).show();
                new AuthService().logout(requireActivity());
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Erro ao excluir conta: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Senha incorreta", Toast.LENGTH_SHORT).show()
        );
    }


}