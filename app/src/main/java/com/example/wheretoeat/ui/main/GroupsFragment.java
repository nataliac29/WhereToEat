package com.example.wheretoeat.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.wheretoeat.Friends;
import com.example.wheretoeat.FriendsAdapter;
import com.example.wheretoeat.LoginActivity;
import com.example.wheretoeat.R;
import com.facebook.AccessToken;
import com.facebook.Profile;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class GroupsFragment extends Fragment {


    public GroupsFragment() {
        // Required empty public constructor
    }
    RecyclerView rvFriends;

    private FriendsAdapter adapter;

    private List<ParseUser> allUsers;

    SwipeRefreshLayout swipeContainer;

    ParseUser currentUser;

    private List<ParseUser> allTempUsers;

    public static final String TAG = "GroupFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_groups, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvFriends = view.findViewById(R.id.rvFriends);
        allUsers = new ArrayList<>();
        adapter = new FriendsAdapter(getContext(), allUsers);

        rvFriends.setAdapter(adapter);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        queryFriends();
        // Lookup the swipe container view
        swipeContainer =  view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                queryFriends();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);


    }



    private void queryFriends() {

        ArrayList<ParseUser> allTempUsers = new ArrayList<>();
        // get current user id

        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
        }
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
            String currentUserId = sharedPreferences.getString("facebook_user_id", null);

            Log.e(TAG, "GROUP FRAGMENT SHARED PREF: " + currentUserId);
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("objectId", currentUserId);
            // start an asynchronous call for posts
            query.findInBackground((users, e) -> {
                if (e == null) {
                    currentUser = users.get(0);
                } else {
                    Log.e(TAG, "Error getting Parse user" + e.getMessage());
                }
            });
        }

        ParseQuery<ParseObject> checkRecipientQuery = ParseQuery.getQuery("Friends");
        checkRecipientQuery.whereEqualTo("recipient_user", currentUser);
//        checkRecipientQuery.include("initial_user");

        ParseQuery<ParseObject> checkInitialQuery = ParseQuery.getQuery("Friends");
        checkInitialQuery.whereEqualTo("initial_user", currentUser);
//        checkRecipientQuery.include("recipient_user");

//
        List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
        queries.add(checkRecipientQuery);
        queries.add(checkInitialQuery);

        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);

        mainQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    // commentList now contains the last ten comments, and the "post"
                    // field has been populated. For example:
                    for (ParseObject friend : objects) {
                        // This does not require a network access.
                        friend.getRelation("recipient_user").getQuery().findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> objects2, ParseException e2) {
                                if (e2 == null) {
                                    for (ParseObject user : objects2) {
                                        if (!(user.getObjectId().equals(currentUser.getObjectId()))) {
                                            ParseUser recipientUser = (ParseUser) user;
                                            allTempUsers.add(recipientUser);
                                        }
                                    }
                                } else {
                                    Toast.makeText(getContext(), e2.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                }
                                friend.getRelation("initial_user").getQuery().findInBackground(new FindCallback<ParseObject>() {
                                    @Override
                                    public void done(List<ParseObject> objects3, ParseException e3) {
                                        if (e3 == null) {
                                            for (ParseObject user : objects3) {
                                                if (!(user.getObjectId().equals(currentUser.getObjectId()))) {
                                                    allTempUsers.add((ParseUser) user);
                                                }
                                            }
                                        } else {
                                            Toast.makeText(getContext(), e3.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        adapter.clear();
                                        allUsers.addAll(allTempUsers);
                                        adapter.notifyDataSetChanged();
                                        for (ParseUser user : allUsers) {
                                            Log.e(TAG, user.getString("firstName"));
                                        }
                                        swipeContainer.setRefreshing(false);
                                    }
                                });
                            }

                        });
                    }
                }
            }
        });
    }
}