package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wheretoeat.ParseService.GroupQuery;
import com.example.wheretoeat.adapters.ViewMatchesAdapter;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import org.json.JSONArray;
import org.json.JSONException;

public class ViewMatchesActivity extends AppCompatActivity implements GroupQuery.getGroupByIdInterface {

    JSONArray restaurants;
    String currGroup;

    ViewMatchesAdapter matchesAdapter;
    RecyclerView rvMovies;
    TextView tvNoMatches;

    GroupQuery groupQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_matches);

        rvMovies = findViewById(R.id.rvMatches);
        tvNoMatches = findViewById(R.id.tvNoMatches);

        //Create the adapter
        restaurants = new JSONArray();
        currGroup = getIntent().getStringExtra("friendGroup");
        matchesAdapter = new ViewMatchesAdapter(ViewMatchesActivity.this, restaurants, currGroup);

        // set the adapter on the recycler view
        rvMovies.setAdapter(matchesAdapter);
        //Set a layout Manager on the recycler view
        rvMovies.setLayoutManager((new LinearLayoutManager(this)));

        // initialize Parse helper class
        groupQuery = GroupQuery.getInstance();

        // retrieve group by id
        groupQuery.getGroupById(currGroup, this);
    }

    @Override
    public void onFinishGetGroupId(ParseObject object, ParseException e) {
        if (e == null) {
            if (object.getJSONArray(Constants.KEY_MUTUALMATCHES).length() != 0) {
                Log.e("View Matches", "here1");
                rvMovies.setVisibility(View.VISIBLE);
                tvNoMatches.setVisibility(View.GONE);
                for (int i = 0; i < object.getJSONArray(Constants.KEY_MUTUALMATCHES).length(); i++) {
                    try {
                        restaurants.put(object.getJSONArray(Constants.KEY_MUTUALMATCHES).get(i));
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
                matchesAdapter.notifyDataSetChanged();
            } else {
                Log.e("View Matches", "here2");
                rvMovies.setVisibility(View.GONE);
                tvNoMatches.setVisibility(View.VISIBLE);
            }

        } else {
            // something went wrong
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}




