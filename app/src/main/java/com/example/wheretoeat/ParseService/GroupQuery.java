package com.example.wheretoeat.ParseService;

import android.util.Log;

import com.example.wheretoeat.Constants;
import com.example.wheretoeat.R;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;


public class GroupQuery {
    private static GroupQuery instance = new GroupQuery();

    private GroupQuery() {}

    public static GroupQuery getInstance() {
        return instance;
    }

    private static final String KEY_RESTAURANTS = Constants.KEY_RESTAURANTS;
    private static final String KEY_GROUPNAME = Constants.KEY_GROUPNAME;



    public void newGroup(JSONArray restaurants, String groupName, GroupQuery.newGroupInterface listener) {
        ParseObject newGroup = new ParseObject("Group");
        newGroup.put(KEY_RESTAURANTS, restaurants);
        newGroup.put(KEY_GROUPNAME, groupName);

        newGroup.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                listener.onFinishNewGroup(e, newGroup);
            }
        });
    }

    public void getGroupById(String groupId, GroupQuery.getGroupByIdInterface listener) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Group");
        // Retrieve the object by id
        query.getInBackground(groupId, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                listener.onFinishGetGroupId(object, e);
            }
        });
    }

    public interface newGroupInterface {
        void onFinishNewGroup(ParseException e, ParseObject group);
    }

    public interface getGroupByIdInterface {
        void onFinishGetGroupId(ParseObject object, ParseException e);
    }
}
