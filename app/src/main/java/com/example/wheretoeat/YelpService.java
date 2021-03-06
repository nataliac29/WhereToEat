package com.example.wheretoeat;

import android.util.Log;

import com.example.wheretoeat.modals.Restaurant;

import okhttp3.Callback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YelpService {

    public static void findRestaurants(String location, String pricePref, Callback callback) {

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.yelp.com/v3/businesses/search?term=restaurants&limit=10").newBuilder();
        urlBuilder.addQueryParameter("location", location);
        if (pricePref != null) {
            urlBuilder.addQueryParameter("price", pricePref);
        }
        String url = urlBuilder.build().toString();

        Log.e("YelpService", "Yelp Request: " + url );

        Request request= new Request.Builder()
                .url(url)
                .header("Authorization", BuildConfig.YELP_API_KEY)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);

    }

    public static void getRestaurantDetails(String id, Callback callback) {

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        String URL = "https://api.yelp.com/v3/businesses/" + id;

        Request request= new Request.Builder()
                .url(URL)
                .header("Authorization", BuildConfig.YELP_API_KEY)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);

    }
    public static void getRestaurantReviews(String id, Callback callback) {

        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        String URL = "https://api.yelp.com/v3/businesses/" + id + "/reviews";

        Request request= new Request.Builder()
                .url(URL)
                .header("Authorization", BuildConfig.YELP_API_KEY)
                .build();

        Call call = client.newCall(request);
        call.enqueue(callback);

    }


    public ArrayList<Restaurant> processResults(Response response) {
        ArrayList<Restaurant> restaurants = new ArrayList<>();

        try {
            String jsonData = response.body().string();
            JSONObject yelpJSON = new JSONObject(jsonData);
            JSONArray businessesJSON = yelpJSON.getJSONArray("businesses");
            for (int i = 0; i < businessesJSON.length(); i++) {
                JSONObject restaurantJSON = businessesJSON.getJSONObject(i);
                String id = restaurantJSON.getString("id");
                String name = restaurantJSON.getString("name");
                String phone = restaurantJSON.optString("display_phone", "Phone not available");
                String website = restaurantJSON.getString("url");
                double rating = restaurantJSON.getDouble("rating");

                String imageUrl = restaurantJSON.getString("image_url");

                double latitude = (double) restaurantJSON.getJSONObject("coordinates").getDouble("latitude");

                double longitude = (double) restaurantJSON.getJSONObject("coordinates").getDouble("longitude");

                ArrayList<String> address = new ArrayList<>();
                JSONArray addressJSON = restaurantJSON.getJSONObject("location")
                        .getJSONArray("display_address");
                for (int y = 0; y < addressJSON.length(); y++) {
                    address.add(addressJSON.get(y).toString());
                }

                ArrayList<String> categories = new ArrayList<>();
                JSONArray categoriesJSON = restaurantJSON.getJSONArray("categories");

                for (int y = 0; y < categoriesJSON.length(); y++) {
                    categories.add(categoriesJSON.getJSONObject(y).getString("title"));
                }
                Restaurant restaurant = new Restaurant(id, name, phone, website, rating,
                        imageUrl, address, latitude, longitude, categories);
                restaurants.add(restaurant);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch (JSONException e){
            e.printStackTrace();
        }
        return restaurants;
    }

}
