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
import com.sudhindra.deltaappdevproject.R;
import com.sudhindra.deltaappdevproject.clients.FirestoreClient;
import com.sudhindra.deltaappdevproject.databinding.FragmentSignUpScreenBinding;
import com.sudhindra.deltaappdevproject.models.Student;
import com.sudhindra.deltaappdevproject.utils.GsonUtil;
import com.sudhindra.deltaappdevproject.utils.ToastUtil;
import com.sudhindra.deltaappdevproject.viewmodels.AuthViewModel;
import com.sudhindra.deltaappdevproject.viewmodels.actions.AuthAction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;

@AndroidEntryPoint
public class SignUpScreen extends Fragment {

    private static final String TAG = "SignUpScreen", APP_ID = "1GP8H9THIC", USERS_INDEX = "users";

    private FragmentSignUpScreenBinding binding;

    private AuthViewModel viewModel;

    private NavController navController;

    private String fName, lName, email, password, confirmPassword, yos;
    private int branch;
    private Student newStudent;
    private ArrayList<String> branchStrings = new ArrayList<>();

    private String algoliaAdminAPIKey;
    private Client client;
    private Index usersIndex;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navController.navigate(R.id.back_to_login);
            }
        });
        branchStrings.addAll(Arrays.asList(getResources().getStringArray(R.array.branch_items)));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignUpScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        viewModel.doAction(new AuthAction.GetAlgoliaAPIKeys(Unit.INSTANCE));
        initObservers();

        navController = Navigation.findNavController(view);

        ArrayAdapter<String> yosAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, getResources().getStringArray(R.array.yos_items));
        ArrayAdapter<String> branchAdapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_menu_popup_item, getResources().getStringArray(R.array.branch_items));
        binding.yosDropdown.setAdapter(yosAdapter);
        binding.branchDropdown.setAdapter(branchAdapter);

        binding.signupBt.setOnClickListener(v -> checkDetails());
    }

    private void initObservers() {
        viewModel.getAPIKeySuccess().observe(getViewLifecycleOwner(), documentSnapshot -> {
            algoliaAdminAPIKey = (String) documentSnapshot.get(FirestoreClient.ALGOLIA_ADMIN_API_KEY);
            client = new Client(APP_ID, algoliaAdminAPIKey);
            usersIndex = client.getIndex(USERS_INDEX);
        });

        viewModel.getSignUpSuccess().observe(getViewLifecycleOwner(), authResult -> storeUserIndex(authResult.getUser().getUid()));

        viewModel.getError().observe(getViewLifecycleOwner(), s -> {
            binding.singUpProgressBar.setVisibility(View.GONE);
            binding.signupBt.setVisibility(View.VISIBLE);
            ToastUtil.toast(this, s);
        });
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
        viewModel.setStudent(newStudent);
        viewModel.doAction(new AuthAction.SigUp(email, password, newStudent));
    }

    private void storeUserIndex(String newUid) {
        try {
            usersIndex.addObjectAsync(new JSONObject(GsonUtil.toJson(newStudent)), newUid, ((jsonObject, e) -> {
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
