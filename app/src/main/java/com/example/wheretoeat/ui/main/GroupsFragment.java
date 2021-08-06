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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.wheretoeat.Constants;
import com.example.wheretoeat.MainActivity;
import com.example.wheretoeat.ParseService.MatchesQuery;
import com.example.wheretoeat.adapters.FriendsAdapter;
import com.example.wheretoeat.R;
import com.example.wheretoeat.modals.CurrentUser;
import com.facebook.AccessToken;
import com.google.android.material.tabs.TabLayout;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class GroupsFragment extends Fragment implements
        MatchesQuery.getCurrUserGroupsInterface,
        MatchesQuery.getBasicGroupInfoInterface
{


    public GroupsFragment() {
        // Required empty public constructor
    }

    // Toast constants
    private final String NO_GROUPS_MESSAGE = "Click the plus tab to add friends!";
    private final String GROUPS_ERROR_MESSAGE = "Error getting your groups";



    RecyclerView rvFriends;

    private FriendsAdapter adapter;

    // store current user's groups, only relevant info
    private List<JSONObject> allGroups;


    // temporary list of current user's groups, only relevant info
    private List<JSONObject> adapterInfo;

    boolean onLastGroup;

    // store whether user needs to match/ others have not finished matching/ or matching done for each group
    // passed to adapter to display correct button text for each group
    private JSONObject groupStates;

    SwipeRefreshLayout swipeContainer;

    ParseUser currentUser;


    // store instances of Parse helper classes
    MatchesQuery matchQuery;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        currentUser = CurrentUser.getInstance().currUser;
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
        if (e == null) {
            // if no groups, don't have to add anything to adapter
            if (objects.size() == 0) {
                Toast.makeText(getContext(), NO_GROUPS_MESSAGE, Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
                return;
            } else {
                // keep track of when last group so know when to update adapter
                onLastGroup = false;
                // iterate through every group
                for (int i = 0; i < objects.size(); i++) {

                    if (i == (objects.size() - 1)) {
                        // mark when on last group to check if any group was repeated
                        onLastGroup = true;
                    }
                    // initialize adapterInfo
                    adapterInfo = new ArrayList<>();

                    ParseObject group = objects.get(i).getParseObject(Constants.KEY_GROUPID);

                    // query gets all users from group and uses that to
                    // get basic info about each group that will be displayed

                    // I do the logic in this function for cleanliness and because it has
                    // easier access to both the group object and users of group
                    matchQuery.getBasicGroupInfo(currentUser, group, this);
                }
            }
        }
        else {
            Toast.makeText(getContext(), GROUPS_ERROR_MESSAGE, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFinishBasicGroupInfo(JSONObject object, ParseException e) {
        // object is not a "Group" object, just object with the groupId, state of group, and other group members
        if (e == null) {
            // add group to temp list
            adapterInfo.add(object);
            // once all added, update adapter
            if (onLastGroup) {
                // set adapter to new list
                adapter.clear();
                // passing temp list to list associated with adapter
                allGroups.addAll(adapterInfo);
                adapter.notifyDataSetChanged();
                swipeContainer.setRefreshing(false);
            }
        } else {
            Toast.makeText(getContext(), GROUPS_ERROR_MESSAGE, Toast.LENGTH_SHORT).show();
        }
    }

}