package com.example.wheretoeat;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

public class RestaurantAdapter extends RecyclerView.Adapter<RestaurantAdapter.ViewHolder>  {
    Context context;
    JSONArray restaurants;
    JSONObject restaurant;
    JSONArray likedRestaurants;
    String friendId;

    DataTransferInterface dtInterface;

    public RestaurantAdapter(Context context, JSONArray restaurants, String friendId, DataTransferInterface dtInterface) {
        this.context = context;
        this.restaurants = restaurants;
        this.friendId = friendId;
        this.dtInterface = dtInterface;
    }

    //Usually involves inflating a layout from XML and retuning the holder
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View restaurantView = LayoutInflater.from(context).inflate(R.layout.item_restaurant, parent, false);
        likedRestaurants = new JSONArray();
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
        Button btnDoneMatching;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRestaurantName = itemView.findViewById(R.id.tvRestaurantName);
            tvRestaurantCategories = itemView.findViewById(R.id.tvRestaurantCategories);
            tvRestaurantRating = itemView.findViewById(R.id.tvRestaurantRating);
            btnLikeRestaurant = itemView.findViewById(R.id.btnLikeRestaurant);
            btnLikeRestaurant.setOnClickListener(this::onLikeClick);
        }

        public JSONArray getLikedRestaurants() {
            return likedRestaurants;
        }

        public void onLikeClick(View v)  {
            // gets item position
            int position = getAdapterPosition();
            // make sure the position is valid, i.e. actually exists in the view
            if (position != RecyclerView.NO_POSITION) {
                // get the movie at the position, this won't work if the class is static
                JSONObject movie = null;
                try {
                    movie = (JSONObject) restaurants.get(position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                likedRestaurants.put(movie);
                dtInterface.onSetValues(likedRestaurants);
                restaurants.remove(position);
                notifyDataSetChanged();
                notifyItemRangeRemoved(position, restaurants.length());
            }
        }



        public void bind(JSONObject restaurant) throws JSONException {
            tvRestaurantName.setText(restaurant.getString("name"));
            tvRestaurantCategories.setText(restaurant.getJSONArray("categories").getString(0));
            double rating = restaurant.getDouble("rating") / 2.0f;
            tvRestaurantRating.setRating((float) rating);

        }
    }
}

