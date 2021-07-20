package com.example.wheretoeat.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.wheretoeat.Friends;
import com.example.wheretoeat.R;
import com.example.wheretoeat.Restaurant;
import com.example.wheretoeat.YelpService;
import com.facebook.AccessToken;
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

    public ArrayList<Restaurant> restaurants = new ArrayList<>();

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
                addRecipientUser(recipientUsername);
            }

        });

    }

    Friends friend = new Friends();

    private void addRecipientUser(String recipientUsername) {
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
                    // after adding recipient user relation, add initial user relation
                    addInitUser();

                } else {
                    // Something went wrong.
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

        });

    }

    private void addInitUser() {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
            ParseRelation<ParseObject> relation = friend.getRelation("initial_user");
            relation.add(currentUser);
            // after relations added, save row to DB in background thread
            saveFriend();
        } else {
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
                        // after relations added, save row to DB in background thread
                        saveFriend();
                    } else {
                        Log.e(TAG, "Error getting Parse user" + e.getMessage());
                    }
                });
            }
        }
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
                etCode.setText("");
                checkLocationPermissions();
            }
        });
    }

    private void checkLocationPermissions() {
        Log.e(TAG, "in checklocationpermissions");
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        else {
            Log.e(TAG, "in checklocationpermissions-- else");
            try {
                getZipCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getZipCode() throws IOException {
        Log.e(TAG, "in getzipcode");
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

        String postalCode = addresses.get(0).getPostalCode();
        Log.e(TAG, "postalCode: " + postalCode);
        getRestaurants(postalCode);
        return postalCode;
    }


    // Register the permissions callback, which handles the user's response to the
// system permissions dialog. Save the return value, an instance of
// ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.

                    Log.e(TAG, "in activtyresultlauncher");
                    try {
                        getZipCode();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Toast.makeText(getContext(), "Need location", Toast.LENGTH_SHORT).show();
                }
            });

    private void getRestaurants(String location) {
        final YelpService yelpService = new YelpService();
        yelpService.findRestaurants(location, new Callback() {

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

    }
