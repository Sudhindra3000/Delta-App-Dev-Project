package com.sudhindra.deltaappdevproject.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentSignUpScreenBinding;
import com.sudhindra.deltaappdevproject.utils.Student;
import com.sudhindra.deltaappdevproject.viewmodels.SignUpViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class SignUpScreen extends Fragment {

    private static final String TAG = "SignUpScreen", APP_ID = "1GP8H9THIC", USERS_INDEX = "users";
    private FragmentSignUpScreenBinding binding;
    private SignUpViewModel signUpViewModel;
    private NavController navController;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String fName, lName, email, password, confirmPassword, yos, newUid;
    private int branch;
    private Student newStudent;
    private ArrayList<String> branchStrings = new ArrayList<>();

    private String algoliaAdminAPIKey;
    private Client client;
    private Index usersIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navController.navigate(R.id.back_to_login);
            }
        });
        branchStrings.addAll(Arrays.asList(getResources().getStringArray(R.array.branch_items)));
        FirestoreClient.getInstance().getAlgoliaAPIKeys().get()
                .addOnSuccessListener(documentSnapshot -> {
                    algoliaAdminAPIKey = (String) documentSnapshot.get(FirestoreClient.ALGOLIA_ADMIN_API_KEY);
                    client = new Client(APP_ID, algoliaAdminAPIKey);
                    usersIndex = client.getIndex(USERS_INDEX);
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onCreate: " + e.getMessage());
                    Toast.makeText(requireContext(), "There was an issue. Try again Later", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignUpScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        signUpViewModel = new ViewModelProvider(requireActivity()).get(SignUpViewModel.class);

        navController = Navigation.findNavController(view);

        ArrayAdapter<String> yosAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, getResources().getStringArray(R.array.yos_items));
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, getResources().getStringArray(R.array.branch_items));
        binding.yosDropdown.setAdapter(yosAdapter);
        binding.branchDropdown.setAdapter(branchAdapter);

        binding.signupBt.setOnClickListener(v -> checkDetails());
    }

    private void checkDetails() {
        fName = binding.fNameEt.getText().toString();
        lName = binding.lNameEt.getText().toString();
        yos = binding.yosDropdown.getText().toString();
        branch = getBranchCodeFromString(binding.branchDropdown.getText().toString());
        email = binding.emailEt2.getText().toString();
        password = binding.passwordEt2.getText().toString();
        confirmPassword = binding.confirmPasswordEt.getText().toString();
        if (fName.isEmpty() || lName.isEmpty() || yos.isEmpty() || binding.branchDropdown.getText().toString().isEmpty()
                || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty())
            Toast.makeText(requireContext(), "Enter All the Fields", Toast.LENGTH_SHORT).show();
        else {
            if (fName.contains(" ") || lName.contains(" ")) {
                Toast.makeText(requireContext(), "First Name and Last Name should not Contain Spaces", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword))
                Toast.makeText(requireContext(), "Password did not match", Toast.LENGTH_SHORT).show();
            else
                signUpUser();
        }
    }

    private int getBranchCodeFromString(String string) {
        if (string.isEmpty()) return -1;
        return branchStrings.indexOf(string);
    }

    private void signUpUser() {
        binding.singUpProgressBar.setVisibility(View.VISIBLE);
        binding.signupBt.setVisibility(View.GONE);
        newStudent = new Student(fName, lName, Integer.parseInt(yos), branch, false, new ArrayList<>());
        signUpViewModel.setStudent(newStudent);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.i(TAG, "signUpUser,onSuccess: ");
                    newUid = authResult.getUser().getUid();
                    storeUserInfo();
                })
                .addOnFailureListener(e -> {
                    binding.singUpProgressBar.setVisibility(View.GONE);
                    binding.signupBt.setVisibility(View.VISIBLE);
                    Log.i(TAG, "signUpUser,onFailure: " + e.getMessage());
                    if (e.getMessage().equals("The given password is invalid. [ Password should be at least 6 characters ]")) {
                        binding.passwordField.setError("Password should be at least 6 characters");
                    } else if (e.getMessage().equals("The email address is already in use by another account.")) {
                        binding.emailField.setError("The email address is already in use by another account");
                    } else
                        Toast.makeText(requireContext(), "Sign Up Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void storeUserInfo() {
        db.collection("users")
                .document(newUid)
                .set(newStudent)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "storeUserInfo,onSuccess: ");
                    try {
                        FirebaseInstanceId.getInstance().getInstanceId()
                                .addOnSuccessListener(instanceIdResult -> FirestoreClient.getInstance().addDeviceTokenToUser(instanceIdResult.getToken()))
                                .addOnFailureListener(ex -> Log.i(TAG, "showHomeScreen: " + ex.getMessage()));
                        storeUserIndex();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "storeUserInfo,onFailure: " + e.getMessage());
                    binding.singUpProgressBar.setVisibility(View.GONE);
                    binding.signupBt.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Sign Up Failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void storeUserIndex() throws JSONException {
        usersIndex.addObjectAsync(new JSONObject(new Gson().toJson(newStudent)), newUid, ((jsonObject, e) -> {
            if (e != null) {
                Log.i(TAG, "storeUserIndex: " + e.getMessage());
                Toast.makeText(requireContext(), "Failed to Sign up. Try again Later", Toast.LENGTH_SHORT).show();
            }
            if (jsonObject != null) {
                Log.i(TAG, "storeUserIndex: " + jsonObject.toString());
                Toast.makeText(requireContext(), "Sign Up Successful", Toast.LENGTH_SHORT).show();
                navController.navigate(R.id.show_profile_setup);
            }
        }));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}