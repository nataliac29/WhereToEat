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
import com.example.wheretoeat.ParseService.GroupQuery;
import com.example.wheretoeat.ParseService.MatchesQuery;
import com.example.wheretoeat.adapters.RestaurantAdapter;
import com.example.wheretoeat.modals.CurrentUser;
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

public class MatchActivity extends AppCompatActivity implements
        RestaurantAdapter.OnClickListener,
        GroupQuery.getGroupByIdInterface,
        MatchesQuery.getMatchRowInterface,
        MatchesQuery.updateMatchesInterface,
        MatchesQuery.checkMatchingDoneInterface,
        MatchesQuery.getUsersInGroupInterface
{


    // Toast messages
    private static final String GROUP_ERROR = "Error getting groups's users";
    private static final String MATCH_ERROR = "Must like at least one restaurant";
    private static final String SAVE_ERROR = "Error saving restaurants";
    private static final String GENERIC_ERROR = "Something went wrong!";

    private static final String SUCCESS = "Success!";
    private static final String DONE = "Done!";
    private static final String LIKE = "Liked!";
    private static final String DISLIKE = "Disliked!";




    public ParseUser currentUser;

    // store group ID user currently matching for
    public String currGroup;

    // store whole Parse Object of the current group
    private ParseObject currGroupObject;

    // restaurants assigned to this group
    public JSONArray restaurants;

    // keep track of the restaurants the user "likes"
    public JSONArray likedRestaurants;

    // store common matched of group
    public JSONArray mutualMatches;

    GroupQuery groupQuery;
    MatchesQuery matchesQuery;


    private SwipeDeck cardStack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        currentUser = CurrentUser.getInstance().currUser;

        cardStack = findViewById(R.id.swipe_deck);


        restaurants = new JSONArray();

        // get objectId of group from Intent Extra
        currGroup = getIntent().getStringExtra("friendGroup");

        // store instances of Parse helper classes
        groupQuery = GroupQuery.getInstance();
        matchesQuery = MatchesQuery.getInstance();


        groupQuery.getGroupById(currGroup, this);
    }

    @Override
    public void onFinishGetGroupId(ParseObject object, ParseException e) {
        if (e == null) {
            currGroupObject = object;

            // for every restaurant, add to restaurants array
            for (int i = 0; i < object.getJSONArray(Constants.KEY_RESTAURANTS).length(); i++) {
                try {
                    restaurants.put(object.getJSONArray(Constants.KEY_RESTAURANTS).get(i));
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

    private void setAdapter() {
        // add to this array as user likes restaurants
        likedRestaurants = new JSONArray();

        RestaurantAdapter restaurantAdapter = new RestaurantAdapter(MatchActivity.this, restaurants, this);

        cardStack.setAdapter(restaurantAdapter);


        // on below line we are setting event callback to our card stack.
        cardStack.setEventCallback(new SwipeDeck.SwipeEventCallback() {
            @Override
            public void cardSwipedLeft(int position) {
                // on card swipe left display a toast message.
                Toast toast = Toast.makeText(MatchActivity.this, DISLIKE, Toast.LENGTH_SHORT);
                toast.show();

                // shorten time Toast is visible so doesn't overlap more recent Toast
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
                Toast toast = Toast.makeText(MatchActivity.this, LIKE, Toast.LENGTH_SHORT);
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
                if (likedRestaurants.length() >= 1) {
                    Toast.makeText(MatchActivity.this, DONE, Toast.LENGTH_SHORT).show();
                // get row in match table corresponding to current user and group
                    matchesQuery.getMatchRow(currentUser, currGroupObject, MatchActivity.this);
                }
                else {
                    Toast.makeText(MatchActivity.this, MATCH_ERROR, Toast.LENGTH_SHORT).show();
                    setAdapter();
                }
            }

            @Override
            public void cardActionDown() {
                // this method is called when card is swipped down.
            }

            @Override
            public void cardActionUp() {
                // this method is called when card is moved up.
            }
        });
    }

    @Override
    public void onFinishGetMatchRow(ParseObject object, ParseException e) {
        if (e == null) {
            try {
                // save restaurant matches in this row
                saveMatches(object);
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
            }
        } else {
            Toast.makeText(MatchActivity.this, GENERIC_ERROR, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMatches(ParseObject match) throws JSONException {
        // add liked restaurants to correct Matches row
        matchesQuery.updateMatches(match, likedRestaurants, this);
    }

    @Override
    public void onFinishUpdateMatches(ParseException e) {
        if (e == null) {
            Toast.makeText(MatchActivity.this, SUCCESS, Toast.LENGTH_SHORT).show();
            // check if the last user to match, if so find mutual matches
            checkMatchingDone();
        } else {
            Toast.makeText(MatchActivity.this, SAVE_ERROR, Toast.LENGTH_SHORT).show();
        }
        // Go back to groups fragment
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void checkMatchingDone() {
        // checks if there is any user in current group with a null "matches" column
        matchesQuery.checkMatchingDone(currGroupObject, this);
    }

    @Override
    public void onFinishCheckingMatchingDone(List<ParseObject> objects, ParseException e) {
        if (e == null) {
            // if no users have null matches (all users have matched)
            if (objects.size() == 0) {
                try {
                    // find common restaurants among all users
                    findMutualMatches();
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
        } else {
            Toast.makeText(MatchActivity.this, GROUP_ERROR, Toast.LENGTH_SHORT).show();
        }
    }

    private void findMutualMatches() throws JSONException {
        // get Match rows for all users in group, in order to access liked restaurants for each
        matchesQuery.getUsersInGroup(currentUser, currGroupObject, false, false, this);
    }

    @Override
    public void onFinishGetUsersInGroup(List<ParseObject> objects, ParseException e) {
        if (e == null) {
            // start mutual matches with first list of liked restaurants
            mutualMatches = objects.get(0).getJSONArray(Constants.KEY_MATCHES);
            for (int i = 1; i < objects.size(); i++) {
                try {
                    // for each subsequent list find common elements with mutual Matches and update mutualMatch to be that merge
                    mutualMatches = findCommonRestaurants(mutualMatches, objects.get(i).getJSONArray(Constants.KEY_MATCHES));
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
            }
            // save list of mutual matches to group's info
            matchesQuery.saveMutualMatches(currGroupObject, mutualMatches);
        } else {
            Toast.makeText(MatchActivity.this, GROUP_ERROR, Toast.LENGTH_SHORT).show();
        }
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


}
