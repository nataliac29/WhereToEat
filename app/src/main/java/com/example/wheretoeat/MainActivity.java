package com.example.wheretoeat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.AccessToken;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.wheretoeat.ui.main.SectionsPagerAdapter;
import com.example.wheretoeat.databinding.ActivityMainBinding;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = binding.fab;

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ParseUser.getCurrentUser() != null) {
                    ParseUser.logOut();
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // this makes sure the Back button won't work
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // same as above
                    startActivity(i);
                }
                AccessToken accessToken = AccessToken.getCurrentAccessToken();
                Log.e("Main_Activity", "access token" + accessToken);
                boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
                if (isLoggedIn) {
                    removeIdSharePref();
                    LoginManager.getInstance().logOut();
                    Intent i = new Intent(MainActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // this makes sure the Back button won't work
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // same as above
                    startActivity(i);
                }
            }
        });
    }

    private void removeIdSharePref() {
        SharedPreferences sharedPreferences = getSharedPreferences("FB_userId", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("facebook_user_id", null);
        editor.apply();
    }
}