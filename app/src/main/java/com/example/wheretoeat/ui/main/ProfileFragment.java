package com.example.wheretoeat.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wheretoeat.Constants;
import com.example.wheretoeat.R;
import com.example.wheretoeat.modals.CurrentUser;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class ProfileFragment extends Fragment {

    TextView tvName;
    TextView tvUsername;

    String name;
    String username;

    ParseUser currentUser;

    public ProfileFragment() {
        // Required empty public constructor
    }


    // Parse keys
    private static final String KEY_NAME = Constants.KEY_NAME;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getCurrentUser();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName = view.findViewById(R.id.tvName);
        tvUsername = view.findViewById(R.id.tvUsername);

        tvName.setText(name);
        tvUsername.setText(username);


    }

    private void getCurrentUser() {
        currentUser = CurrentUser.getInstance().currUser;
        username = currentUser.getUsername();
        name = currentUser.getString(KEY_NAME);
    }
}