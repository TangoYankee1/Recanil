package com.example.recanil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button signupBtn;
    private TextView loginBtn;

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

        // Render API signup will be added here.
    }
}
