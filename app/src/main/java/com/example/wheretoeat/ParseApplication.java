package com.example.wheretoeat;

import android.app.Application;

import com.example.wheretoeat.modals.Friends;
import com.example.wheretoeat.modals.Matches;
import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //Register Parse models
//        ParseObject.registerSubclass(Friends.class);
        ParseObject.registerSubclass(Matches.class);




        //Registering Parse models
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());
    }
}
