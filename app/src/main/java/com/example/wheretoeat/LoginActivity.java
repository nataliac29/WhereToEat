package com.example.wheretoeat;

import androidx.annotation.Nullable;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.wheretoeat.modals.CurrentUser;
import com.facebook.AccessToken;

import com.facebook.GraphRequest;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.facebook.ParseFacebookUtils;

import org.json.JSONException;

import java.util.Arrays;
import java.util.Collection;


public class LoginActivity extends AppCompatActivity {

    // Toast/Alert messages
    public static final String ERROR_LOGGING_IN = "Error logging in";
    public static final String SUCCESS = "Success!";
    public static final String OK = "OK";
    public static final String LOGIN_CANCELLED = "The user cancelled the Facebook login.";
    public static final String NEW_USER = "User signed up and logged in through Facebook.";
    public static final String USER_LOGGED_IN = "User logged in through Facebook.";
    public static final String WELCOME = "Welcome back!";
    public static final String OH_YOU = "Oh, you!";
    public static final String PROFILE = "public_profile";
    public static final String EMAIL = "email";
    public static final String WAIT = "Please, wait a moment.";
    public static final String IN_PROGRESS = "Logging in...";


    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnToSignUp;
    private Button btnLoginFB;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ParseFacebookUtils.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ParseUser.getCurrentUser() != null) {
            goMainActivity();
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnToSignUp = findViewById(R.id.btnToSignUp);
        btnLoginFB = findViewById(R.id.btnLoginFB);

        // login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString();
                String password = etPassword.getText().toString();
                loginUser(username, password);
            }
        });
        // click listener for sign up page
        btnToSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create intent for the new activity
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                // show the activity
                LoginActivity.this.startActivity(intent);
            }
        });

        btnLoginFB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog dialog = new ProgressDialog(LoginActivity.this);
                dialog.setTitle(WAIT);
                dialog.setMessage(IN_PROGRESS);
                dialog.show();
                Collection<String> permissions = Arrays.asList(PROFILE, EMAIL);
                ParseFacebookUtils.logInWithReadPermissionsInBackground(LoginActivity.this, permissions, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        dialog.dismiss();
                        if (e == null) {
                            if (user == null) {
                                Toast.makeText(LoginActivity.this, LOGIN_CANCELLED, Toast.LENGTH_LONG).show();
                            } else if (user.isNew()) {
                                Toast.makeText(LoginActivity.this, NEW_USER, Toast.LENGTH_LONG).show();
                                CurrentUser.getInstance().currUser = ParseUser.getCurrentUser();
                                getUserDetailFromFB();
                            } else {
                                Toast.makeText(LoginActivity.this, USER_LOGGED_IN, Toast.LENGTH_LONG).show();
                                CurrentUser.getInstance().currUser = ParseUser.getCurrentUser();
                                showAlert(OH_YOU, WELCOME);
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }

                });


            }
        });

    }



    // Native app login
    private void loginUser(String username, String password) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Toast.makeText(LoginActivity.this, ERROR_LOGGING_IN, Toast.LENGTH_LONG).show();
                }
                // Navigate to main activity if user has signed in successfully
                goMainActivity();
                Toast.makeText(LoginActivity.this, SUCCESS, Toast.LENGTH_SHORT);
            }

        });
    }


    private void goMainActivity() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
        finish();
    }


    private void getUserDetailFromFB() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), (object, response) -> {
            ParseUser user = ParseUser.getCurrentUser();
            // get info from token to populate user row, get name and user email for username
            try {
                if (object.has("name"))
                    user.put("name", (object.getString("name")));
                if (object.has("email"))
                    user.setUsername(object.getString("email"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            user.saveInBackground(e -> {
                if (e == null) {
                    showAlert("First Time Login!", "Welcome!");
                } else
                    showAlert("Error", e.getMessage());
            });
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(OK, (dialog, which) -> {
                    dialog.cancel();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                });
        AlertDialog ok = builder.create();
        ok.show();
    }


}