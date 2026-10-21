package com.example.recanil;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.security.SecureRandom;

public class OtpActivity extends AppCompatActivity {

    private EditText otpInput;
    private Button verifyOtpBtn;
    private TextView timerText;
    private TextView otpDisplay;

    private String generatedOtp;
    private long expiresAt;
    private CountDownTimer countDownTimer;

    private static final long OTP_DURATION = 30_000L;
    private static final long TIMER_INTERVAL = 1_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_otp);
        if (getIntent().getStringExtra("token") == null) {
            returnToLogin();
            return;
        }

        // Connect Java to XML
        otpInput = findViewById(R.id.otpInput);
        verifyOtpBtn = findViewById(R.id.verifyOtpBtn);
        timerText = findViewById(R.id.timerText);
        otpDisplay = findViewById(R.id.otpDisplay);

        // Generate OTP
        generatedOtp = savedInstanceState == null ? generateOtp()
                : savedInstanceState.getString("otp");
        expiresAt = savedInstanceState == null ? SystemClock.elapsedRealtime() + OTP_DURATION
                : savedInstanceState.getLong("expiresAt");

        // Display OTP for testing
        otpDisplay.setText("Your OTP: " + generatedOtp);

        // Start 30-second countdown
        startOtpTimer();

        // Verify button
        verifyOtpBtn.setOnClickListener(v -> verifyOtp());
        findViewById(R.id.loginAgainBtn).setOnClickListener(v -> returnToLogin());
    }

    /**
     * Generate a random 6-digit OTP.
     */
    private String generateOtp() {

        SecureRandom random = new SecureRandom();

        int otp = 100000 + random.nextInt(900000);

        return String.format(
                Locale.US,
                "%06d",
                otp
        );
    }

    /**
     * Start the 30-second countdown.
     */
    private void startOtpTimer() {

        verifyOtpBtn.setEnabled(true);
        otpInput.setEnabled(true);

        countDownTimer = new CountDownTimer(
                Math.max(0, expiresAt - SystemClock.elapsedRealtime()),
                TIMER_INTERVAL
        ) {

            @Override
            public void onTick(long millisUntilFinished) {

                long seconds = millisUntilFinished / 1000;

                timerText.setText(
                        "OTP expires in "
                                + seconds
                                + " seconds"
                );
            }

            @Override
            public void onFinish() {

                timerText.setText("OTP expired");

                verifyOtpBtn.setEnabled(false);
                otpInput.setEnabled(false);

                Toast.makeText(
                        OtpActivity.this,
                        "OTP expired. Please login again.",
                        Toast.LENGTH_LONG
                ).show();
            }
        };

        countDownTimer.start();
    }

    /**
     * Verify the OTP entered by the user.
     */
    private void verifyOtp() {
        if (SystemClock.elapsedRealtime() >= expiresAt) {
            otpInput.setError("OTP expired. Please login again.");
            verifyOtpBtn.setEnabled(false);
            return;
        }

        String enteredOtp =
                otpInput.getText()
                        .toString()
                        .trim();

        // Empty
        if (enteredOtp.isEmpty()) {

            otpInput.setError("Enter OTP");
            otpInput.requestFocus();

            return;
        }

        // Must be six digits
        if (enteredOtp.length() != 6) {

            otpInput.setError(
                    "OTP must contain 6 digits"
            );

            otpInput.requestFocus();

            return;
        }

        // Correct OTP
        if (enteredOtp.equals(generatedOtp)) {

            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            Toast.makeText(
                    OtpActivity.this,
                    "OTP verified successfully!",
                    Toast.LENGTH_SHORT
            ).show();

            Intent intent = new Intent(
                    OtpActivity.this,
                    MainActivity.class
            );
            intent.putExtra("token", getIntent().getStringExtra("token"));
            intent.putExtra("email", getIntent().getStringExtra("email"));

            startActivity(intent);

            finish();

        } else {

            // Wrong OTP
            otpInput.setError("Incorrect OTP");
            otpInput.requestFocus();

            Toast.makeText(
                    OtpActivity.this,
                    "Incorrect OTP",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void returnToLogin() {
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("otp", generatedOtp);
        state.putLong("expiresAt", expiresAt);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        super.onDestroy();
    }
}
