package com.example.wheretoeat.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.wheretoeat.ParseService.MatchesQuery;
import com.example.wheretoeat.modals.Friends;
import com.example.wheretoeat.MatchActivity;
import com.example.wheretoeat.R;
import com.example.wheretoeat.ViewMatchesActivity;
import com.example.wheretoeat.modals.Matches;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder>{

    private static final String TAG = "FriendsAdapter";


    private Context context;

    // list of current user's groups
    private List<ParseObject> groups;

    ParseUser currentUser;

    // store instance of Parse helper classes
    MatchesQuery matchesQuery;


    public FriendsAdapter(Context context, List<ParseObject> users) {
        this.context = context;
        this.groups = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        getCurrentUser();
        matchesQuery = MatchesQuery.getInstance();
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }


    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParseObject group = groups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    public void clear() {
        groups.clear();
        notifyDataSetChanged();
    }


    class ViewHolder extends RecyclerView.ViewHolder implements MatchesQuery.getUsersInGroupInterface {

        private TextView tvName;
        private TextView tvGroupUsers;
        private Button btnBeginMatch;

        // stores Parse Object of current group
        private ParseObject currGroup;


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
                if (btnBeginMatch.getText().equals("START MATCHING")) {
                    // Create intent to begin
                    Intent intent = new Intent(context, MatchActivity.class);
                    // add group's ID as extra
                    intent.putExtra("friendGroup", currGroup.getObjectId());
                    // start matching activity
                    context.startActivity(intent);
                }
                else {
                    // If not "start matching", button has to be to view group's matches
                    // Button disabled if "waiting for other matches"

                    // set intent for View Matches activity
                    Intent intent = new Intent(context, ViewMatchesActivity.class);
                    // add group's ID as extra
                    intent.putExtra("friendGroup", currGroup.getObjectId());
                    // show the activity
                    context.startActivity(intent);
                }
            }
        }




        public void bind(ParseObject group) {
            // store group in global variable
            currGroup = group;

            // reset group name to visible in case disabled with previous group
            tvName.setVisibility(View.VISIBLE);


            matchesQuery.getUsersInGroup(currentUser, group, true, false, this);


        }

        @Override
        public void onFinishGetUsersInGroup(List<ParseObject> objects, ParseException e) {
            if (e == null) {

                // new array to store other users in group
                List<ParseUser> groupUsers = new ArrayList<>();

                // iterate through all users in group
                for (ParseObject parseObject : objects) {
                    // if iterating on current user
                    if (parseObject.getParseUser("user").getObjectId().equals(currentUser.getObjectId())) {
                        // if current user has not started matching
                        if (parseObject.getJSONArray("matches") == null) {
                            // make UI changes on UI thread
                            ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnBeginMatch.setText("START MATCHING");
                                    btnBeginMatch.setEnabled(true);
                                }
                            });
                        }
                        // if the entire group has finished matching (mutual matches recorded)
                        else if (currGroup.getJSONArray("mutualMatches") != null) {
                            ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnBeginMatch.setText("VIEW MATCHES");
                                    btnBeginMatch.setEnabled(true);
                                }
                            });
                        }
                        // if user has started matching, but no mutual matches recorded, then group is not finished
                        else {
                            ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnBeginMatch.setText("WAITING FOR MATCHES");
                                    // nothing to do while waiting for other's matches
                                    btnBeginMatch.setEnabled(false);
                                }
                            });
                        }

                    } else {
                        // if group is only 2 people, remove group name text view from view
                        if (objects.size() == 2) {
                            tvName.setVisibility(View.GONE);

                            // if group has 3+ people display group name
                        } else {
                            tvName.setText(currGroup.get("groupName").toString());
                        }

                        // add other users to groupUsers to be displayed with group info
                        groupUsers.add(parseObject.getParseUser("user"));
                    }
                }
                // display first and last names of every user in group
                tvGroupUsers.setText(getGroupNames(groupUsers));
            } else {
                Toast.makeText(FriendsAdapter.this.context, "Error getting groups", Toast.LENGTH_SHORT).show();
            }
        }


        private String getGroupNames(List<ParseUser> usersList) {
            String names = "";
            for (int i = 0; i < usersList.size(); i++) {
                if (i == 0) {
                    names = usersList.get(i).getString("firstName") + " " + usersList.get(i).getString("lastName");
                } else {
                    names = names + ", " + usersList.get(i).getString("firstName") + " " + usersList.get(i).getString("lastName");
                }
            }
            return names;
        }
  }


    private void getCurrentUser() {
        if (ParseUser.getCurrentUser() != null) {
            currentUser = ParseUser.getCurrentUser();
        } else {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (isLoggedIn) {
                SharedPreferences sharedPreferences = this.context.getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
                String currentUserId = sharedPreferences.getString("facebook_user_id", null);
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
