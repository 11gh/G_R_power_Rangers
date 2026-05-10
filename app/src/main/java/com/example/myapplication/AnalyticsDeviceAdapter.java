package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class AnalyticsDeviceAdapter extends RecyclerView.Adapter<AnalyticsDeviceAdapter.ViewHolder> {

    private List<Device> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(Device device);
    }

    public AnalyticsDeviceAdapter(List<Device> devices, OnDeviceClickListener listener) {
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_analytics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.tvDeviceName.setText(device.getName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        MaterialCardView cardDevice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            cardDevice = itemView.findViewById(R.id.cardDevice);
        }
    }
}
