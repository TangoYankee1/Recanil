package com.example.recanil;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import com.example.recanil.api.RetrofitClient;
import com.example.recanil.model.Customer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCustomerActivity extends AuthenticatedActivity {
    private EditText lastname, firstname, mobile, address;
    private Button save;
    private Call<Customer> request;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (isFinishing()) return;
        screen("Add Customer");
        text("Customer ID is generated when you save.");
        lastname = field("Last name", R.id.lastNameInput, InputType.TYPE_CLASS_TEXT, 100);
        firstname = field("First name", R.id.firstNameInput, InputType.TYPE_CLASS_TEXT, 100);
        mobile = field("Mobile number", R.id.mobileInput, InputType.TYPE_CLASS_PHONE, 30);
        address = field("Address", R.id.addressInput, InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 500);
        save = button("Save Customer", R.id.saveCustomerBtn);
        save.setOnClickListener(v -> save());
        button("Back to Dashboard", R.id.backBtn).setOnClickListener(v -> finish());
    }

    private EditText field(String label, int id, int type, int length) {
        text(label);
        EditText field = new EditText(this);
        field.setId(id);
        field.setHint(label);
        field.setInputType(type);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(length)});
        content.addView(field, new android.widget.LinearLayout.LayoutParams(-1, -2));
        return field;
    }

    private void save() {
        for (EditText field : new EditText[]{lastname, firstname, mobile, address}) {
            if (field.getText().toString().trim().isEmpty()) {
                field.setError("This field is required");
                field.requestFocus();
                return;
            }
        }
        save.setEnabled(false);
        save.setText("Saving...");
        Customer customer = new Customer(lastname.getText().toString().trim(),
                firstname.getText().toString().trim(), mobile.getText().toString().trim(),
                address.getText().toString().trim());
        request = RetrofitClient.getApiService().addCustomer(authorization(), customer);
        request.enqueue(new Callback<Customer>() {
            @Override public void onResponse(Call<Customer> call, Response<Customer> response) {
                if (isDestroyed()) return;
                restore();
                if (response.isSuccessful() && response.body() != null
                        && response.body().getCustomerid() != null) {
                    toast("Customer saved successfully!");
                    finish();
                } else if (!response.isSuccessful()) error(response);
                else toast("Save response is incomplete. Check Customer List before trying again.");
            }
            @Override public void onFailure(Call<Customer> call, Throwable error) {
                if (call.isCanceled() || isDestroyed()) return;
                restore();
                toast("Connection failed. Check Customer List before retrying the save.");
            }
        });
    }
    private void restore() { save.setEnabled(true); save.setText("Save Customer"); }
    @Override protected void onDestroy() {
        if (request != null) request.cancel();
        super.onDestroy();
    }
}
