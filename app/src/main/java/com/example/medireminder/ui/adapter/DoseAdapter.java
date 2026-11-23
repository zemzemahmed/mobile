package com.example.medireminder.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.DoseLog;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.util.DoseActionHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Today's doses. Each row has its own Taken button. */
public class DoseAdapter extends RecyclerView.Adapter<DoseAdapter.DoseViewHolder> {

    private List<DoseItem> items = new ArrayList<>();

    /** One row of the Home screen. */
    public static class DoseItem {
        public final Medicine medicine;
        public final Schedule schedule;
        public final long scheduledTime;
        public final String status;
        public boolean isNext;

        public DoseItem(Medicine medicine, Schedule schedule, long scheduledTime, String status) {
            this.medicine = medicine;
            this.schedule = schedule;
            this.scheduledTime = scheduledTime;
            this.status = status;
        }
    }

    public void submitList(List<DoseItem> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DoseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dose, parent, false);
        return new DoseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoseViewHolder holder, int position) {
        DoseItem item = items.get(position);
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(item.scheduledTime));
        String name = item.medicine.name;
        if (item.medicine.strength != null && !item.medicine.strength.isEmpty()) {
            name += " (" + item.medicine.strength + ")";
        }
        holder.name.setText(name);
        holder.details.setText(holder.itemView.getContext().getString(
                R.string.dose_summary, time, formatNumber(item.schedule.pillsPerDose)));

        String statusText = statusLabel(holder, item.status);
        if (item.isNext) {
            statusText += " · " + holder.itemView.getContext().getString(R.string.next_marker);
        }
        holder.status.setText(statusText);
        holder.status.setTextColor(statusColor(holder, item.status));

        boolean alreadyTaken = DoseLog.STATUS_TAKEN.equals(item.status);
        holder.takenButton.setVisibility(alreadyTaken ? View.GONE : View.VISIBLE);
        holder.takenButton.setOnClickListener(v -> DoseActionHelper.takeDose(
                v.getContext(), new long[]{item.schedule.id}, item.scheduledTime));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String statusLabel(DoseViewHolder holder, String status) {
        int resId;
        if (DoseLog.STATUS_TAKEN.equals(status)) {
            resId = R.string.status_taken;
        } else if (DoseLog.STATUS_MISSED.equals(status)) {
            resId = R.string.status_missed;
        } else if (DoseLog.STATUS_SNOOZED.equals(status)) {
            resId = R.string.status_snoozed;
        } else {
            resId = R.string.status_upcoming;
        }
        return holder.itemView.getContext().getString(resId);
    }

    private int statusColor(DoseViewHolder holder, String status) {
        int resId;
        if (DoseLog.STATUS_TAKEN.equals(status)) {
            resId = R.color.status_taken;
        } else if (DoseLog.STATUS_MISSED.equals(status)) {
            resId = R.color.status_missed;
        } else if (DoseLog.STATUS_SNOOZED.equals(status)) {
            resId = R.color.status_snoozed;
        } else {
            resId = R.color.status_upcoming;
        }
        return ContextCompat.getColor(holder.itemView.getContext(), resId);
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    static class DoseViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView details;
        final TextView status;
        final Button takenButton;

        DoseViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.doseName);
            details = itemView.findViewById(R.id.doseDetails);
            status = itemView.findViewById(R.id.doseStatus);
            takenButton = itemView.findViewById(R.id.doseTakenButton);
        }
    }
}
