package com.sudhindra.deltaappdevproject.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudhindra.deltaappdevproject.databinding.ChatChannelItemBinding;
import com.sudhindra.deltaappdevproject.utils.ChatChannel;

import java.util.ArrayList;

public class ChatFeedAdapter extends RecyclerView.Adapter<ChatFeedAdapter.ChatFeedViewHolder> {

    private ArrayList<ChatChannel> chatChannels;
    private String currentUserId, currentUsername;
    private ChatFeedListener listener;

    public ChatFeedAdapter(String currentUserId, String currentUsername) {
        this.currentUserId = currentUserId;
        this.currentUsername = currentUsername;
    }

    public void setChatChannels(ArrayList<ChatChannel> chatChannels) {
        this.chatChannels = chatChannels;
    }

    public void setListener(ChatFeedListener listener) {
        this.listener = listener;
    }

    public interface ChatFeedListener {
        void onItemClick(int pos);

        void onItemLongClick(int pos);
    }

    public static class ChatFeedViewHolder extends RecyclerView.ViewHolder {

        private ChatChannelItemBinding binding;

        public ChatFeedViewHolder(@NonNull ChatChannelItemBinding chatChannelItemBinding, ChatFeedListener listener) {
            super(chatChannelItemBinding.getRoot());
            binding = chatChannelItemBinding;
            binding.getRoot().setOnClickListener(view -> listener.onItemClick(getAdapterPosition()));
            binding.getRoot().setOnLongClickListener(view -> {
                listener.onItemLongClick(getAdapterPosition());
                return true;
            });
        }

        private void setDetails(ChatChannel chatChannel, String currentUserId, String currentUsername) {
            String otherUserId = "", otherUsername = "";
            for (String s : chatChannel.getUserIds()) {
                if (!s.equals(currentUserId)) {
                    otherUserId = s;
                    break;
                }
            }
            for (String s : chatChannel.getUserNames()) {
                if (!s.equals(currentUsername)) {
                    otherUsername = s;
                    break;
                }
            }

            binding.setOtherUserId(otherUserId);
            binding.setOtherUsername(otherUsername);
            binding.setChatChannel(chatChannel);
        }
    }

    @NonNull
    @Override
    public ChatFeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ChatChannelItemBinding binding = ChatChannelItemBinding.inflate(inflater, parent, false);
        return new ChatFeedViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatFeedViewHolder holder, int position) {
        holder.setDetails(chatChannels.get(position), currentUserId, currentUsername);
    }

    @Override
    public int getItemCount() {
        return chatChannels.size();
    }

}
