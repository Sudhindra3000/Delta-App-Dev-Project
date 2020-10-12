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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.HomeActivity;
import com.sudhindra.deltaappdevproject.clients.CloudStorageClient;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentProfileSetupBinding;
import com.sudhindra.deltaappdevproject.models.Student;
import com.sudhindra.deltaappdevproject.viewmodels.SignUpViewModel;

public class ProfileSetupFragment extends Fragment {

    private static final String TAG = "ProfileSetupFragment";
    private final int IMAGE_REQUEST = 23;
    private FragmentProfileSetupBinding binding;
    private NavController navController;

    private FirebaseAuth mAuth;
    private Uri imageUri;

    private SignUpViewModel signUpViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
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

        navController = Navigation.findNavController(view);

        signUpViewModel = new ViewModelProvider(requireActivity()).get(SignUpViewModel.class);
        Log.i(TAG, "onViewCreated: " + signUpViewModel.getStudent().toString());

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

    private void uploadProfile() {
        binding.profileSetupPBar.setVisibility(View.VISIBLE);
        binding.changePhotoBt.setVisibility(View.GONE);
        binding.removePhotoBt.setVisibility(View.GONE);
        binding.continueBt.setVisibility(View.GONE);
        if (imageUri != null) {
            CloudStorageClient.getInstance().getProfileImgRef(mAuth.getUid())
                    .putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> updateUserData())
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to Upload Profile", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateUserData() {
        FirestoreClient.getInstance().getUserInfoDocRef(mAuth.getUid())
                .update(Student.HAS_PROFILE_PIC, true)
                .addOnSuccessListener(aVoid -> showHomeFeed())
                .addOnFailureListener(e -> {
                    Log.i(TAG, "updateUserData: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to Upload Profile Picture. Try again Later", Toast.LENGTH_SHORT).show();
                    showHomeFeed();
                });
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