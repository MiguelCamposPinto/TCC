package com.example.tcc.view.adapter;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tcc.R;
import com.example.tcc.model.ChatMessage;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Objects;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatMessage> messages;
    private String myId;

    public ChatAdapter(List<ChatMessage> messages, String myId) {
        this.messages = messages;
        this.myId = myId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        boolean isMine = msg.getSenderId().equals(myId);

        holder.name.setText(msg.getSenderName());
        holder.message.setText(msg.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        holder.timestamp.setText(sdf.format(new Date(msg.getTimestamp())));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.card.getLayoutParams();
        if (isMine) {
            params.gravity = Gravity.END;
            holder.card.setCardBackgroundColor(Color.parseColor("#90CAF9"));
        } else {
            params.gravity = Gravity.START;
            holder.card.setCardBackgroundColor(Color.LTGRAY);
        }
        holder.card.setLayoutParams(params);
    }



    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView name, message, timestamp;
        MaterialCardView card;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textSenderName);
            message = itemView.findViewById(R.id.textMessageBody);
            timestamp = itemView.findViewById(R.id.textTimestamp);
            card = itemView.findViewById(R.id.cardMessage);
        }
    }

}

