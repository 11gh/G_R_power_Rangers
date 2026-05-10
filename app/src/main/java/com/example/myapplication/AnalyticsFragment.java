package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myapplication.databinding.FragmentAnalyticsBinding;
import java.util.List;

public class AnalyticsFragment extends Fragment {

    private FragmentAnalyticsBinding binding;
    private AnalyticsDeviceAdapter deviceAdapter;
    private List<Device> deviceList;
    private DeviceManager deviceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAnalyticsBinding.inflate(inflater, container, false);
        deviceManager = DeviceManager.getInstance(requireContext());
        deviceList = deviceManager.getDevices();
        
        setupRecyclerView();
        setupTotalCard();

        return binding.getRoot();
    }

    private void setupRecyclerView() {
        deviceAdapter = new AnalyticsDeviceAdapter(deviceList, device -> {
            navigateToDeviceDetail(device);
        });
        binding.rvDeviceAnalytics.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDeviceAnalytics.setAdapter(deviceAdapter);
    }

    private void setupTotalCard() {
        binding.cardTotal.setOnClickListener(v -> {
            navigateToTotalDetail();
        });
    }

    private void navigateToDeviceDetail(Device device) {
        DeviceAnalyticsDetailFragment detailFragment = DeviceAnalyticsDetailFragment.newInstance(device.getId(), device.getName());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToTotalDetail() {
        TotalAnalyticsDetailFragment detailFragment = new TotalAnalyticsDetailFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
