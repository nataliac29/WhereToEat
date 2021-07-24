package com.example.wheretoeat.modals;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.util.Date;

@ParseClassName("Friends")
public class Friends extends ParseObject {
    public static final String KEY_INIT_USER = "initial_user";
    public static final String KEY_RECIPIENT_USER = "recipient_user";
    public static final String KEY_RESTAURANTS = "restaurants";


    public ParseUser getInitUser () {
        return getParseUser(KEY_INIT_USER);
    }

    public void setInitUser (ParseUser user) {
        put(KEY_INIT_USER, user);
    }

    public ParseUser getRecipientUser () {
        return getParseUser(KEY_RECIPIENT_USER);
    }

    public void setRecipientUser (ParseUser user) {
        put(KEY_RECIPIENT_USER, user);
    }

    public JSONArray getRestaurants () {
        return getJSONArray(KEY_RESTAURANTS);
    }

    public void setRestaurants (JSONArray restaurants) {
        put(KEY_RESTAURANTS, restaurants);
    }

}