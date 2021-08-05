package com.example.wheretoeat.modals;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.util.ArrayList;

@ParseClassName("Matches")
public class Matches extends ParseObject {
    private static final String KEY_MATCHES = "matches";
    private static final String KEY_USER= "user";
    private static final String KEY_GROUP = "group_id";


    public String getGroupId () {
        return getString(KEY_GROUP);
    }

    public void setGroupId (String id) {
        put(KEY_GROUP, id);
    }

    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }

    public void setUser(ParseUser user) {
        put(KEY_USER, user);
    }
    public JSONArray getMatches () {
        return getJSONArray(KEY_MATCHES);
    }

    public void setMatches (JSONArray matches) {
        put(KEY_MATCHES, matches);
    }

}