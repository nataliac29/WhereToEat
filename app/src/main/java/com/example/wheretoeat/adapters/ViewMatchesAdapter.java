package com.example.wheretoeat.adapters;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.wheretoeat.R;
import com.example.wheretoeat.RestaurantDetailsActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class ViewMatchesAdapter extends RecyclerView.Adapter<ViewMatchesAdapter.ViewHolder>  {
    Context context;
    JSONArray restaurants;
    String friendId;


    public ViewMatchesAdapter(Context context, JSONArray restaurants, String friendId ) {
        this.context = context;
        this.restaurants = restaurants;
        this.friendId = friendId;

    }

    //Usually involves inflating a layout from XML and retuning the holder
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View restaurantView = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        return new ViewHolder(restaurantView);
    }
    //involves populating data into the item through holder
    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        //Get the movie at the passed in position
        JSONObject restaurant = null;
        try {
            restaurant = (JSONObject) restaurants.get(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Bind the movie data into the viewholder
        try {
            holder.bind(restaurant);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return restaurants.length();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvRestaurantName;
        RatingBar tvRestaurantRating;
        TextView tvRestaurantCategories;
        Button btnLikeRestaurant;
        ImageView ivCover;
        String imageUrl;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantCategories = itemView.findViewById(R.id.tvRestaurantCategories);
            tvRestaurantRating = itemView.findViewById(R.id.tvRestaurantRating);
            btnLikeRestaurant = itemView.findViewById(R.id.btnLikeRestaurant);
            ivCover = itemView.findViewById(R.id.ivCover);
            btnLikeRestaurant.setOnClickListener(this::onLikeClick);
        }


        public void onLikeClick(View v)  {
            // gets item position
            int position = getAdapterPosition();
            // make sure the position is valid, i.e. actually exists in the view
            if (position != RecyclerView.NO_POSITION) {
                // get the movie at the position, this won't work if the class is static
                JSONObject restaurant = null;
                try {
                    restaurant = (JSONObject) restaurants.get(position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(context, RestaurantDetailsActivity.class);
                // serialize the movie using parceler, use its short name as a key
                try {
                    intent.putExtra("restaurantId", restaurant.getString("id"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // show the activity
                context.startActivity(intent);
            }
        }



        public void bind(JSONObject restaurant) throws JSONException {
            tvRestaurantName.setText(restaurant.getString("name"));
            tvRestaurantCategories.setText(restaurant.getJSONArray("categories").getString(0));
            double rating = restaurant.getDouble("rating") / 2.0f;
            tvRestaurantRating.setRating((float) rating);
            btnLikeRestaurant.setText("View more details");


            ivCover = itemView.findViewById(R.id.ivCover);


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


        }
    }
}
