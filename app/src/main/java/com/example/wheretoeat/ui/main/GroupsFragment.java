package com.example.wheretoeat.ui.main;

import android.content.Context;
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

import com.example.wheretoeat.adapters.FriendsAdapter;
import com.example.wheretoeat.R;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment {


    public GroupsFragment() {
        // Required empty public constructor
    }


    /*

    1. query Matches, get all group id's
    2. for each group id, also get user info




     */
    RecyclerView rvFriends;

    private FriendsAdapter adapter;

    private List<ParseObject> allGroups;

    private List<ParseObject> collectGroups;

    SwipeRefreshLayout swipeContainer;

    ParseUser currentUser;


    public static final String TAG = "GroupFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        getCurrentUser();
        return inflater.inflate(R.layout.fragment_groups, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvFriends = view.findViewById(R.id.rvFriends);
        allGroups = new ArrayList<>();


        adapter = new FriendsAdapter(getContext(), allGroups);

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
        collectGroups = new ArrayList<>();

        ParseQuery<ParseObject> getGroups = ParseQuery.getQuery("Matches");
        getGroups.whereEqualTo("user", currentUser);
        getGroups.include("groupId");
        getGroups.orderByDescending("createdAt");

        getGroups.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
               if (e == null) {
                   if (objects.size() == 0) {
                        Toast.makeText(getContext(), "Click the plus tab to add friends!", Toast.LENGTH_SHORT).show();
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                        swipeContainer.setRefreshing(false);
                        return;
                    } else {
                       for (ParseObject parseObject : objects) {
                           Log.e(TAG, parseObject.getParseObject("groupId").toString());
                           collectGroups.add(parseObject.getParseObject("groupId"));
                           Log.e(TAG, "collectGroups" + collectGroups.toString());
                       }
                       adapter.clear();
                       Log.e(TAG, "all groups: " + collectGroups.toString());
                       allGroups.addAll(collectGroups);
                       adapter.notifyDataSetChanged();
                       swipeContainer.setRefreshing(false);
                   }
               } else {
                   Log.e(TAG, "error", e);
               }
            }
        });
    }

    private void getCurrentUser() {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
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
                        } else {
                            Log.e(TAG, "Error getting Parse user" + e.getMessage());
                        }

                    }
                });
            }
        }
    }
}