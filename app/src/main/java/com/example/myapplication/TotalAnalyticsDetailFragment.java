package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.myapplication.databinding.FragmentTotalAnalyticsDetailBinding;
import java.util.Locale;

public class TotalAnalyticsDetailFragment extends Fragment {

    private FragmentTotalAnalyticsDetailBinding binding;
    private Esp32WebSocketManager wsManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wsManager = Esp32WebSocketManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTotalAnalyticsDetailBinding.inflate(inflater, container, false);
        
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupWebSocketListener();

        return binding.getRoot();
    }

    private void setupWebSocketListener() {
        wsManager.setMessageReceivedListener(new Esp32WebSocketManager.OnMessageReceivedListener() {
            @Override
            public void onTelemetryReceived(Esp32WebSocketManager.TelemetryMessage data) {
                if (binding == null) return;
                updateTotalData(data.energy, data.billSyp);
            }

            @Override
            public void onRelayStatusReceived(Esp32WebSocketManager.RelayStatusMessage data) {}

            @Override
            public void onAlertReceived(Esp32WebSocketManager.AlertMessage data) {}

            @Override
            public void onEmergencyReceived(Esp32WebSocketManager.EmergencyMessage data) {}
        });
    }

    private void updateTotalData(double energy, double price) {
        binding.tvTotalEnergyDetail.setText(String.format(Locale.getDefault(), "%.2f kWh", energy));
        binding.tvTotalPriceDetail.setText(String.format(Locale.getDefault(), "%,.0f SYP", price));
        binding.tvStatusInfo.setText("Live data updated");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
