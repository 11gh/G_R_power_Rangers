package com.example.myapplication;

import com.google.gson.annotations.SerializedName;

public class PowerMetric {
    @SerializedName("pwr_w")
    private Double powerW;
    
    @SerializedName("bill_est")
    private Double billEst;

    public Double getPowerW() { return powerW; }
    public Double getBillEst() { return billEst; }
}