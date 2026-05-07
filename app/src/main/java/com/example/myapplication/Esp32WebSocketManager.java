package com.example.myapplication;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class Esp32WebSocketManager {
    private static final String TAG = "Esp32WS";
    private static Esp32WebSocketManager instance;
    private final OkHttpClient client;
    private WebSocket webSocket;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isConnected = false;
    private OnConnectionStatusListener connectionStatusListener;
    private OnMessageReceivedListener messageReceivedListener;
    
    private String discoveredIp = null;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;

    public interface OnConnectionStatusListener {
        void onStatusChange(boolean connected);
    }

    public interface OnMessageReceivedListener {
        void onTelemetryReceived(TelemetryMessage data);
        void onRelayStatusReceived(RelayStatusMessage data);
        void onAlertReceived(AlertMessage data);
        void onEmergencyReceived(EmergencyMessage data);
    }

    private Esp32WebSocketManager() {
        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
    }

    public static synchronized Esp32WebSocketManager getInstance() {
        if (instance == null) {
            instance = new Esp32WebSocketManager();
        }
        return instance;
    }

    public void initDiscovery(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        startDiscovery();
    }

    private void startDiscovery() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) { Log.e(TAG, "Discovery failed: " + errorCode); }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) { Log.e(TAG, "Stop Discovery failed: " + errorCode); }
            @Override
            public void onDiscoveryStarted(String serviceType) { Log.d(TAG, "Service discovery started"); }
            @Override
            public void onDiscoveryStopped(String serviceType) { Log.i(TAG, "Discovery stopped"); }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                if (serviceInfo.getServiceName().contains("power-ranger")) {
                    nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}
                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            discoveredIp = serviceInfo.getHost().getHostAddress();
                            mainHandler.post(() -> connect());
                        }
                    });
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) { isConnected = false; }
        };
        nsdManager.discoverServices("_ws._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void setConnectionStatusListener(OnConnectionStatusListener listener) { this.connectionStatusListener = listener; }
    public void setMessageReceivedListener(OnMessageReceivedListener listener) { this.messageReceivedListener = listener; }

    public void connect() {
        if (isConnected || discoveredIp == null) return;
        String wsUrl = "ws://" + discoveredIp + ":81";
        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                isConnected = true;
                notifyConnectionStatus(true);
            }
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) { parseIncomingMessage(text); }
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) { isConnected = false; notifyConnectionStatus(false); reconnect(); }
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) { isConnected = false; notifyConnectionStatus(false); reconnect(); }
        });
    }

    private void reconnect() { mainHandler.postDelayed(() -> { if (discoveredIp != null) connect(); else startDiscovery(); }, 5000); }
    private void notifyConnectionStatus(boolean connected) { mainHandler.post(() -> { if (connectionStatusListener != null) connectionStatusListener.onStatusChange(connected); }); }

    private void parseIncomingMessage(String json) {
        try {
            BaseMessage base = gson.fromJson(json, BaseMessage.class);
            if (base.type == null) return;
            switch (base.type) {
                case "telemetry":
                    TelemetryMessage tm = gson.fromJson(json, TelemetryMessage.class);
                    mainHandler.post(() -> { if (messageReceivedListener != null) messageReceivedListener.onTelemetryReceived(tm); });
                    break;
                case "emergency":
                    EmergencyMessage em = gson.fromJson(json, EmergencyMessage.class);
                    mainHandler.post(() -> { if (messageReceivedListener != null) messageReceivedListener.onEmergencyReceived(em); });
                    break;
                case "relay_status":
                    RelayStatusMessage rm = gson.fromJson(json, RelayStatusMessage.class);
                    mainHandler.post(() -> { if (messageReceivedListener != null) messageReceivedListener.onRelayStatusReceived(rm); });
                    break;
            }
        } catch (Exception e) { Log.e(TAG, "Parsing Error", e); }
    }

    public void sendRelayCommand(String deviceId, int targetState) {
        if (webSocket != null && isConnected) {
            GenericCommand cmd = new GenericCommand("toggle_relay", deviceId);
            cmd.targetState = targetState;
            webSocket.send(gson.toJson(cmd));
        }
    }

    public void sendLimitValue(String deviceId, double limit) {
        if (webSocket != null && isConnected) {
            GenericCommand cmd = new GenericCommand("set_limit", deviceId);
            cmd.limitValue = limit;
            webSocket.send(gson.toJson(cmd));
        }
    }

    public void sendModeCommand(String deviceId, String mode) {
        if (webSocket != null && isConnected) {
            GenericCommand cmd = new GenericCommand("set_mode", deviceId);
            cmd.mode = mode;
            webSocket.send(gson.toJson(cmd));
        }
    }

    public void sendScheduleCommand(String deviceId, String startTime, double workHrs, double offHrs) {
        if (webSocket != null && isConnected) {
            GenericCommand cmd = new GenericCommand("set_schedule", deviceId);
            cmd.startTime = startTime;
            cmd.workDuration = workHrs;
            cmd.offDuration = offHrs;
            webSocket.send(gson.toJson(cmd));
        }
    }

    private static class BaseMessage { String type; }

    public static class TelemetryMessage {
        @SerializedName("volts") public double volts;
        @SerializedName("current") public double current;
        @SerializedName("pwr_w") public double pwrW;
        @SerializedName("bill_syp") public double billSyp;
        @SerializedName("relay") public int relayState;
        @SerializedName("mode") public String mode;
        // Fields for compatibility if needed
        public double energy;
        public double pf;
        public double freq;
    }

    public static class EmergencyMessage {
        public String reason;
        public double v;
        public double i;
    }

    public static class RelayStatusMessage {
        @SerializedName("device_id") public String deviceId;
        public int state;
    }

    public static class AlertMessage { public String title; public String body; }

    private static class GenericCommand {
        String action;
        @SerializedName("device_id") String deviceId;
        @SerializedName("target_state") Integer targetState;
        @SerializedName("limit_value") Double limitValue;
        String mode;
        @SerializedName("start_time") String startTime;
        @SerializedName("work_duration") Double workDuration;
        @SerializedName("off_duration") Double offDuration;

        GenericCommand(String action, String deviceId) { this.action = action; this.deviceId = deviceId; }
    }
}