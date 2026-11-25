package com.example.medireminder.ui.activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medireminder.R;
import com.example.medireminder.data.entity.Medicine;
import com.example.medireminder.data.entity.Schedule;
import com.example.medireminder.data.repository.MedicineRepository;
import com.example.medireminder.util.AlarmScheduler;
import com.example.medireminder.util.AppExecutor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Screen for adding a new medicine or editing an existing one. */
public class AddEditMedicineActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICINE_ID = "medicineId";

    private EditText nameInput;
    private EditText strengthInput;
    private EditText notesInput;
    private EditText startDateInput;
    private EditText endDateInput;
    private EditText stockInput;
    private EditText sameDoseValueInput;
    private Spinner formSpinner;
    private CheckBox activeCheckbox;
    private CheckBox sameDoseCheckbox;
    private LinearLayout alarmsContainer;

    private MedicineRepository repository;
    private long medicineId;
    private final List<AlarmRow> alarmRows = new ArrayList<>();
    private final List<Schedule> existingSchedules = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medicine);

        nameInput = findViewById(R.id.medicineNameInput);
        strengthInput = findViewById(R.id.medicineStrengthInput);
        notesInput = findViewById(R.id.medicineNotesInput);
        startDateInput = findViewById(R.id.startDateInput);
        endDateInput = findViewById(R.id.endDateInput);
        stockInput = findViewById(R.id.stockInput);
        sameDoseValueInput = findViewById(R.id.sameDoseValueInput);
        formSpinner = findViewById(R.id.medicineFormSpinner);
        activeCheckbox = findViewById(R.id.activeCheckbox);
        sameDoseCheckbox = findViewById(R.id.sameDoseCheckbox);
        alarmsContainer = findViewById(R.id.alarmsContainer);

        repository = new MedicineRepository(this);
        medicineId = getIntent().getLongExtra(EXTRA_MEDICINE_ID, 0);
        dateFormat.setLenient(false);

        // Dates are chosen with a calendar - no typing or keyboard format needed.
        startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
        endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));
        findViewById(R.id.clearEndDateButton).setOnClickListener(v -> endDateInput.setText(""));

        if (medicineId == 0) {
            setTitle(R.string.add_medicine_title);
            startDateInput.setText(dateFormat.format(Calendar.getInstance().getTime()));
            addAlarmRow(8, 0, 1.0, null);   // one default alarm at 08:00, every day
        } else {
            setTitle(R.string.edit_medicine_title);
            loadMedicineForEditing();
        }

        findViewById(R.id.addAlarmButton).setOnClickListener(v -> {
            addAlarmRow(8, 0, 1.0, null);
            applySameDose();
        });

        sameDoseCheckbox.setOnCheckedChangeListener((button, checked) -> applySameDose());

        findViewById(R.id.saveButton).setOnClickListener(v -> saveMedicine());
    }

    /** Loads an existing medicine on the background thread, then fills the form. */
    private void loadMedicineForEditing() {
        AppExecutor.database().execute(() -> {
            Medicine medicine = repository.getMedicineById(medicineId);
            List<Schedule> schedules = repository.getSchedulesForMedicineSync(medicineId);
            runOnUiThread(() -> fillForm(medicine, schedules));
        });
    }

    private void fillForm(Medicine medicine, List<Schedule> schedules) {
        if (medicine == null) {
            Toast.makeText(this, "Medicine not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        nameInput.setText(medicine.name);
        strengthInput.setText(medicine.strength == null ? "" : medicine.strength);
        notesInput.setText(medicine.notes == null ? "" : medicine.notes);
        startDateInput.setText(medicine.startDate == null ? "" : medicine.startDate);
        endDateInput.setText(medicine.endDate == null ? "" : medicine.endDate);
        stockInput.setText(formatNumber(medicine.pillsInStock));
        activeCheckbox.setChecked(medicine.active);

        String[] forms = getResources().getStringArray(R.array.form_options);
        for (int i = 0; i < forms.length; i++) {
            if (forms[i].equals(medicine.form)) {
                formSpinner.setSelection(i);
                break;
            }
        }

        existingSchedules.clear();
        if (schedules != null) {
            existingSchedules.addAll(schedules);
        }
        alarmRows.clear();
        alarmsContainer.removeAllViews();
        if (schedules == null || schedules.isEmpty()) {
            addAlarmRow(8, 0, 1.0, null);
        } else {
            for (Schedule schedule : schedules) {
                addAlarmRow(schedule.hour, schedule.minute, schedule.pillsPerDose, schedule.daysOfWeek);
            }
        }
        applySameDose();
    }

    /** Adds one editable alarm row to the screen. */
    private void addAlarmRow(int hour, int minute, double pills, String daysOfWeek) {
        AlarmRow row = new AlarmRow(hour, minute, pills, daysOfWeek);
        alarmRows.add(row);
        alarmsContainer.addView(row.view);
    }

    /** Applies the "same dose" shortcut to every alarm row. */
    private void applySameDose() {
        boolean useSame = sameDoseCheckbox.isChecked();
        double value = parseDouble(sameDoseValueInput.getText().toString(), 1.0);
        if (value < 0.5) {
            value = 0.5;
        }
        for (AlarmRow row : alarmRows) {
            row.pills = value;
            row.updatePillsText();
            row.minus.setEnabled(!useSame);
            row.plus.setEnabled(!useSame);
        }
    }

    private void saveMedicine() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.error_name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (alarmRows.isEmpty()) {
            Toast.makeText(this, R.string.error_at_least_one_alarm, Toast.LENGTH_SHORT).show();
            return;
        }

        String startDate = startDateInput.getText().toString().trim();
        String endDate = endDateInput.getText().toString().trim();
        long startMillis = parseDateOrError(startDate);
        long endMillis = parseDateOrError(endDate);
        if (startMillis == Long.MIN_VALUE || endMillis == Long.MIN_VALUE) {
            Toast.makeText(this, R.string.error_date_format, Toast.LENGTH_SHORT).show();
            return;
        }
        if (startMillis != Long.MIN_VALUE && endMillis != Long.MIN_VALUE && endMillis < startMillis) {
            Toast.makeText(this, R.string.error_end_before_start, Toast.LENGTH_SHORT).show();
            return;
        }

        double stock = parseDouble(stockInput.getText().toString(), 0.0);
        if (stock < 0) {
            Toast.makeText(this, R.string.error_stock, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] forms = getResources().getStringArray(R.array.form_options);
        String form = forms[formSpinner.getSelectedItemPosition()];

        Medicine medicine = new Medicine();
        medicine.id = medicineId;
        medicine.name = name;
        medicine.strength = strengthInput.getText().toString().trim();
        medicine.form = form;
        medicine.notes = notesInput.getText().toString().trim();
        medicine.startDate = startDate;
        medicine.endDate = endDate;
        medicine.pillsInStock = stock;
        medicine.active = activeCheckbox.isChecked();

        List<Schedule> schedules = new ArrayList<>();
        for (AlarmRow row : alarmRows) {
            double pills = sameDoseCheckbox.isChecked()
                    ? Math.max(0.5, parseDouble(sameDoseValueInput.getText().toString(), 1.0))
                    : row.pills;
            schedules.add(new Schedule(0, row.hour, row.minute, pills, row.daysToString()));
        }

        // Cancel the old alarms first, then let Room save and rebuild the new ones.
        AlarmScheduler.cancelAlarmsForSchedules(this, existingSchedules);
        repository.saveMedicineWithSchedules(medicine, schedules,
                savedId -> AlarmScheduler.scheduleAlarmsForMedicine(getApplicationContext(), savedId));

        Toast.makeText(this, R.string.saved_message, Toast.LENGTH_SHORT).show();
        finish();
    }

    /** Opens a calendar and writes the chosen day as YYYY-MM-DD. */
    private void showDatePicker(final EditText target) {
        Calendar calendar = Calendar.getInstance();
        String current = target.getText().toString().trim();
        if (!current.isEmpty()) {
            try {
                calendar.setTime(dateFormat.parse(current));
            } catch (ParseException ignored) {
                // Keep today's date if the stored value is not readable.
            }
        }

        new DatePickerDialog(this,
                (picker, year, month, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth);
                    target.setText(dateFormat.format(chosen.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private long parseDateOrError(String value) {
        if (value.isEmpty()) {
            return Long.MIN_VALUE; // no date entered: allowed
        }
        try {
            return dateFormat.parse(value).getTime();
        } catch (ParseException e) {
            return Long.MIN_VALUE;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private String formatNumber(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** One alarm row keeps its own state (time, pills, selected days). */
    private class AlarmRow {
        final View view;
        final Button timeButton;
        final Button minus;
        final Button plus;
        final TextView pillsText;
        final CheckBox[] days;
        int hour;
        int minute;
        double pills;

        AlarmRow(int hour, int minute, double pills, String daysOfWeek) {
            this.hour = hour;
            this.minute = minute;
            this.pills = pills;

            view = getLayoutInflater().inflate(R.layout.item_alarm, alarmsContainer, false);
            timeButton = view.findViewById(R.id.alarmTimeButton);
            minus = view.findViewById(R.id.alarmMinusButton);
            plus = view.findViewById(R.id.alarmPlusButton);
            pillsText = view.findViewById(R.id.alarmPillsText);
            days = new CheckBox[]{
                    view.findViewById(R.id.dayMon), view.findViewById(R.id.dayTue),
                    view.findViewById(R.id.dayWed), view.findViewById(R.id.dayThu),
                    view.findViewById(R.id.dayFri), view.findViewById(R.id.daySat),
                    view.findViewById(R.id.daySun)
            };

            if (daysOfWeek == null) {
                setAllDays(true);
            } else {
                applyDaysFromString(daysOfWeek);
            }

            updateTimeButton();
            updatePillsText();

            timeButton.setOnClickListener(v -> new TimePickerDialog(
                    AddEditMedicineActivity.this,
                    (picker, selectedHour, selectedMinute) -> {
                        this.hour = selectedHour;
                        this.minute = selectedMinute;
                        updateTimeButton();
                    },
                    hour, minute, true
            ).show());

            minus.setOnClickListener(v -> changePills(-0.5));
            plus.setOnClickListener(v -> changePills(0.5));
            view.findViewById(R.id.alarmRemoveButton).setOnClickListener(v -> {
                alarmsContainer.removeView(view);
                alarmRows.remove(this);
            });
        }

        void updateTimeButton() {
            timeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        }

        void updatePillsText() {
            pillsText.setText(formatNumber(pills));
        }

        void changePills(double delta) {
            double next = pills + delta;
            pills = Math.max(0.5, next);   // never below half a pill
            updatePillsText();
        }

        void setAllDays(boolean checked) {
            for (CheckBox box : days) {
                box.setChecked(checked);
            }
        }

        void applyDaysFromString(String value) {
            setAllDays(false);
            for (String part : value.split(",")) {
                try {
                    int day = Integer.parseInt(part.trim());
                    if (day >= 1 && day <= 7) {
                        days[day - 1].setChecked(true);
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore a badly stored value; the fallback below keeps the alarm usable.
                }
            }
            if (daysToString().isEmpty()) {
                setAllDays(true);
            }
        }

        /** 1 = Monday ... 7 = Sunday, as required by the Schedule entity. */
        String daysToString() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < days.length; i++) {
                if (days[i].isChecked()) {
                    if (result.length() > 0) {
                        result.append(',');
                    }
                    result.append(i + 1);
                }
            }
            return result.toString();
        }
    }
}
