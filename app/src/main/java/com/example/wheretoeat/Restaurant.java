package com.example.wheretoeat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Restaurant {
    private String id;
    private String name;
    private String phone;
    private String website;
    private double rating;
    private String imageUrl;
    private ArrayList<String> address = new ArrayList<>();
    private double latitude;
    private double longitude;
    private ArrayList<String> categories = new ArrayList<>();

    public Restaurant(String id, String name, String phone, String website,
                      double rating, String imageUrl, ArrayList<String> address,
                      double latitude, double longitude, ArrayList<String> categories) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.website = website;
        this.rating = rating;
        this.imageUrl = imageUrl;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.categories = categories;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getWebsite() {
        return  website;
    }

    public double getRating() {
        return rating;
    }

    public String getImageUrl(){
        return imageUrl;
    }

    public ArrayList<String> getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public ArrayList<String> getCategories() {
        return categories;
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", this.id);
            obj.put("name", this.name);
            obj.put("website", this.website);
            obj.put("rating", this.rating);
            obj.put("categories", this.categories);
        } catch (JSONException e) {
            Log.e("Restaurant class", "error getting jsonObject", e);
        }
        return obj;
    }
}