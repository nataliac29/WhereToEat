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

import com.example.wheretoeat.ParseService.GroupQuery;
import com.example.wheretoeat.ParseService.MatchesQuery;
import com.example.wheretoeat.ParseService.UserQuery;
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

public class GroupsFragment extends Fragment implements MatchesQuery.getCurrUserGroupsInterface
{


    public GroupsFragment() {
        // Required empty public constructor
    }


    RecyclerView rvFriends;

    private FriendsAdapter adapter;

    // store current user's groups
    private List<ParseObject> allGroups;

    // temporary list of current user's groups
    private List<ParseObject> collectGroups;

    SwipeRefreshLayout swipeContainer;

    ParseUser currentUser;


    // store instances of Parse helper classes
    MatchesQuery matchQuery;


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

        // list of current user's groups
        allGroups = new ArrayList<>();

        // set adapter
        adapter = new FriendsAdapter(getContext(), allGroups);

        rvFriends.setAdapter(adapter);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        // get instance of Matches Parse helper class
        matchQuery = MatchesQuery.getInstance();

        // get the current user's groups
        getCurrUserGroups();

        // Lookup the swipe container view
        swipeContainer =  view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getCurrUserGroups();
            }
        });

        // Configure the refreshing colors of swipe container
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);


    }

    private void getCurrUserGroups() {
        // get the current user's groups, include the group object, order by newest group first
        matchQuery.getCurrUserGroups(currentUser, true, true, GroupsFragment.this);
    }

    @Override
    public void onFinishGetCurrUserGroups(List<ParseObject> objects, ParseException e) {
        collectGroups = new ArrayList<>();
        if (e == null) {
            // if user is not part of any groups
            if (objects.size() == 0) {
                Toast.makeText(getContext(), "Click the plus tab to add friends!", Toast.LENGTH_SHORT).show();
                adapter.clear();
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
                return;
            } else {
                for (ParseObject parseObject : objects) {
                    // add group to running list
                    collectGroups.add(parseObject.getParseObject("groupId"));
                }
                // once all groups added, clear and load adapter
                adapter.clear();
                allGroups.addAll(collectGroups);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        } else {
            Toast.makeText(getContext(), "Error fetching groups", Toast.LENGTH_SHORT).show();
        }
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