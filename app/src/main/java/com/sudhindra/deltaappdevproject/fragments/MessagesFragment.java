package com.sudhindra.deltaappdevproject.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.Query;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.ChatActivity;
import com.sudhindra.deltaappdevproject.activities.NewChatActivity;
import com.sudhindra.deltaappdevproject.adapters.ChatFeedAdapter;
import com.sudhindra.deltaappdevproject.clients.FilesClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentMessagesBinding;
import com.sudhindra.deltaappdevproject.models.ChatChannel;
import com.sudhindra.deltaappdevproject.models.Message;
import com.sudhindra.deltaappdevproject.models.Student;
import com.sudhindra.deltaappdevproject.utils.GsonUtil;

import java.io.IOException;
import java.util.ArrayList;

public class MessagesFragment extends Fragment {

    private static final String TAG = "MessagesFragment";
    private final int chatFeedLimit = 10;
    private FragmentMessagesBinding binding;

    // Data
    private FirebaseAuth mAuth;
    private Student student;
    private ArrayList<String> channelIds = new ArrayList<>();
    private ArrayList<ChatChannel> chatChannels = new ArrayList<>();
    private ChatFeedAdapter chatFeedAdapter;
    private boolean noActiveChatChannels = true;
    private CollectionReference chatChannelsCollection;
    private DocumentSnapshot lastChannel = null;

    private boolean loading = true, refreshing = false, endOfCollection = false, firstTime = true;

    private ViewTreeObserver.OnScrollChangedListener scrollChangedListener = () -> {
        View view = binding.messagesScrollView.getChildAt(binding.messagesScrollView.getChildCount() - 1);

        int diff = (view.getBottom() - (binding.messagesScrollView.getHeight() + binding.messagesScrollView.getScrollY()));

        if (diff == 0 && loading && !refreshing) {
            loading = false;
            Log.i(TAG, "onScrolled: LastItem");
            paginate();
        }
    };

    private ChatFeedAdapter.ChatFeedListener listener = new ChatFeedAdapter.ChatFeedListener() {
        @Override
        public void onItemClick(int pos) {
            showMessages(pos);
        }

        @Override
        public void onItemLongClick(int pos) {
            showChatOptions(pos);
        }
    };

