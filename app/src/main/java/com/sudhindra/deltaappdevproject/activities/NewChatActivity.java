package com.sudhindra.deltaappdevproject.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.FilesClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityNewChatBinding;
import com.sudhindra.deltaappdevproject.fragments.SearchFragment;
import com.sudhindra.deltaappdevproject.models.Student;

import java.io.File;

public class NewChatActivity extends AppCompatActivity {

    private Intent intent;
    private Intent intent1;

    private SearchFragment searchFragment;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityNewChatBinding binding = ActivityNewChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.newMessageToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                SharedPreferences sharedPreferences = getSharedPreferences("userPref", MODE_PRIVATE);
                String currentUserName = sharedPreferences.getString("currentUserName", "");
                searchFragment = new SearchFragment(SearchFragment.NEW_CHAT_MODE, currentUserName, true);
                searchFragment.setListener(listener);
            } else
                handleWhenNoCurrentUser();
        } else {
            Student student = new Gson().fromJson(intent.getStringExtra("student"), Student.class);
            searchFragment = new SearchFragment(SearchFragment.NEW_CHAT_MODE, student);
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        fragmentManager.beginTransaction().add(R.id.newChatFragContianer, searchFragment, "newChatScreen").commit();

        progressDialog = new ProgressDialog(this, R.style.AppTheme_ProgressDialogTheme);
        progressDialog.setMessage("Loading File...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private SearchFragment.SearchFragmentListener listener = (currentUserName, otherUserId, otherUserName) -> {
        intent1 = new Intent(NewChatActivity.this, ChatActivity.class);
        intent1.putExtra("currentUserName", currentUserName);
        intent1.putExtra("otherUserId", otherUserId);
        intent1.putExtra("otherUserName", otherUserName);
        intent1.putExtra(android.content.Intent.EXTRA_TEXT, intent.getStringExtra((android.content.Intent.EXTRA_TEXT)));
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (intent.getType().startsWith("image/")) {
            File receivingFile = new File(getFilesDir(), "receivingImage");
            receivingFile.delete();
            progressDialog.show();
            FilesClient.FileCopyTask fileCopyTask = new FilesClient.FileCopyTask(this, this, FilesClient.FROM_NEW_CHAT, receivingFile);
            fileCopyTask.execute(uri);
            return;
        } else if (uri != null) {
            String fileName = FilesClient.getFileName(uri, this);
            intent1.putExtra("fileName", fileName);
            File receivingFileDir = new File(getFilesDir(), "receivingFileDir");
            receivingFileDir.mkdirs();
            File receivingFile = new File(receivingFileDir, fileName);
            receivingFile.delete();
            progressDialog.show();
            FilesClient.FileCopyTask fileCopyTask = new FilesClient.FileCopyTask(this, this, FilesClient.FROM_NEW_CHAT, receivingFile);
            fileCopyTask.execute(uri);
            return;
        }
        progressDialog.dismiss();
        intent1.putExtra("receivingData", true);
        intent1.setDataAndType(intent.getData(), intent.getType());
        startActivity(intent1);
        finish();
    };

    public void onFileCopyTaskComplete() {
        progressDialog.dismiss();
        intent1.putExtra("receivingData", true);
        intent1.setDataAndType(intent.getData(), intent.getType());
        startActivity(intent1);
        finish();
    }

    private void handleWhenNoCurrentUser() {
        Toast.makeText(this, "User Not Logged In", Toast.LENGTH_SHORT).show();
        onBackPressed();
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

    @Override
    public void onBackPressed() {
        // Todo: Handle when Opened from ShareSheet
        super.onBackPressed();
    }
}