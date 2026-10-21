package com.example.recanil.api;

import org.json.JSONObject;
import retrofit2.Response;

public final class ApiErrors {
    private ApiErrors() { }
    public static String message(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String message = new JSONObject(response.errorBody().string()).optString("message");
                if (!message.isEmpty()) return message;
            }
        } catch (Exception ignored) { }
        return "Server error: " + response.code();
    }
}
