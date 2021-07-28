package com.example.wheretoeat.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.wheretoeat.DataTransferInterface;
import com.example.wheretoeat.R;
import com.example.wheretoeat.modals.Restaurant;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;
import org.w3c.dom.Text;

import java.util.List;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class RestaurantAdapter extends BaseAdapter {
    Context context;
    JSONArray restaurants;
    JSONObject restaurant;
    JSONArray likedRestaurants;
    String friendId;

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

//    //Usually involves inflating a layout from XML and retuning the holder
//    @NotNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
//        View restaurantView = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
//        likedRestaurants = new JSONArray();
//        return new ViewHolder(restaurantView);
//    }
//    //involves populating data into the item through holder
//    @Override
//    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
//        //Get the movie at the passed in position
//        JSONObject restaurant = null;
//        try {
//            restaurant = (JSONObject) restaurants.get(position);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        // Bind the movie data into the viewholder
//        try {
//            holder.bind(restaurant);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

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
            imageUrl = restaurant.getString("imageUrl");
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
            tvRestaurantName.setText(restaurant.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            tvRestaurantCategories.setText(restaurant.getJSONArray("categories").getString(0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        double rating = 0;
        try {
            rating = restaurant.getDouble("rating") / 2.0f;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        tvRestaurantRating.setRating((float) rating);
        btnLikeRestaurant.setText("LIKE");
        btnDislike.setText("clear");

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

