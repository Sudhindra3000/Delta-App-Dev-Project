package com.sudhindra.deltaappdevproject.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.HomeActivity;
import com.sudhindra.deltaappdevproject.databinding.FragmentProfileSetupBinding;
import com.sudhindra.deltaappdevproject.utils.ToastUtil;
import com.sudhindra.deltaappdevproject.viewmodels.AuthViewModel;
import com.sudhindra.deltaappdevproject.viewmodels.actions.AuthAction;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileSetupFragment extends Fragment {

    private static final String TAG = "ProfileSetupFragment";
    private final int IMAGE_REQUEST = 23;

    private FragmentProfileSetupBinding binding;

    private AuthViewModel viewModel;

    private NavController navController;

    private Uri imageUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
//                Todo: Delete user on back pressed
                navController.navigate(R.id.back_to_sign_up_screen);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        initObservers();

        navController = Navigation.findNavController(view);

        AuthViewModel viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        Log.i(TAG, "onViewCreated: " + viewModel.getStudent().toString());

        binding.addPhotoBt.setOnClickListener(v -> checkForPermission());

        binding.skipBt.setOnClickListener(v -> showHomeFeed());

        binding.changePhotoBt.setOnClickListener(v -> checkForPermission());

        binding.removePhotoBt.setOnClickListener(v -> {
            binding.profileImageSetup.setImageResource(R.drawable.default_profile);
            imageUri = null;
            binding.addPhotoBt.setVisibility(View.VISIBLE);
            binding.skipBt.setVisibility(View.VISIBLE);
            binding.changePhotoBt.setVisibility(View.GONE);
            binding.removePhotoBt.setVisibility(View.GONE);
            binding.continueBt.setVisibility(View.GONE);
        });

        binding.continueBt.setOnClickListener(v -> uploadProfile());
    }

    private void initObservers() {
        viewModel.getProfileUploadSuccess().observe(getViewLifecycleOwner(), unit -> showHomeFeed());

        viewModel.getError().observe(getViewLifecycleOwner(), s -> ToastUtil.toast(this, s));
    }

    private void uploadProfile() {
        binding.profileSetupPBar.setVisibility(View.VISIBLE);
        binding.changePhotoBt.setVisibility(View.GONE);
        binding.removePhotoBt.setVisibility(View.GONE);
        binding.continueBt.setVisibility(View.GONE);
        if (imageUri != null)
            viewModel.doAction(new AuthAction.UploadProfile(imageUri));
    }

    private void showHomeFeed() {
        startActivity(new Intent(requireActivity(), HomeActivity.class));
        requireActivity().finish();
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
            if (requireActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
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
            binding.profileImageSetup.setImageURI(imageUri);
            binding.addPhotoBt.setVisibility(View.GONE);
            binding.skipBt.setVisibility(View.GONE);
            binding.changePhotoBt.setVisibility(View.VISIBLE);
            binding.removePhotoBt.setVisibility(View.VISIBLE);
            binding.continueBt.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
