package com.example.myapplication;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<Alert> alerts;
    private OnAlertLongClickListener longClickListener;

    public interface OnAlertLongClickListener {
        void onAlertLongClick(Alert alert, int position);
    }

    public AlertAdapter(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public void setOnAlertLongClickListener(OnAlertLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alerts.get(position);
        holder.title.setText(alert.getTitle());
        holder.message.setText(alert.getMessage());
        
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                alert.getTimestamp(), 
                System.currentTimeMillis(), 
                DateUtils.MINUTE_IN_MILLIS);
        holder.timestamp.setText(relativeTime);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onAlertLongClick(alert, holder.getBindingAdapterPosition());
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < alerts.size()) {
            alerts.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.alertTitle);
            message = itemView.findViewById(R.id.alertMessage);
            timestamp = itemView.findViewById(R.id.alertTimestamp);
        }
    }
}