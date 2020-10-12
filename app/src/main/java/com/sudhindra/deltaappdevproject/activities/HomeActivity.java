package com.sudhindra.deltaappdevproject.activities;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.ExternalStorageClient;
import com.sudhindra.deltaappdevproject.databinding.ActivityHomeBinding;
import com.sudhindra.deltaappdevproject.fragments.HomeFragment;
import com.sudhindra.deltaappdevproject.fragments.MessagesFragment;
import com.sudhindra.deltaappdevproject.fragments.ProfileFragment;
import com.sudhindra.deltaappdevproject.fragments.SearchFragment;
import com.sudhindra.deltaappdevproject.models.Student;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private FragmentManager fragmentManager;
    private HomeFragment homeFragment;
    private SearchFragment searchFragment;
    private MessagesFragment messagesFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        fragmentManager = getSupportFragmentManager();

        navBarInit();
    }

    private void navBarInit() {
        homeFragment = new HomeFragment();
        searchFragment = new SearchFragment(SearchFragment.SEARCH_USERS_MODE, (Student) null);
        messagesFragment = new MessagesFragment();
        profileFragment = new ProfileFragment(ProfileFragment.CURRENT_USER_MODE);

        homeFragment.setListener(post -> profileFragment.updateNewPost(post));

        activeFragment = homeFragment;

        fragmentManager.beginTransaction().add(R.id.fragContainer, profileFragment, "4").hide(profileFragment).commit();
        fragmentManager.beginTransaction().add(R.id.fragContainer, messagesFragment, "3").hide(messagesFragment).commit();
        fragmentManager.beginTransaction().add(R.id.fragContainer, searchFragment, "2").hide(searchFragment).commit();
        fragmentManager.beginTransaction().add(R.id.fragContainer, homeFragment, "1").commit();

        binding.bottomNavigationView.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
        binding.bottomNavigationView.setOnNavigationItemReselectedListener(navigationItemReselectedListener);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.home_menu:
                    fragmentManager.beginTransaction().hide(activeFragment).show(homeFragment).commit();
                    activeFragment = homeFragment;
                    return true;

                case R.id.search_menu:
                    fragmentManager.beginTransaction().hide(activeFragment).show(searchFragment).commit();
                    activeFragment = searchFragment;
                    return true;

                case R.id.messages_menu:
                    fragmentManager.beginTransaction().hide(activeFragment).show(messagesFragment).commit();
                    activeFragment = messagesFragment;
                    return true;

                case R.id.profile_menu:
                    fragmentManager.beginTransaction().hide(activeFragment).show(profileFragment).commit();
                    activeFragment = profileFragment;
                    return true;
            }
            return false;
        }
    };

    private BottomNavigationView.OnNavigationItemReselectedListener navigationItemReselectedListener = item -> {
        switch (item.getItemId()) {
            case R.id.home_menu:
                homeFragment.onReselected();
                break;

            case R.id.search_menu:
                searchFragment.onReselected();
                break;

            case R.id.messages_menu:
                messagesFragment.onReselected();
                break;

            case R.id.profile_menu:
                profileFragment.onReselected();
                break;
        }
    };

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
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}