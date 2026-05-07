package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_test, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.tvDeviceName.setText(device.getName());
        
        // Map consumption to energy value
        holder.tvEnergyValue.setText(String.format(Locale.getDefault(), "%.1f", device.getConsumptionKw()));
        // Price value (placeholder or calculated if available)
        holder.tvPriceValue.setText("---");

        updateUIState(holder, device);

        holder.swDeviceToggle.setOnCheckedChangeListener(null);
        holder.swDeviceToggle.setChecked(device.getState());
        holder.swDeviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            device.setState(isChecked);
            updateUIState(holder, device);
            if (listener != null) {
                listener.onDeviceToggle(device, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeviceLongClick(device, holder.getBindingAdapterPosition());
                return true;
            }
            return false;
        });
    }

    private void updateUIState(DeviceViewHolder holder, Device device) {
        if (device.getState()) {
            holder.cardDevice.setStrokeColor(Color.parseColor("#81ECFF"));
            holder.tvDeviceName.setTextColor(Color.parseColor("#F3F7FD"));
        } else {
            holder.cardDevice.setStrokeColor(Color.parseColor("#444444"));
            holder.tvDeviceName.setTextColor(Color.parseColor("#A7ABB1"));
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardDevice;
        TextView tvDeviceName, tvEnergyValue, tvPriceValue;
        MaterialSwitch swDeviceToggle;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDevice = itemView.findViewById(R.id.cardDevice);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvEnergyValue = itemView.findViewById(R.id.item_energy_value);
            tvPriceValue = itemView.findViewById(R.id.item_price_value);
            swDeviceToggle = itemView.findViewById(R.id.swDeviceToggle);
        }
    }
}
