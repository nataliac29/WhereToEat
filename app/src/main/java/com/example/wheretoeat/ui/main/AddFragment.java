package com.example.wheretoeat.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.wheretoeat.Friends;
import com.example.wheretoeat.R;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.util.List;


public class AddFragment extends Fragment {
    /*
    button listener
        update parse table
        g back to groups table, add that friend to top of list
    */

    Button btnCode;
    EditText etCode;
    ParseUser currentUser;
    ParseUser recipientUser;
    public static final String TAG = "AddFragment";


    public AddFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false);

    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etCode = view.findViewById(R.id.etCode);
        btnCode = view.findViewById(R.id.btnCode);

        btnCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String recipientUsername = etCode.getText().toString();
                saveFriend(recipientUsername);
            }

            });
        };

        Friends friend = new Friends();

        private void saveFriend(String recipientUsername) {
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", recipientUsername);
            query.findInBackground(new FindCallback<ParseUser>() {
                @Override
                public void done(List<ParseUser> objects, ParseException e) {
                    if (e == null) {
                        // The query was successful, returns the users that matches
                        // the criteria.
                        recipientUser = objects.get(0);
                        ParseRelation<ParseObject> relation = friend.getRelation("recipient_user");
                        relation.add(recipientUser);
//                        Log.e(TAG, "Recipient user: " + recipientUser.toString());
//                        friend.put("recipient_user", recipientUser);
                        saveInitUser();

                    } else {
                        // Something went wrong.
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }

    private void saveInitUser() {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
            ParseRelation<ParseObject> relation = friend.getRelation("initial_user");
            relation.add(currentUser);
        }
        else {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (isLoggedIn) {
                SharedPreferences sharedPreferences = getContext().getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
                String currentUserId = sharedPreferences.getString("facebook_user_id", null);
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", currentUserId);
                // start an asynchronous call for posts
                query.findInBackground((users, e) -> {
                    if (e == null) {
                        currentUser = users.get(0);
                        ParseRelation<ParseObject> relation = friend.getRelation("initial_user");
                        relation.add(currentUser);
                        saveParseFriend();
                    } else {
                        Log.e(TAG, "Error getting Parse user" + e.getMessage());
                    }
                });
            }
        }
    }

    private void saveParseFriend() {
        friend.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(getContext(), "Error while saving!", Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG, "Post save was successful");
                etCode.setText("");
                //closes the activity, pass data to parent
                PageViewModel viewPager = new PageViewModel();
                viewPager.setIndex(0);

            }
        });
    }

    }