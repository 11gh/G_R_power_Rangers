package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.databinding.FragmentHomeBinding;
import com.google.gson.Gson;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private AlertAdapter alertAdapter;
    private List<Alert> alertList;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;
    private GridMonitorWidget gridMonitor;
    private final Gson gson = new Gson();
    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");
    private boolean isDevicesExpanded = true;
    private Esp32WebSocketManager wsManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        
        setupAlerts();
        setupDevices();
        setupViewAllToggle();

        // Initial state: Devices are expanded by default
        binding.tvViewAll.setText(R.string.view_none);
        binding.tvViewAll.setTextColor(Color.parseColor("#A7ABB1"));

        // Initialize Grid Monitor Widget
        gridMonitor = new GridMonitorWidget(binding.gridMonitor.getRoot());

        setupWebSocket();

        return binding.getRoot();
    }

    private void setupWebSocket() {
        wsManager = Esp32WebSocketManager.getInstance();
        wsManager.connect();

        wsManager.setConnectionStatusListener(connected -> {
            if (binding == null) return;
            // [ESP32 DEV GUIDE] Update UI to show connection state
            if (connected) {
                binding.tvPowerUnit.setAlpha(1.0f);
            } else {
                binding.tvPowerUnit.setAlpha(0.5f); // Dim unit when disconnected
            }
        });

        wsManager.setMessageReceivedListener(new Esp32WebSocketManager.OnMessageReceivedListener() {
            @Override
            public void onTelemetryReceived(Esp32WebSocketManager.TelemetryMessage data) {
                updateTelemetryUI(data);
            }

            @Override
            public void onRelayStatusReceived(Esp32WebSocketManager.RelayStatusMessage data) {
                updateDeviceStatusUI(data);
            }

            @Override
            public void onAlertReceived(Esp32WebSocketManager.AlertMessage data) {
                showAlert(data.title, data.body);
            }
        });
    }

    private void updateTelemetryUI(Esp32WebSocketManager.TelemetryMessage metric) {
        if (binding == null) return;
        // Update Power
        if (metric.pwrKw <= 0) {
            binding.viewPowerAccent.setBackgroundColor(Color.parseColor("#666666"));
            binding.tvPowerValue.setText("0");
            binding.tvPowerUnit.setText(R.string.unit_kw);
        } else {
            binding.viewPowerAccent.setBackgroundColor(Color.parseColor("#2FF801"));
            binding.tvPowerValue.setText(String.format(Locale.getDefault(), "%.2f", metric.pwrKw));
            binding.tvPowerUnit.setText(R.string.unit_kw);
        }

        // Update Bill
        if (metric.billSyp <= 0) {
            binding.viewBillAccent.setBackgroundColor(Color.parseColor("#666666"));
            binding.tvBillValue.setText("0");
        } else {
            binding.viewBillAccent.setBackgroundColor(Color.parseColor("#81ECFF"));
            binding.tvBillValue.setText(currencyFormat.format(metric.billSyp));
        }

        // Update Grid Monitor
        try {
            JSONObject gridData = new JSONObject();
            gridData.put("v", metric.volts);
            gridMonitor.processJsonData(gridData.toString());
        } catch (Exception e) {
            Log.e("HomeFragment", "Grid update error", e);
        }
    }

    private void updateDeviceStatusUI(Esp32WebSocketManager.RelayStatusMessage data) {
        if (binding == null) return;
        for (int i = 0; i < deviceList.size(); i++) {
            Device d = deviceList.get(i);
            if (d.getId().equals(data.deviceId)) {
                d.setState(data.state == 1);
                d.setConsumptionKw(data.kw);
                deviceAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void setupAlerts() {
        alertList = new ArrayList<>();
        alertAdapter = new AlertAdapter(alertList);
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAlerts.setHasFixedSize(true);
        binding.rvAlerts.setNestedScrollingEnabled(false);
        binding.rvAlerts.setAdapter(alertAdapter);

        // Manage visibility based on content
        alertAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updateAlertsVisibility();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updateAlertsVisibility();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updateAlertsVisibility();
            }
        });

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                alertAdapter.removeAt(viewHolder.getBindingAdapterPosition());
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvAlerts);
    }

    private void updateAlertsVisibility() {
        int visibility = alertList.isEmpty() ? View.GONE : View.VISIBLE;
        binding.rvAlerts.setVisibility(visibility);
    }

    private void setupDevices() {
        deviceList = new ArrayList<>();
        // Initial sample data
        deviceList.add(new Device("living_room_ac", "Living Room AC", 1.5, true, R.drawable.ic_ac));
        deviceList.add(new Device("kitchen_fridge", "Kitchen Fridge", 0.3, true, R.drawable.ic_fridge));
        deviceList.add(new Device("water_heater", "Water Heater", 0.0, false, R.drawable.ic_water_heater));

        deviceAdapter = new DeviceAdapter(deviceList, (device, isChecked) -> sendCommandToESP32(device.getId(), isChecked ? 1 : 0));
        
        binding.rvDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDeviceList.setHasFixedSize(true);
        binding.rvDeviceList.setNestedScrollingEnabled(false);
        binding.rvDeviceList.setAdapter(deviceAdapter);
    }

    private void setupViewAllToggle() {
        binding.tvViewAll.setOnClickListener(v -> {
            TransitionManager.beginDelayedTransition(binding.getRoot());
            if (isDevicesExpanded) {
                // Collapse
                binding.rvDeviceList.setVisibility(View.GONE);
                binding.tvViewAll.setText(R.string.view_all);
                binding.tvViewAll.setTextColor(Color.parseColor("#81ECFF"));
                isDevicesExpanded = false;
            } else {
                // Expand
                binding.rvDeviceList.setVisibility(View.VISIBLE);
                binding.tvViewAll.setText(R.string.view_none);
                binding.tvViewAll.setTextColor(Color.parseColor("#A7ABB1"));
                isDevicesExpanded = true;
            }
        });
    }

    private void sendCommandToESP32(String deviceId, int state) {
        // [ESP32 DEV GUIDE] Dispatching command via WebSocket
        if (wsManager != null) {
            wsManager.sendRelayCommand(deviceId, state);
        }
    }

    @SuppressWarnings("unused")
    public void onDeviceStatusReceived(String jsonInput) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                JSONObject data = new JSONObject(jsonInput);
                String deviceId = data.getString("device_id");
                int state = data.getInt("state");
                double consumption = data.optDouble("consumption_kw", 0.0);

                for (int i = 0; i < deviceList.size(); i++) {
                    Device d = deviceList.get(i);
                    if (d.getId().equals(deviceId)) {
                        d.setState(state == 1);
                        d.setConsumptionKw(consumption);
                        deviceAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error parsing device status", e);
            }
        });
    }

    @SuppressWarnings("unused")
    public void refreshTelemetry(String jsonInput) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                PowerMetric metric = gson.fromJson(jsonInput, PowerMetric.class);

                // Update Power
                if (metric.getPowerW() == null || metric.getPowerW() == 0) {
                    binding.viewPowerAccent.setBackgroundColor(Color.parseColor("#666666"));
                    binding.tvPowerValue.setText("0");
                    binding.tvPowerUnit.setText(R.string.unit_w);
                } else {
                    binding.viewPowerAccent.setBackgroundColor(Color.parseColor("#2FF801"));
                    if (metric.getPowerW() >= 1000) {
                        binding.tvPowerValue.setText(String.format(Locale.getDefault(), "%.1f", metric.getPowerW() / 1000.0));
                        binding.tvPowerUnit.setText(R.string.unit_kw);
                    } else {
                        binding.tvPowerValue.setText(String.valueOf(metric.getPowerW().intValue()));
                        binding.tvPowerUnit.setText(R.string.unit_w);
                    }
                }

                // Update Bill
                if (metric.getBillEst() == null || metric.getBillEst() == 0) {
                    binding.viewBillAccent.setBackgroundColor(Color.parseColor("#666666"));
                    binding.tvBillValue.setText("0");
                } else {
                    binding.viewBillAccent.setBackgroundColor(Color.parseColor("#81ECFF"));
                    binding.tvBillValue.setText(currencyFormat.format(metric.getBillEst()));
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error refreshing telemetry", e);
            }
        });
    }

    @SuppressWarnings("unused")
    public void updateUI(JSONObject data) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                if (data.has("v")) {
                    gridMonitor.processJsonData(data.toString());
                }
                if (data.has("error")) {
                    showAlert(getString(R.string.system_error), data.getString("error"));
                }
            } catch (Exception e) {
                Log.e("HomeFragment", "Error updating UI", e);
            }
        });
    }

    public void showAlert(String title, String message) {
        Alert newAlert = new Alert(title, message, System.currentTimeMillis());
        alertList.add(0, newAlert);
        alertAdapter.notifyItemInserted(0);
        binding.rvAlerts.scrollToPosition(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}