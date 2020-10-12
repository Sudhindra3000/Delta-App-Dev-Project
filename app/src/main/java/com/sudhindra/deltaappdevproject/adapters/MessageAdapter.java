package com.sudhindra.deltaappdevproject.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;
import com.sudhindra.deltaappdevproject.clients.FilesClient;
import com.sudhindra.deltaappdevproject.databinding.ChatProgressItemBinding;
import com.sudhindra.deltaappdevproject.databinding.ReceivedMessageFileItemBinding;
import com.sudhindra.deltaappdevproject.databinding.ReceivedMessageImageItemBinding;
import com.sudhindra.deltaappdevproject.databinding.ReceivedMessageTextItemBinding;
import com.sudhindra.deltaappdevproject.databinding.SentMessageFileItemBinding;
import com.sudhindra.deltaappdevproject.databinding.SentMessageImageItemBinding;
import com.sudhindra.deltaappdevproject.databinding.SentMessageTextItemBinding;
import com.sudhindra.deltaappdevproject.models.GlideApp;
import com.sudhindra.deltaappdevproject.models.Message;

import java.io.File;
import java.util.ArrayList;


public class MessageAdapter extends RecyclerView.Adapter {

    private static final int RIGHT_TEXT = 0, RIGHT_IMG = 2, RIGHT_FILE = 4, LEFT_TEXT = 5, LEFT_IMG = 6, LEFT_FILE = 7, PROGRESS_ITEM = 8;
    private ArrayList<Message> messages;
    private String currentUserId;
    private MessageListener listener;
    private boolean newMessage = false;

    private Uri newPhotoUri = null;

    private File channelFilesDir;

    public interface MessageListener {
        void onImageClicked(int pos, ImageView imageView);

        void onFileClicked(int pos);

        void onFileLongClicked(int pos);
    }

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setChannelFilesDir(File channelFilesDir) {
        this.channelFilesDir = channelFilesDir;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public void setNewMessage(boolean newMessage) {
        this.newMessage = newMessage;
    }

    public void setNewPhotoUri(Uri newPhotoUri) {
        this.newPhotoUri = newPhotoUri;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSentTimeInMillis() == -1)
            return PROGRESS_ITEM;
        if (message.getSenderId().equals(currentUserId)) {
            switch (message.getType()) {
                case Message.TEXT:
                    return RIGHT_TEXT;
                case Message.IMAGE:
                    return RIGHT_IMG;
                case Message.FILE:
                    return RIGHT_FILE;
            }
        }
        switch (message.getType()) {
            case Message.TEXT:
                return LEFT_TEXT;
            case Message.IMAGE:
                return LEFT_IMG;
            case Message.FILE:
                return LEFT_FILE;
            default:
                return 0;
        }
    }

    private class ViewHolderRightText extends RecyclerView.ViewHolder {

        private SentMessageTextItemBinding binding;

        public ViewHolderRightText(@NonNull SentMessageTextItemBinding sentMessageTextItemBinding) {
            super(sentMessageTextItemBinding.getRoot());
            binding = sentMessageTextItemBinding;
        }

        private void setDetails(int pos, Message message) {
            binding.sentMessageText.setText(message.getText());
            binding.sentMessageTime.setText(message.getTimeText());
            if (!newMessage)
                binding.sentMessageStatus.setText("Sent");
            else if (pos == 0)
                binding.sentMessageStatus.setText("Sending...");
        }
    }

    private class ViewHolderRightImg extends RecyclerView.ViewHolder {

        private SentMessageImageItemBinding binding;

        public ViewHolderRightImg(@NonNull SentMessageImageItemBinding sentMessageImageItemBinding, MessageListener listener) {
            super(sentMessageImageItemBinding.getRoot());
            binding = sentMessageImageItemBinding;
            binding.sentMessageImg.setOnClickListener(v -> listener.onImageClicked(getAdapterPosition(), binding.sentMessageImg));
        }

        private void setDetails(int pos, Message message) {
            if (pos != 0 || newPhotoUri == null)
                GlideApp.with(itemView)
                        .load(CloudStorageClient.getInstance().getMessageFileRef(message.getSentTimeInMillis()))
                        .fitCenter()
                        .placeholder(R.drawable.default_post_image)
                        .dontAnimate()
                        .into(binding.sentMessageImg);
            else
                binding.sentMessageImg.setImageURI(newPhotoUri);
            if (message.getText() != null && !message.getText().isEmpty())
                binding.sentMessageText.setText(message.getText());
            else
                binding.sentMessageText.setVisibility(View.GONE);
            binding.sentMessageTime.setText(message.getTimeText());
            if (!newMessage)
                binding.sentMessageStatus.setText("Sent");
            else if (pos == 0)
                binding.sentMessageStatus.setText("Sending...");
        }
    }

