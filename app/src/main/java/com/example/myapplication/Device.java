package com.example.myapplication;

public class Device {
    private String id;
    private String name;
    private double consumptionKw;
    private boolean state; 
    private int iconResId;
    private double limit;
    
    // Scheduling fields
    private String startTime; // Format: "HH:mm"
    private double workDuration; // Hours
    private double offDuration; // Hours

    public Device() {
    }

    public Device(String id, String name, double consumptionKw, boolean state, int iconResId) {
        this.id = id;
        this.name = name;
        this.consumptionKw = consumptionKw;
        this.state = state;
        this.iconResId = iconResId;
        this.limit = 5000;
        this.startTime = "08:00";
        this.workDuration = 2.0;
        this.offDuration = 1.0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getConsumptionKw() { return consumptionKw; }
    public boolean getState() { return state; }
    public int getIconResId() { return iconResId; }
    public double getLimit() { return limit; }
    public String getStartTime() { return startTime; }
    public double getWorkDuration() { return workDuration; }
    public double getOffDuration() { return offDuration; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setState(boolean state) { this.state = state; }
    public void setConsumptionKw(double consumptionKw) { this.consumptionKw = consumptionKw; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public void setLimit(double limit) { this.limit = limit; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setWorkDuration(double workDuration) { this.workDuration = workDuration; }
    public void setOffDuration(double offDuration) { this.offDuration = offDuration; }
}
