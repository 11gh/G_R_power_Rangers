package com.example.myapplication;

import androidx.annotation.NonNull;

public class Device {
    private String id;
    private String name;
    private double consumptionKw;
    private boolean state; 
    private int iconResId;
    private double limit;
    
    // Scheduling fields
    private String startTime; // Format: "HH:mm"
    private double workDuration; // Seconds (Modified for demo)
    private double offDuration; // Seconds (Modified for demo)
    private int remainingSeconds; // For countdown UI
    private boolean isWorkingPhase = true; // To track if we are in work or off duration
    
    // Pause states
    private boolean isLimitPaused = false;
    private boolean isSchedulePaused = false;
    private boolean isLimitActive = false;
    private boolean isScheduleActive = false;

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
        this.workDuration = 10.0; // Default 10 seconds for demo
        this.offDuration = 10.0;  // Default 10 seconds for demo
        this.remainingSeconds = 10;
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
    public int getRemainingSeconds() { return remainingSeconds; }
    public boolean isWorkingPhase() { return isWorkingPhase; }
    public boolean isLimitPaused() { return isLimitPaused; }
    public boolean isSchedulePaused() { return isSchedulePaused; }
    public boolean isLimitActive() { return isLimitActive; }
    public boolean isScheduleActive() { return isScheduleActive; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setState(boolean state) { this.state = state; }
    public void setConsumptionKw(double consumptionKw) { this.consumptionKw = consumptionKw; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public void setLimit(double limit) { this.limit = limit; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setWorkDuration(double workDuration) { this.workDuration = workDuration; }
    public void setOffDuration(double offDuration) { this.offDuration = offDuration; }
    public void setRemainingSeconds(int remainingSeconds) { this.remainingSeconds = remainingSeconds; }
    public void setWorkingPhase(boolean workingPhase) { isWorkingPhase = workingPhase; }
    public void setLimitPaused(boolean limitPaused) { isLimitPaused = limitPaused; }
    public void setSchedulePaused(boolean schedulePaused) { isSchedulePaused = schedulePaused; }
    public void setLimitActive(boolean limitActive) { isLimitActive = limitActive; }
    public void setScheduleActive(boolean scheduleActive) { isScheduleActive = scheduleActive; }

    @NonNull
    @Override
    public String toString() {
        return name != null ? name : "";
    }
}
