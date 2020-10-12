package com.sudhindra.deltaappdevproject.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.activities.HomeActivity;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentLoginScreenBinding;


public class LoginScreen extends Fragment {

    private static final String TAG = "LoginScreen";
    private FragmentLoginScreenBinding binding;

    private FirebaseAuth mAuth;
    private String email, password;
    private NavController navController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().finishAffinity();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        binding.loginBt.setOnClickListener(v -> {
            email = binding.emailEt.getText().toString();
            password = binding.passwordEt.getText().toString();
            if (!email.trim().isEmpty() && !password.trim().isEmpty())
                checkDetails();
            else {
                if (email.trim().isEmpty() && password.trim().isEmpty())
                    Toast.makeText(requireContext(), "Enter Email and Password", Toast.LENGTH_SHORT).show();
                else {
                    if (email.trim().isEmpty())
                        Toast.makeText(requireContext(), "Email is Required", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(requireContext(), "Password is Required", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.signupTv.setOnClickListener(v -> showSignUpScreen());
    }

    private void checkDetails() {
        binding.loginBt.setVisibility(View.INVISIBLE);
        binding.loginPb.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> showHomeScreen())
                .addOnFailureListener(e -> {
                    binding.loginBt.setVisibility(View.VISIBLE);
                    binding.loginPb.setVisibility(View.INVISIBLE);
                    Log.i(TAG, "onFailure: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to Log In", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSignUpScreen() {
        navController.navigate(R.id.show_sign_up_screen);
    }

    private void showHomeScreen() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnSuccessListener(instanceIdResult -> FirestoreClient.getInstance().addDeviceTokenToUser(instanceIdResult.getToken()))
                .addOnFailureListener(e -> Log.i(TAG, "showHomeScreen: " + e.getMessage()));
        startActivity(new Intent(requireActivity(), HomeActivity.class));
        requireActivity().finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}