package com.example.wheretoeat.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.wheretoeat.MainActivity;
import com.example.wheretoeat.ParseService.GroupQuery;
import com.example.wheretoeat.ParseService.MatchesQuery;
import com.example.wheretoeat.ParseService.UserQuery;
import com.example.wheretoeat.R;
import com.example.wheretoeat.modals.Matches;
import com.example.wheretoeat.modals.Restaurant;
import com.example.wheretoeat.YelpService;
import com.facebook.AccessToken;
import com.google.android.material.tabs.TabLayout;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class AddFragment extends Fragment implements
        EditPreferencesDialogFragment.EditPreferencesDialogListener,
        MatchesQuery.getCurrUserGroupsInterface,
        MatchesQuery.getUsersInGroupInterface,
        MatchesQuery.checkUsernamesInterface,
        UserQuery.getUserFromUsernameInterface,
        GroupQuery.newGroupInterface
{

    public static final String TAG = "AddFragment";

    ParseUser currentUser;

    // to get list of usernames user has added
    Button btnCode;
    EditText etCode;

    // String from user, usernames of users to add
    String recipientUsernames;

    // to store new group
    ParseObject currGroup;

    // List of usernames to add to group
    ArrayList<String> usernames;

    // Store new users of group as ParseUsers
    ArrayList<ParseUser> newGroupUsers;


    // user preferences for group
    String zipCode;
    String pricePref;
    String groupName;

    // keeps track of whether user is attempting to add more than one person
    boolean isGroup;

    // keep track of whether user adding already added friend
    boolean groupAlreadyExists;

    // when iterating through user's existing groups, check if on last group
    boolean onLastGroup;



    // stores list of restaurants all members of group will vote on
    public ArrayList<Restaurant> restaurants = new ArrayList<>();


    // store instances of Parse helper classes
    MatchesQuery matchQuery;
    UserQuery userQuery;
    GroupQuery groupQuery;




    public AddFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get current user from beginning
        getCurrentUser();

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

        matchQuery = MatchesQuery.getInstance( );
        userQuery = UserQuery.getInstance();
        groupQuery = GroupQuery.getInstance();

        etCode = view.findViewById(R.id.etCode);
        btnCode = view.findViewById(R.id.btnCode);


        btnCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recipientUsernames = etCode.getText().toString();
                // start validating usernames by checking if all usernames inputted exist + aren't current user
                checkUsernames(recipientUsernames);
            }
        });

    }


    // Call this method to launch the edit dialog
    private void showEditDialog() {
        FragmentManager fm = getFragmentManager();
        EditPreferencesDialogFragment editPreferencesDialogFragment = EditPreferencesDialogFragment.newInstance(isGroup);
        // SETS the target fragment for use later when sending results
        editPreferencesDialogFragment.setTargetFragment(AddFragment.this, 300);
        editPreferencesDialogFragment.show(fm, "fragment_edit_name");
    }

    // once user is done with dialog
    @Override
    public void onFinishEditDialog(String location, String price, String newGroupName) {
        etCode.setText("");
        zipCode = location;
        pricePref = price;
        groupName = newGroupName;

        // once restaurant preferences set, fetch restaurants
        getRestaurants(zipCode);
    }

    // returns true if there is already a connection between the users, false if not
    private void checkUsernames(String recipientUsername) {
        // convert string from user into array of usernames
        usernames = usernamesToList(recipientUsername);

        // if one of the added usernames is the current user, notify user
        if (recipientUsername.contains(currentUser.getUsername())) {
            Toast.makeText(getContext(), "You cannot add yourself, please remove " + currentUser.getUsername(), Toast.LENGTH_SHORT).show();
            return;
        }

        // check all usernames correspond to a user
        matchQuery.checkUsernamesValid(usernames, this);
    }

    @Override
    public void onFinishCheckUsernames(int count, ParseException e) {
        if (e == null) {
            // if not all usernames are valid, notify user
            if (count != usernames.size()) {
                Toast.makeText(getContext(), "user does not exist", Toast.LENGTH_SHORT).show();
            } else {
                // if only adding one user, will check if that user has already been added
                checkGroupExists();
            }
        }
    }

    private void checkGroupExists() {
        // if adding more than one user, don't check if relation already exists (can have multiple groups of same users)
        if (usernames.size() != 1) {
            isGroup = true;
            // launch preferences dialog fragment
            showEditDialog();
        } else {
            // query for all the current users groups
            matchQuery.getCurrUserGroups(currentUser, false, false, this);
        }
    }

    @Override
    public void onFinishGetCurrUserGroups(List<ParseObject> objects, ParseException e) {
        if (e == null) {
            // if user doesn't have any other groups, go straight to preferences
            if (objects.size() == 0) {
                showEditDialog();
            }

            onLastGroup = false;

            // iterate through every group
            for (int i = 0; i < objects.size(); i++) {

                if (i == (objects.size() - 1)) {
                    // mark when on last group to check if any group was repeated
                    onLastGroup = true;
                }

                ParseObject group = objects.get(i).getParseObject(getString(R.string.groupId));
                // get the other users in the same group as user
                matchQuery.getUsersInGroup(currentUser, group, true, true, this);

            }
        } else {
            Toast.makeText(getContext(), "Error getting your groups.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFinishGetUsersInGroup(List<ParseObject> objects, ParseException e) {
        if (e == null) {
            // if this is a 2 person group
            if (objects.size() == 1) {
                // check if the other user matches the user that was to be added
                if (objects.get(0).getParseObject(getString(R.string.user)).getString(getString(R.string.username)).equals(usernames.get(0))){
                    groupAlreadyExists = true;
                    Toast.makeText(getContext(), "You've already added this user", Toast.LENGTH_SHORT).show();
                }
            }
            if (onLastGroup) {
                checkShowDialog();
            }
        } else {
            Toast.makeText(getContext(), "Error fetching your groups", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkShowDialog() {
        // if groupAlreadyExists was never set to true, go ahead and show dialog
        if (!groupAlreadyExists) {
            showEditDialog();
        }
    }


    private void getRestaurants(String location) {
        final YelpService yelpService = new YelpService();
        // get restaurants based on preferences
        yelpService.findRestaurants(location, pricePref, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                restaurants = yelpService.processResults(response);
                JSONArray jsonArray = new JSONArray();
                // add each restaurant to array
                for (int i=0; i < restaurants.size(); i++) {
                    // getJSONObject returns only the information from each restaurant we want to save
                    jsonArray.put(restaurants.get(i).getJSONObject());
                }

                // save these restaurants in new row in Group table
                newGroup(jsonArray);
            }
        });
    }

    private void newGroup(JSONArray restaurants) {
        groupQuery.newGroup(restaurants, groupName, this);
    }

    @Override
    public void onFinishNewGroup(ParseException e, ParseObject newGroup) {
        if (e == null){

            // store new group
            currGroup = newGroup;

            // get ParseUser object corresponding to each username
            getUsersFromUsername();

        } else {
            Toast.makeText(getContext(), "Error creating new group", Toast.LENGTH_SHORT).show();
        }
    }


    private void getUsersFromUsername() {
        // get user objects from list of usernames
        userQuery.getUserFromUsername(usernames, this);
    }

    @Override
    public void onFinishGetUserFromUsername(List<ParseUser> objects, ParseException e) {
        // store all users of group
        newGroupUsers = new ArrayList<>();
        newGroupUsers.add(currentUser);
        newGroupUsers.addAll(objects);
        // save new row in Match table for each user
        addMatchRows();
    }

    private void addMatchRows() {
        for (int i = 0; i < newGroupUsers.size(); i++) {
            // add row in Match table to keep track of matches for particular group
            matchQuery.addMatchesRow(newGroupUsers.get(i), currGroup);
            // when on last user, switch tabs to home (groups) tab and notify user
            if (i == (usernames.size() - 1)) {
                Toast.makeText(getContext(), "Success!", Toast.LENGTH_SHORT).show();
                TabLayout tabs = (TabLayout)((MainActivity)getActivity()).findViewById(R.id.tabs);
                tabs.getTabAt(0).select();
            }
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
                query.whereEqualTo(getString(R.string.objectId), currentUserId);
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

    private ArrayList<String> usernamesToList(String usernames) {
        // step one : converting comma separate String to array of String
        String[] elements = usernames.replaceAll("\\s", "").split(",");
        // step two : convert String array to list of String
        List<String> fixedLenghtList = Arrays.asList(elements);
        // step three : copy fixed list to an ArrayList
        return new ArrayList<String>(fixedLenghtList);
    }
}
