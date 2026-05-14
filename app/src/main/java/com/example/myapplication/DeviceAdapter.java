package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.databinding.ItemDeviceTestBinding;
import java.util.List;
import java.util.Locale;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<Device> devices;
    private OnDeviceChangeListener listener;

    public interface OnDeviceChangeListener {
        void onDeviceToggle(Device device, boolean isChecked);
        void onDeviceClick(Device device);
        void onDeviceLongClick(Device device, int position);
    }

    public DeviceAdapter(List<Device> devices, OnDeviceChangeListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeviceTestBinding binding = ItemDeviceTestBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new DeviceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.bind(device, listener, position);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeviceTestBinding binding;

        public DeviceViewHolder(@NonNull ItemDeviceTestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Device device, OnDeviceChangeListener listener, int position) {
            binding.tvDeviceName.setText(device.getName());
            
            // تحديث قيم الاستهلاك والتكلفة للجهاز (قيم تجريبية حالياً أو من الكائن)
            binding.itemEnergyValue.setText(String.format(Locale.getDefault(), "%.1f", device.getConsumptionKw()));
            binding.itemPriceValue.setText(String.format(Locale.getDefault(), "%.0f", device.getLimit())); // مثال

            updateUIState(device);

            binding.swDeviceToggle.setOnCheckedChangeListener(null);
            binding.swDeviceToggle.setChecked(device.getState());
            binding.swDeviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                device.setState(isChecked);
                updateUIState(device);
                if (listener != null) {
                    listener.onDeviceToggle(device, isChecked);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceLongClick(device, getBindingAdapterPosition());
                    return true;
                }
                return false;
            });
        }

        private void updateUIState(Device device) {
            if (device.getState()) {
                binding.cardDevice.setStrokeColor(Color.parseColor("#81ECFF"));
                binding.tvDeviceName.setTextColor(Color.parseColor("#F3F7FD"));
                binding.itemEnergyValue.setTextColor(Color.parseColor("#F3F7FD"));
                binding.itemPriceValue.setTextColor(Color.parseColor("#F3F7FD"));
            } else {
                binding.cardDevice.setStrokeColor(Color.parseColor("#444444"));
                binding.tvDeviceName.setTextColor(Color.parseColor("#A7ABB1"));
                binding.itemEnergyValue.setTextColor(Color.parseColor("#6D7275"));
                binding.itemPriceValue.setTextColor(Color.parseColor("#6D7275"));
            }
        }
    }
}
