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
    public String currGroup;
    private ParseObject currGroupObject;
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

        currGroup = getIntent().getStringExtra("friendGroup");

        getCurrentUser();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Group");
        Log.e("MatchActivity", "in update restaurants");
        // Retrieve the object by id
        query.getInBackground(currGroup, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    currGroupObject = object;
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
                getMatchRow();
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


    private void getMatchRow() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Matches");
        query.whereEqualTo("user", currentUser);
        query.whereEqualTo("groupId", currGroupObject);

        query.getFirstInBackground(new GetCallback<ParseObject>() {
           @Override
           public void done(ParseObject object, ParseException e) {
               if (e == null) {
                   try {
                       saveMatches(object);
                   } catch (JSONException jsonException) {
                       jsonException.printStackTrace();
                   }
               } else {
                   Log.e(TAG, "error getting user's match row", e);
               }
           }
       });

    }


    private void saveMatches(ParseObject match) throws JSONException {
        match.put("matches", likedRestaurants);
        match.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.e("MatchActivity", "success saving matches");
                    checkMatchingDone();
                } else {
                    Log.e("MatchActivity", "Matches save was not successful", e);
                }
            }
        });
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }



    private void checkMatchingDone() {
        Log.e(TAG, "in checkmatchingdone");

        ParseQuery<ParseObject> checkifMatched = ParseQuery.getQuery("Matches");
        checkifMatched.whereEqualTo("groupId", currGroupObject);
        checkifMatched.whereEqualTo("matches", null);

        Log.e(TAG, "in checkMatchingDone, before query");

        checkifMatched.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Log.e(TAG, "in checkMatchingDone, e not null");
                    // if other user has submitted their matches
                    if (objects.size() == 0) {
                        try {
                            findMutualMatches();
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

    private void findMutualMatches() throws JSONException {

        ParseQuery<ParseObject> findAllMatches = ParseQuery.getQuery("Matches");
        findAllMatches.whereEqualTo("groupId", currGroupObject);

        findAllMatches.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    mutualMatches = objects.get(0).getJSONArray("matches");
                    for (int i = 1; i < objects.size(); i++) {
                        try {
                            mutualMatches = findCommonRestaurants(mutualMatches, objects.get(i).getJSONArray("matches"));
                        } catch (JSONException jsonException) {
                            jsonException.printStackTrace();
                        }
                    }
                    saveMutualMatches();
                } else {
                    Log.e(TAG, "error getting matches", e);
                }
            }
        });

        Log.e(TAG, "in findMutualMatches");
        JSONArray currentUserMatches = likedRestaurants;

    }

    private void saveMutualMatches() {
        currGroupObject.put("mutualMatches", mutualMatches);
        currGroupObject.saveInBackground();

    }


    private JSONArray findCommonRestaurants(JSONArray array1, JSONArray array2) throws JSONException {
        JSONArray tempMutualMatches = new JSONArray();
        for (int i = 0; i < array1.length(); i++) {
            for (int j = 0; j < array2.length(); j++) {
                JSONObject rest1 = (JSONObject) array1.get(i);
                JSONObject rest2 = (JSONObject) array2.get(j);

                if (rest1.get("id").equals(rest2.get("id"))) {
                    tempMutualMatches.put(array1.get(i));
                }
            }
        }
        return tempMutualMatches;
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


    private void getCurrentUser() {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
        } else {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (isLoggedIn) {
                SharedPreferences sharedPreferences = this.getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
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
