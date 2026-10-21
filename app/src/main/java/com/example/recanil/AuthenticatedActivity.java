package com.example.recanil;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import com.example.recanil.api.ApiErrors;
import retrofit2.Response;

/** The demo keeps the user token only in the current activity navigation flow. */
public abstract class AuthenticatedActivity extends AppCompatActivity {
    protected String token;
    protected LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        token = getIntent().getStringExtra("token");
        if (token == null || token.isEmpty()) { logout(); }
    }

    protected void screen(String title) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 250));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        scroll.addView(content);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        TextView heading = text(title);
        heading.setTextSize(28);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    protected TextView text(String label) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(16);
        view.setTextColor(Color.rgb(17, 24, 39));
        view.setPadding(0, 16, 0, 16);
        content.addView(view);
        return view;
    }

    protected Button button(String label, int id) {
        Button view = new Button(this);
        view.setId(id);
        view.setText(label);
        view.setAllCaps(false);
        content.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    protected void open(Class<?> activity) {
        Intent intent = new Intent(this, activity);
        intent.putExtra("token", token);
        intent.putExtra("email", getIntent().getStringExtra("email"));
        startActivity(intent);
    }

    protected String authorization() { return "Bearer " + token; }
    protected void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    protected void error(Response<?> response) {
        if (response.code() == 401) {
            toast("Session expired. Please log in again.");
            logout();
        } else toast(ApiErrors.message(response));
    }
    protected void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
