package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.wheretoeat.adapters.ViewMatchesAdapter;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;

public class ViewMatchesActivity extends AppCompatActivity {

    JSONArray restaurants;
    String currGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_matches);

        RecyclerView rvMovies = findViewById(R.id.rvMatches);
        //Create the adapter
        restaurants = new JSONArray();
        currGroup = getIntent().getStringExtra("friendGroup");
        ViewMatchesAdapter matchesAdapter = new ViewMatchesAdapter(ViewMatchesActivity.this, restaurants, currGroup);

        // set the adapter on the recycler view
        rvMovies.setAdapter(matchesAdapter);
        //Set a layout Manager on the recycler view
        rvMovies.setLayoutManager((new LinearLayoutManager(this)));

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Group");
        Log.e("ViewMatchActivity", "in update restaurants");
        // Retrieve the object by id
        query.getInBackground(currGroup, (object, e) -> {
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




