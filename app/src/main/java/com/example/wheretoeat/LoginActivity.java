package com.example.wheretoeat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
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
import org.parceler.Parcels;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import java.util.Arrays;
import java.util.List;


public class LoginActivity extends AppCompatActivity {
    public static final String TAG = "LoginActivity";
    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnToSignUp;

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
            goMainActivity();
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnToSignUp = findViewById(R.id.btnToSignUp);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "OnClick login button");
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                loginUser(username, password);
            }
        });

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

        final String EMAIL = "email";

        loginButton = (LoginButton) findViewById(R.id.fb_login_button);
         loginButton.setReadPermissions(Arrays.asList(EMAIL, "public_profile"));
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if(Profile.getCurrentProfile() == null) {
                    mProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                            String id = currentProfile.getId();
                            addNewUser(id);
                            mProfileTracker.stopTracking();
                            goMainActivity();
                        }
                    };
                    // no need to call startTracking() on mProfileTracker
                    // because it is called by its constructor, internally.
                }
                else {
                    Profile profile = Profile.getCurrentProfile();
                    Log.v("facebook - profile", profile.getFirstName());
                }
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
    private void loginUser(String username, String password) {
        Log.i(TAG, "Attempting to login user" + username);
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Issue with login", e);
                    return;
                }
                ///Navigate to main activity if user has signed in successfully
                goMainActivity();
                Toast.makeText(LoginActivity.this, "Success!", Toast.LENGTH_SHORT);
            }

        });
    }

        private void addNewUser(String id){
            ParseQuery<ParseUser> query = ParseUser.getQuery();
            query.whereEqualTo("objectId", id);
            // start an asynchronous call for posts
            query.findInBackground((users, e) -> {
                if (e == null) {
                    Log.e(TAG, "users" + users);
                    if (users.size() == 0) {
                        Log.e(TAG, "here");
                        GraphRequest request = GraphRequest.newMeRequest(
                                AccessToken.getCurrentAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(JSONObject object, GraphResponse response) {
                                        Log.e(TAG, "here pt2");
                                        JSONObject json = response.getJSONObject();
                                        try {
                                            if (json != null) {
                                                Log.e(TAG, "here pt3");
                                                String firstName = json.getString("first_name");
                                                String lastName = json.getString("last_name");
                                                String username = json.getString("email");
                                                String password = "default";
                                                signupUser(username, password, firstName, lastName);
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
                    }
                } else {
                    Log.e(TAG, "check existing user" + e.getMessage());
                    // Something went wrong.
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }


    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void signupUser(String username, String password, String firstName, String lastName) {
        Log.e(TAG, "In signup user");
        ParseUser user = new ParseUser();
        CustomUser customUser = new CustomUser(user);
        // Set core properties
        customUser.setFirstName(firstName);
        customUser.setLastName(lastName);
        user.setUsername(username);
        user.setPassword(password);
        // Invoke signUpInBackground
        user.signUpInBackground(new SignUpCallback() {
            public void done(ParseException e) {
                if (e == null) {
                    Log.e(TAG, "In signup user --success");
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
}
