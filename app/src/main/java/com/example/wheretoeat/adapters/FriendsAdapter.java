package com.example.wheretoeat.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wheretoeat.Constants;
import com.example.wheretoeat.ParseService.MatchesQuery;
import com.example.wheretoeat.MatchActivity;
import com.example.wheretoeat.R;
import com.example.wheretoeat.ViewMatchesActivity;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder>{


    // key for intent extra to MatchActivity and ViewMatchesActivity
    private final String KEY_INTENT = "friendGroup";

    // Toast messages
    private final String USER_ERROR = "Error getting Parse User";


    private Context context;

    // list of current user's groups
    private List<JSONObject> groups;

    ParseUser currentUser;

    // store instance of Parse helper classes
    MatchesQuery matchesQuery;


    public FriendsAdapter(Context context, List<JSONObject> users) {
        this.context = context;
        this.groups = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        currentUser = ParseUser.getCurrentUser();
        matchesQuery = MatchesQuery.getInstance();
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }


    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JSONObject group = groups.get(position);
        try {
            holder.bind(group);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void clear() {
        groups.clear();
        notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder{

        private TextView tvName;
        private TextView tvGroupUsers;
        private Button btnBeginMatch;

        // stores Parse Object of current group
        private String currGroupId;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvGroupUsers = itemView.findViewById(R.id.tvGroupUsers);
            btnBeginMatch = itemView.findViewById(R.id.btnBeginMatch);
            btnBeginMatch.setOnClickListener(this::onClick);


        }

        private void onClick(View view) {
            // gets item position
            int position = getAdapterPosition();
            // make sure the position is valid, i.e. actually exists in the view
            if (position != RecyclerView.NO_POSITION) {
                // start activity based button text, which is set based on state of group's matches

                // if current user has not matches for this group
                if (btnBeginMatch.getText().equals(context.getString(R.string.StartMatching))) {
                    // Create intent to begin
                    Intent intent = new Intent(context, MatchActivity.class);
                    // add group's ID as extra
                    intent.putExtra(KEY_INTENT, currGroupId);
                    // start matching activity
                    context.startActivity(intent);
                }
                else {
                    // If not "start matching", button has to be to view group's matches
                    // Button disabled if "waiting for other matches"

                    // set intent for View Matches activity
                    Intent intent = new Intent(context, ViewMatchesActivity.class);
                    // add group's ID as extra
                    intent.putExtra(KEY_INTENT, currGroupId);
                    // show the activity
                    context.startActivity(intent);
                }
            }
        }




        public void bind(JSONObject group) throws JSONException {
            // store group in global variable
            currGroupId = group.getString(MatchesQuery.KEY_GROUPID);
            // reset group name to visible in case disabled with previous group
            tvName.setVisibility(View.VISIBLE);

            // depending on state of group adjust button text
            switch(group.getString(MatchesQuery.KEY_GROUP_STATE)) {
                case MatchesQuery.STATE_START:
                    btnBeginMatch.setText(R.string.StartMatching);
                    btnBeginMatch.setEnabled(true);
                    break;
                case MatchesQuery.STATE_VIEW:
                    btnBeginMatch.setText(R.string.ViewMatches);
                    btnBeginMatch.setEnabled(true);
                    break;
                case MatchesQuery.STATE_WAIT:
                    btnBeginMatch.setText(R.string.WaitMatches);
                    btnBeginMatch.setEnabled(false);
                    break;
            }

            String groupName = group.getString(MatchesQuery.KEY_GROUP_NAME);
            // if only a 2 person group, no group name, so remove group name text view and bold name of other user
            if ( groupName.equals("")) {
                tvName.setVisibility(View.GONE);
                tvGroupUsers.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                tvName.setText(groupName);
                // make sure text view is not left bold from previous group
                tvGroupUsers.setTypeface(Typeface.DEFAULT);
            }

            tvGroupUsers.setText(group.getString(MatchesQuery.KEY_GROUP_MEMBERS));

        }
  }


}
