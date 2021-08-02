package com.example.wheretoeat.modals;

import com.parse.ParseUser;

// Java program implementing Singleton class
// with getInstance() method
class CurrentUser
{
    // static variable single_instance of type Singleton
    private static CurrentUser single_instance = null;

    // variable of type String
    public ParseUser s;

    // private constructor restricted to this class itself
    private CurrentUser() { }

    // static method to create instance of Singleton class
    public static CurrentUser getInstance()
    {
        if (single_instance == null)
            single_instance = new CurrentUser();

        return single_instance;
    }
}