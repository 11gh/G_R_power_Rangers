package com.example.myapplication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentDevicesBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DevicesFragment extends Fragment {

    private FragmentDevicesBinding binding;
    private String deviceId;
    private String deviceName;
    private Esp32WebSocketManager wsManager;
    private List<Device> deviceList;
    private DeviceDropdownAdapter dropdownAdapter;
    private DeviceManager deviceManager;

    private final MaterialButtonToggleGroup.OnButtonCheckedListener modeListener = (group, checkedId, isChecked) -> {
        if (isChecked) {
            if (checkedId == R.id.btnManual) {
                binding.scrollAutoSettings.setVisibility(View.GONE);
                if (deviceId != null) wsManager.sendModeCommand(deviceId, "manual");
            } else if (checkedId == R.id.btnAuto) {
                binding.scrollAutoSettings.setVisibility(View.VISIBLE);
                if (deviceId != null) wsManager.sendModeCommand(deviceId, "auto");
            }
        }
    };

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
        deviceManager = DeviceManager.getInstance(requireContext());
        deviceList = deviceManager.getDevices();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDevicesBinding.inflate(inflater, container, false);
        
        setupDeviceSelector();
        setupRelayControl();
        setupToggleGroup();
        setupAutoSettings();
        setupAddDevice();
        
        return binding.getRoot();
    }

    private void setupDeviceSelector() {
        dropdownAdapter = new DeviceDropdownAdapter(requireContext(), deviceList);
        binding.autoCompleteDeviceSelector.setAdapter(dropdownAdapter);

        int selectedIndex = -1;
        if (deviceId != null) {
            for (int i = 0; i < deviceList.size(); i++) {
                if (deviceList.get(i).getId().equals(deviceId)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (selectedIndex == -1 && !deviceList.isEmpty()) {
            selectedIndex = 0;
        }

        if (selectedIndex != -1) {
            Device selected = deviceList.get(selectedIndex);
            binding.autoCompleteDeviceSelector.setText(selected.getName(), false);
            updateSelectedDevice(selected);
        }

        binding.autoCompleteDeviceSelector.setOnItemClickListener((parent, view, position, id) -> {
            Device selectedDevice = (Device) parent.getItemAtPosition(position);
            if (selectedDevice != null) {
                updateSelectedDevice(selectedDevice);
                binding.autoCompleteDeviceSelector.dismissDropDown();
            }
        });
    }

    private void updateSelectedDevice(Device device) {
        this.deviceId = device.getId();
        this.deviceName = device.getName();
        binding.tvDeviceTitle.setText(deviceName);

        // استعادة حالة التكلفة
        binding.sliderLimit.setValue((float) device.getLimit());
        binding.tvLimitValue.setText(String.format(Locale.getDefault(), "%.0f ليرة", device.getLimit()));
        
        if (device.isLimitActive()) {
            binding.btnPauseLimit.setVisibility(View.VISIBLE);
            binding.btnPauseLimit.setText(device.isLimitPaused() ? "استمرار" : "إيقاف مؤقت");
        } else {
            binding.btnPauseLimit.setVisibility(View.GONE);
        }

        // استعادة حالة الخطة الزمنية
        binding.btnStartTime.setText(device.getStartTime());
        binding.sliderWorkDuration.setValue((float) device.getWorkDuration());
        binding.sliderOffDuration.setValue((float) device.getOffDuration());
        binding.tvWorkDuration.setText(String.format(Locale.getDefault(), "مدة العمل: %.0f ثانية", device.getWorkDuration()));
        binding.tvOffDuration.setText(String.format(Locale.getDefault(), "مدة التوقف: %.0f ثانية", device.getOffDuration()));

        if (device.isScheduleActive()) {
            binding.btnPauseSchedule.setVisibility(View.VISIBLE);
            binding.btnPauseSchedule.setText(device.isSchedulePaused() ? "استمرار" : "إيقاف مؤقت");
        } else {
            binding.btnPauseSchedule.setVisibility(View.GONE);
        }
    }

    private void setupAutoSettings() {
        // --- قسم الحد الأقصى للتكلفة ---
        binding.sliderLimit.addOnChangeListener((slider, value, fromUser) -> 
            binding.tvLimitValue.setText(String.format(Locale.getDefault(), "%.0f ليرة", value)));

        binding.btnSaveOnlyLimit.setOnClickListener(v -> {
            Device dev = getSelectedDevice();
            if (dev == null) return;
            float limit = binding.sliderLimit.getValue();
            dev.setLimit(limit);
            dev.setLimitActive(true);
            dev.setLimitPaused(false);
            deviceManager.saveDevices();
            
            wsManager.sendLimitValue(deviceId, (double) limit);
            binding.btnPauseLimit.setVisibility(View.VISIBLE);
            binding.btnPauseLimit.setText("إيقاف مؤقت");
            Toast.makeText(getContext(), "تم بدء مراقبة التكلفة", Toast.LENGTH_SHORT).show();
        });

        binding.btnPauseLimit.setOnClickListener(v -> {
            Device dev = getSelectedDevice();
            if (dev == null) return;
            boolean newState = !dev.isLimitPaused();
            dev.setLimitPaused(newState);
            deviceManager.saveDevices();
            
            wsManager.sendPauseCommand(deviceId, "limit", newState);
            binding.btnPauseLimit.setText(newState ? "استمرار" : "إيقاف مؤقت");
        });

        binding.btnCancelLimit.setOnClickListener(v -> {
            Device dev = getSelectedDevice();
            if (dev == null) return;
            dev.setLimitActive(false);
            deviceManager.saveDevices();
            
            wsManager.sendLimitValue(deviceId, 0.0);
            binding.btnPauseLimit.setVisibility(View.GONE);
            Toast.makeText(getContext(), "تم إلغاء مراقبة التكلفة", Toast.LENGTH_SHORT).show();
        });

        // --- قسم الخطة الزمنية ---
        binding.sliderWorkDuration.addOnChangeListener((slider, value, fromUser) -> 
            binding.tvWorkDuration.setText(String.format(Locale.getDefault(), "مدة العمل: %.0f ثانية", value)));
            
        binding.sliderOffDuration.addOnChangeListener((slider, value, fromUser) -> 
            binding.tvOffDuration.setText(String.format(Locale.getDefault(), "مدة التوقف: %.0f ثانية", value)));

        binding.btnSaveOnlySchedule.setOnClickListener(v -> {
            Device dev = getSelectedDevice();
            if (dev == null) return;
            String startTime = binding.btnStartTime.getText().toString();
            float workSecs = binding.sliderWorkDuration.getValue();
            float offSecs = binding.sliderOffDuration.getValue();
            
            dev.setStartTime(startTime);
            dev.setWorkDuration(workSecs);
            dev.setOffDuration(offSecs);
            dev.setScheduleActive(true);
            dev.setSchedulePaused(false);
            deviceManager.saveDevices();

            wsManager.sendScheduleCommand(deviceId, startTime, (double) workSecs, (double) offSecs);
            binding.btnPauseSchedule.setVisibility(View.VISIBLE);
            binding.btnPauseSchedule.setText("إيقاف مؤقت");
            Toast.makeText(getContext(), "تم حفظ الخطة الزمنية", Toast.LENGTH_SHORT).show();
        });

        binding.btnPauseSchedule.setOnClickListener(v -> {
            Device dev = getSelectedDevice();
            if (dev == null) return;
            boolean newState = !dev.isSchedulePaused();
            dev.setSchedulePaused(newState);
            deviceManager.saveDevices();
            
            wsManager.sendPauseCommand(deviceId, "schedule", newState);
            binding.btnPauseSchedule.setText(newState ? "استمرار" : "إيقاف مؤقت");
        });

        binding.btnCancelSchedule.setOnClickListener(v -> {
            Device dev = getSelectedDevice();
            if (dev == null) return;
            dev.setScheduleActive(false);
            deviceManager.saveDevices();
            
            wsManager.sendScheduleCommand(deviceId, "00:00", 0.0, 0.0);
            binding.btnPauseSchedule.setVisibility(View.GONE);
            Toast.makeText(getContext(), "تم إيقاف الخطة الزمنية", Toast.LENGTH_SHORT).show();
        });

        binding.btnStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> 
                binding.btnStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
    }

    private Device getSelectedDevice() {
        if (deviceId == null) return null;
        for (Device d : deviceList) {
            if (d.getId().equals(deviceId)) return d;
        }
        return null;
    }

    private void setupRelayControl() {
        binding.swMainRelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (deviceId != null) wsManager.sendRelayCommand(deviceId, isChecked ? 1 : 0);
        });
    }

    private void setupToggleGroup() {
        binding.toggleGroupMode.addOnButtonCheckedListener(modeListener);
    }

    private void setupAddDevice() {
        binding.btnAddDevice.setOnClickListener(v -> showAddDeviceDialog());
    }

    private void showAddDeviceDialog() {
        EditText etName = new EditText(getContext()); etName.setHint("اسم الجهاز");
        EditText etId = new EditText(getContext()); etId.setHint("ID الجهاز");
        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20); layout.addView(etName); layout.addView(etId);

        new AlertDialog.Builder(requireContext()).setTitle("إضافة جهاز جديد").setView(layout)
                .setPositiveButton("إضافة", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String id = etId.getText().toString();
                    if (!name.isEmpty() && !id.isEmpty()) {
                        Device newDevice = new Device(id, name, 0, false, R.drawable.ic_power);
                        deviceManager.addDevice(newDevice);
                        dropdownAdapter.notifyDataSetChanged();
                        binding.autoCompleteDeviceSelector.setText(name, false);
                        updateSelectedDevice(newDevice);
                    }
                }).setNegativeButton("إلغاء", null).show();
    }

    public void updateMetrics(double current, double voltage, double power, double energy, double pf, double freq, double billSyp) {
        if (binding == null) return;
        binding.tvCurrentValue.setText(String.format(Locale.getDefault(), "%.2f A", current));
        binding.tvVoltageValue.setText(String.format(Locale.getDefault(), "%.1f V", voltage));
        binding.tvPowerValue.setText(String.format(Locale.getDefault(), "%.1f W", power));
        binding.tvEnergyValue.setText(String.format(Locale.getDefault(), "%.3f Kwh", energy));
        binding.tvPFValue.setText(String.format(Locale.getDefault(), "%.2f", pf));
        binding.tvFreqValue.setText(String.format(Locale.getDefault(), "%.1f Hz", freq));
    }

    @Override
    public void onStart() {
        super.onStart();
        wsManager.setMessageReceivedListener(new Esp32WebSocketManager.OnMessageReceivedListener() {
            @Override public void onTelemetryReceived(Esp32WebSocketManager.TelemetryMessage data) {
                updateMetrics(data.current, data.volts, data.pwrW, data.energy, data.pf, data.freq, data.billSyp);
                binding.swMainRelay.setOnCheckedChangeListener(null);
                binding.swMainRelay.setChecked(data.relayState == 1);
                setupRelayControl();
            }
            @Override public void onRelayStatusReceived(Esp32WebSocketManager.RelayStatusMessage data) {}
            @Override public void onAlertReceived(Esp32WebSocketManager.AlertMessage data) {}
            @Override public void onEmergencyReceived(Esp32WebSocketManager.EmergencyMessage data) {}
        });
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }

    private class DeviceDropdownAdapter extends ArrayAdapter<Device> {
        public DeviceDropdownAdapter(@NonNull Context context, @NonNull List<Device> devices) { super(context, R.layout.item_device_dropdown, devices); }
        @NonNull @Override public View getView(int pos, @Nullable View conv, @NonNull ViewGroup par) {
            if (conv == null) conv = LayoutInflater.from(getContext()).inflate(R.layout.item_device_dropdown, par, false);
            Device dev = getItem(pos);
            if (dev != null) {
                ((TextView) conv.findViewById(R.id.tvDeviceName)).setText(dev.getName());
                ((ImageView) conv.findViewById(R.id.ivDeviceIcon)).setImageResource(dev.getIconResId());
            }
            return conv;
        }
        @NonNull @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence c) { FilterResults r = new FilterResults(); r.values = deviceList; r.count = deviceList.size(); return r; }
                @Override protected void publishResults(CharSequence c, FilterResults r) { notifyDataSetChanged(); }
                @Override public CharSequence convertResultToString(Object v) { return ((Device) v).getName(); }
            };
        }
    }
}
