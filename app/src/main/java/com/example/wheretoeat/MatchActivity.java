package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.daprlabs.cardstack.SwipeDeck;
import com.example.wheretoeat.adapters.RestaurantAdapter;
import com.example.wheretoeat.modals.Friends;
import com.example.wheretoeat.modals.Matches;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MatchActivity extends AppCompatActivity implements RestaurantAdapter.OnClickListener {

    private static final String TAG = "MatchActivity";


    public ParseUser currentUser;
    public String currFriend;
    public Friends currFriendObject;
    public JSONArray restaurants;
    public JSONArray likedRestaurants;
    public JSONArray mutualMatches;
    private Button btnDoneMatching;
    private ParseUser otherUser;

    private SwipeDeck cardStack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        cardStack = findViewById(R.id.swipe_deck);


        restaurants = new JSONArray();

        currFriend = getIntent().getStringExtra("friendGroup");


        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friends");
        Log.e("MatchActivity", "in update restaurants");
        // Retrieve the object by id
        query.getInBackground(currFriend, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
//                    Log.e("MatchActivity", object.getJSONArray("restaurants").toString());
                    for (int i = 0; i < object.getJSONArray("restaurants").length(); i++) {
                        try {
                            restaurants.put(object.getJSONArray("restaurants").get(i));
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                        }
                    }
                    setAdapter();

                } else {
                    // something went wrong
                    Toast.makeText(MatchActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setAdapter() {
        // add to this array as user likes restaurants
        likedRestaurants = new JSONArray();

//        Log.e(TAG, "restaurants in set adapter" + restaurants.toString());

        RestaurantAdapter restaurantAdapter = new RestaurantAdapter(MatchActivity.this, restaurants, this);

        cardStack.setAdapter(restaurantAdapter);


        // on below line we are setting event callback to our card stack.
        cardStack.setEventCallback(new SwipeDeck.SwipeEventCallback() {
            @Override
            public void cardSwipedLeft(int position) {
                // on card swipe left we are displaying a toast message.
                Toast toast = Toast.makeText(MatchActivity.this, "Disliked!", Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toast.cancel();
                    }
                }, 1000);
            }

            @Override
            public void cardSwipedRight(int position) {
                // on card swiped to right we are displaying a toast message.
                Toast toast = Toast.makeText(MatchActivity.this, "Liked!", Toast.LENGTH_SHORT);
                toast.show();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toast.cancel();
                    }
                }, 1000);
                try {
                    likedRestaurants.put(restaurants.get(position));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void cardsDepleted() {
                // this method is called when no card is present
                Toast.makeText(MatchActivity.this, "Done!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "CARDS DONE");
                Matches match = new Matches();
                addUser(match);
            }

            @Override
            public void cardActionDown() {
                // this method is called when card is swipped down.
                Log.i("TAG", "CARDS MOVED DOWN");

            }

            @Override
            public void cardActionUp() {
                // this method is called when card is moved up.
                Log.i("TAG", "CARDS MOVED UP");
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
        match.put("matches", likedRestaurants);
        match.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.e("MatchActivity", "success saving matches");
                    findOtherUser();
                } else {
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
                            otherUser = (ParseUser) user;
                            Log.e(TAG, "got other user pt1" + otherUser.getObjectId());
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

        Log.e(TAG, "in checkMatchingDone, before query");

        checkifMatched.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Log.e(TAG, "in checkMatchingDone, e not null");
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
        JSONArray currentUserMatches = likedRestaurants;

        mutualMatches = new JSONArray();
        for (int i = 0; i < otherUserMatches.length(); i++) {
            for (int j = 0; j < currentUserMatches.length(); j++) {
                JSONObject rest1 = (JSONObject) otherUserMatches.get(i);
                JSONObject rest2 = (JSONObject) currentUserMatches.get(j);

                if (rest1.get("id").equals(rest2.get("id"))) {
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
                    Log.e("MatchActivity", "sucessfully saved mutual matches");
                } else {
                    Log.e("MatchActivity", "error saving mutual matches");
                }
            }
        });
    }
// trigger cards to swipe when the user clicks the button instead of actually swiping
        @Override
        public void onRestaurantLike() {
            cardStack.swipeTopCardRight(100);
        }

        @Override
        public void onRestaurantDislike() {
            cardStack.swipeTopCardLeft(100);
        }

}
