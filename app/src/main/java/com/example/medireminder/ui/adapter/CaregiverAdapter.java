package com.example.medireminder.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Caregiver;

import java.util.ArrayList;
import java.util.List;

/** One row per caregiver, with an Edit and a Delete button. */
public class CaregiverAdapter extends RecyclerView.Adapter<CaregiverAdapter.CaregiverViewHolder> {

    private List<Caregiver> items = new ArrayList<>();
    private final OnCaregiverClickListener listener;

    public interface OnCaregiverClickListener {
        void onEdit(Caregiver caregiver);
        void onDelete(Caregiver caregiver);
    }

    public CaregiverAdapter(OnCaregiverClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Caregiver> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CaregiverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_caregiver, parent, false);
        return new CaregiverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CaregiverViewHolder holder, int position) {
        Caregiver caregiver = items.get(position);
        holder.name.setText(caregiver.name);
        holder.phone.setText(caregiver.phone);
        holder.itemView.setOnClickListener(v -> listener.onEdit(caregiver));
        holder.delete.setOnClickListener(v -> listener.onDelete(caregiver));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CaregiverViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView phone;
        final Button delete;

        CaregiverViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.caregiverName);
            phone = itemView.findViewById(R.id.caregiverPhone);
            delete = itemView.findViewById(R.id.caregiverDelete);
        }
    }
}
