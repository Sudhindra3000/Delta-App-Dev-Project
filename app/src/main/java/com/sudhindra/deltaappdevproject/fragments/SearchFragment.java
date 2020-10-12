package com.sudhindra.deltaappdevproject.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.ChatActivity;
import com.sudhindra.deltaappdevproject.activities.UserActivity;
import com.sudhindra.deltaappdevproject.adapters.UserAdapter;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentSearchBinding;
import com.sudhindra.deltaappdevproject.utils.Student;
import com.sudhindra.deltaappdevproject.utils.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment", APP_ID = "1GP8H9THIC", USERS_INDEX = "users";
    public static final int SEARCH_USERS_MODE = 24, NEW_CHAT_MODE = 56;
    private FragmentSearchBinding binding;

    private int mode;
    private boolean receivingData = false;
    private boolean endOfCollection = false, fetching = false, stop = false, loading = true, paginating = false;

    // UI
    private ArrayList<User> userResults;
    private UserAdapter userAdapter;

    private ViewTreeObserver.OnScrollChangedListener scrollChangedListener = () -> {
        View view = binding.resultsScrollview.getChildAt(binding.resultsScrollview.getChildCount() - 1);

        int diff = (view.getBottom() - (binding.resultsScrollview.getHeight() + binding.resultsScrollview.getScrollY()));

        if (diff == 0 && loading) {
            loading = false;
            paginate();
        }
    };

    // Firebase
    private String currentUserId = FirebaseAuth.getInstance().getUid(), queryName;
    private Student student;
    private String currentUserName;

    // Algolia
    private String algoliaAdminAPIKey;
    private Client client;
    private Index usersIndex;
    private int page = 1, nbPages = 1;

    private SearchFragmentListener listener;

    public void setListener(SearchFragmentListener listener) {
        this.listener = listener;
    }

    public interface SearchFragmentListener {
        void onNewChatFromReceivedData(String currentUserName, String otherUserId, String otherUserName);
    }

    public SearchFragment(int mode, @Nullable Student student) {
        this.mode = mode;
        this.student = student;
        if (student != null)
            currentUserName = student.getFullName();
    }

    public SearchFragment(int mode, @NonNull String currentUserName, boolean receivingData) {
        this.mode = mode;
        this.currentUserName = currentUserName;
        this.receivingData = receivingData;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mode == SEARCH_USERS_MODE)
            FirestoreClient.getInstance().getUserInfoDocRef(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        student = documentSnapshot.toObject(Student.class);
                        currentUserName = student.getFullName();
                    })
                    .addOnFailureListener(e -> Log.i(TAG, "onCreate: " + e.getMessage()));
        FirestoreClient.getInstance().getAlgoliaAPIKeys().get()
                .addOnSuccessListener(documentSnapshot -> {
                    algoliaAdminAPIKey = (String) documentSnapshot.get(FirestoreClient.ALGOLIA_ADMIN_API_KEY);
                    client = new Client(APP_ID, algoliaAdminAPIKey);
                    usersIndex = client.getIndex(USERS_INDEX);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Unable to Connect", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "onCreate: " + e.getMessage());
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buildRecyclerView();

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                binding.resultsScrollview.fullScroll(NestedScrollView.FOCUS_UP);
                stop = false;
                paginating = false;
                endOfCollection = false;
                page = 0;
                userResults.clear();
                binding.noResultsTv.setVisibility(View.GONE);
                if (!newText.isEmpty()) {
                    if (fetching)
                        stop = true;
                    queryName = newText;
                    binding.searchIllustration.setVisibility(View.INVISIBLE);
                    searchUsers();
                } else {
                    stop = true;
                    Log.i(TAG, "onQueryTextChange: empty");
                    binding.searchIllustration.setVisibility(View.VISIBLE);
                    binding.searchRecyclerView.setVisibility(View.INVISIBLE);
                    binding.progressBarBottom.setVisibility(View.GONE);
                    binding.progressBarTop.setVisibility(View.GONE);
                    userResults.clear();
                }
                return false;
            }
        });
    }

    public void onReselected() {
        if (binding.searchRecyclerView.getVisibility() == View.VISIBLE)
            binding.resultsScrollview.smoothScrollTo(0, 0);
    }

    private void buildRecyclerView() {
        binding.searchRecyclerView.setHasFixedSize(true);
        userResults = new ArrayList<>();
        userAdapter = new UserAdapter();
        userAdapter.setListener(pos -> {
            if (mode == SEARCH_USERS_MODE)
                showUserProfile(userResults.get(pos));
            if (mode == NEW_CHAT_MODE)
                showChatScreen(userResults.get(pos));
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.searchRecyclerView.setLayoutManager(layoutManager);
        binding.searchRecyclerView.setAdapter(userAdapter);

        binding.resultsScrollview.getViewTreeObserver().addOnScrollChangedListener(scrollChangedListener);
    }

    private void paginate() {
        Log.i(TAG, "paginate: ");
        paginating = true;
        page++;
        searchUsers();
    }

    private void searchUsers() {
        if (!endOfCollection) {
            int usersLimit = 10;
            Query query = new Query(queryName)
                    .setAttributesToRetrieve(Student.FIRST_NAME_KEY, Student.LAST_NAME_KEY)
                    .setAttributesToHighlight()
                    .setPage(page)
                    .setHitsPerPage(usersLimit);
            fetching = true;
            if (!paginating) {
                binding.progressBarTop.setVisibility(View.VISIBLE);
                binding.searchRecyclerView.setVisibility(View.INVISIBLE);
                binding.progressBarBottom.setVisibility(View.GONE);
            }
            if (usersIndex != null)
                usersIndex.searchAsync(query, ((jsonObject, e) -> {
                    if (e != null) {
                        Log.i(TAG, "searchUsers: " + e.getMessage());
                        Toast.makeText(requireContext(), "Failed to load Search Results", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (jsonObject != null) {
                        if (!paginating)
                            userResults.clear();
                        fetching = false;
                        if (stop) {
                            stop = false;
                            return;
                        }
                        try {
                            JSONArray hits = jsonObject.getJSONArray("hits");
                            nbPages = jsonObject.getInt("nbPages");
                            Log.i(TAG, "searchUsers: page = " + page);
                            Log.i(TAG, "searchUsers: nbPages = " + nbPages);
                            binding.progressBarTop.setVisibility(View.GONE);
                            for (int i = 0; i < hits.length(); i++) {
                                JSONObject object = hits.getJSONObject(i);
                                if (!object.getString("objectID").equals(currentUserId))
                                    userResults.add(new User(object.getString("objectID"), object.getString(Student.FIRST_NAME_KEY) + " " + object.getString(Student.LAST_NAME_KEY)));
                            }
                            if (userResults.isEmpty() && binding.searchIllustration.getVisibility() != View.VISIBLE) {
                                binding.noResultsTv.setText(getResources().getString(R.string.noResultsText) + " \"" + queryName + "\"");
                                binding.noResultsTv.setVisibility(View.VISIBLE);
                                return;
                            }
                            binding.searchRecyclerView.setVisibility(View.VISIBLE);
                            binding.progressBarBottom.setVisibility(View.VISIBLE);
                            userAdapter.setSearchResults(userResults);
                            if (binding.searchIllustration.getVisibility() != View.VISIBLE) {
                                Log.i(TAG, "searchUsers: searchIllustration is invisible");
                            } else {
                                Log.i(TAG, "searchUsers: searchIllustration is visible");
                                userResults.clear();
                            }
                            loading = true;
                            userAdapter.notifyDataSetChanged();
                            if (nbPages == page + 1) {
                                Log.i(TAG, "searchUsers: endOfCollection");
                                binding.progressBarBottom.setVisibility(View.GONE);
                                endOfCollection = true;
                                page = 0;
                                nbPages = 1;
                            }
                        } catch (JSONException ex) {
                            ex.printStackTrace();
                        }
                    }
                }));
            else {
                Toast.makeText(requireContext(), "Unable to Connect", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUserProfile(User searchResult) {
        Intent intent = new Intent(requireActivity(), UserActivity.class);
        intent.putExtra("uid", searchResult.getUid());
        intent.putExtra("currentUserName", currentUserName);
        startActivity(intent);
    }

    private void showChatScreen(User user) {
        if (!receivingData) {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("currentUserName", currentUserName);
            intent.putExtra("otherUserId", user.getUid());
            intent.putExtra("otherUserName", user.getName());
            startActivity(intent);
            requireActivity().finish();
        } else {
            listener.onNewChatFromReceivedData(currentUserName, user.getUid(), user.getName());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.resultsScrollview.getViewTreeObserver().removeOnScrollChangedListener(scrollChangedListener);
        binding = null;
    }
}