package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;

public class ViewMatchesActivity extends AppCompatActivity {

    JSONArray restaurants;
    String currFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_matches);

        RecyclerView rvMovies = findViewById(R.id.rvMatches);
        //Create the adapter
        restaurants = new JSONArray();
        currFriend = getIntent().getStringExtra("friendGroup");
        ViewMatchesAdapter matchesAdapter = new ViewMatchesAdapter(ViewMatchesActivity.this, restaurants, currFriend);

        // set the adapter on the recycler view
        rvMovies.setAdapter(matchesAdapter);
        //Set a layout Manager on the recycler view
        rvMovies.setLayoutManager((new LinearLayoutManager(this)));

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Friends");
        Log.e("ViewMatchActivity", "in update restaurants");
        // Retrieve the object by id
        query.getInBackground(currFriend, (object, e) -> {
            if (e == null) {
                Log.e("ViewMatchActivity", object.getJSONArray("mutualMatches").toString());
                for (int i=0; i <  object.getJSONArray("mutualMatches").length(); i++) {
                    try {
                        restaurants.put(object.getJSONArray("mutualMatches").get(i));
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
                matchesAdapter.notifyDataSetChanged();

            } else {
                // something went wrong
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }
}




