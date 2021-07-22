package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RestaurantDetailsActivity extends AppCompatActivity {

    public final static String TAG = "RestaurantDetailsAct";

    String restaurantId;

    ImageView ivPhoto;
    TextView tvNameDetails;
    TextView tvLink;
    RatingBar tvRatingDetails;
    TextView tvCategoriesDetails;
    TextView tvPhone;
    TextView tvAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        ivPhoto = findViewById(R.id.ivPhoto);
        tvNameDetails = findViewById(R.id.tvNameDetails);
        tvLink = findViewById(R.id.tvLink);
        ivPhoto = findViewById(R.id.ivPhoto);
        tvRatingDetails = findViewById(R.id.tvRatingDetails);
        tvCategoriesDetails = findViewById(R.id.tvCategoriesDetails);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);



        restaurantId = getIntent().getStringExtra("restaurantId");
        Log.e(TAG, "in here");
        getRestaurantDetails();
    }


    private void getRestaurantDetails() {
        final YelpService yelpService = new YelpService();
        yelpService.getRestaurantDetails(restaurantId, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonData = response.body().string();
                try {
                    JSONObject yelpJSON = new JSONObject(jsonData);
                    Log.e(TAG, yelpJSON.toString());
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            try {
                                setViews(yelpJSON);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    private void setViews(JSONObject yelpJSON) throws JSONException {

        tvNameDetails.setText(yelpJSON.getString("name"));

        tvLink.setText(yelpJSON.getString("url"));

        double rating = yelpJSON.getDouble("rating") / 2.0f;
        tvRatingDetails.setRating((float) rating);

        tvCategoriesDetails.setText(getCategories(yelpJSON.getJSONArray("categories")));

        tvPhone.setText(yelpJSON.getString("display_phone"));

        tvAddress.setText(getAddress(yelpJSON.getJSONObject("location").getJSONArray("display_address")));

        // for cover image
        String imageURL = yelpJSON.getString("image_url");
        int radius = 10; // corner radius, higher value = more rounded
        int margin = 0;
        Glide.with(this)
                .load(imageURL)
                .centerInside() // scale image to fill the entire ImageView
                .transform(new RoundedCornersTransformation(radius, margin))
                .into(ivPhoto);


    }

    private String getCategories(JSONArray categoriesList) {
        String categories = "";
        for (int i = 0; i < categoriesList.length(); i++) {
            try {
                categories = categories + " " + categoriesList.getJSONObject(i).getString("title");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return categories;
    }

    private String getAddress(JSONArray addressList) {
        String address = "";
        for (int i = 0; i < addressList.length(); i++) {
            try {
                address = address + " " + addressList.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return address;
    }
}




