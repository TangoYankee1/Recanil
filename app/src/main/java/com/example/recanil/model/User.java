package com.example.recanil.model;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("LastName")
    private String lastName;

    @SerializedName("FirstName")
    private String firstName;

    @SerializedName("Email")
    private String email;

    @SerializedName("Password")
    private String password;

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
