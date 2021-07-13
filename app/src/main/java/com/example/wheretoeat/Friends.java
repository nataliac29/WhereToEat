package com.example.wheretoeat;

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


    public String getInitUser () {
        return getString(KEY_INIT_USER);
    }

    public void setInitUser (String username) {
        put(KEY_INIT_USER, username);
    }

    public String getRecipientUser () {
        return getString(KEY_RECIPIENT_USER);
    }

    public void setRecipientUser (String username) {
        put(KEY_RECIPIENT_USER, username);
    }

    public JSONArray getRestaurants () {
        return getJSONArray(KEY_RESTAURANTS);
    }

    public void setRestaurants (JSONArray restaurants) {
        put(KEY_RESTAURANTS, restaurants);
    }

}