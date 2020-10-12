package com.sudhindra.deltaappdevproject.models;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;

import com.google.firebase.auth.FirebaseAuth;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatChannel {

    public static final String USER_IDS = "userIds", LAST_MESSAGE = "lastMessage";
    private ArrayList<String> userIds;
    private ArrayList<String> userNames;
    private Message lastMessage;

    public ChatChannel() {
    }

    public ChatChannel(@NonNull ArrayList<String> userIds, @NonNull ArrayList<String> userNames, @Nullable Message lastMessage) {
        this.userIds = userIds;
        this.userNames = userNames;
        this.lastMessage = lastMessage;
    }

    public ArrayList<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(ArrayList<String> userIds) {
        this.userIds = userIds;
    }

    public ArrayList<String> getUserNames() {
        return userNames;
    }

    public void setUserNames(ArrayList<String> userNames) {
        this.userNames = userNames;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMessage = lastMessage;
    }

    @BindingAdapter("android:loadChatChannelProfile")
    public static void loadChatChannelProfile(CircleImageView circleImageView, String otherUserId) {
        GlideApp.with(circleImageView)
                .load(CloudStorageClient.getInstance().getProfileImgRef(otherUserId))
                .placeholder(R.drawable.default_profile)
                .into(circleImageView);
    }

    @BindingAdapter("android:setLastMessageText")
    public static void setLastMessageText(TextView textView, Message lastMessage) {
        if (lastMessage.getSenderId().equals(FirebaseAuth.getInstance().getUid())) {
            switch (lastMessage.getType()) {
                case Message.TEXT:
                    textView.setText("You: " + lastMessage.getText());
                    break;
                case Message.IMAGE:
                    textView.setText(lastMessage.getText().equals("") ? "You: \uD83D\uDCF8 Photo" : "You: \uD83D\uDCF8 " + lastMessage.getText());
                    break;
                case Message.FILE:
                    textView.setText("You: \uD83D\uDCC4 " + lastMessage.getFileName());
                    break;
            }
        } else {
            switch (lastMessage.getType()) {
                case Message.TEXT:
                    textView.setText(lastMessage.getText());
                    break;
                case Message.IMAGE:
                    textView.setText(lastMessage.getText().equals("") ? "\uD83D\uDCF8 Photo" : "\uD83D\uDCF8 " + lastMessage.getText());
                    break;
                case Message.FILE:
                    textView.setText("\uD83D\uDCC4 " + lastMessage.getFileName());
                    break;
            }
        }
    }
}
