package com.example.tcc.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private List<User> userList;
    private FirebaseFirestore db;

    public UsersAdapter(List<User> userList) {
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.nameText.setText(user.getName());
        holder.emailText.setText(user.getEmail());

        holder.removeButton.setOnClickListener(v -> {
            db.collection("users")
                    .document(user.getUid())
                    .update("buildingId", null)
                    .addOnSuccessListener(aVoid -> {
                        userList.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(holder.itemView.getContext(), "Usuário removido do prédio", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, emailText;
        Button removeButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.textUserName);
            emailText = itemView.findViewById(R.id.textUserEmail);
            removeButton = itemView.findViewById(R.id.buttonRemoveUser);
        }
    }
}
