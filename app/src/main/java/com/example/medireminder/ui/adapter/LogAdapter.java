package com.example.medireminder.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.DoseLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** The history list: one line per logged dose. */
public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    public static class LogItem {
        public final DoseLog log;
        public final String medicineName;

        public LogItem(DoseLog log, String medicineName) {
            this.log = log;
            this.medicineName = medicineName;
        }
    }

    private List<LogItem> items = new ArrayList<>();

    public void submitList(List<LogItem> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogItem item = items.get(position);
        String time = new SimpleDateFormat("EEE dd MMM, HH:mm", Locale.getDefault())
                .format(new Date(item.log.scheduledTime));
        holder.info.setText(item.medicineName + "\n" + time);

        int label;
        int color;
        if (DoseLog.STATUS_TAKEN.equals(item.log.status)) {
            label = R.string.status_taken;
            color = R.color.status_taken;
        } else if (DoseLog.STATUS_MISSED.equals(item.log.status)) {
            label = R.string.status_missed;
            color = R.color.status_missed;
        } else {
            label = R.string.status_snoozed;
            color = R.color.status_snoozed;
        }
        holder.status.setText(label == 0 ? "" : holder.itemView.getContext().getString(label));
        holder.status.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), color));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        final TextView info;
        final TextView status;

        LogViewHolder(View itemView) {
            super(itemView);
            info = itemView.findViewById(R.id.logInfo);
            status = itemView.findViewById(R.id.logStatus);
        }
    }
}
