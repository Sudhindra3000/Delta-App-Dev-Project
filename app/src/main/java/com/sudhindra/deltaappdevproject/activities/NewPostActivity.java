package com.sudhindra.deltaappdevproject.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityNewPostBinding;
import com.sudhindra.deltaappdevproject.models.GlideApp;
import com.sudhindra.deltaappdevproject.models.Post;

public class NewPostActivity extends AppCompatActivity {

    private static final String TAG = "NewPostActivity";
    private ActivityNewPostBinding binding;
    private final int IMAGE_REQUEST = 23;

    private Intent intent;

    private String currentUserName;
    private String uid;

    private Uri imageUri;
    private String description;

    private Post post = new Post();
    private long postTime;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.newPostToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                SharedPreferences sharedPreferences = getSharedPreferences("userPref", MODE_PRIVATE);
                currentUserName = sharedPreferences.getString("currentUserName", "");
                uid = FirebaseAuth.getInstance().getUid();
                updateUI();
                if ("text/plain".equals(type))
                    handleTextSharing();
                else if (type.startsWith("image/"))
                    handleImageSharing();
            } else
                handleWhenNoCurrentUser();
        } else {
            currentUserName = intent.getStringExtra("currentUserName");
            uid = intent.getStringExtra("uid");
            updateUI();
        }

        binding.addPostPhotoBt.setOnClickListener(v -> requestForPermission());

        binding.newPostChangePhotoBt.setOnClickListener(v -> requestForPermission());

        binding.removePostPhotoBt.setOnClickListener(v -> {
            binding.removePostPhotoBt.setVisibility(View.INVISIBLE);
            binding.newPostChangePhotoBt.setVisibility(View.INVISIBLE);
            binding.addPostPhotoBt.setVisibility(View.VISIBLE);
            binding.newPostPhoto.setVisibility(View.GONE);
            imageUri = null;
        });

        binding.postButton.setOnClickListener(v -> post());
    }

    private void updateUI() {
        GlideApp.with(this)
                .load(CloudStorageClient.getInstance().getProfileImgRef(uid))
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(binding.newPostProfileImg);
        binding.newPostUsername.setText(currentUserName);
    }

    private void post() {
        description = binding.newPostDescriptionEt.getText().toString();
        hideKeyboard(this);
        binding.newPostDescriptionEt.clearFocus();
        if (description.trim().isEmpty() && imageUri == null) {
            Toast.makeText(this, "Enter a Description", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!description.trim().isEmpty() || imageUri != null) {
            binding.postButton.setVisibility(View.INVISIBLE);
            binding.newPostProgressBar.setVisibility(View.VISIBLE);
            binding.addPostPhotoBt.setVisibility(View.GONE);
            binding.newPostChangePhotoBt.setVisibility(View.GONE);
            binding.removePostPhotoBt.setVisibility(View.GONE);
            postTime = System.currentTimeMillis();
            if (imageUri != null) {
                uploadImage();
                return;
            }
            post.setImagePost(false);
            uploadPost();
        }
    }

    private void uploadImage() {
        CloudStorageClient.getInstance().getPostsStorage().child(String.valueOf(postTime))
                .putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    post.setImagePost(true);
                    uploadPost();
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "uploadImage: " + e.getMessage());
                    Toast.makeText(this, "Failed to Upload Image", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadPost() {
        post.setUid(uid);
        Log.i(TAG, "uploadPost: " + uid);
        post.setUserName(currentUserName);
        post.setPostedTimeInMillis(postTime);
        post.setPostDescription(description);
        FirestoreClient.getInstance().getPostsCollection()
                .document(String.valueOf(postTime))
                .set(post)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Post Uploaded", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("newPostJson", new Gson().toJson(post));
                    setResult(RESULT_OK, intent);
                    onBackPressed();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to Upload Post", Toast.LENGTH_SHORT).show());
    }

    // Receiving Data from Other Apps
    private void handleImageSharing() {
        description = intent.getStringExtra(android.content.Intent.EXTRA_TEXT);
        imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        binding.addPostPhotoBt.setVisibility(View.INVISIBLE);
        binding.removePostPhotoBt.setVisibility(View.VISIBLE);
        binding.newPostChangePhotoBt.setVisibility(View.VISIBLE);
        binding.newPostPhoto.setVisibility(View.VISIBLE);
        binding.newPostPhoto.setImageURI(imageUri);
        if (description != null) {
            binding.newPostDescriptionEt.setText(description);
            binding.newPostDescriptionEt.requestFocus();
        }
    }

    private void handleTextSharing() {
        description = intent.getStringExtra(android.content.Intent.EXTRA_TEXT);
        binding.newPostDescriptionEt.setText(description);
        binding.newPostDescriptionEt.requestFocus();
    }

    private void handleWhenNoCurrentUser() {
        Toast.makeText(this, "User Not Logged In", Toast.LENGTH_SHORT).show();
        onBackPressed();
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

    private void requestForPermission() {
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
            binding.addPostPhotoBt.setVisibility(View.INVISIBLE);
            binding.removePostPhotoBt.setVisibility(View.VISIBLE);
            binding.newPostChangePhotoBt.setVisibility(View.VISIBLE);
            binding.newPostPhoto.setVisibility(View.VISIBLE);
            binding.newPostPhoto.setImageURI(imageUri);
            binding.newPostScrollView.fullScroll(ScrollView.FOCUS_UP);
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

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}