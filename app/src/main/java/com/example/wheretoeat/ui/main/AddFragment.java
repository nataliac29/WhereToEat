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
import com.example.wheretoeat.modals.Friends;
import com.example.wheretoeat.R;
import com.example.wheretoeat.modals.Restaurant;
import com.example.wheretoeat.YelpService;
import com.facebook.AccessToken;
import com.google.android.material.tabs.TabLayout;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
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


    String zipCode;
    String pricePref;


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
//                checkFriendExists(recipientUsername);
            }
        });

    }


    // Call this method to launch the edit dialog
    private void showEditDialog() {
        FragmentManager fm = getFragmentManager();
        EditPreferencesDialogFragment editPreferencesDialogFragment = EditPreferencesDialogFragment.newInstance("Some Title", currentUser, recipientUsername );
        // SETS the target fragment for use later when sending results
        editPreferencesDialogFragment.setTargetFragment(AddFragment.this, 300);
        editPreferencesDialogFragment.show(fm, "fragment_edit_name");
    }

    @Override
    public void onFinishEditDialog(String location, String price) {
        Toast.makeText(getContext(), "Hi, " + location, Toast.LENGTH_SHORT).show();
        etCode.setText("");
        zipCode = location;
        pricePref = price;
        addRecipientUser(recipientUser.getObjectId());
    }

    int resultSize;

    // returns true if there is already a connection between the users, false if not
    private void checkFriendExists(String recipientUsername) {
        Log.e(TAG, "logged in user: " + currentUser.getObjectId());
        // query to get recipient user
        ParseQuery<ParseUser> getRecipientQuery = ParseUser.getQuery();
        getRecipientQuery.whereEqualTo("username", recipientUsername);

        // query to get relationship if one exists
        ParseQuery<ParseObject> checkRecipientQuery = ParseQuery.getQuery("Friends");
        checkRecipientQuery.whereEqualTo("recipient_user", currentUser);


        ParseQuery<ParseObject> checkInitialQuery = ParseQuery.getQuery("Friends");
        checkInitialQuery.whereEqualTo("initial_user", currentUser);


        List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
        queries.add(checkRecipientQuery);
        queries.add(checkInitialQuery);

        ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);

        // get recipient first, then run second query to see if it returns row
        getRecipientQuery.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() != 0) {
                        Log.e(TAG, "getting recipient user" + objects.toString());
                        recipientUser = objects.get(0);

                        checkRecipientQuery.whereEqualTo("initial_user", recipientUser);
                        checkInitialQuery.whereEqualTo("recipient_user", recipientUser);

                        mainQuery.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> objects, ParseException e1) {
                                if (e == null) {
                                    Log.e(TAG, "result of check friend exists: " + objects.toString());
                                    resultSize = objects.size();
                                    // no relationship already exists, continue with creating row
                                    if (resultSize == 0) {
                                        showEditDialog();
                                    } else {
                                        Toast.makeText(getContext(), "You have already added this user", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });

                    }
                    else {
                        Toast.makeText(getContext(), "User does not exist", Toast.LENGTH_SHORT).show();

                    }
                }
                else {
                    Log.e(TAG, "error getting recipient user", e);
                }
            }
        });

    }

    Friends friend = new Friends();

    private void addRecipientUser(String recipientUsername) {
        ParseRelation<ParseObject> relation = friend.getRelation("recipient_user");
        relation.add(recipientUser);
        // after adding recipient user relation, add initial user relation
        addInitUser();
    }

    private void addInitUser() {
        ParseRelation<ParseObject> relation = friend.getRelation("initial_user");
        relation.add(currentUser);
        // after relations added, save row to DB in background thread
        saveFriend();
    }

    private void saveFriend() {
        friend.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error while saving", e);
                    Toast.makeText(getContext(), "Error while saving!", Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG, "Post save was successful");
                getRestaurants(zipCode);
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
                updateRestaurants(jsonArray);

            }
        });
    }
    public void updateRestaurants(JSONArray restaurants) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friends");
        query.whereEqualTo("objectId", friend.getObjectId());
        Log.e(TAG, "in update restaurants");
        Log.e(TAG, friend.getObjectId());
        // Retrieve the object by id
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Friends currFriend = (Friends) objects.get(0);
                    // Update the fields we want to
                    currFriend.put("restaurants", restaurants);
                    // All other fields will remain the same
                    currFriend.saveInBackground(new SaveCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                //success, saved!
                                Log.d("MyApp", "Successfully saved!");

                                TabLayout tabs = (TabLayout)((MainActivity)getActivity()).findViewById(R.id.tabs);
                                tabs.getTabAt(0).select();

                            } else {
                                //fail to save!
                                e.printStackTrace();
                                Log.e(TAG, "saving error", e);
                            }
                        }
                    });

                } else {
                    // something went wrong
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
}
