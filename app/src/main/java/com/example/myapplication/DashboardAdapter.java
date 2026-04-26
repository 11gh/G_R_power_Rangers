package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.databinding.ItemDeviceBinding;
import com.example.myapplication.databinding.LayoutDashboardHeaderBinding;
import com.example.myapplication.databinding.LayoutDashboardGridBinding;
import com.example.myapplication.databinding.LayoutDashboardSectionHeaderBinding;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DashboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_GRID = 1;
    public static final int TYPE_SECTION_HEADER = 2;
    public static final int TYPE_DEVICE = 3;

    private List<Device> devices;
    private DeviceAdapter.OnDeviceChangeListener deviceChangeListener;
    private Double powerW = 0.0;
    private Double billEst = 0.0;
    private String gridDataJson = null;
    private GridMonitorWidget gridMonitor;
    private DecimalFormat currencyFormat = new DecimalFormat("#,###");

    public DashboardAdapter(List<Device> devices, DeviceAdapter.OnDeviceChangeListener listener) {
        this.devices = devices;
        this.deviceChangeListener = listener;
    }

    public void updateTelemetry(Double power, Double bill) {
        this.powerW = power;
        this.billEst = bill;
        notifyItemChanged(0);
    }

    public void updateGridData(String json) {
        this.gridDataJson = json;
        notifyItemChanged(1);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position == 1) return TYPE_GRID;
        if (position == 2) return TYPE_SECTION_HEADER;
        return TYPE_DEVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(LayoutDashboardHeaderBinding.inflate(inflater, parent, false));
            case TYPE_GRID:
                return new GridViewHolder(LayoutDashboardGridBinding.inflate(inflater, parent, false));
            case TYPE_SECTION_HEADER:
                return new SectionHeaderViewHolder(LayoutDashboardSectionHeaderBinding.inflate(inflater, parent, false));
            case TYPE_DEVICE:
                return new DeviceViewHolder(ItemDeviceBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(powerW, billEst);
        } else if (holder instanceof GridViewHolder) {
            ((GridViewHolder) holder).bind(gridDataJson);
        } else if (holder instanceof SectionHeaderViewHolder) {
            ((SectionHeaderViewHolder) holder).bind();
        } else if (holder instanceof DeviceViewHolder) {
            ((DeviceViewHolder) holder).bind(devices.get(position - 3), deviceChangeListener);
        }
    }

    @Override
    public int getItemCount() {
        return 3 + devices.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private LayoutDashboardHeaderBinding binding;
        HeaderViewHolder(LayoutDashboardHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(Double power, Double bill) {
            if (power == null || power == 0) {
                binding.viewPowerAccent.setBackgroundColor(Color.parseColor("#666666"));
                binding.tvPowerValue.setText("0");
                binding.tvPowerUnit.setText("W");
            } else {
                binding.viewPowerAccent.setBackgroundColor(Color.parseColor("#2FF801"));
                if (power >= 1000) {
                    binding.tvPowerValue.setText(String.format("%.1f", power / 1000.0));
                    binding.tvPowerUnit.setText("kW");
                } else {
                    binding.tvPowerValue.setText(String.valueOf(power.intValue()));
                    binding.tvPowerUnit.setText("W");
                }
            }
            if (bill == null || bill == 0) {
                binding.viewBillAccent.setBackgroundColor(Color.parseColor("#666666"));
                binding.tvBillValue.setText("0");
            } else {
                binding.viewBillAccent.setBackgroundColor(Color.parseColor("#81ECFF"));
                binding.tvBillValue.setText(currencyFormat.format(bill));
            }
        }
    }

    class GridViewHolder extends RecyclerView.ViewHolder {
        private LayoutDashboardGridBinding binding;
        GridViewHolder(LayoutDashboardGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            gridMonitor = new GridMonitorWidget(binding.gridMonitor.getRoot());
        }
        void bind(String json) {
            if (json != null) {
                gridMonitor.processJsonData(json);
            }
        }
    }

    class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
        private LayoutDashboardSectionHeaderBinding binding;
        SectionHeaderViewHolder(LayoutDashboardSectionHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind() {
            // Setup toggle logic if needed
        }
    }

    class DeviceViewHolder extends RecyclerView.ViewHolder {
        private ItemDeviceBinding binding;
        DeviceViewHolder(ItemDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(Device device, DeviceAdapter.OnDeviceChangeListener listener) {
            binding.tvDeviceName.setText(device.getName());
            binding.ivDeviceIcon.setImageResource(device.getIconResId());
            
            if (device.getState()) {
                binding.cardDevice.setCardBackgroundColor(Color.parseColor("#0E1418"));
                binding.tvDeviceName.setTextColor(Color.parseColor("#F3F7FD"));
                binding.tvDeviceStatus.setText(String.format("%.1f kW", device.getConsumptionKw()));
                binding.tvDeviceStatus.setTextColor(Color.parseColor("#81ECFF"));
                binding.ivDeviceIcon.setColorFilter(Color.parseColor("#81ECFF"));
            } else {
                binding.cardDevice.setCardBackgroundColor(Color.parseColor("#6D7275"));
                binding.tvDeviceName.setTextColor(Color.parseColor("#1F272C"));
                binding.tvDeviceStatus.setText("Standby");
                binding.tvDeviceStatus.setTextColor(Color.parseColor("#1F272C"));
                binding.ivDeviceIcon.setColorFilter(Color.parseColor("#1F272C"));
            }

            binding.swDeviceToggle.setOnCheckedChangeListener(null);
            binding.swDeviceToggle.setChecked(device.getState());
            binding.swDeviceToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onDeviceToggle(device, isChecked);
                }
            });
        }
    }
}