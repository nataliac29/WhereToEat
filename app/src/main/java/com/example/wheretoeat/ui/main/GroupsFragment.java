package com.example.wheretoeat.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.wheretoeat.Friends;
import com.example.wheretoeat.FriendsAdapter;
import com.example.wheretoeat.R;
import com.facebook.AccessToken;
import com.facebook.Profile;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class GroupsFragment extends Fragment {

    public GroupsFragment() {
        // Required empty public constructor
    }

    RecyclerView rvFriends;

    private FriendsAdapter adapter;

    private List<ParseUser> allUsers;

    SwipeRefreshLayout swipeContainer;

    String currentUserId;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_groups, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvFriends = view.findViewById(R.id.rvFriends);
        allUsers = new ArrayList<>();
        adapter = new FriendsAdapter(getContext(), allUsers);

        rvFriends.setAdapter(adapter);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext()));

        // Lookup the swipe container view
        swipeContainer =  view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                queryFriends();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        queryFriends();
    }



    private void queryFriends() {

        // get current user id

        if (ParseUser.getCurrentUser() != null) {
            currentUserId = ParseUser.getCurrentUser().getObjectId();
        }
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            Profile profile = Profile.getCurrentProfile();
            currentUserId = profile.getId();
        }

        ParseQuery<Friends> checkRecipientQuery = ParseQuery.getQuery("Friends");
        checkRecipientQuery.whereEqualTo("recipient_user", currentUserId);

        ParseQuery<Friends> checkInitialQuery = ParseQuery.getQuery("Friends");
        checkInitialQuery.whereEqualTo("initial_user", currentUserId);

        List<ParseQuery<Friends>> queries = new ArrayList<ParseQuery<Friends>>();
        queries.add(checkRecipientQuery);
        queries.add(checkInitialQuery);

        ParseQuery<Friends> mainQuery = ParseQuery.or(queries);

        mainQuery.findInBackground((userList, e) -> {
            if(e == null){
                for (Friends user : userList) {
                    Log.d("Object found ",user.getObjectId());
                }
            }else{
                Toast.makeText(getContext(), "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK ) {
//            // Get data from the intent (the tweet)
//            Post post = Parcels.unwrap(data.getParcelableExtra("post"));
//
//            //Update the RV with the new tweet
//            //Modify data source
//            allPosts.add(0, post);
//            //update the adapter
//            adapter.notifyItemInserted(0);
//            rvPosts.smoothScrollToPosition(0);
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
}


//        // save received posts to list and notify adapter of new data
//        adapter.clear();
//        // ...the data has come back, add new items to your adapter...
//        adapter.addAll(posts);
//        adapter.notifyDataSetChanged();
//        swipeContainer.setRefreshing(false);