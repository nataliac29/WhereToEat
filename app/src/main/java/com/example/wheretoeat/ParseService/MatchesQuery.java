package com.example.wheretoeat.ParseService;

import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.wheretoeat.R;
import com.example.wheretoeat.modals.Matches;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class MatchesQuery {
    private static MatchesQuery instance = new MatchesQuery();

    private MatchesQuery() {}

    public static MatchesQuery getInstance() {
        return instance;
    }

    private static final String user_key = String.valueOf(R.string.user);
    private static final String groupId_key = String.valueOf(R.string.groupId);
    private static final String matches_key = String.valueOf(R.string.matches);
    private static final String username_key = String.valueOf(R.string.username);
    private static final String mutualMatches_key = String.valueOf(R.string.mutualMatches);





    public void getCurrUserGroups(ParseUser currUser, boolean includeGroup, boolean order, MatchesQuery.getCurrUserGroupsInterface listener) {
        ParseQuery<ParseObject> getGroups = ParseQuery.getQuery("Matches");
        getGroups.whereEqualTo(user_key, currUser);
        if (includeGroup) {
            getGroups.include(groupId_key);
        }
        if (order) {
            getGroups.orderByDescending(String.valueOf(R.string.createdAt));
        }
        // get all groups current user is a part of
        getGroups.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                listener.onFinishGetCurrUserGroups(objects, e);
            }
        });
    }

    public void getUsersInGroup(ParseUser currUser, ParseObject group, boolean includeUser, boolean omitCurrUser, MatchesQuery.getUsersInGroupInterface listener) {
        ParseQuery<ParseObject> getGroupUsers = ParseQuery.getQuery("Matches");
        getGroupUsers.whereEqualTo(groupId_key, group);

        if (includeUser) {
            getGroupUsers.include(user_key);
        }
        if (omitCurrUser) {
            getGroupUsers.whereNotEqualTo(user_key, currUser);
        }
        getGroupUsers.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                listener.onFinishGetUsersInGroup(objects, e);

            }
        });
    }


    public void checkUsernamesValid(ArrayList<String> usernames, MatchesQuery.checkUsernamesInterface listener) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereContainedIn(username_key, usernames);

        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                listener.onFinishCheckUsernames(count, e);
            }
        });
    }

    public void addMatchesRow(ParseUser user, ParseObject group) {
        Matches newMatch = new Matches();
        newMatch.put(user_key, user);
        newMatch.put(groupId_key, group);

        // Saves the new object.
        newMatch.saveInBackground();
    }

    public void getMatchRow(ParseUser user, ParseObject group, MatchesQuery.getMatchRowInterface listener) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Matches");
        query.whereEqualTo(user_key, user);
        query.whereEqualTo(groupId_key, group);

        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                listener.onFinishGetMatchRow(object, e);
            }
        });
    }

    public void updateMatches(ParseObject matches, JSONArray restaurants, MatchesQuery.updateMatchesInterface listener) {
        matches.put("matches", restaurants);
        matches.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                listener.onFinishUpdateMatches(e);
            }
        });

    }

    public void checkMatchingDone(ParseObject group, MatchesQuery.checkMatchingDoneInterface listener) {

        ParseQuery<ParseObject> checkifMatched = ParseQuery.getQuery("Matches");
        checkifMatched.whereEqualTo(groupId_key, group);
        checkifMatched.whereEqualTo(matches_key, null);


        checkifMatched.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                listener.onFinishCheckingMatchingDone(objects, e);
            }
        });

    }
    public void saveMutualMatches(ParseObject group, JSONArray mutualMatches) {
        group.put(mutualMatches_key, mutualMatches);
        group.saveInBackground();
    }

    public interface getCurrUserGroupsInterface {
        void onFinishGetCurrUserGroups(List<ParseObject> objects, ParseException e);
    }
    public interface getUsersInGroupInterface {
        void onFinishGetUsersInGroup(List<ParseObject> objects, ParseException e);
    }
    public interface checkUsernamesInterface {
        void onFinishCheckUsernames(int count, ParseException e);
    }
    public interface getMatchRowInterface {
        void onFinishGetMatchRow(ParseObject object, ParseException e);
    }
    public interface updateMatchesInterface {
        void onFinishUpdateMatches(ParseException e);
    }
    public interface checkMatchingDoneInterface {
        void onFinishCheckingMatchingDone(List<ParseObject> objects, ParseException e);
    }
}
