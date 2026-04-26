package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class GridMonitorWidget {
    private final LineChart chart;
    private final TextView tvMin, tvCurrent, tvMax, tvStatus;
    private final View statusDot;
    private float minVoltage = Float.MAX_VALUE;
    private float maxVoltage = Float.MIN_VALUE;
    private final List<Entry> entries = new ArrayList<>();
    private static final int MAX_POINTS = 30;

    public GridMonitorWidget(View root) {
        chart = root.findViewById(R.id.gridChart);
        tvMin = root.findViewById(R.id.tvMinVoltage);
        tvCurrent = root.findViewById(R.id.tvCurrentVoltage);
        tvMax = root.findViewById(R.id.tvMaxVoltage);
        tvStatus = root.findViewById(R.id.tvStatusText);
        statusDot = root.findViewById(R.id.statusDot);
        setupChart();
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setEnabled(false);
        leftAxis.setDrawGridLines(false);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public void processJsonData(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("v")) {
                float v = obj.get("v").getAsFloat();
                updateUI(v);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI(float voltage) {
        if (voltage < minVoltage) minVoltage = voltage;
        if (voltage > maxVoltage) maxVoltage = voltage;

        tvCurrent.setText(String.format("%.1fV", voltage));
        tvMin.setText(String.format("%.1fV", minVoltage));
        tvMax.setText(String.format("%.1fV", maxVoltage));

        boolean isStable = voltage >= 200 && voltage <= 240;
        updateStatus(isStable);
        updateGridChart(voltage, isStable);
    }

    private void updateStatus(boolean isStable) {
        if (isStable) {
            tvStatus.setText("Stable");
            tvStatus.setTextColor(Color.parseColor("#2FF801"));
            statusDot.setBackgroundColor(Color.parseColor("#2FF801"));
        } else {
            tvStatus.setText("Unstable");
            tvStatus.setTextColor(Color.parseColor("#FF5252"));
            statusDot.setBackgroundColor(Color.parseColor("#FF5252"));
        }
    }

    private void updateGridChart(float newVoltage, boolean isStable) {
        int color = isStable ? Color.parseColor("#2FF801") : Color.parseColor("#FFA500");
        
        entries.add(new Entry(entries.size(), newVoltage));
        if (entries.size() > MAX_POINTS) {
            entries.remove(0);
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setX(i);
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Voltage");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        
        // Add shadow effect (simplified as MPAndroidChart doesn't support complex shadows natively)
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(20);
        dataSet.setFillColor(color);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
    }
}