    private class ViewHolderRightFile extends RecyclerView.ViewHolder {

        private SentMessageFileItemBinding binding;

        public ViewHolderRightFile(@NonNull SentMessageFileItemBinding sentMessageFileItemBinding, MessageListener listener) {
            super(sentMessageFileItemBinding.getRoot());
            binding = sentMessageFileItemBinding;

            binding.sentMessageFileCard.setOnClickListener(v -> {
                if (binding.sentMessageFilePv.getVisibility() == View.GONE || messages.get(getAdapterPosition()).isUploading())
                    listener.onFileClicked(getAdapterPosition());
            });

            binding.sentMessageFileCard.setOnLongClickListener(v -> {
                if (binding.sentMessageFileDownloadBt.getVisibility() == View.GONE && binding.sentMessageFilePv.getVisibility() == View.GONE)
                    listener.onFileLongClicked(getAdapterPosition());
                return true;
            });
        }

        private void setDetails(int pos, Message message) {
            File file = FilesClient.getMessageFile(channelFilesDir, message);
            binding.sentMessageFileName.setText(message.getFileName());
            if (!file.exists()) {
                if (message.getFileProgress() >= 0) {
                    binding.sentMessageFileDownloadBt.setVisibility(View.GONE);
                    binding.sentMessageFilePv.setVisibility(View.VISIBLE);
                    binding.sentMessageFilePv.setProgress(message.getFileProgress());
                } else
                    binding.sentMessageFileDownloadBt.setVisibility(View.VISIBLE);
            } else {
                if (message.getFileProgress() >= 0) {
                    binding.sentMessageFileDownloadBt.setVisibility(View.GONE);
                    binding.sentMessageFilePv.setVisibility(View.VISIBLE);
                    binding.sentMessageFilePv.setProgressInTime(binding.sentMessageFilePv.getProgress(), message.getFileProgress(), 600);
                } else
                    binding.sentMessageFilePv.setVisibility(View.GONE);
            }
            if (message.getText() != null && !message.getText().isEmpty())
                binding.sentMessageText.setText(message.getText());
            else
                binding.sentMessageText.setVisibility(View.GONE);
            binding.sentMessageTime.setText(message.getTimeText());
            if (!newMessage)
                binding.sentMessageStatus.setText("Sent");
            else if (pos == 0)
                binding.sentMessageStatus.setText("Sending...");
        }
    }

    private static class ViewHolderLeftText extends RecyclerView.ViewHolder {

        private ReceivedMessageTextItemBinding binding;

        public ViewHolderLeftText(@NonNull ReceivedMessageTextItemBinding receivedMessageTextItemBinding) {
            super(receivedMessageTextItemBinding.getRoot());
            binding = receivedMessageTextItemBinding;
        }

        private void setDetails(Message message) {
            binding.receivedMessageText.setText(message.getText());
            binding.receivedMessageTime.setText(message.getTimeText());
        }
    }

    private static class ViewHolderLeftImg extends RecyclerView.ViewHolder {

        private ReceivedMessageImageItemBinding binding;

        public ViewHolderLeftImg(@NonNull ReceivedMessageImageItemBinding receivedMessageImageItemBinding, MessageListener listener) {
            super(receivedMessageImageItemBinding.getRoot());
            binding = receivedMessageImageItemBinding;
            binding.receivedMessageImg.setOnClickListener(v -> listener.onImageClicked(getAdapterPosition(), binding.receivedMessageImg));
        }

        private void setDetails(Message message) {
            GlideApp.with(itemView)
                    .load(CloudStorageClient.getInstance().getMessageFileRef(message.getSentTimeInMillis()))
                    .fitCenter()
                    .placeholder(R.drawable.default_post_image)
                    .dontAnimate()
                    .into(binding.receivedMessageImg);
            if (message.getText() != null && !message.getText().isEmpty())
                binding.receivedMessageText.setText(message.getText());
            else
                binding.receivedMessageText.setVisibility(View.GONE);
            binding.receivedMessageTime.setText(message.getTimeText());
        }
    }

    private class ViewHolderLeftFile extends RecyclerView.ViewHolder {

