package com.sudhindra.deltaappdevproject.activities;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.ExternalStorageClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityUserBinding;
import com.sudhindra.deltaappdevproject.fragments.ProfileFragment;
import com.sudhindra.deltaappdevproject.models.Student;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private ActivityUserBinding binding;
    private Student student;
    private FragmentManager fragmentManager = getSupportFragmentManager();
    private ProfileFragment profileFragment;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        String uid = getIntent().getStringExtra("uid");
        String currentUserName = getIntent().getStringExtra("currentUserName");

        binding.toolbar.setOnClickListener(v -> profileFragment.onReselected());

        profileFragment = new ProfileFragment(ProfileFragment.OTHER_USER_MODE);
        profileFragment.setOtherUserId(uid);
        profileFragment.setCurrentUserName(currentUserName);
        fragmentManager.beginTransaction().add(R.id.searchResultContainer, profileFragment, "searchResult").commit();
        FirestoreClient.getInstance().getUserInfoDocRef(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    student = documentSnapshot.toObject(Student.class);
                    profileFragment.setStudent(student);
                    profileFragment.updateUI();

                    binding.userActPb.setVisibility(View.GONE);
                    binding.searchResultContainer.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onCreate: " + e.getMessage());
                    Toast.makeText(this, "Failed to Load User Info", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ExternalStorageClient.WRITE_EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You can save the Image now", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}