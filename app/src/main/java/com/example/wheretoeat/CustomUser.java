package com.example.wheretoeat;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.json.JSONArray;

import java.util.ArrayList;


public class CustomUser {
    public static final String KEY_FIRST_NAME = "firstName";
    public static final String KEY_LAST_NAME= "lastName";

    private ParseUser user;

    public CustomUser(ParseUser parseUser) {
        user = parseUser;
    }

    public String getFirstName() {
        return user.getString(KEY_FIRST_NAME);
    }

    public void setFirstName(String firstName) {
        user.put(KEY_FIRST_NAME, firstName);
        user.saveInBackground();
    }
    public String getLastName() {
        return user.getString(KEY_LAST_NAME);
    }

    public void setLastName (String lastName) {
        user.put(KEY_LAST_NAME, lastName);
        user.saveInBackground();

    }


}