        private ReceivedMessageFileItemBinding binding;

        public ViewHolderLeftFile(@NonNull ReceivedMessageFileItemBinding receivedMessageFileItemBinding, MessageListener listener) {
            super(receivedMessageFileItemBinding.getRoot());
            binding = receivedMessageFileItemBinding;

            binding.receivedMessageFileCard.setOnClickListener(v -> {
                if (binding.recievedMessageFilePv.getVisibility() == View.GONE) {
                    listener.onFileClicked(getAdapterPosition());
                }
            });

            binding.receivedMessageFileCard.setOnLongClickListener(v -> {
                if (binding.receivedMessageFileDownloadBt.getVisibility() == View.GONE && binding.recievedMessageFilePv.getVisibility() == View.GONE)
                    listener.onFileLongClicked(getAdapterPosition());
                return true;
            });
        }

        private void setDetails(Message message) {
            File file = FilesClient.getMessageFile(channelFilesDir, message);
            binding.receivedMessageFileName.setText(message.getFileName());
            if (!file.exists()) {
                if (message.getFileProgress() >= 0) {
                    binding.receivedMessageFileDownloadBt.setVisibility(View.GONE);
                    binding.recievedMessageFilePv.setVisibility(View.VISIBLE);
                    binding.recievedMessageFilePv.setProgress(message.getFileProgress());
                } else
                    binding.receivedMessageFileDownloadBt.setVisibility(View.VISIBLE);
            } else {
                if (message.getFileProgress() >= 0) {
                    binding.receivedMessageFileDownloadBt.setVisibility(View.GONE);
                    binding.recievedMessageFilePv.setVisibility(View.VISIBLE);
                    binding.recievedMessageFilePv.setProgressInTime(binding.recievedMessageFilePv.getProgress(), message.getFileProgress(), 600);
                } else
                    binding.recievedMessageFilePv.setVisibility(View.GONE);
            }
            if (message.getText() != null && !message.getText().isEmpty())
                binding.receivedMessageText.setText(message.getText());
            else
                binding.receivedMessageText.setVisibility(View.GONE);
            binding.receivedMessageTime.setText(message.getTimeText());
        }
    }

    private static class ViewHolderProgress extends RecyclerView.ViewHolder {
        public ViewHolderProgress(@NonNull ChatProgressItemBinding chatProgressItemBinding) {
            super(chatProgressItemBinding.getRoot());
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case RIGHT_TEXT:
                SentMessageTextItemBinding binding = SentMessageTextItemBinding.inflate(inflater, parent, false);
                return new ViewHolderRightText(binding);
            case RIGHT_IMG:
                SentMessageImageItemBinding binding1 = SentMessageImageItemBinding.inflate(inflater, parent, false);
                return new ViewHolderRightImg(binding1, listener);
            case RIGHT_FILE:
                SentMessageFileItemBinding binding2 = SentMessageFileItemBinding.inflate(inflater, parent, false);
                return new ViewHolderRightFile(binding2, listener);
            case LEFT_TEXT:
                ReceivedMessageTextItemBinding binding3 = ReceivedMessageTextItemBinding.inflate(inflater, parent, false);
                return new ViewHolderLeftText(binding3);
            case LEFT_IMG:
                ReceivedMessageImageItemBinding binding4 = ReceivedMessageImageItemBinding.inflate(inflater, parent, false);
                return new ViewHolderLeftImg(binding4, listener);
            case LEFT_FILE:
                ReceivedMessageFileItemBinding binding5 = ReceivedMessageFileItemBinding.inflate(inflater, parent, false);
                return new ViewHolderLeftFile(binding5, listener);
            case PROGRESS_ITEM:
                ChatProgressItemBinding binding6 = ChatProgressItemBinding.inflate(inflater, parent, false);
                return new ViewHolderProgress(binding6);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        switch (getItemViewType(position)) {
            case RIGHT_TEXT:
                ((ViewHolderRightText) holder).setDetails(position, message);
                return;
            case RIGHT_IMG:
                ((ViewHolderRightImg) holder).setDetails(position, message);
                return;
            case RIGHT_FILE:
                ((ViewHolderRightFile) holder).setDetails(position, message);
                return;
            case LEFT_TEXT:
                ((ViewHolderLeftText) holder).setDetails(message);
                return;
            case LEFT_IMG:
                ((ViewHolderLeftImg) holder).setDetails(message);
                return;
            case LEFT_FILE:
                ((ViewHolderLeftFile) holder).setDetails(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
