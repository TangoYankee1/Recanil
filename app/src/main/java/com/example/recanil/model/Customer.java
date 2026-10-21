package com.example.recanil.model;

public class Customer {
    private Long customerid;
    private String lastname;
    private String firstname;
    private String mobileno;
    private String address;

    public Customer(String lastname, String firstname, String mobileno, String address) {
        this.lastname = lastname;
        this.firstname = firstname;
        this.mobileno = mobileno;
        this.address = address;
    }
    public Long getCustomerid() { return customerid; }
    public String getLastname() { return lastname; }
    public String getFirstname() { return firstname; }
    public String getMobileno() { return mobileno; }
    public String getAddress() { return address; }
}
