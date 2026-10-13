package com.example.recanil;

import android.content.Intent;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;

import com.example.recanil.api.RetrofitClient;

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

/** Exercises the real login Activity, Retrofit request, JSON parsing and navigation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LoginFlowTest {
    private static final String USERS = "[{\"Email\":\"tester@example.com\",\"Password\":\"Correct123!\"}]";
    private MockWebServer server;
    private Retrofit originalRetrofit;
    private Field retrofitField;
    private ActivityController<LoginActivity> controller;
    private LoginActivity activity;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        retrofitField = RetrofitClient.class.getDeclaredField("retrofit");
        retrofitField.setAccessible(true);
        originalRetrofit = (Retrofit) retrofitField.get(null);
        retrofitField.set(null, new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(GsonConverterFactory.create()).build());
        ShadowToast.reset();
        controller = Robolectric.buildActivity(LoginActivity.class).setup();
        activity = controller.get();
    }

    @After
    public void tearDown() throws Exception {
        if (controller != null) controller.pause().stop().destroy();
        if (retrofitField != null) retrofitField.set(null, originalRetrofit);
        if (server != null) server.shutdown();
    }

    @Test
    public void validLoginOpensOtpWithEmail() throws Exception {
        server.enqueue(new MockResponse().setBody(USERS));
        submit("tester@example.com", "Correct123!");
        assertFalse(button().isEnabled());
        assertEquals("Logging in...", button().getText().toString());
        await(() -> activity.isFinishing());
        Intent intent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(intent);
        assertEquals(OtpActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("tester@example.com", intent.getStringExtra("email"));
        assertEquals("Login successful!", ShadowToast.getTextOfLatestToast());
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(request);
        assertEquals("GET", request.getMethod());
        assertEquals("/users", request.getPath());
    }

    @Test
    public void emailIgnoresCaseAndSurroundingSpaces() throws Exception {
        server.enqueue(new MockResponse().setBody(USERS));
        submit("  TESTER@example.com  ", "Correct123!");
        await(() -> activity.isFinishing());
        assertNotNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void wrongPasswordStaysOnLogin() throws Exception {
        assertRejected("tester@example.com", "wrong", USERS, "Invalid email or password");
    }

    @Test
    public void unknownEmailStaysOnLogin() throws Exception {
        assertRejected("unknown@example.com", "Correct123!", USERS, "Invalid email or password");
    }

    @Test
    public void passwordIsCaseSensitiveAndPreservesSpaces() throws Exception {
        assertRejected("tester@example.com", "correct123!", USERS, "Invalid email or password");
        ShadowToast.reset();
        assertRejected("tester@example.com", " Correct123! ", USERS, "Invalid email or password");
    }

    @Test
    public void emptyEmailIsRejectedWithoutNetworkRequest() {
        submit("   ", "Correct123!");
        assertEquals("Enter your email", input(R.id.usernameInput).getError().toString());
        assertEquals(0, server.getRequestCount());
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void emptyPasswordIsRejectedWithoutNetworkRequest() {
        submit("tester@example.com", "");
        assertEquals("Enter your password", input(R.id.passwordInput).getError().toString());
        assertEquals(0, server.getRequestCount());
        assertNull(shadowOf(activity).getNextStartedActivity());
    }

    @Test
    public void emptyUsersStaysOnLogin() throws Exception {
        assertRejected("tester@example.com", "Correct123!", "[]", "No users found");
    }

    @Test
    public void missingCredentialsInResponseDoNotAuthenticate() throws Exception {
        assertRejected("tester@example.com", "Correct123!", "[{}, {\"Email\":\"tester@example.com\"}]",
                "Invalid email or password");
    }

    @Test
    public void serverErrorRestoresLoginButton() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(500));
        submit("tester@example.com", "Correct123!");
        await(() -> ShadowToast.getTextOfLatestToast() != null);
        assertEquals("Server error: 500", ShadowToast.getTextOfLatestToast());
        assertStayedOnLogin();
    }

    @Test
    public void failedLoginCanBeRetriedSuccessfully() throws Exception {
        assertRejected("tester@example.com", "wrong", USERS, "Invalid email or password");
        ShadowToast.reset();
        server.enqueue(new MockResponse().setBody(USERS));
        submit("tester@example.com", "Correct123!");
        await(() -> activity.isFinishing());
        assertNotNull(shadowOf(activity).getNextStartedActivity());
        assertEquals(2, server.getRequestCount());
    }

    @Test
    public void malformedResponseRestoresLoginButton() throws Exception {
        server.enqueue(new MockResponse().setBody("invalid-json"));
        submit("tester@example.com", "Correct123!");
        await(() -> ShadowToast.getTextOfLatestToast() != null);
        assertTrue(ShadowToast.getTextOfLatestToast().startsWith("Connection failed:"));
        assertStayedOnLogin();
    }

    private void assertRejected(String email, String password, String body, String message) throws Exception {
        server.enqueue(new MockResponse().setBody(body));
        submit(email, password);
        await(() -> ShadowToast.getTextOfLatestToast() != null);
        assertEquals(message, ShadowToast.getTextOfLatestToast());
        assertStayedOnLogin();
    }

    private void assertStayedOnLogin() {
        assertNull(shadowOf(activity).getNextStartedActivity());
        assertFalse(activity.isFinishing());
        assertTrue(button().isEnabled());
        assertEquals("Login", button().getText().toString());
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
        assertTrue("Login callback did not finish within 5 seconds", condition.getAsBoolean());
    }
}
