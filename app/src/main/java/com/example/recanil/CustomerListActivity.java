package com.example.recanil;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.recanil.api.RetrofitClient;
import com.example.recanil.model.Customer;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerListActivity extends AuthenticatedActivity {
    private LinearLayout rows;
    private TextView status;
    private Button refresh, more;
    private int count;
    private Call<List<Customer>> request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        screen("Customer List");
        button("Add Customer", R.id.addCustomerBtn).setOnClickListener(v -> open(AddCustomerActivity.class));
        refresh = button("Refresh", R.id.refreshBtn);
        refresh.setOnClickListener(v -> load(0));
        status = text("");
        rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        content.addView(rows);
        more = button("Load More", R.id.loadMoreBtn);
        more.setVisibility(android.view.View.GONE);
        more.setOnClickListener(v -> load(count));
        button("Back to Dashboard", R.id.backBtn).setOnClickListener(v -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        if (!isFinishing()) load(0);
    }

    private void load(int offset) {
        if (request != null) request.cancel();
        refresh.setEnabled(false);
        more.setEnabled(false);
        status.setText("Loading customers...");
        request = RetrofitClient.getApiService().getCustomers(authorization(), offset);
        request.enqueue(new Callback<List<Customer>>() {
            @Override public void onResponse(Call<List<Customer>> call, Response<List<Customer>> response) {
                if (call.isCanceled() || isDestroyed()) return;
                refresh.setEnabled(true);
                more.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    status.setText("Unable to load customers. Tap Refresh to retry.");
                    if (!response.isSuccessful()) error(response);
                    return;
                }
                if (offset == 0) { rows.removeAllViews(); count = 0; }
                List<Customer> customers = response.body();
                for (Customer customer : customers) {
                    TextView row = new TextView(CustomerListActivity.this);
                    row.setText("#" + customer.getCustomerid() + "  " + customer.getLastname()
                            + ", " + customer.getFirstname() + "\n" + customer.getMobileno()
                            + "\n" + customer.getAddress());
                    row.setTextColor(android.graphics.Color.rgb(17, 24, 39));
                    row.setTextSize(16);
                    row.setPadding(0, 20, 0, 20);
                    rows.addView(row);
                }
                count += customers.size();
                status.setText(count == 0 ? "No customers yet. Tap Add Customer to get started."
                        : count + " customers loaded");
                more.setVisibility(customers.size() == 50 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            @Override public void onFailure(Call<List<Customer>> call, Throwable error) {
                if (call.isCanceled() || isDestroyed()) return;
                refresh.setEnabled(true);
                more.setEnabled(true);
                status.setText("Connection failed. Tap Refresh to retry.");
            }
        });
    }

    @Override protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }
}
