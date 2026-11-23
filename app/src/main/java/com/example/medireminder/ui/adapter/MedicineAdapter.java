package com.example.medireminder.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.MedicineWithSchedules;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws one card per medicine in the RecyclerView.
 */
public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private List<MedicineWithSchedules> items = new ArrayList<>();
    private final OnMedicineClickListener listener;

    public MedicineAdapter(OnMedicineClickListener listener) {
        this.listener = listener;
    }

    /** Called by the Fragment whenever Room gives us a new list. */
    public void submitList(List<MedicineWithSchedules> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        MedicineWithSchedules item = items.get(position);
        Medicine medicine = item.medicine;
        int alarmCount = item.schedules == null ? 0 : item.schedules.size();

        String name = medicine.name;
        if (medicine.strength != null && !medicine.strength.isEmpty()) {
            name = name + " (" + medicine.strength + ")";
        }
        holder.nameText.setText(name);

        String summary = itemViewString(holder, R.string.medicine_summary,
                alarmCount, formatNumber(medicine.pillsInStock));
        holder.detailsText.setText(summary);

        int statusLabel = medicine.active ? R.string.status_active : R.string.status_paused;
        holder.statusText.setText(itemViewString(holder, statusLabel));

        holder.pauseButton.setText(medicine.active ? R.string.pause : R.string.resume);
        holder.pauseButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onPauseClick(items.get(adapterPosition));
            }
        });
        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onDeleteClick(items.get(adapterPosition));
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                listener.onMedicineClick(items.get(adapterPosition).medicine.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Small helper so we keep the text in strings.xml. */
    private String itemViewString(MedicineViewHolder holder, int resId, Object... args) {
        return holder.itemView.getContext().getString(resId, args);
    }

    /** Shows "2" instead of "2.0" when the number is a whole number. */
    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** The Fragment decides what happens when a card is tapped. */
    public interface OnMedicineClickListener {
        void onMedicineClick(long medicineId);
        void onPauseClick(MedicineWithSchedules item);
        void onDeleteClick(MedicineWithSchedules item);
    }

    static class MedicineViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView detailsText;
        final TextView statusText;
        final Button pauseButton;
        final Button deleteButton;

        MedicineViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.medicineName);
            detailsText = itemView.findViewById(R.id.medicineDetails);
            statusText = itemView.findViewById(R.id.medicineStatus);
            pauseButton = itemView.findViewById(R.id.medicinePauseButton);
            deleteButton = itemView.findViewById(R.id.medicineDeleteButton);
        }
    }
}
