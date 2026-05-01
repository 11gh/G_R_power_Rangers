package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    }

    public DeviceAdapter(List<Device> devices, OnDeviceChangeListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.tvDeviceName.setText(device.getName());
        holder.ivDeviceIcon.setImageResource(device.getIconResId());
        
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
    }

    private void updateUIState(DeviceViewHolder holder, Device device) {
        if (device.getState()) {
            holder.cardDevice.setCardBackgroundColor(Color.parseColor("#0E1418"));
            holder.tvDeviceName.setTextColor(Color.parseColor("#F3F7FD"));
            holder.tvDeviceStatus.setText(String.format(Locale.getDefault(), "%.1f kW", device.getConsumptionKw()));
            holder.tvDeviceStatus.setTextColor(Color.parseColor("#81ECFF"));
            holder.ivDeviceIcon.setColorFilter(Color.parseColor("#81ECFF"));
        } else {
            holder.cardDevice.setCardBackgroundColor(Color.parseColor("#6D7275"));
            holder.tvDeviceName.setTextColor(Color.parseColor("#1F272C"));
            holder.tvDeviceStatus.setText("Standby");
            holder.tvDeviceStatus.setTextColor(Color.parseColor("#1F272C"));
            holder.ivDeviceIcon.setColorFilter(Color.parseColor("#1F272C"));
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardDevice;
        ImageView ivDeviceIcon;
        TextView tvDeviceName, tvDeviceStatus;
        MaterialSwitch swDeviceToggle;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDevice = itemView.findViewById(R.id.cardDevice);
            ivDeviceIcon = itemView.findViewById(R.id.ivDeviceIcon);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus);
            swDeviceToggle = itemView.findViewById(R.id.swDeviceToggle);
        }
    }
}