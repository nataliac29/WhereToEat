package com.example.wheretoeat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.json.JSONArray;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder>{

    private static final String TAG = "FriendsAdapter";


    private Context context;
    private List<ParseUser> users;
    ParseUser currentUser;


    public FriendsAdapter(Context context, List<ParseUser> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }


    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParseUser user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void clear() {
        users.clear();
        notifyDataSetChanged();
    }

    // Add a list of items -- change to type used
    public void addAll(List<ParseUser> list) {
        users.addAll(list);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvName;
        private Button btnBeginMatch;

        private Friends currFriend;

        private JSONArray restaurants;




        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            btnBeginMatch = itemView.findViewById(R.id.btnBeginMatch);
            btnBeginMatch.setOnClickListener(this::onClick);


        }

        private void onClick(View view) {
            // gets item position
            int position = getAdapterPosition();
            // make sure the position is valid, i.e. actually exists in the view
            if (position != RecyclerView.NO_POSITION) {
                // get the movie at the position, this won't work if the class is static
                if (btnBeginMatch.getText().equals("Start Matching")) {
                    Log.e(TAG, "button text check");
                    Intent intent = new Intent(context, MatchActivity.class);
                    // serialize the movie using parceler, use its short name as a key
                    intent.putExtra("friendGroup", currFriend.getObjectId());
                    // show the activity
                    context.startActivity(intent);
                }
                else {
                    Intent intent = new Intent(context, ViewMatchesActivity.class);
                    // serialize the movie using parceler, use its short name as a key
//                    intent.putExtra("restaurants", (Parcelable) restaurants);
                    // show the activity
                    context.startActivity(intent);
                }
            }
        }


        public void bind(ParseUser user) {
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

            tvName.setText(user.get("firstName").toString());



            ParseQuery<ParseObject> ifCurrUserRecipientQuery = ParseQuery.getQuery("Friends");
            ifCurrUserRecipientQuery.whereEqualTo("recipient_user", currentUser);
            ifCurrUserRecipientQuery.whereEqualTo("initial_user", user);

            ParseQuery<ParseObject> ifCurrUserInitQuery = ParseQuery.getQuery("Friends");
            ifCurrUserInitQuery.whereEqualTo("initial_user", currentUser);
            ifCurrUserInitQuery.whereEqualTo("recipient_user", user);


            List<ParseQuery<ParseObject>> queries = new ArrayList<ParseQuery<ParseObject>>();
            queries.add(ifCurrUserRecipientQuery);
            queries.add(ifCurrUserInitQuery);

            ParseQuery<ParseObject> mainQuery = ParseQuery.or(queries);

            // resets button if previously disabled
            btnBeginMatch.setEnabled(true);
            mainQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null) {
                        for (ParseObject friend : objects) {
                            currFriend = (Friends) friend;
                            if (friend.getJSONArray("mutualMatches") != null) {
                                    ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            btnBeginMatch.setText("View Matches");
                                            restaurants = friend.getJSONArray("mutualMatches");
                                        }
                                    });
                            } else {
                                ParseQuery<ParseObject> checkifMatched = ParseQuery.getQuery("Matches");
                                checkifMatched.whereEqualTo("user", currentUser);
                                checkifMatched.whereEqualTo("group_id", friend);

                                checkifMatched.findInBackground(new FindCallback<ParseObject>() {
                                    @Override
                                    public void done(List<ParseObject> matchList, ParseException eMatch) {
                                        if (eMatch == null) {
                                            if (matchList.size() == 0) {
                                                    ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            btnBeginMatch.setText("Start Matching");
                                                        }
                                                    });
                                            }
                                            else {
                                                ((AppCompatActivity) context).runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        btnBeginMatch.setText("Waiting for friend's matches");
                                                        btnBeginMatch.setEnabled(false);
                                                    }
                                                });
                                            }
                                        } else {
                                           Log.e(TAG, "in checkifMatched exception", eMatch);
                                        }
                                    }
                                });

                            }
                        }
                    } else {
                        Log.e(TAG, "in main query exception", e);
                    }
                }


            });
        }

    }
}
