package com.example.recanil;

import android.os.Bundle;

public class MainActivity extends AuthenticatedActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        screen("Dashboard");
        String email = getIntent().getStringExtra("email");
        text(email == null ? "Welcome" : "Welcome, " + email);
        button("Add Customer", R.id.addCustomerBtn).setOnClickListener(v -> open(AddCustomerActivity.class));
        button("Customer List", R.id.customerListBtn).setOnClickListener(v -> open(CustomerListActivity.class));
        button("Log Out", R.id.logoutBtn).setOnClickListener(v -> logout());
    }
}
