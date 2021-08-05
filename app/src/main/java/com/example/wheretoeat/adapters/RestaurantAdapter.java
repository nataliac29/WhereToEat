package com.example.wheretoeat.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;


import com.bumptech.glide.Glide;
import com.example.wheretoeat.Constants;
import com.example.wheretoeat.R;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class RestaurantAdapter extends BaseAdapter {
    Context context;
    JSONArray restaurants;
    JSONObject restaurant;


    private static final Integer FIRST_CATEGORY = 0;


    public interface OnClickListener {
        void onRestaurantLike();
        void onRestaurantDislike();
    }

    private OnClickListener onClickListener;

    public RestaurantAdapter(Context context, JSONArray restaurants, OnClickListener onClickListener) {
        this.context = context;
        this.restaurants = restaurants;
        this.onClickListener = onClickListener;
    }

    // Returns the total count of items in the list
    @Override
    public int getCount() {
        return restaurants.length();
    }

    @Override
    public Object getItem(int position) {
        try {
            return restaurants.get(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        TextView tvRestaurantName;
        RatingBar tvRestaurantRating;
        TextView tvRestaurantCategories;
        Button btnLikeRestaurant;
        Button btnDislike;
        ImageView ivCover;


        String imageUrl;

        View itemView = convertView;
        if (itemView == null) {
            // on below line we are inflating our layout.
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restaurant, parent, false);
        }

        try {
            restaurant = (JSONObject) restaurants.get(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
        tvRestaurantCategories = itemView.findViewById(R.id.tvRestaurantCategories);
        tvRestaurantRating = itemView.findViewById(R.id.tvRestaurantRating);
        btnLikeRestaurant = itemView.findViewById(R.id.btnLikeRestaurant);
        ivCover = itemView.findViewById(R.id.ivCover);
        btnDislike = itemView.findViewById(R.id.btnDislike);


        int radius = 10; // corner radius, higher value = more rounded
        int margin = 0;

        try {
            imageUrl = restaurant.getString(Constants.KEY_IMAGE_URL);
        } catch (JSONException e) {
             imageUrl = null;
            e.printStackTrace();
        }

        Glide.with(context)
                .load(imageUrl)
                .centerInside() // scale image to fill the entire ImageView
                .transform(new RoundedCornersTransformation(radius, margin))
                .into(ivCover);


        try {
            tvRestaurantName.setText(restaurant.getString(Constants.KEY_NAME));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            tvRestaurantCategories.setText(restaurant.getJSONArray(Constants.KEY_CATEGORIES).getString(FIRST_CATEGORY));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        double rating = 0;
        try {
            rating = restaurant.getDouble(Constants.KEY_RATING) / 2.0f;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tvRestaurantRating.setRating((float) rating);
        btnLikeRestaurant.setText(R.string.MatchLike);
        btnDislike.setText(R.string.MatchDislike);

        btnLikeRestaurant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onRestaurantLike();
            }
        });

        btnDislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onRestaurantDislike();
            }
        });


        return itemView;

    }
}

