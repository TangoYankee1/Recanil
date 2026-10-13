package com.example.recanil;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;
import java.util.Random;

public class OtpActivity extends AppCompatActivity {

    private EditText otpInput;
    private Button verifyOtpBtn;
    private TextView timerText;
    private TextView otpDisplay;

    private String generatedOtp;
    private CountDownTimer countDownTimer;

    private static final long OTP_DURATION = 30_000L;
    private static final long TIMER_INTERVAL = 1_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_otp);

        // Connect Java to XML
        otpInput = findViewById(R.id.otpInput);
        verifyOtpBtn = findViewById(R.id.verifyOtpBtn);
        timerText = findViewById(R.id.timerText);
        otpDisplay = findViewById(R.id.otpDisplay);

        // Generate OTP
        generatedOtp = generateOtp();

        // Display OTP for testing
        otpDisplay.setText("Your OTP: " + generatedOtp);

        // Start 30-second countdown
        startOtpTimer();

        // Verify button
        verifyOtpBtn.setOnClickListener(v -> verifyOtp());
    }

    /**
     * Generate a random 6-digit OTP.
     */
    private String generateOtp() {

        Random random = new Random();

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
                OTP_DURATION,
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

    @Override
    protected void onDestroy() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        super.onDestroy();
    }
}