    public void onReselected() {
        if (!noActiveChatChannels)
            binding.messagesScrollView.smoothScrollTo(0, 0);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        chatChannelsCollection = FirestoreClient.getInstance().getChatChannelCollection();
        FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    student = documentSnapshot.toObject(Student.class);
                    buildRecyclerView();
                    checkForActiveChatChannels();
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onCreate: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to Load Messages", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.messagesRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));

        binding.messagesToolbar.setOnClickListener(v -> onReselected());
        binding.newChatButton.setOnClickListener(v -> showNewChatScreen());

        binding.messagesToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete_message_files) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Warning!")
                        .setMessage("Are you sure you want to delete All Message Files")
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            try {
                                FilesClient.deleteAllMessageFiles(requireContext());
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(requireContext(), "Failed to Delete Files", Toast.LENGTH_SHORT).show();
                            }
                            Toast.makeText(requireContext(), "All Message Files are Deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("CANCEL", (dialogInterface, i) -> {

                        })
                        .show();
            }
            return true;
        });
    }

    private void buildRecyclerView() {
        chatFeedAdapter = new ChatFeedAdapter(mAuth.getUid(), student.getFullName());
        chatFeedAdapter.setChatChannels(chatChannels);
        chatFeedAdapter.setListener(listener);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());

        binding.chatChannelsRecyclerView.setHasFixedSize(true);
        binding.chatChannelsRecyclerView.setAdapter(chatFeedAdapter);
        binding.chatChannelsRecyclerView.setLayoutManager(layoutManager);

        binding.messagesScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);

        binding.messagesRefreshLayout.setOnRefreshListener(this::refresh);
    }

    private void refresh() {
        Log.i(TAG, "refresh: ");
        refreshing = true;
        if (!noActiveChatChannels) {
            endOfCollection = false;
            lastChannel = null;
            firstTime = true;
            chatChannels.clear();
            channelIds.clear();
            chatFeedAdapter = new ChatFeedAdapter(mAuth.getUid(), student.getFullName());
            chatFeedAdapter.setChatChannels(chatChannels);
            chatFeedAdapter.setListener(listener);
            getChatChannels();
        } else {
            checkForActiveChatChannels();
        }
    }

    private void checkForActiveChatChannels() {
        FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid())
                .collection(ChatActivity.ENGAGED_CHAT_CHANNELS)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.getDocuments().isEmpty()) {
                        noActiveChatChannels = true;
                        binding.messagesFragPb.setVisibility(View.GONE);
                        binding.messagesIllustration.setVisibility(View.VISIBLE);
                        binding.messagesHelperText.setVisibility(View.VISIBLE);
                        if (refreshing) {
                            refreshing = false;
                            binding.messagesRefreshLayout.setRefreshing(false);
                        } else {
                            listenForChangeInChatChannels();
                        }
                        return;
                    }

                    noActiveChatChannels = false;
                    binding.messagesIllustration.setVisibility(View.GONE);
                    binding.messagesHelperText.setVisibility(View.GONE);
                    binding.messagesFragPb.setVisibility(View.VISIBLE);
                    getChatChannels();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to Load Messages", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "checkForActiveChatChannels: " + e.getMessage());
                });
    }

    private void listenForChangeInChatChannels() {
        if (mAuth.getUid() != null) {
            Query query;
            query = chatChannelsCollection.whereArrayContains(ChatChannel.USER_IDS, mAuth.getUid())
                    .orderBy(FieldPath.of(ChatChannel.LAST_MESSAGE, Message.SENT_TIME_IN_MILLIS), Query.Direction.DESCENDING)
                    .limit(1);
            query.addSnapshotListener((queryDocumentSnapshots, e) -> {
                if (e != null) {
                    Log.i(TAG, "listenForChangeInChatChannels: " + e.getMessage());
                    return;
                }
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    String channelId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    ChatChannel chatChannel = queryDocumentSnapshots.toObjects(ChatChannel.class).get(0);
                    if (chatChannels.isEmpty()) {
                        Log.i(TAG, "listenForChangeInChatChannels: newChatChannel added");
                        noActiveChatChannels = false;
                        endOfCollection = true;
                        chatChannels.add(chatChannel);
                        channelIds.add(channelId);
                        binding.messagesIllustration.setVisibility(View.GONE);
                        binding.messagesHelperText.setVisibility(View.GONE);
                        binding.chatChannelsRecyclerView.setVisibility(View.VISIBLE);
                        chatFeedAdapter.notifyDataSetChanged();
                    }
                    if (!firstTime) {
                        binding.chatChannelsRecyclerView.setHasFixedSize(false);
                        if (channelId.equals(channelIds.get(0))) {
                            chatChannels.set(0, chatChannel);
                            chatFeedAdapter.notifyItemChanged(0);
                            binding.chatChannelsRecyclerView.setHasFixedSize(true);
                            return;
                        }
                        if (channelIds.contains(channelId)) {
                            int oldIndex = channelIds.indexOf(channelId);
                            channelIds.remove(channelId);
                            channelIds.add(0, channelId);
                            chatChannels.remove(oldIndex);
                            chatChannels.add(0, chatChannel);
                            chatFeedAdapter.notifyItemMoved(oldIndex, 0);
                            chatFeedAdapter.notifyItemChanged(0);
                            binding.chatChannelsRecyclerView.setHasFixedSize(true);
                            return;
                        }
                        channelIds.add(0, channelId);
                        chatChannels.add(0, chatChannel);
                        chatFeedAdapter.notifyItemInserted(0);
                        binding.chatChannelsRecyclerView.setHasFixedSize(true);
                    } else firstTime = false;
                }
            });
        }
    }

    private void paginate() {
        getChatChannels();
    }

    private void getChatChannels() {
        if (!endOfCollection && mAuth.getUid() != null) {
            Query query;
            if (lastChannel != null)
                query = chatChannelsCollection.whereArrayContains(ChatChannel.USER_IDS, mAuth.getUid())
                        .orderBy(FieldPath.of(ChatChannel.LAST_MESSAGE, Message.SENT_TIME_IN_MILLIS), Query.Direction.DESCENDING)
                        .startAfter(lastChannel)
                        .limit(chatFeedLimit);
            else
                query = chatChannelsCollection.whereArrayContains(ChatChannel.USER_IDS, mAuth.getUid())
                        .orderBy(FieldPath.of(ChatChannel.LAST_MESSAGE, Message.SENT_TIME_IN_MILLIS), Query.Direction.DESCENDING)
                        .limit(chatFeedLimit);
            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        chatChannels.addAll(queryDocumentSnapshots.toObjects(ChatChannel.class));
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                            channelIds.add(documentSnapshot.getId());
                        if (!refreshing) {
                            binding.chatChannelsRecyclerView.setHasFixedSize(false);
                            chatFeedAdapter.notifyItemRangeInserted(chatChannels.size() - queryDocumentSnapshots.size(), queryDocumentSnapshots.size() + 1);
                            binding.chatChannelsRecyclerView.setHasFixedSize(true);
                        } else {
                            refreshing = false;
                            binding.messagesRefreshLayout.setRefreshing(false);
                            binding.chatFeedPb.setVisibility(View.VISIBLE);
                            binding.chatChannelsRecyclerView.setAdapter(chatFeedAdapter);
                            chatFeedAdapter.notifyDataSetChanged();
                        }

                        loading = true;
                        if (lastChannel == null) {
                            listenForChangeInChatChannels();
                            binding.messagesFragPb.setVisibility(View.GONE);
                            binding.chatFeedPb.setVisibility(View.VISIBLE);
                        }
                        if (queryDocumentSnapshots.size() == chatFeedLimit) {
                            lastChannel = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                            return;
                        }
                        Log.i(TAG, "getChatChannels: endOfCollection");
                        endOfCollection = true;
                        binding.chatFeedPb.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "getChatChannels: " + e.getMessage());
                        Toast.makeText(requireContext(), "Failed to Load Messages", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showMessages(int pos) {
        ChatChannel chatChannel = chatChannels.get(pos);
        String otherUserId = "", otherUsername = "";
        for (String s : chatChannel.getUserIds()) {
            if (!s.equals(mAuth.getUid())) {
                otherUserId = s;
                break;
            }
        }
        for (String s : chatChannel.getUserNames()) {
            if (!s.equals(student.getFullName())) {
                otherUsername = s;
                break;
            }
        }
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("currentUserName", student.getFullName());
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserName", otherUsername);
        intent.putExtra(ChatActivity.CHANNEL_MODE, ChatActivity.CHANNEL_EXISTS);
        intent.putExtra(ChatActivity.CHANNEL_ID, channelIds.get(pos));
        startActivity(intent);
    }

    private void showChatOptions(int pos) {
        String channelId = channelIds.get(pos);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chat Options")
                .setItems(R.array.chatOptions, (dialogInterface, i) -> {
                    switch (i) {
                        case 0:
                            try {
                                FilesClient.deleteMessageFilesInChatChannel(channelId, requireContext());
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(requireContext(), "Failed to Delete Files", Toast.LENGTH_SHORT).show();
                            }
                            Toast.makeText(requireContext(), "All Files in the Chat are Deleted", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showNewChatScreen() {
        Intent intent = new Intent(requireContext(), NewChatActivity.class);
        intent.putExtra("student", GsonUtil.toJson(student));
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.messagesScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
        binding = null;
    }
}