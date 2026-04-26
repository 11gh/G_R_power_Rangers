package com.example.myapplication;

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
    private final String wsUrl = "ws://192.168.4.1:81"; // [ESP32 DEV GUIDE] Default ESP32 AP IP

    public interface OnConnectionStatusListener {
        void onStatusChange(boolean connected);
    }

    public interface OnMessageReceivedListener {
        void onTelemetryReceived(TelemetryMessage data);
        void onRelayStatusReceived(RelayStatusMessage data);
        void onAlertReceived(AlertMessage data);
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

    public void setConnectionStatusListener(OnConnectionStatusListener listener) {
        this.connectionStatusListener = listener;
    }

    public void setMessageReceivedListener(OnMessageReceivedListener listener) {
        this.messageReceivedListener = listener;
    }

    public void connect() {
        if (isConnected) return;

        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                isConnected = true;
                Log.d(TAG, "Connected to ESP32");
                notifyConnectionStatus(true);
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                // [ESP32 DEV GUIDE] Background parsing of incoming JSON
                parseIncomingMessage(text);
            }

            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                webSocket.close(1000, null);
                Log.d(TAG, "Closing: " + reason);
            }

            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                isConnected = false;
                Log.d(TAG, "Closed: " + reason);
                notifyConnectionStatus(false);
                reconnect();
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                isConnected = false;
                Log.e(TAG, "Failure: " + t.getMessage());
                notifyConnectionStatus(false);
                reconnect();
            }
        });
    }

    private void reconnect() {
        mainHandler.postDelayed(this::connect, 5000); // Attempt reconnection every 5 seconds
    }

    private void notifyConnectionStatus(boolean connected) {
        mainHandler.post(() -> {
            if (connectionStatusListener != null) {
                connectionStatusListener.onStatusChange(connected);
            }
        });
    }

    private void parseIncomingMessage(String json) {
        try {
            BaseMessage base = gson.fromJson(json, BaseMessage.class);
            if (base.type == null) return;

            switch (base.type) {
                case "telemetry":
                    TelemetryMessage tm = gson.fromJson(json, TelemetryMessage.class);
                    mainHandler.post(() -> {
                        if (messageReceivedListener != null) messageReceivedListener.onTelemetryReceived(tm);
                    });
                    break;
                case "relay_status":
                    RelayStatusMessage rm = gson.fromJson(json, RelayStatusMessage.class);
                    mainHandler.post(() -> {
                        if (messageReceivedListener != null) messageReceivedListener.onRelayStatusReceived(rm);
                    });
                    break;
                case "alert":
                    AlertMessage am = gson.fromJson(json, AlertMessage.class);
                    mainHandler.post(() -> {
                        if (messageReceivedListener != null) messageReceivedListener.onAlertReceived(am);
                    });
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Parsing Error", e);
        }
    }

    /*
     * [ESP32 DEV GUIDE]
     * To toggle a relay, send the following JSON:
     * {
     *   "action": "toggle_relay",
     *   "device_id": "living_room_ac",
     *   "target_state": 1
     * }
     */
    public void sendRelayCommand(String deviceId, int targetState) {
        if (webSocket != null && isConnected) {
            RelayCommand cmd = new RelayCommand("toggle_relay", deviceId, targetState);
            webSocket.send(gson.toJson(cmd));
        }
    }

    // --- Data Models ---

    private static class BaseMessage {
        String type;
    }

    /*
     * [ESP32 DEV GUIDE]
     * Inbound Telemetry (Send every 1000ms):
     * {
     *   "type": "telemetry",
     *   "pwr_kw": 4.2,
     *   "bill_syp": 24500,
     *   "volts": 220.5,
     *   "freq": 50.0
     * }
     */
    public static class TelemetryMessage {
        @SerializedName("pwr_kw") public double pwrKw;
        @SerializedName("bill_syp") public double billSyp;
        @SerializedName("volts") public double volts;
        @SerializedName("freq") public double freq;
    }

    /*
     * [ESP32 DEV GUIDE]
     * Inbound Device Status (Send on state change):
     * {
     *   "type": "relay_status",
     *   "device_id": "living_room_ac",
     *   "state": 1,
     *   "kw": 1.5
     * }
     */
    public static class RelayStatusMessage {
        @SerializedName("device_id") public String deviceId;
        public int state;
        public double kw;
    }

    /*
     * [ESP32 DEV GUIDE]
     * Inbound Alerts (Send strictly on faults):
     * {
     *   "type": "alert",
     *   "title": "Voltage Fluctuation",
     *   "body": "Zone 2 Protected."
     * }
     */
    public static class AlertMessage {
        public String title;
        public String body;
    }

    private static class RelayCommand {
        String action;
        @SerializedName("device_id") String deviceId;
        @SerializedName("target_state") int targetState;

        RelayCommand(String action, String deviceId, int targetState) {
            this.action = action;
            this.deviceId = deviceId;
            this.targetState = targetState;
        }
    }
}
