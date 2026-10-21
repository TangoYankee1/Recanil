package com.example.recanil.api;

import com.example.recanil.model.AuthResponse;
import com.example.recanil.model.LoginRequest;
import com.example.recanil.model.SignupRequest;
import com.example.recanil.model.Customer;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ApiService {

    @POST("signup")
    Call<AuthResponse> signup(@Body SignupRequest request);
    @POST("login")
    Call<AuthResponse> login(@Body LoginRequest request);
    @POST("customers")
    Call<Customer> addCustomer(@Header("Authorization") String authorization, @Body Customer customer);
    @GET("customers")
    Call<List<Customer>> getCustomers(@Header("Authorization") String authorization,
                                    @Query("offset") int offset);
}
