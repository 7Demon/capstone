package com.example.capstone;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.capstone.db.TaskDao;
import com.example.capstone.models.Task;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAssignmentFragment extends Fragment {

    private TaskDao taskDao;
    private List<Task> taskList = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskDao = new TaskDao(requireContext());
        taskDao.open();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_assignment, container, false);
        initUI(view);
        loadTasks();
        return view;
    }

    private void initUI(View view) {
        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());
        view.findViewById(R.id.backButton).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void loadTasks() {
        View view = getView();
        if (view == null) return;

        taskList.clear();
        taskList.addAll(taskDao.getAllTasks());

        LinearLayout container = view.findViewById(R.id.tasksContainer);
        container.removeAllViews();

        for (Task task : taskList) {
            View taskView = createTaskView(task, container);
            container.addView(taskView);
        }
    }

    private View createTaskView(Task task, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View taskView = inflater.inflate(R.layout.item_task, parent, false);

        CheckBox cbTask = taskView.findViewById(R.id.cbTask);
        TextView tvStartDate = taskView.findViewById(R.id.tvStartDate);
        TextView tvDueDate = taskView.findViewById(R.id.tvDueDate);
        TextView tvDuration = taskView.findViewById(R.id.tvDuration);

        if (cbTask != null) {
            cbTask.setText(task.getTitle());
            cbTask.setChecked(task.isCompleted());
            cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                taskDao.updateTask(task);
            });
        }

        if (tvStartDate != null) tvStartDate.setText(formatDateTime(task.getStartDate()));
        if (tvDueDate != null) tvDueDate.setText(formatDateTime(task.getDueDate()));
        if (tvDuration != null) tvDuration.setText(calculateRemainingTime(task.getDueDate()));

        return taskView;
    }

    private String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            Date date = originalFormat.parse(dateTime);
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dateTime;
        }
    }

    private void showAddTaskDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_task);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
        }

        TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
        TextInputEditText etStartDate = dialog.findViewById(R.id.etStartDate);
        TextInputEditText etDueDate = dialog.findViewById(R.id.etDueDate);
        TextView tvDuration = dialog.findViewById(R.id.tvDuration);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        String currentDateTime = getCurrentDateTime();
        etStartDate.setText(currentDateTime);
        etStartDate.setEnabled(false);

        etDueDate.setOnClickListener(v -> showDateTimePicker(etDueDate, tvDuration, currentDateTime));

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String dueDateTime = etDueDate.getText() != null ? etDueDate.getText().toString().trim() : "";

            if (title.isEmpty() || dueDateTime.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua bidang", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidDueDate(currentDateTime, dueDateTime)) {
                Toast.makeText(requireContext(), "Waktu deadline harus setelah waktu mulai", Toast.LENGTH_SHORT).show();
                return;
            }

            // PERBAHAN PENTING DI SINI
            Task newTask = new Task(title, currentDateTime, dueDateTime);
            taskDao.insertTask(newTask);
            loadTasks();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDateTimePicker(TextInputEditText dueDateField, TextView durationField, String startDateTime) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                String selectedDateTime = String.format(Locale.getDefault(),
                                        "%02d/%02d/%02d %02d:%02d",
                                        month + 1, dayOfMonth, year % 100, hourOfDay, minute);

                                dueDateField.setText(selectedDateTime);
                                durationField.setText(calculateDurationText(startDateTime, selectedDateTime));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            Date minDate = sdf.parse(startDateTime);
            if (minDate != null) {
                datePickerDialog.getDatePicker().setMinDate(minDate.getTime());
            }
        } catch (ParseException e) {
            Log.e("DateTimePicker", "Error parsing start date", e);
        }

        datePickerDialog.show();
    }

    private String calculateDurationText(String startDateTime, String dueDateTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            Date startDate = sdf.parse(startDateTime);
            Date dueDate = sdf.parse(dueDateTime);

            if (startDate != null && dueDate != null) {
                long diff = dueDate.getTime() - startDate.getTime();
                long minutes = (diff / (1000 * 60)) % 60;
                long hours = (diff / (1000 * 60 * 60)) % 24;
                long days = diff / (1000 * 60 * 60 * 24);

                return String.format(Locale.getDefault(),
                        "Durasi: %d hari %d jam %d menit",
                        days, hours, minutes);
            }
        } catch (ParseException e) {
            Log.e("Duration", "Error parsing date", e);
        }
        return "Durasi: -";
    }

    private String calculateRemainingTime(String dueDateTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            Date dueDate = sdf.parse(dueDateTime);
            Date now = new Date();

            if (dueDate != null) {
                long diffMillis = dueDate.getTime() - now.getTime();
                if (diffMillis > 0) {
                    long minutes = (diffMillis / (1000 * 60)) % 60;
                    long hours = (diffMillis / (1000 * 60 * 60)) % 24;
                    long days = diffMillis / (1000 * 60 * 60 * 24);

                    return String.format(Locale.getDefault(),
                            "Sisa: %d hari %d jam %d menit",
                            days, hours, minutes);
                } else {
                    return "⏰ Waktu Habis";
                }
            }
        } catch (ParseException e) {
            Log.e("RemainingTime", "Error parsing date", e);
        }
        return "-";
    }

    private boolean isValidDueDate(String startDateTime, String dueDateTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            Date startDate = sdf.parse(startDateTime);
            Date dueDate = sdf.parse(dueDateTime);
            return dueDate != null && startDate != null && dueDate.after(startDate);
        } catch (ParseException e) {
            return false;
        }
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
        startRealtimeUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRealtimeUpdates();
    }

    private void startRealtimeUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDurations();
                handler.postDelayed(this, 60000);
            }
        };
        handler.post(updateRunnable);
    }

    private void stopRealtimeUpdates() {
        handler.removeCallbacks(updateRunnable);
    }

    private void updateDurations() {
        View view = getView();
        if (view == null) return;

        LinearLayout container = view.findViewById(R.id.tasksContainer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View taskView = container.getChildAt(i);
            TextView tvDuration = taskView.findViewById(R.id.tvDuration);
            Task task = taskList.get(i);
            tvDuration.setText(calculateRemainingTime(task.getDueDate()));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskDao.close();
    }
}