package com.example.wheretoeat.ParseService;

import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class MatchesQuery {
    public static void getCurrUserGroups(ParseUser currUser, boolean includeGroup, boolean order, Fragment fragment) {
        ParseQuery<ParseObject> getGroups = ParseQuery.getQuery("Matches");
        getGroups.whereEqualTo("user", currUser);
        if (includeGroup) {
            getGroups.include("groupId");
        }
        if (order) {
            getGroups.orderByDescending("createdAt");
        }
        // get all groups current user is a part of
        getGroups.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                MatchesQuery.getCurrUserGroupsInterface listener = (MatchesQuery.getCurrUserGroupsInterface) fragment;
                listener.onFinishGetCurrUserGroups(objects, e);
            }
        });
    }

    public static void getUsersInGroup(ParseUser currUser, ParseObject group, boolean includeUser, boolean omitCurrUser, Fragment fragment, ActivityCompat activityCompat) {
        ParseQuery<ParseObject> getGroupUsers = ParseQuery.getQuery("Matches");
        getGroupUsers.whereEqualTo("groupId", group);
        getGroupUsers.whereNotEqualTo("user", currUser);
        if (includeUser) {
            getGroupUsers.include("user");
        }
        if (omitCurrUser) {
            getGroupUsers.whereNotEqualTo("user", currUser);
        }
        getGroupUsers.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                getUsersInGroupInterface listener;
                if (fragment != null) {
                    listener = (getUsersInGroupInterface) fragment;
                }
                else {
                    listener = (getUsersInGroupInterface) activityCompat;
                }
                listener.onFinishGetUsersInGroup(objects, e);

            }
        });
    }


    public static void checkUsernamesValid(ArrayList<String> usernames, Fragment fragment) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereContainedIn("username", usernames);

        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                MatchesQuery.checkUsernamesInterface listener = (MatchesQuery.checkUsernamesInterface) fragment ;
                listener.onFinishCheckUsernames(count, e);
            }
        });
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
}
