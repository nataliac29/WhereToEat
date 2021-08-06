package com.example.wheretoeat.ParseService;

import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.wheretoeat.Constants;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MatchesQuery {
    private static MatchesQuery instance = new MatchesQuery();

    private MatchesQuery() {}

    public static MatchesQuery getInstance() {
        return instance;
    }

    private static final String KEY_USER = Constants.KEY_USER;
    public static final String KEY_GROUPID = Constants.KEY_GROUPID;
    private static final String KEY_MATCHES = Constants.KEY_MATCHES;
    private static final String KEY_USERNAME = Constants.KEY_USERNAME;
    private static final String KEY_MUTUALMATCHES = Constants.KEY_MUTUALMATCHES;
    private static final String KEY_CREATEDAT = Constants.KEY_CREATEDAT;

    // other constants
    private static final int SIZE_NON_GROUP = 2;


    // following keys public so can be references in friends adapter to check object returned

    // basic group object keys
    public static final String KEY_GROUP_STATE = "state";
    public static final String KEY_GROUP_MEMBERS = "groupMembers";
    public static final String KEY_GROUP_NAME = Constants.KEY_GROUPNAME;

    // group state options
    public static final String STATE_START = "startMatching";
    public static final String STATE_WAIT = "waitMatches";
    public static final String STATE_VIEW = "viewMatches";






    public void getCurrUserGroups(ParseUser currUser, boolean includeGroup, boolean order, MatchesQuery.getCurrUserGroupsInterface listener) {
        ParseQuery<ParseObject> getGroups = ParseQuery.getQuery("Matches");
        getGroups.whereEqualTo(KEY_USER, currUser);
        if (includeGroup) {
            getGroups.include(KEY_GROUPID);
        }
        if (order) {
            getGroups.orderByDescending(KEY_CREATEDAT);
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
        getGroupUsers.whereEqualTo(KEY_GROUPID, group);

        if (includeUser) {
            getGroupUsers.include(KEY_USER);
        }
        if (omitCurrUser) {
            getGroupUsers.whereNotEqualTo(KEY_USER, currUser);
        }
        getGroupUsers.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                listener.onFinishGetUsersInGroup(objects, e);

            }
        });
    }
    public void getBasicGroupInfo(ParseUser currUser, ParseObject group, MatchesQuery.getBasicGroupInfoInterface listener) {
        ParseQuery<ParseObject> getGroupUsers = ParseQuery.getQuery("Matches");
        getGroupUsers.whereEqualTo(KEY_GROUPID, group);

        // include whole user object from pointer to get first/last name of each group member
        getGroupUsers.include(KEY_USER);

        getGroupUsers.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                // new object to store this group's basic info
                JSONObject currentGroupInfo = new JSONObject();

                // list for names of other group members
                List<ParseUser> groupUsers = new ArrayList<>();

                // add key with groupId
                try {
                    currentGroupInfo.put(KEY_GROUPID, group.getObjectId());
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                // iterate through all users in group to find state of group
                for (ParseObject parseObject : objects) {
                    // if iterating on current user
                    if (parseObject.getParseUser(KEY_USER).getObjectId().equals(currUser.getObjectId())) {
                        // if current user has not started matching
                        if (parseObject.getJSONArray(KEY_MATCHES) == null) {
                            try {
                                currentGroupInfo.put(KEY_GROUP_STATE, STATE_START);
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                        // if the entire group has finished matching (mutual matches recorded)
                        else if (group.getJSONArray(KEY_MUTUALMATCHES) != null) {
                            try {
                                currentGroupInfo.put(KEY_GROUP_STATE, STATE_VIEW);
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                        // if user has started matching, but no mutual matches recorded, then group is not finished
                        else {
                            try {
                                currentGroupInfo.put(KEY_GROUP_STATE, STATE_WAIT);
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }

                    } else {
                        // add other users to groupUsers to be displayed with group info
                        groupUsers.add(parseObject.getParseUser(KEY_USER));
                    }
                }
                // if group is only 2 people, remove group name text view from view
                if (objects.size() == SIZE_NON_GROUP) {
                    try {
                        currentGroupInfo.put(KEY_GROUP_NAME, "");
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                    // if group has 3+ people display group name
                } else {
                    try {
                        currentGroupInfo.put(KEY_GROUP_NAME, group.getString(Constants.KEY_GROUPNAME));
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }
                }
                // display first and last names of every user in group
                try {
                    currentGroupInfo.put(KEY_GROUP_MEMBERS, getGroupNames(groupUsers));
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                listener.onFinishBasicGroupInfo(currentGroupInfo, e);
            }
        });
    }


    public void checkUsernamesValid(ArrayList<String> usernames, MatchesQuery.checkUsernamesInterface listener) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereContainedIn(KEY_USERNAME, usernames);

        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                listener.onFinishCheckUsernames(count, e);
            }
        });
    }

    public void addMatchesRow(ParseUser user, ParseObject group) {
        Matches newMatch = new Matches();
        newMatch.put(KEY_USER, user);
        newMatch.put(KEY_GROUPID, group);

        // Saves the new object.
        newMatch.saveInBackground();
    }

    public void getMatchRow(ParseUser user, ParseObject group, MatchesQuery.getMatchRowInterface listener) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Matches");
        query.whereEqualTo(KEY_USER, user);
        query.whereEqualTo(KEY_GROUPID, group);

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
        checkifMatched.whereEqualTo(KEY_GROUPID, group);
        checkifMatched.whereEqualTo(KEY_MATCHES, null);


        checkifMatched.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                listener.onFinishCheckingMatchingDone(objects, e);
            }
        });

    }
    public void saveMutualMatches(ParseObject group, JSONArray mutualMatches) {
        group.put(KEY_MUTUALMATCHES, mutualMatches);
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

    public interface getBasicGroupInfoInterface {
        void onFinishBasicGroupInfo(JSONObject objects, ParseException e);
    }


    // helper functions
    private String getGroupNames(List<ParseUser> usersList) {
        String names = "";
        for (int i = 0; i < usersList.size(); i++) {
            names = names + ", " + usersList.get(i).getString(Constants.KEY_NAME);
        }
        // substring removes the comma put before the first group member
        return names.substring(2);
    }
}
