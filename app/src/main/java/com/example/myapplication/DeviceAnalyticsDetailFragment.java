package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentDeviceAnalyticsDetailBinding;
import java.util.Locale;

public class DeviceAnalyticsDetailFragment extends Fragment {

    private FragmentDeviceAnalyticsDetailBinding binding;
    private String deviceId;
    private String deviceName;
    private Esp32WebSocketManager wsManager;

    public static DeviceAnalyticsDetailFragment newInstance(String deviceId, String deviceName) {
        DeviceAnalyticsDetailFragment fragment = new DeviceAnalyticsDetailFragment();
        Bundle args = new Bundle();
        args.putString("device_id", deviceId);
        args.putString("device_name", deviceName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            deviceId = getArguments().getString("device_id");
            deviceName = getArguments().getString("device_name");
        }
        wsManager = Esp32WebSocketManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceAnalyticsDetailBinding.inflate(inflater, container, false);
        
        binding.tvDeviceName.setText(deviceName);
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupWebSocketListener();

        return binding.getRoot();
    }

    private void setupWebSocketListener() {
        wsManager.setMessageReceivedListener(new Esp32WebSocketManager.OnMessageReceivedListener() {
            @Override
            public void onTelemetryReceived(Esp32WebSocketManager.TelemetryMessage data) {
                // Here we check if the telemetry is for this specific device
                // Assuming telemetry contains device-specific data if it's an 'analytics' update
                // Or if it's the general telemetry, we might need a specific message for device detail
                // For now, let's update if it's general or if you have a specific message type
            }

            @Override
            public void onRelayStatusReceived(Esp32WebSocketManager.RelayStatusMessage data) {
                if (data.deviceId != null && data.deviceId.equals(deviceId)) {
                    // Update UI if needed
                }
            }

            @Override
            public void onAlertReceived(Esp32WebSocketManager.AlertMessage data) {}

            @Override
            public void onEmergencyReceived(Esp32WebSocketManager.EmergencyMessage data) {}
            
            // Note: If your ESP32 sends a specific "DeviceStats" message, add it here in the listener interface
        });
    }

    // Example method to update data when received (could be called from listener)
    private void updateDeviceData(double energy, double price) {
        if (binding == null) return;
        binding.tvEnergyDetail.setText(String.format(Locale.getDefault(), "%.2f kWh", energy));
        binding.tvPriceDetail.setText(String.format(Locale.getDefault(), "%,.0f SYP", price));
        binding.tvStatusInfo.setText("Data updated from circuit");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
