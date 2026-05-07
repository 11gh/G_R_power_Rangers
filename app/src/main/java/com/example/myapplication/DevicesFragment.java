package com.example.myapplication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
                wsManager.sendModeCommand(deviceId, "manual");
            } else if (checkedId == R.id.btnAuto) {
                binding.scrollAutoSettings.setVisibility(View.VISIBLE);
                wsManager.sendModeCommand(deviceId, "auto");
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

        int selectedIndex = 0;
        if (deviceId != null) {
            for (int i = 0; i < deviceList.size(); i++) {
                if (deviceList.get(i).getId().equals(deviceId)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        if (!deviceList.isEmpty()) {
            binding.autoCompleteDeviceSelector.setText(deviceList.get(selectedIndex).getName(), false);
            updateSelectedDevice(deviceList.get(selectedIndex));
        }

        binding.autoCompleteDeviceSelector.setOnItemClickListener((parent, view, position, id) -> {
            updateSelectedDevice(deviceList.get(position));
        });
    }

    private void updateSelectedDevice(Device device) {
        this.deviceId = device.getId();
        this.deviceName = device.getName();
        binding.tvDeviceTitle.setText(deviceName);

        // Load saved Limit
        float savedLimit = (float) device.getLimit();
        binding.sliderLimit.setValue(savedLimit);
        binding.tvLimitValue.setText(String.format(Locale.getDefault(), "%.0f ليرة", savedLimit));
        binding.progressLimit.setProgress(0); 

        // Load saved Schedule
        binding.btnStartTime.setText(device.getStartTime());
        binding.sliderWorkDuration.setValue((float) device.getWorkDuration());
        binding.tvWorkDuration.setText(String.format(Locale.getDefault(), "مدة العمل: %.1f ساعة", device.getWorkDuration()));
        binding.sliderOffDuration.setValue((float) device.getOffDuration());
        binding.tvOffDuration.setText(String.format(Locale.getDefault(), "مدة التوقف: %.1f ساعة", device.getOffDuration()));
    }

    private void setupAddDevice() {
        binding.btnAddDevice.setOnClickListener(v -> showAddDeviceDialog());
    }

    private void showDeleteConfirmation(Device device) {
        new AlertDialog.Builder(requireContext())
                .setTitle("حذف الجهاز")
                .setMessage("هل أنت متأكد من رغبتك في حذف جهاز " + device.getName() + "؟")
                .setPositiveButton("حذف", (dialog, which) -> {
                    deleteDevice(device);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteDevice(Device device) {
        deviceManager.removeDevice(device);
        dropdownAdapter.notifyDataSetChanged();
        
        if (device.getId().equals(deviceId)) {
            if (!deviceList.isEmpty()) {
                updateSelectedDevice(deviceList.get(0));
                binding.autoCompleteDeviceSelector.setText(deviceList.get(0).getName(), false);
            } else {
                deviceId = null;
                deviceName = null;
                binding.tvDeviceTitle.setText("لا يوجد أجهزة");
                binding.autoCompleteDeviceSelector.setText("", false);
            }
        }
        Toast.makeText(getContext(), "تم حذف الجهاز", Toast.LENGTH_SHORT).show();
    }

    private void showAddDeviceDialog() {
        final EditText etName = new EditText(getContext());
        etName.setHint("اسم الجهاز (مثلاً: غسالة)");
        final EditText etId = new EditText(getContext());
        etId.setHint("ID الجهاز (مثلاً: washer_1)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(etName);
        layout.addView(etId);

        new AlertDialog.Builder(requireContext())
                .setTitle("إضافة جهاز جديد")
                .setView(layout)
                .setPositiveButton("إضافة", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String id = etId.getText().toString();
                    if (!name.isEmpty() && !id.isEmpty()) {
                        Device newDevice = new Device(id, name, 0, false, R.drawable.ic_power);
                        deviceManager.addDevice(newDevice);
                        dropdownAdapter.notifyDataSetChanged();
                        updateSelectedDevice(newDevice);
                        binding.autoCompleteDeviceSelector.setText(name, false);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    @Override
    public void onStart() {
        super.onStart();
        wsManager.setMessageReceivedListener(new Esp32WebSocketManager.OnMessageReceivedListener() {
            @Override
            public void onTelemetryReceived(Esp32WebSocketManager.TelemetryMessage data) {
                updateMetrics(data.current, data.volts, data.pwrW, data.energy, data.pf, data.freq, data.billSyp);
                
                binding.swMainRelay.setOnCheckedChangeListener(null);
                binding.swMainRelay.setChecked(data.relayState == 1);
                setupRelayControl();
                
                binding.toggleGroupMode.removeOnButtonCheckedListener(modeListener);
                if ("auto".equals(data.mode)) {
                    binding.toggleGroupMode.check(R.id.btnAuto);
                    binding.scrollAutoSettings.setVisibility(View.VISIBLE);
                } else {
                    binding.toggleGroupMode.check(R.id.btnManual);
                    binding.scrollAutoSettings.setVisibility(View.GONE);
                }
                binding.toggleGroupMode.addOnButtonCheckedListener(modeListener);
            }

            @Override
            public void onEmergencyReceived(Esp32WebSocketManager.EmergencyMessage data) {
                showEmergencyDialog(data.reason, data.v, data.i);
            }

            @Override
            public void onRelayStatusReceived(Esp32WebSocketManager.RelayStatusMessage data) {
                if (data.deviceId != null && data.deviceId.equals(deviceId)) {
                    binding.swMainRelay.setOnCheckedChangeListener(null);
                    binding.swMainRelay.setChecked(data.state == 1);
                    setupRelayControl();
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

    private void showEmergencyDialog(String reason, double v, double i) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("🚨 حماية كهربائية (Emergency)")
                .setMessage("تم فصل الجهاز لحمايته!\n\nالسبب: " + reason + 
                           "\nالجهد: " + String.format(Locale.getDefault(), "%.1f V", v) +
                           "\nالتيار: " + String.format(Locale.getDefault(), "%.1f A", i))
                .setPositiveButton("موافق", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void setupRelayControl() {
        binding.swMainRelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (deviceId != null) {
                wsManager.sendRelayCommand(deviceId, isChecked ? 1 : 0);
            }
        });
    }

    private void setupToggleGroup() {
        binding.toggleGroupMode.addOnButtonCheckedListener(modeListener);
    }

    private void setupAutoSettings() {
        // Limit Slider
        binding.sliderLimit.addOnChangeListener((slider, value, fromUser) -> {
            binding.tvLimitValue.setText(String.format(Locale.getDefault(), "%.0f ليرة", value));
        });

        // Work Duration Slider
        binding.sliderWorkDuration.addOnChangeListener((slider, value, fromUser) -> {
            binding.tvWorkDuration.setText(String.format(Locale.getDefault(), "مدة العمل: %.1f ساعة", value));
        });

        // Off Duration Slider
        binding.sliderOffDuration.addOnChangeListener((slider, value, fromUser) -> {
            binding.tvOffDuration.setText(String.format(Locale.getDefault(), "مدة التوقف: %.1f ساعة", value));
        });

        // Start Time Button
        binding.btnStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minuteOfHour) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                binding.btnStartTime.setText(time);
            }, hour, minute, true);
            timePickerDialog.show();
        });

        // Start Limit Button
        binding.btnSaveOnlyLimit.setOnClickListener(v -> {
            if (deviceId == null) return;
            float limit = binding.sliderLimit.getValue();
            for (Device d : deviceList) {
                if (d.getId().equals(deviceId)) {
                    d.setLimit(limit);
                    break;
                }
            }
            deviceManager.saveDevices();
            wsManager.sendLimitValue(deviceId, (double) limit);
            binding.progressLimit.setProgress(0);
            Toast.makeText(getContext(), "تم بدء مراقبة التكلفة", Toast.LENGTH_SHORT).show();
        });

        // Cancel Limit Button
        binding.btnCancelLimit.setOnClickListener(v -> {
            if (deviceId == null) return;
            for (Device d : deviceList) {
                if (d.getId().equals(deviceId)) {
                    d.setLimit(0);
                    break;
                }
            }
            deviceManager.saveDevices();
            wsManager.sendLimitValue(deviceId, 0.0);
            binding.sliderLimit.setValue(0);
            binding.tvLimitValue.setText("0 ليرة");
            binding.progressLimit.setProgress(0);
            Toast.makeText(getContext(), "تم إلغاء مراقبة التكلفة", Toast.LENGTH_SHORT).show();
        });

        // Save Schedule Button
        binding.btnSaveOnlySchedule.setOnClickListener(v -> {
            if (deviceId == null) return;
            String startTime = binding.btnStartTime.getText().toString();
            float workHrs = binding.sliderWorkDuration.getValue();
            float offHrs = binding.sliderOffDuration.getValue();

            for (Device d : deviceList) {
                if (d.getId().equals(deviceId)) {
                    d.setStartTime(startTime);
                    d.setWorkDuration(workHrs);
                    d.setOffDuration(offHrs);
                    break;
                }
            }
            deviceManager.saveDevices();
            wsManager.sendScheduleCommand(deviceId, startTime, (double) workHrs, (double) offHrs);
            Toast.makeText(getContext(), "تم حفظ الخطة الزمنية بنجاح", Toast.LENGTH_SHORT).show();
        });

        // Cancel Schedule Button
        binding.btnCancelSchedule.setOnClickListener(v -> {
            if (deviceId == null) return;
            String startTime = "00:00";
            float workHrs = 0.0f;
            float offHrs = 0.0f;

            for (Device d : deviceList) {
                if (d.getId().equals(deviceId)) {
                    d.setStartTime(startTime);
                    d.setWorkDuration(workHrs);
                    d.setOffDuration(offHrs);
                    break;
                }
            }
            deviceManager.saveDevices();
            wsManager.sendScheduleCommand(deviceId, startTime, 0.0, 0.0);
            
            binding.btnStartTime.setText(startTime);
            binding.sliderWorkDuration.setValue(0);
            binding.tvWorkDuration.setText("مدة العمل: 0 ساعة");
            binding.sliderOffDuration.setValue(0);
            binding.tvOffDuration.setText("مدة التوقف: 0 ساعة");
            
            Toast.makeText(getContext(), "تم إلغاء الخطة الزمنية", Toast.LENGTH_SHORT).show();
        });
    }

    public void updateMetrics(double current, double voltage, double power, double energy, double pf, double freq, double billSyp) {
        if (binding == null) return;
        
        binding.tvCurrentValue.setText(String.format(Locale.getDefault(), "%.2f A", current));
        binding.tvVoltageValue.setText(String.format(Locale.getDefault(), "%.1f V", voltage));
        binding.tvPowerValue.setText(String.format(Locale.getDefault(), "%.1f W", power));
        binding.tvEnergyValue.setText(String.format(Locale.getDefault(), "%.2f Wh", energy));
        binding.tvPFValue.setText(String.format(Locale.getDefault(), "%.2f", pf));
        binding.tvFreqValue.setText(String.format(Locale.getDefault(), "%.1f Hz", freq));

        double limit = binding.sliderLimit.getValue();
        if (limit > 0) {
            int progress = (int) ((billSyp / limit) * 100);
            binding.progressLimit.setProgress(Math.min(progress, 100), true);
            binding.tvLimitValue.setText(String.format(Locale.getDefault(), "%.0f / %.0f ليرة", billSyp, limit));
        } else {
            binding.progressLimit.setProgress(0);
            binding.tvLimitValue.setText(String.format(Locale.getDefault(), "%.0f ليرة", billSyp));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class DeviceDropdownAdapter extends ArrayAdapter<Device> {
        public DeviceDropdownAdapter(@NonNull Context context, @NonNull List<Device> devices) {
            super(context, R.layout.item_device_dropdown, devices);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_device_dropdown, parent, false);
            }
            Device device = getItem(position);
            TextView tvName = convertView.findViewById(R.id.tvDeviceName);
            ImageView ivIcon = convertView.findViewById(R.id.ivDeviceIcon);

            if (device != null) {
                tvName.setText(device.getName());
                ivIcon.setImageResource(device.getIconResId());
            }

            convertView.setOnLongClickListener(v -> {
                showDeleteConfirmation(device);
                return true;
            });

            return convertView;
        }
    }
}
