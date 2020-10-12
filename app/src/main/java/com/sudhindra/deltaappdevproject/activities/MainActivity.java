package com.sudhindra.deltaappdevproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sudhindra.deltaappdevproject.R;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private boolean stop = false;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);

        mAuth = FirebaseAuth.getInstance();
        checkForUser();

        File receivingFileDir = new File(getFilesDir(), "receivingFileDir");
        if (receivingFileDir.exists()) {
            try {
                FileUtils.cleanDirectory(receivingFileDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkForUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) showLoginScreen();
        else showHomeScreen();
    }

    private void showLoginScreen() {
        Intent intent = new Intent(MainActivity.this, LoginSignUpActivity.class);
        new Handler().postDelayed(() -> {
            startActivity(intent);
            finish();
        }, 3500);
    }

    private void showHomeScreen() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        new Handler().postDelayed(() -> {
            if (!stop) {
                startActivity(intent);
                finish();
            }
        }, 3500);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stop = true;
    }
}