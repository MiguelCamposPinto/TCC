package com.example.tcc.view;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.tcc.R;
import com.example.tcc.model.ChatMessage;
import com.example.tcc.view.adapter.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private EditText editText;
    private ImageButton sendButton;
    private ChatAdapter adapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private String buildingId;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewChat);
        editText = view.findViewById(R.id.editTextMessage);
        sendButton = view.findViewById(R.id.buttonSend);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        buildingId = getArguments().getString("buildingId");

        adapter = new ChatAdapter(messages, auth.getUid());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> enviarMensagem());

        escutarMensagens();
        return view;
    }

    private void enviarMensagem() {
        String texto = editText.getText().toString().trim();
        if (texto.isEmpty()) return;

        String userId = auth.getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String userName = doc.getString("name");
                ChatMessage msg = new ChatMessage(
                        UUID.randomUUID().toString(),
                        userId,
                        userName,
                        texto,
                        System.currentTimeMillis()
                );

        db.collection("buildings")
                .document(buildingId)
                .collection("chat")
                .document(msg.getId())
                .set(msg);
            }
        });

        editText.setText("");
    }

    private void escutarMensagens() {
        db.collection("buildings")
                .document(buildingId)
                .collection("chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    messages.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        messages.add(msg);
                    }
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messages.size() - 1);
                });
    }
}
