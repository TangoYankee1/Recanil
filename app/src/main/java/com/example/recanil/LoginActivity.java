package com.example.recanil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.recanil.api.ApiErrors;
import com.example.recanil.api.RetrofitClient;
import com.example.recanil.model.AuthResponse;
import com.example.recanil.model.LoginRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput, passwordInput;
    private Button loginBtn;
    private Call<AuthResponse> request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.recanil);
        usernameInput = findViewById(R.id.usernameInput);
        usernameInput.setHint("Email");
        usernameInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        passwordInput = findViewById(R.id.passwordInput);
        loginBtn = findViewById(R.id.loginBtn);
        findViewById(R.id.registerBtn).setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class)));
        loginBtn.setOnClickListener(v -> login());
    }

    private void login() {
        String email = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (email.isEmpty()) {
            usernameInput.setError("Enter your email");
            usernameInput.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            passwordInput.requestFocus();
            return;
        }
        loginBtn.setEnabled(false);
        loginBtn.setText("Logging in...");
        request = RetrofitClient.getApiService().login(new LoginRequest(email, password));
        request.enqueue(new Callback<AuthResponse>() {
            @Override public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (isDestroyed()) return;
                restoreButton();
                AuthResponse auth = response.body();
                if (!response.isSuccessful()) {
                    toast(ApiErrors.message(response));
                } else if (auth == null || !auth.isSuccess()
                        || auth.getToken() == null || auth.getToken().isEmpty()) {
                    toast("Login response is missing a session. Please try again.");
                } else {
                    toast("Login successful!");
                    Intent intent = new Intent(LoginActivity.this, OtpActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("token", auth.getToken());
                    startActivity(intent);
                    finish();
                }
            }
            @Override public void onFailure(Call<AuthResponse> call, Throwable error) {
                if (call.isCanceled() || isDestroyed()) return;
                restoreButton();
                toast("Connection failed: please check your connection and try again.");
            }
        });
    }

    private void restoreButton() { loginBtn.setEnabled(true); loginBtn.setText("Login"); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }
}
