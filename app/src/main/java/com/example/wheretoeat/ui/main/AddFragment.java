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


public class AddFragment extends Fragment implements EditPreferencesDialogFragment.EditPreferencesDialogListener {
    /*
    button listener
        update parse table
        g back to groups table, add that friend to top of list
    */

    Button btnCode;
    EditText etCode;
    ParseUser currentUser;
    String recipientUsername;
    ParseUser recipientUser;
    ParseObject currGroup;

    ArrayList<String> usernames;


    String zipCode;
    String pricePref;
    String groupName;

    boolean isGroup;

    List<List<String>> allUserGroups = new ArrayList<>();


    public ArrayList<Restaurant> restaurants = new ArrayList<>();



    public static final String TAG = "AddFragment";



    public AddFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        etCode = view.findViewById(R.id.etCode);
        btnCode = view.findViewById(R.id.btnCode);


        btnCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recipientUsername = etCode.getText().toString();
                checkFriendExists(recipientUsername);
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

    @Override
    public void onFinishEditDialog(String location, String price, String newGroupName) {
        etCode.setText("");
        zipCode = location;
        pricePref = price;
        groupName = newGroupName;
        getRestaurants(zipCode);
    }

    boolean groupAlreadyExists = false;
    boolean onLastGroup;

    // returns true if there is already a connection between the users, false if not
    private void checkFriendExists(String recipientUsername) {
        Log.e(TAG, "logged in user: " + currentUser.getObjectId());

        usernames = usernamesToList(recipientUsername);

        if (recipientUsername.contains(currentUser.getUsername())) {
            Toast.makeText(getContext(), "You cannot add yourself, please remove " + currentUser.getUsername(), Toast.LENGTH_SHORT).show();
            return;
        }

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereContainedIn("username", usernames);

        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                if (e == null) {
                    if (count != usernames.size()) {
                        Toast.makeText(getContext(), "user does not exist", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Log.e(TAG, "to checkgroupexists");
                        checkGroupExists();
                    }
                }
            }
        });


        // get all groups
       // for each group, get list of who's in it
        // check against potential new list
    }

    private void checkGroupExists() {
        Log.e(TAG, "in checkgroupexists");

        if (usernames.size() != 1) {
            isGroup = true;
            showEditDialog();
        } else {


        //if adding one user, check if that relationship already exists
        ParseQuery<ParseObject> checkFriends = ParseQuery.getQuery("Matches");
        checkFriends.whereEqualTo("user", currentUser);
            onLastGroup = false;
        // get all groups current user is a part of
        checkFriends.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() == 0) {
                        showEditDialog();
                    }
                    Log.e(TAG, "all your groups:" + objects.toString());
                    // iterate through every group
                    for (int i = 0; i < objects.size(); i++) {
                        Log.e(TAG, "here pt 1");

                        ParseObject group = objects.get(i);
                        // for each one of these groups, get all the users in each one
                        ParseQuery<ParseObject> getGroupUsers = ParseQuery.getQuery("Matches");
                        getGroupUsers.whereEqualTo("groupId", group.getParseObject("groupId"));
                        getGroupUsers.whereNotEqualTo("user", currentUser);
                        getGroupUsers.include("user");

                        ArrayList<String> collectGroups = new ArrayList<>();

                        if (i == (objects.size() - 1)) {
                            Log.e(TAG, "here pt 2");
                            onLastGroup = true;
                        }
                        getGroupUsers.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> groups, ParseException e) {
                                Log.e(TAG, "here pt 3");
                                if (e == null) {
                                    if (groups.size() == 1) {
                                        if (groups.get(0).getParseObject("user").getString("username").equals(usernames.get(0))){
                                            Log.e(TAG, "same user");
                                            groupAlreadyExists = true;
                                            Toast.makeText(getContext(), "You've already added this user", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    if (onLastGroup) {
                                        checkShowDialog();
                                    }
                                } else {
                                    Log.e(TAG, "error getting all users of each group", e);
                                }

                            }
                        });
                    }
                } else {
                    Log.e(TAG, "error checking friends", e);
                }
            }
        });
        }
    }


    // update group with restaurants

    // row in matches for each member of group

    private void saveGroup(JSONArray restaurants) {

        ParseObject newGroup = new ParseObject("Group");
        newGroup.put("restaurants", restaurants);
        Log.e(TAG, "GROUPNAME" + groupName);
        newGroup.put("groupName", groupName);

        // Saves the new object.
        newGroup.saveInBackground(e -> {
            if (e==null){
                currGroup = newGroup;
                addMatchRowsUser();
            }else{
                //Something went wrong
                Log.e(TAG, "error saving restaurnts", e);
            }
        });

    }


    private void getRestaurants(String location) {
        final YelpService yelpService = new YelpService();
        yelpService.findRestaurants(location, pricePref, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                String jsonData = response.body().string();
//                Log.e(TAG, jsonData);
                restaurants = yelpService.processResults(response);
                JSONArray jsonArray = new JSONArray();
                for (int i=0; i < restaurants.size(); i++) {
                    jsonArray.put(restaurants.get(i).getJSONObject());
                }
                saveGroup(jsonArray);

            }
        });
    }

    private void addMatchRowsUser() {
        Matches newMatch = new Matches();
        newMatch.put("user", currentUser);
        newMatch.put("groupId", currGroup);

        // Saves the new object.
        newMatch.saveInBackground(error -> {
            if (error==null){
                addMatchRows();
            }else{
                //Something went wrong
                Log.e(TAG, "error saving restaurnts", error);
            }
        });
    }

    private void addMatchRows() {
        for (int i = 0; i < usernames.size(); i++) {
            // get each user from username
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("username", usernames.get(i));

            query.getFirstInBackground(new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser object, ParseException e) {
                    if (e == null) {
                        if (object != null) {
                            Matches newMatch = new Matches();
                            newMatch.put("user", object);
                            newMatch.put("groupId", currGroup);

                            // Saves the new object.
                            newMatch.saveInBackground(error -> {
                                if (error==null){
                                    return;
                                }else{
                                    //Something went wrong
                                    Log.e(TAG, "error saving restaurnts", error);
                                }
                            });
                        }
                    } else {
                        Log.e(TAG, "error getting new user", e);
                    }
                }
            });

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
                Log.e(TAG, "SHARED PREF ID" + currentUserId);
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

    private ArrayList<String> usernamesToList(String usernames) {
        // step one : converting comma separate String to array of String
        String[] elements = usernames.replaceAll("\\s", "").split(",");
        // step two : convert String array to list of String
        List<String> fixedLenghtList = Arrays.asList(elements);
        // step three : copy fixed list to an ArrayList
        return new ArrayList<String>(fixedLenghtList);
    }

    private boolean sameUsers(ArrayList potentialUsernames) {
        Collections.sort(potentialUsernames);
        Collections.sort(usernames);
        return (usernames.equals(potentialUsernames));
    }

    private void checkShowDialog() {
        if (!groupAlreadyExists) {
            showEditDialog();
        }
    }
}

/*

                                    if (sameUsers(collectGroups)) {
                                        groupAlreadyExists = true;
                                        Toast.makeText(getContext(), "This group already exists", Toast.LENGTH_SHORT).show();
                                    }
 */