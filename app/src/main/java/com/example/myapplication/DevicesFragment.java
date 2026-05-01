package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentDevicesBinding;
import java.util.Locale;

public class DevicesFragment extends Fragment {

    private FragmentDevicesBinding binding;
    private String deviceId;
    private String deviceName;
    private Esp32WebSocketManager wsManager;

    public static DevicesFragment newInstance(String deviceId, String deviceName) {
        DevicesFragment fragment = new DevicesFragment();
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
        binding = FragmentDevicesBinding.inflate(inflater, container, false);
        
        if (deviceName != null) {
            binding.tvDeviceTitle.setText(deviceName);
        }

        setupRelayControl();
        setupToggleGroup();
        setupAutoSettings();
        
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Register listener to receive real-time data from ESP32
        wsManager.setMessageReceivedListener(new Esp32WebSocketManager.OnMessageReceivedListener() {
            @Override
            public void onTelemetryReceived(Esp32WebSocketManager.TelemetryMessage data) {
                updateMetrics(data.current, data.volts, data.pwrKw * 1000.0, data.energy, data.pf, data.freq);
            }

            @Override
            public void onRelayStatusReceived(Esp32WebSocketManager.RelayStatusMessage data) {
                if (data.deviceId.equals(deviceId)) {
                    binding.swMainRelay.setOnCheckedChangeListener(null);
                    binding.swMainRelay.setChecked(data.state == 1);
                    setupRelayControl(); // re-enable listener
                }
            }

            @Override
            public void onAlertReceived(Esp32WebSocketManager.AlertMessage data) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), data.title + ": " + data.body, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupRelayControl() {
        binding.swMainRelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Send command to ESP32 to toggle relay
            wsManager.sendRelayCommand(deviceId, isChecked ? 1 : 0);
        });
    }

    private void setupToggleGroup() {
        binding.toggleGroupMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnManual) {
                    binding.cardAutoSettings.setVisibility(View.GONE);
                    wsManager.sendModeCommand(deviceId, "manual");
                } else if (checkedId == R.id.btnAuto) {
                    binding.cardAutoSettings.setVisibility(View.VISIBLE);
                    wsManager.sendModeCommand(deviceId, "auto");
                }
            }
        });
    }

    private void setupAutoSettings() {
        binding.btnSaveLimit.setOnClickListener(v -> {
            String limitStr = binding.etLimitPrice.getText().toString();
            if (!limitStr.isEmpty()) {
                double limitValue = Double.parseDouble(limitStr);
                wsManager.sendLimitValue(deviceId, limitValue);
                Toast.makeText(getContext(), "تم حفظ الحد: " + limitValue + " ليرة", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "الرجاء إدخال قيمة صحيحة", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateMetrics(double current, double voltage, double power, double energy, double pf, double freq) {
        if (binding == null) return;
        binding.tvCurrentValue.setText(String.format(Locale.getDefault(), "%.2f A", current));
        binding.tvVoltageValue.setText(String.format(Locale.getDefault(), "%.1f V", voltage));
        binding.tvPowerValue.setText(String.format(Locale.getDefault(), "%.1f W", power));
        binding.tvEnergyValue.setText(String.format(Locale.getDefault(), "%.2f Wh", energy));
        binding.tvPFValue.setText(String.format(Locale.getDefault(), "%.2f", pf));
        binding.tvFrequencyValue.setText(String.format(Locale.getDefault(), "%.1f Hz", freq));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}