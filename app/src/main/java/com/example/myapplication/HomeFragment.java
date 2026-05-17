package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myapplication.databinding.FragmentHomeTestBinding;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeTestBinding binding;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;
    private Esp32WebSocketManager wsManager;
    private DeviceManager deviceManager;
    private final DecimalFormat currencyFormat = new DecimalFormat("#,###");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeTestBinding.inflate(inflater, container, false);
        deviceManager = DeviceManager.getInstance(requireContext());
        
        setupDevices();
        setupWebSocket();

        return binding.getRoot();
    }

    private void setupWebSocket() {
        wsManager = Esp32WebSocketManager.getInstance();
        wsManager.connect();

        wsManager.setConnectionStatusListener(connected -> {
            if (binding == null) return;
            binding.tvPowerUnit.setAlpha(connected ? 1.0f : 0.5f);
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
            public void onAlertReceived(Esp32WebSocketManager.AlertMessage data) { }

            @Override
            public void onEmergencyReceived(Esp32WebSocketManager.EmergencyMessage data) {
                if (getContext() != null) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Emergency")
                            .setMessage(data.reason)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        });
    }

    private void updateTelemetryUI(Esp32WebSocketManager.TelemetryMessage metric) {
        if (binding == null) return;

        // تحديث قيمة الاستهلاك (kWh)
        double energyKwh = metric.energy;
        binding.energyValue.setText(String.format(Locale.getDefault(), "%.3f", energyKwh));

        // حساب السعر بناءً على المعادلة المطلوبة:
        // أول 0.01 ك.و.س بسعر 600 ليرة
        // ما بعد ذلك، كل 0.01 ك.و.س بسعر 1400 ليرة
        double totalBill = 0;
        if (energyKwh <= 0.01) {
            totalBill = (energyKwh / 0.01) * 600;
        } else {
            totalBill = 600 + ((energyKwh - 0.01) / 0.01) * 1400;
        }
        binding.priceValue.setText(currencyFormat.format(totalBill));

        // إظهار القدرة اللحظية (الواط) بجانب الوحدة إذا لزم الأمر
        binding.tvPowerUnit.setText(String.format(Locale.getDefault(), "%.1f W", metric.pwrW));
    }

    private void updateDeviceStatusUI(Esp32WebSocketManager.RelayStatusMessage data) {
        if (binding == null) return;
        for (int i = 0; i < deviceList.size(); i++) {
            Device d = deviceList.get(i);
            if (d.getId().equals(data.deviceId)) {
                d.setState(data.state == 1);
                deviceAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void setupDevices() {
        deviceList = deviceManager.getDevices();
        deviceAdapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceChangeListener() {
            @Override public void onDeviceToggle(Device device, boolean isChecked) { wsManager.sendRelayCommand(device.getId(), isChecked ? 1 : 0); }
            @Override public void onDeviceClick(Device device) { navigateToDeviceDetails(device); }
            @Override public void onDeviceLongClick(Device device, int position) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("حذف الجهاز")
                        .setMessage("هل أنت متأكد من حذف جهاز " + device.getName() + "؟")
                        .setPositiveButton("حذف", (dialog, which) -> {
                            deviceManager.removeDeviceAt(position);
                            deviceAdapter.notifyItemRemoved(position);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });
        binding.rvDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDeviceList.setAdapter(deviceAdapter);
    }

    private void navigateToDeviceDetails(Device device) {
        DevicesFragment detailsFragment = DevicesFragment.newInstance(device.getId(), device.getName());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
