package com.example.myapplication;

public class Device {
    private String id;
    private String name;
    private double consumptionKw;
    private boolean state; // true = ON, false = OFF
    private int iconResId;

    public Device(String id, String name, double consumptionKw, boolean state, int iconResId) {
        this.id = id;
        this.name = name;
        this.consumptionKw = consumptionKw;
        this.state = state;
        this.iconResId = iconResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getConsumptionKw() { return consumptionKw; }
    public boolean getState() { return state; }
    public int getIconResId() { return iconResId; }

    public void setState(boolean state) { this.state = state; }
    public void setConsumptionKw(double consumptionKw) { this.consumptionKw = consumptionKw; }
}