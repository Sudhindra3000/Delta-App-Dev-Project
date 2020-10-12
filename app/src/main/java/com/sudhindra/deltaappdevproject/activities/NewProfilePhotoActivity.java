package com.sudhindra.deltaappdevproject.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.algolia.search.saas.Client;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityNewProfilePhotoBinding;
import com.sudhindra.deltaappdevproject.models.GlideApp;
import com.sudhindra.deltaappdevproject.models.Student;

public class NewProfilePhotoActivity extends AppCompatActivity {

    private static final String TAG = "NewProfilePhotoActivity", APP_ID = "1GP8H9THIC", USERS_INDEX = "users";
    private static final int IMAGE_REQUEST = 45;
    private ActivityNewProfilePhotoBinding binding;

    private FirebaseAuth mAuth;
    private Student student;
    private Uri imageUri = null;

    private String algoliaAdminAPIKey;
    private Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewProfilePhotoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.newProfilePhotoToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        student = new Gson().fromJson(getIntent().getStringExtra("student"), Student.class);
        mAuth = FirebaseAuth.getInstance();

        FirestoreClient.getInstance().getAlgoliaAPIKeys().get()
                .addOnSuccessListener(documentSnapshot -> {
                    algoliaAdminAPIKey = (String) documentSnapshot.get(FirestoreClient.ALGOLIA_ADMIN_API_KEY);
                    client = new Client(APP_ID, algoliaAdminAPIKey);
                    client.getIndex(USERS_INDEX);
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onCreate: " + e.getMessage());
                    Toast.makeText(this, "Failed to Fetch Data", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    onBackPressed();
                    finish();
                });

        if (!student.getHasProfilePic()) {
            binding.addPhotoBt.setVisibility(View.VISIBLE);
            binding.changePhotoBt.setVisibility(View.GONE);
            binding.removePhotoBt.setVisibility(View.GONE);
        } else
            GlideApp.with(this)
                    .load(CloudStorageClient.getInstance().getProfileImgRef(mAuth.getUid()))
                    .placeholder(R.drawable.default_post_image)
                    .into(binding.newProfileImageView);

        binding.addPhotoBt.setOnClickListener(v -> checkForPermission());
        binding.changePhotoBt.setOnClickListener(v -> checkForPermission());
        binding.removePhotoBt.setOnClickListener(v -> {
            binding.removePhotoBt.setVisibility(View.GONE);
            binding.changePhotoBt.setVisibility(View.GONE);
            binding.addPhotoBt.setVisibility(View.VISIBLE);
            binding.newProfileImageView.setImageResource(R.drawable.default_profile);
            imageUri = null;
        });

        binding.profilePhotoSaveBt.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        binding.addPhotoBt.setVisibility(View.GONE);
        binding.changePhotoBt.setVisibility(View.GONE);
        binding.removePhotoBt.setVisibility(View.GONE);
        binding.profilePhotoSaveBt.setVisibility(View.INVISIBLE);
        binding.profilePhotoPb.setVisibility(View.VISIBLE);
        if (imageUri != null) {
            CloudStorageClient.getInstance().getProfileImgRef(mAuth.getUid())
                    .putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> userHasProfile())
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "saveProfile: " + e.getMessage());
                        Toast.makeText(this, "Failed to Upload Profile Photo", Toast.LENGTH_SHORT).show();
                        binding.changePhotoBt.setVisibility(View.VISIBLE);
                        binding.removePhotoBt.setVisibility(View.VISIBLE);
                        binding.profilePhotoSaveBt.setVisibility(View.VISIBLE);
                        binding.profilePhotoPb.setVisibility(View.INVISIBLE);
                    });
        } else {
            if (student.getHasProfilePic()) {
                CloudStorageClient.getInstance().getProfileImgRef(mAuth.getUid())
                        .delete()
                        .addOnSuccessListener(aVoid -> userDoesNotHaveProfile())
                        .addOnFailureListener(e -> {
                            Log.i(TAG, "saveProfile: " + e.getMessage());
                            Toast.makeText(this, "Failed to Update Profile", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }

    private void userHasProfile() {
        FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid())
                .update(Student.HAS_PROFILE_PIC, true)
                .addOnSuccessListener(aVoid -> {
                    setResult(RESULT_OK);
                    onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "saveProfile: " + e.getMessage());
                    Toast.makeText(this, "Failed to Upload Profile Photo", Toast.LENGTH_SHORT).show();
                    binding.changePhotoBt.setVisibility(View.VISIBLE);
                    binding.removePhotoBt.setVisibility(View.VISIBLE);
                    binding.profilePhotoSaveBt.setVisibility(View.VISIBLE);
                    binding.profilePhotoPb.setVisibility(View.INVISIBLE);
                });
    }

    private void userDoesNotHaveProfile() {
        FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid())
                .update(Student.HAS_PROFILE_PIC, false)
                .addOnSuccessListener(aVoid -> {
                    setResult(RESULT_OK);
                    onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "saveProfile: " + e.getMessage());
                    Toast.makeText(this, "Failed to Upload Profile Photo", Toast.LENGTH_SHORT).show();
                    binding.changePhotoBt.setVisibility(View.VISIBLE);
                    binding.removePhotoBt.setVisibility(View.VISIBLE);
                    binding.profilePhotoSaveBt.setVisibility(View.VISIBLE);
                    binding.profilePhotoPb.setVisibility(View.INVISIBLE);
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == IMAGE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openPhoneGallery();
        }
    }

    public void openPhoneGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private void checkForPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IMAGE_REQUEST);
            else
                openPhoneGallery();
        } else
            openPhoneGallery();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && data != null) {
            imageUri = data.getData();
            binding.newProfileImageView.setImageURI(imageUri);
            binding.addPhotoBt.setVisibility(View.GONE);
            binding.changePhotoBt.setVisibility(View.VISIBLE);
            binding.removePhotoBt.setVisibility(View.VISIBLE);
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