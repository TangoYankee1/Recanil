package com.example.recanil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Patterns;
import com.example.recanil.api.ApiErrors;
import com.example.recanil.api.RetrofitClient;
import com.example.recanil.model.AuthResponse;
import com.example.recanil.model.SignupRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button signupBtn;
    private TextView loginBtn;
    private Call<AuthResponse> request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.signup);

        // Find views
        usernameInput = findViewById(R.id.usernameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput =
                findViewById(R.id.confirmPasswordInput);

        signupBtn = findViewById(R.id.signupBtn);
        loginBtn = findViewById(R.id.loginBtn);

        // Login link
        loginBtn.setOnClickListener(v -> {

            Intent intent = new Intent(
                    SignupActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);

            finish();
        });

        // Signup button
        signupBtn.setOnClickListener(v -> {
            signup();
        });
    }

    private void signup() {

        String username =
                usernameInput.getText().toString().trim();

        String email =
                emailInput.getText().toString().trim();

        String password =
                passwordInput.getText().toString();

        String confirmPassword =
                confirmPasswordInput.getText().toString();

        // Validate username
        if (username.isEmpty()) {
            usernameInput.setError("Enter a username");
            usernameInput.requestFocus();
            return;
        }

        // Validate email
        if (email.isEmpty()) {
            emailInput.setError("Enter your email");
            emailInput.requestFocus();
            return;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordInput.setError("Enter a password");
            passwordInput.requestFocus();
            return;
        }

        // Password length
        if (password.length() < 6) {
            passwordInput.setError(
                    "Password must be at least 6 characters"
            );
            passwordInput.requestFocus();
            return;
        }

        // Confirm password
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(
                    "Confirm your password"
            );
            confirmPasswordInput.requestFocus();
            return;
        }

        // Password match
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(
                    "Passwords do not match"
            );
            confirmPasswordInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            return;
        }
        signupBtn.setEnabled(false);
        signupBtn.setText("Creating account...");
        request = RetrofitClient.getApiService().signup(new SignupRequest(username, email, password));
        request.enqueue(new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isDestroyed()) return;
                restoreButton();
                AuthResponse result = response.body();
                if (response.isSuccessful() && result != null && result.isSuccess()) {
                    Toast.makeText(SignupActivity.this, result.getMessage(), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SignupActivity.this, response.isSuccessful()
                            ? "Unable to create account. Please try again."
                            : ApiErrors.message(response), Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable error) {
                if (call.isCanceled() || isDestroyed()) return;
                restoreButton();
                Toast.makeText(SignupActivity.this, "Connection failed. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }
    private void restoreButton() { signupBtn.setEnabled(true); signupBtn.setText("CREATE ACCOUNT"); }
    @Override protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }
}
