package com.example.wheretoeat.ui.main;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.example.wheretoeat.R;
import com.example.wheretoeat.YelpService;
import com.example.wheretoeat.modals.Friends;
import com.example.wheretoeat.modals.Restaurant;
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

public class EditPreferencesDialogFragment extends DialogFragment implements LocationListener,TextView.OnEditorActionListener {

    private EditText mEditText;
    private static ParseUser currentUser;
    private static String recipientUsername;
    ParseUser recipientUser;

    //for location functionality
    LocationManager lm;
    Double latitude;
    Double longitude;
    Criteria criteria;
    String bestProvider;


    public ArrayList<Restaurant> restaurants = new ArrayList<>();



    private static final String TAG = "DialogFragment";

    public EditPreferencesDialogFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static EditPreferencesDialogFragment newInstance(String title, ParseUser currUser, String username) {
        EditPreferencesDialogFragment frag = new EditPreferencesDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        frag.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialogTheme);
        currentUser = currUser;
        recipientUsername = username;
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return getActivity().getLayoutInflater().inflate(R.layout.fragment_edit_preferences, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        mEditText = (EditText) view.findViewById(R.id.etCustomLocation);
        // Fetch arguments from bundle and set title
        String title = "Edit Restaurant Preferences";
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        mEditText.requestFocus();
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        mEditText.setOnEditorActionListener(this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        return dialog;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (EditorInfo.IME_ACTION_DONE == actionId) {
            sendBackResult();
            // Close the dialog and return back to the parent activity
            dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (lm != null) {
            lm.removeUpdates(this);
        }
    }


    public interface EditPreferencesDialogListener {
        void onFinishEditDialog(String inputText);
    }

    // Call this method to send the data back to the parent fragment
    public void sendBackResult() {
        // Notice the use of `getTargetFragment` which will be set when the dialog is displayed
        EditPreferencesDialogListener listener = (EditPreferencesDialogListener) getTargetFragment();
        listener.onFinishEditDialog(mEditText.getText().toString());
        dismiss();
    }


    int resultSize;

    // returns true if there is already a connection between the users, false if not
    private void checkFriendExists(String recipientUsername) {


//        Attempt at inner queries to avoid adding another query to get recipient user, did not work
//        ParseQuery<ParseObject> innerQuery = ParseQuery.getQuery("User");
//        innerQuery.whereEqualTo("username", recipientUsername);
//
//        ParseQuery<ParseObject> checkRecipientQuery = ParseQuery.getQuery("Friends");
//        checkRecipientQuery.whereMatchesQuery("recipient_user", innerQuery);
//        checkRecipientQuery.whereEqualTo("initial_user", recipientUsername);
//
//
//        ParseQuery<ParseObject> checkInitialQuery = ParseQuery.getQuery("Friends");
//        checkInitialQuery.whereMatchesQuery("initial_user", innerQuery);
//        checkInitialQuery.whereEqualTo("recipient_user", recipientUsername);
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
                                        addRecipientUser(recipientUsername);
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
                getLongLat();
            } catch (IOException e) {
                Log.e(TAG, "check location permissions error", e);
                e.printStackTrace();

            }
        }
    }

    private void getLongLat() throws IOException {
        Log.e(TAG, "in getzipcode");
        lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(lm.getBestProvider(criteria, true));
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            getZipCode();
        }
        else {
            lm.requestLocationUpdates(bestProvider, 1000, 0, this::onLocationChanged);
        }

    }
    @Override
    public void onLocationChanged(Location location) {
        //Hey, a non null location! Sweet!

        //remove location callback:
        lm.removeUpdates(this);

        //open the map:
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Toast.makeText(getContext(), "latitude:" + latitude + " longitude:" + longitude, Toast.LENGTH_SHORT).show();
        try {
            getZipCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void getZipCode() throws IOException {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5

        String postalCode = addresses.get(0).getPostalCode();
        Log.e(TAG, "postalCode: " + postalCode);
        getRestaurants(postalCode);
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