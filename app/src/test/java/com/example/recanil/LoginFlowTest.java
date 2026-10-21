package com.example.recanil;

import android.content.Intent;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import com.example.recanil.api.RetrofitClient;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LoginFlowTest {
    private MockWebServer server;
    private Retrofit original;
    private Field field;
    private ActivityController<LoginActivity> controller;
    private LoginActivity activity;

    @Before public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        field = RetrofitClient.class.getDeclaredField("retrofit");
        field.setAccessible(true);
        original = (Retrofit) field.get(null);
        field.set(null, new Retrofit.Builder().baseUrl(server.url("/"))
                .callbackExecutor(command -> new android.os.Handler(Looper.getMainLooper()).post(command))
                .addConverterFactory(GsonConverterFactory.create()).build());
        ShadowToast.reset();
        controller = Robolectric.buildActivity(LoginActivity.class).setup();
        activity = controller.get();
    }
    @After public void tearDown() throws Exception {
        controller.pause().stop().destroy();
        field.set(null, original);
        server.shutdown();
    }
    @Test public void loginPostsCredentialsAndPassesTokenToOtp() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"success\":true,\"token\":\"demo-token\"}"));
        submit("  TESTER@example.com  ", " Correct123! ");
        assertFalse(button().isEnabled());
        await(activity::isFinishing);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("POST", request.getMethod());
        assertEquals("/login", request.getPath());
        JSONObject body = new JSONObject(request.getBody().readUtf8());
        assertEquals("TESTER@example.com", body.getString("email"));
        assertEquals(" Correct123! ", body.getString("password"));
        Intent next = shadowOf(activity).getNextStartedActivity();
        assertEquals(OtpActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("demo-token", next.getStringExtra("token"));
    }
    @Test public void badCredentialsStayOnLoginAndCanRetry() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401)
                .setBody("{\"message\":\"Invalid email or password\"}"));
        submit("tester@example.com", "wrong");
        await(() -> ShadowToast.getTextOfLatestToast() != null);
        assertEquals("Invalid email or password", ShadowToast.getTextOfLatestToast());
        assertTrue(button().isEnabled());
        assertNull(shadowOf(activity).getNextStartedActivity());
        server.enqueue(new MockResponse().setBody("{\"success\":true,\"token\":\"retry-token\"}"));
        submit("tester@example.com", "Correct123!");
        await(activity::isFinishing);
    }
    @Test public void successfulResponseWithoutTokenDoesNotAuthenticate() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"success\":true}"));
        submit("tester@example.com", "Correct123!");
        await(() -> ShadowToast.getTextOfLatestToast() != null);
        assertFalse(activity.isFinishing());
        assertNull(shadowOf(activity).getNextStartedActivity());
        assertTrue(button().isEnabled());
    }
    @Test public void malformedResponseAllowsRetry() throws Exception {
        server.enqueue(new MockResponse().setBody("not-json"));
        submit("tester@example.com", "Correct123!");
        await(() -> ShadowToast.getTextOfLatestToast() != null);
        assertTrue(button().isEnabled());
        assertNull(shadowOf(activity).getNextStartedActivity());
    }
    @Test public void emptyFieldsMakeNoRequest() {
        submit("  ", "password");
        assertEquals("Enter your email", input(R.id.usernameInput).getError().toString());
        submit("tester@example.com", "");
        assertEquals("Enter your password", input(R.id.passwordInput).getError().toString());
        assertEquals(0, server.getRequestCount());
    }
    private void submit(String email, String password) {
        input(R.id.usernameInput).setText(email);
        input(R.id.passwordInput).setText(password);
        button().performClick();
    }
    private EditText input(int id) { return activity.findViewById(id); }
    private Button button() { return activity.findViewById(R.id.loginBtn); }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertTrue("Login request did not complete; requests=" + server.getRequestCount()
                + "; toast=" + ShadowToast.getTextOfLatestToast(), condition.getAsBoolean());
    }
}
