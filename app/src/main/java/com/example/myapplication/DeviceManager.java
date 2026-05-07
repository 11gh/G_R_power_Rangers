package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DeviceManager {
    private static final String PREF_NAME = "device_prefs";
    private static final String KEY_DEVICES = "devices_list";
    private static final String KEY_FIRST_RUN = "first_run_done";
    private static DeviceManager instance;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private List<Device> deviceList;

    private DeviceManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadDevices();
    }

    public static synchronized DeviceManager getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceManager(context);
        }
        return instance;
    }

    private void loadDevices() {
        String json = sharedPreferences.getString(KEY_DEVICES, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Device>>() {}.getType();
            deviceList = gson.fromJson(json, type);
        }
        
        // Load defaults only if it's the absolute first run
        boolean firstRunDone = sharedPreferences.getBoolean(KEY_FIRST_RUN, false);
        if (!firstRunDone && (deviceList == null || deviceList.isEmpty())) {
            deviceList = new ArrayList<>();
            deviceList.add(new Device("living_room_ac", "Living Room AC", 1.5, true, R.drawable.ic_ac));
            deviceList.add(new Device("kitchen_fridge", "Kitchen Fridge", 0.3, true, R.drawable.ic_fridge));
            deviceList.add(new Device("water_heater", "Water Heater", 0.0, false, R.drawable.ic_water_heater));
            sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, true).apply();
            saveDevices();
        } else if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
    }

    public void saveDevices() {
        String json = gson.toJson(deviceList);
        sharedPreferences.edit().putString(KEY_DEVICES, json).apply();
    }

    public List<Device> getDevices() {
        return deviceList;
    }

    public void addDevice(Device device) {
        deviceList.add(device);
        saveDevices();
    }

    public void removeDevice(Device device) {
        // Remove by ID to ensure it works even if objects are recreated
        Device toRemove = null;
        for (Device d : deviceList) {
            if (d.getId().equals(device.getId())) {
                toRemove = d;
                break;
            }
        }
        if (toRemove != null) {
            deviceList.remove(toRemove);
            saveDevices();
        }
    }
    
    public void removeDeviceAt(int position) {
        if (position >= 0 && position < deviceList.size()) {
            deviceList.remove(position);
            saveDevices();
        }
    }
}
