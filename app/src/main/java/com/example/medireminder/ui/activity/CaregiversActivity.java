package com.example.medireminder.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Caregiver;
import com.example.medireminder.data.repository.CaregiverRepository;
import com.example.medireminder.ui.adapter.CaregiverAdapter;

import java.util.List;

/** Add, edit and delete the people who receive the missed-dose SMS. */
public class CaregiversActivity extends AppCompatActivity implements CaregiverAdapter.OnCaregiverClickListener {

    private static final int REQUEST_PICK_CONTACT = 200;

    private CaregiverRepository repository;
    private CaregiverAdapter adapter;
    private TextView emptyText;
    private EditText dialogPhoneInput; // only while the dialog is open

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caregivers);
        setTitle(R.string.caregivers_title);

        repository = new CaregiverRepository(this);
        emptyText = findViewById(R.id.noCaregiversText);

        RecyclerView recycler = findViewById(R.id.caregiversRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CaregiverAdapter(this);
        recycler.setAdapter(adapter);

        repository.getAllCaregivers().observe(this, (List<Caregiver> caregivers) -> {
            adapter.submitList(caregivers);
            emptyText.setVisibility(caregivers == null || caregivers.isEmpty()
                    ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.addCaregiverFab).setOnClickListener(v -> showCaregiverDialog(null));
    }

    private void showCaregiverDialog(final Caregiver caregiver) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_caregiver, null);
        EditText nameInput = view.findViewById(R.id.caregiverNameInput);
        dialogPhoneInput = view.findViewById(R.id.caregiverPhoneInput);

        if (caregiver != null) {
            nameInput.setText(caregiver.name);
            dialogPhoneInput.setText(caregiver.phone);
        }

        view.findViewById(R.id.pickContactButton).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_CONTACT);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(caregiver == null ? R.string.add_caregiver : R.string.edit_caregiver)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String phone = dialogPhoneInput.getText().toString().trim();
                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, R.string.error_caregiver_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Caregiver result = caregiver == null ? new Caregiver() : caregiver;
                    result.name = name;
                    result.phone = phone;
                    if (caregiver == null) {
                        repository.insert(result);
                    } else {
                        repository.update(result);
                    }
                });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_CONTACT || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        Cursor cursor = getContentResolver().query(uri,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String phone = cursor.getString(0);
            if (dialogPhoneInput != null && phone != null) {
                dialogPhoneInput.setText(phone);
            }
            cursor.close();
        }
    }

    @Override
    public void onEdit(Caregiver caregiver) {
        showCaregiverDialog(caregiver);
    }

    @Override
    public void onDelete(final Caregiver caregiver) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_caregiver_title)
                .setMessage(R.string.delete_caregiver_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> repository.delete(caregiver))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
