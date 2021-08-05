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
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;

public class ProfileFragment extends Fragment {

    TextView tvName;
    TextView tvUsername;

    String firstName;
    String lastName;
    String username;

    ParseUser currentUser;

    public ProfileFragment() {
        // Required empty public constructor
    }

    // Toast Message
    public static final String ERROR_GETTING_USER = "Error getting user";

    // Parse keyss
    private static final String KEY_FIRST_NAME = Constants.KEY_FIRST_NAME;
    public static final String KEY_LAST_NAME = Constants.KEY_LAST_NAME;


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

        tvName.setText(firstName + " " + lastName);
        tvUsername.setText(username);


    }

    private void getCurrentUser() {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
            username = currentUser.getUsername();
            firstName = currentUser.getString(KEY_FIRST_NAME);
            lastName = currentUser.getString(KEY_LAST_NAME);

        } else {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (isLoggedIn) {
                SharedPreferences sharedPreferences = getContext().getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
                String currentUserId = sharedPreferences.getString("facebook_user_id", null);
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", currentUserId);
                // start an asynchronous call for posts
                query.findInBackground(new FindCallback<ParseUser>() {
                    @Override
                    public void done(List<ParseUser> objects, ParseException e) {
                        if (e == null) {
                            currentUser = objects.get(0);
                            username = currentUser.getUsername();
                            firstName = currentUser.getString(KEY_FIRST_NAME);
                            firstName = currentUser.getString(KEY_LAST_NAME);

                        } else {
                            Toast.makeText(getContext(), ERROR_GETTING_USER, Toast.LENGTH_SHORT).show();
                        }

                    }
                });
            }
        }
    }
}