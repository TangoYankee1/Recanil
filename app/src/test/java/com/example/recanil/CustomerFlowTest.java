package com.example.recanil;

import android.content.Intent;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
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
import java.time.Duration;
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
public class CustomerFlowTest {
    private MockWebServer server;
    private Field field;
    private Retrofit original;
    private ActivityController<?> controller;
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
    }
    @After public void tearDown() throws Exception {
        if (controller != null) controller.pause().stop().destroy();
        field.set(null, original);
        server.shutdown();
    }
    private Intent session(Class<?> type) {
        return new Intent(org.robolectric.RuntimeEnvironment.getApplication(), type)
                .putExtra("token", "user-token").putExtra("email", "student@example.com");
    }
    @Test public void otpPassesSessionToDashboardAndSurvivesRotation() {
        ActivityController<OtpActivity> otp = Robolectric.buildActivity(OtpActivity.class, session(OtpActivity.class)).setup();
        controller = otp;
        String displayed = ((TextView) otp.get().findViewById(R.id.otpDisplay)).getText().toString();
        otp.recreate();
        assertEquals(displayed, ((TextView) otp.get().findViewById(R.id.otpDisplay)).getText().toString());
        ((EditText) otp.get().findViewById(R.id.otpInput)).setText(displayed.substring(displayed.length() - 6));
        otp.get().findViewById(R.id.verifyOtpBtn).performClick();
        Intent next = shadowOf(otp.get()).getNextStartedActivity();
        assertEquals(MainActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("user-token", next.getStringExtra("token"));
    }
    @Test public void expiredOtpCannotOpenDashboard() {
        ActivityController<OtpActivity> otp = Robolectric.buildActivity(OtpActivity.class, session(OtpActivity.class)).setup();
        controller = otp;
        String displayed = ((TextView) otp.get().findViewById(R.id.otpDisplay)).getText().toString();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));
        ((EditText) otp.get().findViewById(R.id.otpInput)).setText(displayed.substring(displayed.length() - 6));
        otp.get().findViewById(R.id.verifyOtpBtn).performClick();
        assertNull(shadowOf(otp.get()).getNextStartedActivity());
        assertFalse(otp.get().findViewById(R.id.verifyOtpBtn).isEnabled());
    }
    @Test public void dashboardOpensCustomerScreensWithUserToken() {
        ActivityController<MainActivity> dashboard = Robolectric.buildActivity(MainActivity.class, session(MainActivity.class)).setup();
        controller = dashboard;
        dashboard.get().findViewById(R.id.addCustomerBtn).performClick();
        Intent next = shadowOf(dashboard.get()).getNextStartedActivity();
        assertEquals(AddCustomerActivity.class.getName(), next.getComponent().getClassName());
        assertEquals("user-token", next.getStringExtra("token"));
        dashboard.get().findViewById(R.id.customerListBtn).performClick();
        assertEquals(CustomerListActivity.class.getName(), shadowOf(dashboard.get())
                .getNextStartedActivity().getComponent().getClassName());
    }
    @Test public void customerSaveValidatesAndPostsAllFieldsWithUserToken() throws Exception {
        ActivityController<AddCustomerActivity> add = Robolectric.buildActivity(AddCustomerActivity.class, session(AddCustomerActivity.class)).setup();
        controller = add;
        AddCustomerActivity activity = add.get();
        activity.findViewById(R.id.saveCustomerBtn).performClick();
        assertEquals(0, server.getRequestCount());
        assertNotNull(((EditText) activity.findViewById(R.id.lastNameInput)).getError());
        ((EditText) activity.findViewById(R.id.lastNameInput)).setText("Dela Cruz");
        ((EditText) activity.findViewById(R.id.firstNameInput)).setText("Juan");
        ((EditText) activity.findViewById(R.id.mobileInput)).setText("09171234567");
        ((EditText) activity.findViewById(R.id.addressInput)).setText("Manila");
        server.enqueue(new MockResponse().setResponseCode(201)
                .setBody("{\"customerid\":1,\"lastname\":\"Dela Cruz\",\"firstname\":\"Juan\",\"mobileno\":\"09171234567\",\"address\":\"Manila\"}"));
        activity.findViewById(R.id.saveCustomerBtn).performClick();
        await(activity::isFinishing);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/customers", request.getPath());
        assertEquals("Bearer user-token", request.getHeader("Authorization"));
        JSONObject body = new JSONObject(request.getBody().readUtf8());
        assertEquals("09171234567", body.getString("mobileno"));
        assertFalse(body.has("customerid"));
    }
    @Test public void expiredSessionOnCustomerListReturnsToLogin() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401).setBody("{\"message\":\"Session expired\"}"));
        ActivityController<CustomerListActivity> list = Robolectric.buildActivity(CustomerListActivity.class, session(CustomerListActivity.class)).setup();
        controller = list;
        await(list.get()::isFinishing);
        Intent next = shadowOf(list.get()).getNextStartedActivity();
        assertEquals(LoginActivity.class.getName(), next.getComponent().getClassName());
        assertTrue((next.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
    }
    @Test public void signupSendsAccountAndReturnsToLogin() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201)
                .setBody("{\"success\":true,\"message\":\"Account created. You can now log in.\"}"));
        ActivityController<SignupActivity> signup = Robolectric.buildActivity(SignupActivity.class).setup();
        controller = signup;
        SignupActivity activity = signup.get();
        ((EditText) activity.findViewById(R.id.usernameInput)).setText("Student");
        ((EditText) activity.findViewById(R.id.emailInput)).setText("student@example.com");
        ((EditText) activity.findViewById(R.id.passwordInput)).setText("Correct123!");
        ((EditText) activity.findViewById(R.id.confirmPasswordInput)).setText("Correct123!");
        activity.findViewById(R.id.signupBtn).performClick();
        await(activity::isFinishing);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertEquals("/signup", request.getPath());
        assertEquals("Student", new JSONObject(request.getBody().readUtf8()).getString("username"));
        assertEquals(LoginActivity.class.getName(), shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertTrue("Activity callback did not complete", condition.getAsBoolean());
    }
}
