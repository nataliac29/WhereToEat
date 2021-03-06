package com.example.wheretoeat.ParseService;

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

import java.util.ArrayList;
import java.util.List;

public class UserQuery {
    private static UserQuery instance = new UserQuery();

    private UserQuery() {}

    public static UserQuery getInstance() {
        return instance;
    }

    private static final String KEY_USERNAME = Constants.KEY_USERNAME;


    public void getUserFromUsername(ArrayList<String> usernames, UserQuery.getUserFromUsernameInterface listener) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereContainedIn(KEY_USERNAME, usernames);

        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> objects, ParseException e) {
                listener.onFinishGetUserFromUsername(objects, e);
            }
        });
    }


    public interface getUserFromUsernameInterface {
        void onFinishGetUserFromUsername(List<ParseUser> objects, ParseException e);
    }
}
