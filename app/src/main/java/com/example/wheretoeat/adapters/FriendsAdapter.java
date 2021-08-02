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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

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
    private List<ParseObject> groups;
    ParseUser currentUser;


    public FriendsAdapter(Context context, List<ParseObject> users) {
        this.context = context;
        this.groups = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

    // Add a list of items -- change to type used
    public void addAll(List<ParseObject> list) {
        groups.addAll(list);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName;
        private TextView tvGroupUsers;
        private Button btnBeginMatch;

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
                // get the movie at the position, this won't work if the class is static
                if (btnBeginMatch.getText().equals("START MATCHING")) {
                    Log.e(TAG, "button text check");
                    Intent intent = new Intent(context, MatchActivity.class);
                    // serialize the movie using parceler, use its short name as a key
                    intent.putExtra("friendGroup", currGroup.getObjectId());
                    // show the activity
                    context.startActivity(intent);
                }
                else {
                    Log.e(TAG, "in start matching button");
                    Intent intent = new Intent(context, ViewMatchesActivity.class);
                    intent.putExtra("friendGroup", currGroup.getObjectId());
                    // show the activity
                    context.startActivity(intent);
                }
            }
        }


        public void bind(ParseObject group) {
            Log.e(TAG, "in bind");

            currGroup = group;

            List<ParseUser> groupUsers = new ArrayList<>();


            if (ParseUser.getCurrentUser() != null) {
                currentUser = ParseUser.getCurrentUser();
            }
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if (isLoggedIn) {
                SharedPreferences sharedPreferences = context.getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
                String currentUserId = sharedPreferences.getString("facebook_user_id", null);
                ParseQuery<ParseUser> query = ParseUser.getQuery();
                query.whereEqualTo("objectId", currentUserId);
                // start an asynchronous call for posts
                query.findInBackground((users, e) -> {
                    if (e == null) {
                        currentUser = users.get(0);
                    } else {
                        Log.e(TAG, "Error getting Parse user" + e.getMessage());
                    }
                });
            }

            tvName.setVisibility(View.VISIBLE);

            ParseQuery<ParseObject> getOtherUsers = ParseQuery.getQuery("Matches");
            getOtherUsers.whereEqualTo("groupId", group);
            getOtherUsers.include("user");


            getOtherUsers.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        for (ParseObject parseObject : objects) {
                            if (parseObject.getParseUser("user").getObjectId().equals(currentUser.getObjectId())) {
                                Log.e(TAG, "matches user");
                                if (parseObject.getJSONArray("matches") == null) {
                                    Log.e(TAG, "matches null");
                                    ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                        @Override
                                            public void run() {
                                            btnBeginMatch.setText("START MATCHING");
                                            btnBeginMatch.setEnabled(true);
                                        }
                                    });
                                }
                                else if (group.getJSONArray("mutualMatches") != null) {
                                    Log.e(TAG, "mutual matches null");
                                    ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnBeginMatch.setText("VIEW MATCHES");
                                            btnBeginMatch.setEnabled(true);
                                        }
                                    });
                                }
                                else {
                                    ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnBeginMatch.setText("WAITING FOR MATCHES");
                                            btnBeginMatch.setEnabled(false);
                                        }
                                    });
                                }

                            } else {
                                if (objects.size() == 2) {
                                    tvName.setVisibility(View.GONE);
                                } else{
                                    tvName.setText(group.get("groupName").toString());
                                }
                                groupUsers.add(parseObject.getParseUser("user"));
                            }
                        }
                        tvGroupUsers.setText(getGroupNames(groupUsers));
                    } else {
                        Log.e(TAG, "error getting other users in group", e);
                    }
                }
            });

        }
        private String getGroupNames(List<ParseUser> usersList) {
            Log.e(TAG, "in get group names");
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

}
