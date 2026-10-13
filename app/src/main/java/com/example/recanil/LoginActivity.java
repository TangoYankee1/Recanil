package com.example.recanil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.recanil.api.ApiService;
import com.example.recanil.api.RetrofitClient;
import com.example.recanil.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginBtn;
    private TextView registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recanil);

        // Find views
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);

        // Register button
        registerBtn.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    SignupActivity.class
            );

            startActivity(intent);
        });

        // Login button
        loginBtn.setOnClickListener(v -> login());
    }

    private void login() {

        String email = usernameInput
                .getText()
                .toString()
                .trim();

        String password = passwordInput
                .getText()
                .toString();

        // Validate email
        if (email.isEmpty()) {

            usernameInput.setError("Enter your email");
            usernameInput.requestFocus();

            return;
        }

        // Validate password
        if (password.isEmpty()) {

            passwordInput.setError("Enter your password");
            passwordInput.requestFocus();

            return;
        }

        // Disable button while connecting
        loginBtn.setEnabled(false);
        loginBtn.setText("Logging in...");

        ApiService apiService =
                RetrofitClient.getApiService();

        apiService.getUsers().enqueue(
                new Callback<List<User>>() {

                    @Override
                    public void onResponse(
                            Call<List<User>> call,
                            Response<List<User>> response) {

                        // Enable button again
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");

                        // Server error
                        if (!response.isSuccessful()) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Server error: "
                                            + response.code(),
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        // Get users
                        List<User> users = response.body();

                        if (users == null || users.isEmpty()) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "No users found",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

                        // Check credentials
                        boolean loginSuccessful = false;

                        for (User user : users) {

                            if (user.getEmail() != null
                                    && user.getPassword() != null
                                    && user.getEmail()
                                    .equalsIgnoreCase(email)
                                    && user.getPassword()
                                    .equals(password)) {

                                loginSuccessful = true;
                                break;
                            }
                        }

                        // Login successful
                        if (loginSuccessful) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Login successful!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            /*
                             * Go to OTP verification.
                             *
                             * OtpActivity must be located at:
                             *
                             * app/src/main/java/
                             * com/example/recanil/OtpActivity.java
                             */
                            Intent intent = new Intent(
                                    LoginActivity.this,
                                    OtpActivity.class
                            );

                            // Send email to OTP activity
                            intent.putExtra(
                                    "email",
                                    email
                            );

                            startActivity(intent);

                            finish();

                        } else {

                            // Invalid credentials
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Invalid email or password",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<User>> call,
                            Throwable t) {

                        // Enable button again
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Login");

                        t.printStackTrace();

                        Toast.makeText(
                                LoginActivity.this,
                                "Connection failed: "
                                        + t.getClass()
                                        .getSimpleName()
                                        + "\n"
                                        + t.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }
}
