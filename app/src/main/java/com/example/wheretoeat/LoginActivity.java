package com.example.wheretoeat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.wheretoeat.modals.CustomUser;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;


public class LoginActivity extends AppCompatActivity {
    public static final String TAG = "LoginActivity";
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnToSignUp;

    private String username;

    //    Facebook Login
    CallbackManager callbackManager;
    LoginButton loginButton;
    ProfileTracker mProfileTracker;



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        callbackManager = CallbackManager.Factory.create();

        if (ParseUser.getCurrentUser() != null) {
            goMainActivity();
        }

        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        if (isLoggedIn) {
            SharedPreferences sharedPreferences = this.getSharedPreferences("FB_userId", Context.MODE_PRIVATE);
            String currentUserId = sharedPreferences.getString("facebook_user_id", null);
            if (currentUserId == null) {
                GraphRequest request = GraphRequest.newMeRequest(
                        AccessToken.getCurrentAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.e(TAG, "here pt2");
                                JSONObject json = response.getJSONObject();
                                Log.e(TAG, json.toString());
                                if (json != null) {
                                    try {
                                        username = json.getString("email");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Log.e(TAG, username);
                                    // check if user has signed in before
                                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                                    query.whereEqualTo("username", username);
                                    // start an asynchronous call for user
                                    query.findInBackground(new FindCallback<ParseUser>() {
                                        @Override
                                        public void done(List<ParseUser> objects, ParseException e) {
                                            Log.e(TAG, "in done login");
                                            if (objects.size() == 0) {
                                                // error, show error text
                                                Log.e(TAG, "error auto logging in");
                                                // Add user to Parse user table, using email as username
                                            } else {
                                                // if user has already signed up, store id in Shared Preferences
                                                Log.e(TAG, "here pt56");
                                                addIdSharePref(objects.get(0).getObjectId());
                                            }
                                        }
                                    });
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email");
                request.setParameters(parameters);
                request.executeAsync();
            }
            goMainActivity();
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnToSignUp = findViewById(R.id.btnToSignUp);

        // login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "OnClick login button");
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                loginUser(username, password);
            }
        });
        // click listener for sign up page
        btnToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "OnClick login button");
                // create intent for the new activity
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                // show the activity
                LoginActivity.this.startActivity(intent);
            }
        });

        // Setting permissions for FB login
        final String EMAIL = "email";
        loginButton = (LoginButton) findViewById(R.id.fb_login_button);
        loginButton.setReadPermissions(Arrays.asList(EMAIL, "public_profile"));

        // Callback registration, login FB user
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // get more info about logged in FB user for Parse
                GraphRequest request = GraphRequest.newMeRequest(
                        AccessToken.getCurrentAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.e(TAG, "here pt2");
                                JSONObject json = response.getJSONObject();
                                try {
                                    if (json != null) {
                                        String username = json.getString("email");
                                        String firstName = json.getString("first_name");
                                        String lastName = json.getString("last_name");
                                        String id = json.getString("id");
                                        // check if user has signed in before
                                        checkNewUser(username, firstName, lastName, id);
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "graphql error", e);
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,first_name,last_name,email");
                request.setParameters(parameters);
                request.executeAsync();
                goMainActivity();
            }

            @Override
            public void onCancel() {
                Log.e(TAG, "FB login on cancel");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(TAG, "FB login on error", exception);
            }
        });

    }

    // Native app login
    private void loginUser(String username, String password) {
        Log.i(TAG, "Attempting to login user" + username);
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with login", e);
                    return;
                }
                // Navigate to main activity if user has signed in successfully
                goMainActivity();
                Toast.makeText(LoginActivity.this, "Success!", Toast.LENGTH_SHORT);
            }

        });
    }
    // check if FB user has logged in before, if not call signupUser to create new Parse user
    // if they have, save id in shared preferences
    private void checkNewUser(String username, String firstName, String lastName, String id){
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("username", username);
        // start an asynchronous call for posts
        query.findInBackground((users, e) -> {
            if (e == null) {
                Log.e(TAG, "users" + users);
                // if first time logging in
                if (users.size() == 0) {
                    Log.e(TAG, "here");
                    // Add user to Parse user table, using email as username
                    signupUser(username, "default", firstName, lastName);
                }
                else {
                    // if user has already signed up, store id in Shared Preferences
                    addIdSharePref(users.get(0).getObjectId());
                }
            } else {
                Log.e(TAG, "check existing user" + e.getMessage());
                // Something went wrong.
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void signupUser(String username, String password, String firstName, String lastName) {
        Log.e(TAG, "In signup user");
        ParseUser user = new ParseUser();
        CustomUser customUser = new CustomUser(user);
        // Set core properties
        user.setUsername(username);
        user.setPassword(password);
        // Set custom properties
        customUser.setFirstName(firstName);
        customUser.setLastName(lastName);

        // Invoke signUpInBackground
        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.e(TAG, "In signup user --success");
                    // add id to Shared Preferences
                    addIdSharePref(user.getObjectId());
                    goMainActivity();
                    Toast.makeText(LoginActivity.this, "Success!", Toast.LENGTH_SHORT);
                } else {
                    Log.e(TAG, "Issue with login", e);
                    Toast.makeText(LoginActivity.this, "Error!", Toast.LENGTH_SHORT);
                    return;
                }
            }
        });

    }
    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    private void addIdSharePref(String id) {
        SharedPreferences sharedPreferences = getSharedPreferences("FB_userId", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("facebook_user_id", id);
//        editor.putString("facebook_user_id", user.getObjectId());
        editor.apply();
    }


}