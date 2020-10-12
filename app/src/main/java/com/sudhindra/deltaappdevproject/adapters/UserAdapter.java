package com.sudhindra.deltaappdevproject.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sudhindra.deltaappdevproject.databinding.SearchResultRowBinding;
import com.sudhindra.deltaappdevproject.models.User;

import java.util.ArrayList;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private ArrayList<User> searchResults;
    private SearchResultListener listener;

    public void setSearchResults(ArrayList<User> searchResults) {
        this.searchResults = searchResults;
    }

    public void setListener(SearchResultListener listener) {
        this.listener = listener;
    }

    public interface SearchResultListener {
        void onItemClick(int pos);
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {

        SearchResultRowBinding binding;

        public UserViewHolder(@NonNull SearchResultRowBinding searchResultRowBinding, SearchResultListener listener) {
            super(searchResultRowBinding.getRoot());
            binding = searchResultRowBinding;
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(getAdapterPosition()));
        }

        public void setDetails(User details) {
            binding.setUser(details);
        }
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        SearchResultRowBinding binding = SearchResultRowBinding.inflate(layoutInflater, parent, false);
        return new UserViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setDetails(searchResults.get(position));
    }

    @Override
    public int getItemCount() {
        if (searchResults == null) return 0;
        return searchResults.size();
    }
}
