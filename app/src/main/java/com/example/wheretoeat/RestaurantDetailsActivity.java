package com.example.wheretoeat;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

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
    TextView tvPrice;


    private ViewGroup reviewLinearLayout;
    private ViewGroup imagesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_details);

        tvNameDetails = findViewById(R.id.tvNameDetails);
        tvLink = findViewById(R.id.tvLink);
        tvRatingDetails = findViewById(R.id.tvRatingDetails);
        tvCategoriesDetails = findViewById(R.id.tvCategoriesDetails);
        tvPhone = findViewById(R.id.tvPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvPrice = findViewById(R.id.tvPrice);



        restaurantId = getIntent().getStringExtra("restaurantId");

        reviewLinearLayout = findViewById(R.id.detailsLayout);
        imagesLayout = findViewById(R.id.imagesLayout);

        getRestaurantDetails();
        getReviews();
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
//                    Log.e(TAG, yelpJSON.toString());
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

        tvPrice.setText(yelpJSON.getString("price"));

        JSONArray photoURLS =  yelpJSON.getJSONArray("photos");

        Log.e(TAG, "IMAGE OBJECT:" + photoURLS.toString());

        for (int i = 0; i < photoURLS.length(); i++) {
            ImageView photo = new ImageView(this);
            String imageURL = photoURLS.getString(i);
            int radius = 10; // corner radius, higher value = more rounded
            int margin = 0;
            Glide.with(this)
                    .load(imageURL)
                    .centerInside() // scale image to fill the entire ImageView
                    .transform(new RoundedCornersTransformation(radius, margin))
                    .into(photo);
            imagesLayout.addView(photo);
        }

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

    private void getReviews() {
        final YelpService yelpService = new YelpService();
        yelpService.getRestaurantReviews(restaurantId, new Callback() {

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
                                JSONArray reviews = yelpJSON.getJSONArray("reviews");
                                for (int i = 0; i < reviews.length(); i++) {
                                    JSONObject reviewObject = reviews.getJSONObject(i);
                                    // getting name of reviewer
                                    JSONObject reviewer = reviewObject.getJSONObject("user");
                                    String name = reviewer.getString("name");

                                    double rating = reviewObject.getDouble("rating");

                                    String reviewBody = reviewObject.getString("text");

                                    String timestamp = reviewObject.getString("time_created");

                                    addReviews(rating, name, reviewBody, timestamp);

                                }
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

    private void addReviews(double rating, String name, String review, String timestamp) {
        View layout2 = LayoutInflater.from(this).inflate(R.layout.item_review, reviewLinearLayout, false);


        RatingBar rbReview = layout2.findViewById(R.id.rbReview);
        TextView tvReviewerName = layout2.findViewById(R.id.tvReviewerName);
        TextView tvReviewBody = layout2.findViewById(R.id.tvReviewBody);
        TextView tvTimestamp = layout2.findViewById(R.id.tvTimestamp);


        double reviewRating = rating / 2.0f;
        rbReview.setRating((float) reviewRating);

        tvReviewerName.setText(name);
        tvReviewBody.setText(review);
        tvTimestamp.setText(timestamp);

        reviewLinearLayout.addView(layout2);
    }
}




