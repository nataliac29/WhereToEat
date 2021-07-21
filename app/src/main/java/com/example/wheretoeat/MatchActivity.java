package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class MatchActivity extends AppCompatActivity implements DataTransferInterface {

    private static final String TAG = "MatchActivity";


    public ParseUser currentUser;
    public String currFriend;
    public Friends currFriendObject;
    public JSONArray restaurants;
    public JSONArray likedRestaurants;
    public JSONArray mutualMatches;
    private Button btnDoneMatching;
    private ParseUser otherUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);


        RecyclerView rvMovies = findViewById(R.id.rvRestaurants);
        //Create the adapter
        restaurants = new JSONArray();
        currFriend = getIntent().getStringExtra("friendGroup");
        RestaurantAdapter movieAdapter = new RestaurantAdapter(MatchActivity.this, restaurants, currFriend, this::onSetValues );

        // set the adapter on the recycler view
        rvMovies.setAdapter(movieAdapter);
        //Set a layout Manager on the recycler view
        rvMovies.setLayoutManager((new LinearLayoutManager(this)));

        likedRestaurants = new JSONArray();

        btnDoneMatching = findViewById(R.id.btnDoneMatching);

        Log.d("MatchActivity", String.format("Showing details for '%s'", currFriend));

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friends");
        Log.e("MatchActivity", "in update restaurants");
        // Retrieve the object by id
        query.getInBackground(currFriend, (object, e) -> {
            if (e == null) {
                Log.e("MatchActivity", object.getJSONArray("restaurants").toString());
                for (int i=0; i <  object.getJSONArray("restaurants").length(); i++) {
                    try {
                        restaurants.put(object.getJSONArray("restaurants").get(i));
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
                movieAdapter.notifyDataSetChanged();

            } else {
                // something went wrong
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnDoneMatching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Matches match = new Matches();
                addUser(match);
            }
        });

    }

    private void addUser(Matches match) {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
            ParseRelation<ParseObject> userRelation = match.getRelation("user");
            userRelation.add(currentUser);
            addGroupId(match);
        }
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            SharedPreferences sharedPreferences = MatchActivity.this.getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
            String currentUserId = sharedPreferences.getString("facebook_user_id", null);
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("objectId", currentUserId);
            // start an asynchronous call for posts
            query.findInBackground((users, e) -> {
                if (e == null) {
                    currentUser = users.get(0);
                    ParseRelation<ParseObject> userRelation = match.getRelation("user");
                    userRelation.add(currentUser);
                    addGroupId(match);
                } else {
                    Log.e("MatchActivity", "Error getting Parse user" + e.getMessage());
                }
            });
        }

    }

    private void addGroupId(Matches match) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friends");
        query.whereEqualTo("objectId", currFriend);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    currFriendObject = (Friends) objects.get(0);
                    ParseRelation<ParseObject> userRelation = match.getRelation("group_id");
                    userRelation.add(currFriendObject);
                    try {
                        saveMatches(match);
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }

                } else {
                    // something went wrong
                    Toast.makeText(MatchActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveMatches(Matches match) throws JSONException {
        match.put("matches", likedRestaurants.get(0));
        match.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.e("MatchActivity", "success saving matches");
                    findOtherUser();
                }
                else {
                    Log.e("MatchActivity", "Matches save was not successful", e);
                }
            }
        });
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    // check if the other user has finished matching, if so find mutual matches
    private void findOtherUser() {
        currFriendObject.getRelation("recipient_user").getQuery().findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects2, ParseException e2) {
                if (e2 == null) {
                    for (ParseObject user : objects2) {
                        if (!(user.getObjectId().equals(currentUser.getObjectId()))) {
                            Log.e(TAG, "got other user pt1" + otherUser.getObjectId());
                            otherUser = (ParseUser) user;
                        } else {
                            currFriendObject.getRelation("initial_user").getQuery().findInBackground(new FindCallback<ParseObject>() {
                                @Override
                                public void done(List<ParseObject> objects, ParseException e) {
                                    if (e == null) {
                                        for (ParseObject user : objects) {
                                            otherUser = (ParseUser) user;
                                            Log.e(TAG, "got other user" + otherUser.getObjectId());
                                            checkMatchingDone();
                                        }
                                    } else {
                                        Log.e("MatchActivity", "error getting other user", e);

                                    }
                                }
                            });
                        }
                    }
                } else {
                    Log.e("MatchActivity", "error getting other user", e2);
                }
            }
        });
    }


 private void checkMatchingDone() {
     Log.e(TAG, "in checkmatchingdone");

     ParseQuery<ParseObject> checkifMatched = ParseQuery.getQuery("Matches");
     checkifMatched.whereEqualTo("user", otherUser);
     checkifMatched.whereEqualTo("group_id", currFriendObject);

     Log.e(TAG, "in checkMatchingDone, before query" );

     checkifMatched.findInBackground(new FindCallback<ParseObject>() {
         @Override
         public void done(List<ParseObject> objects, ParseException e) {
             if (e == null) {
                 Log.e(TAG, "in checkMatchingDone, e not null" );
                 // if other user has submitted their matches
                 if (objects.size() != 0) {

                     JSONArray otherUserMatches = objects.get(0).getJSONArray("matches");
                     Log.e(TAG, "in checkMatchingDone, found other user matches" + otherUserMatches.toString());
                     try {
                         findMutualMatches(otherUserMatches);
                     } catch (JSONException jsonException) {
                         jsonException.printStackTrace();
                     }
                 }
             } else {
                 Log.e("MatchActivity", "error checking if matching done", e);
             }
         }
     });
 }

    private void findMutualMatches(JSONArray otherUserMatches) throws JSONException {
        Log.e(TAG, "in findMutualMatches");
        JSONArray currentUserMatches = (JSONArray) likedRestaurants.get(0);

        mutualMatches = new JSONArray();
        for (int i = 0; i < otherUserMatches.length(); i++)
        {
            for (int j = 0; j < currentUserMatches.length(); j++)
            {
                JSONObject rest1 = (JSONObject) otherUserMatches.get(i);
                JSONObject rest2 = (JSONObject) currentUserMatches.get(j);

                if(rest1.get("id").equals(rest2.get("id")))
                {
                    mutualMatches.put(otherUserMatches.get(i));
                }
            }
        }
        Log.e(TAG, "findmutualmatches-- mutual matches" + mutualMatches.toString());
        currFriendObject.put("mutualMatches", mutualMatches);
        currFriendObject.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.e("MatchActivity", "sucessfully saved mutual matches" );
                }
                else {
                    Log.e("MatchActivity", "error saving mutual matches" );
                }
            }
        });
    }





    @Override
    public void onSetValues(JSONArray al) {
        likedRestaurants.put(al);
        Log.e("MatchActivity", "value of liked restaurants" + likedRestaurants.toString());
    }




}