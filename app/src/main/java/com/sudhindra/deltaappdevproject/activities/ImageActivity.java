package com.sudhindra.deltaappdevproject.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.sudhindra.deltaappdevproject.GlideApp;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;
import com.sudhindra.deltaappdevproject.clients.ExternalStorageClient;
import com.sudhindra.deltaappdevproject.clients.TextRecognitionClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityImageBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ImageActivity extends AppCompatActivity {

    private ActivityImageBinding binding;
    private long time;

    private TextRecognitionClient textRecognitionClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.imageToolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        textRecognitionClient = new TextRecognitionClient(this);

        binding.imageToolbar.getBackground().setDither(true);

        time = getIntent().getLongExtra("timeInMillis", 0);
        GlideApp.with(this)
                .load(CloudStorageClient.getInstance().getMessageFileRef(time))
                .placeholder(R.drawable.default_post_image)
                .into(binding.imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_activity_menu, menu);
        return true;
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
            case R.id.message_text_recogniton:
                textRecognitionClient.processImage(binding.imageView);
                return true;
            case R.id.message_save:
                ExternalStorageClient.checkWriteExternalStoragePermission(this, binding.imageView, String.valueOf(time), this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}