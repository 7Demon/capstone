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
import android.widget.ImageButton;
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
    private static final String TAG = "TaskAssignmentFragment";
    private static final SimpleDateFormat TASK_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    private TaskDao taskDao;
    private List<Task> taskList = new ArrayList<>();
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            taskDao = new TaskDao(requireContext());
            taskDao.open();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TaskDao: " + e.getMessage());
        }
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
        try {
            view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());
            view.findViewById(R.id.backButton).setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI: " + e.getMessage());
        }
    }

    private void loadTasks() {
        try {
            View view = getView();
            if (view == null) return;

            taskList.clear();
            taskList.addAll(taskDao.getAllTasks());

            LinearLayout container = view.findViewById(R.id.tasksContainer);
            container.removeAllViews();

            for (Task task : taskList) {
                View taskView = createTaskView(task, container);
                if (taskView != null) {
                    container.addView(taskView);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tasks: " + e.getMessage());
        }
    }

    private View createTaskView(Task task, ViewGroup parent) {
        try {
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            View taskView = inflater.inflate(R.layout.item_task, parent, false);

            CheckBox cbTask = taskView.findViewById(R.id.cbTask);
            TextView tvStartDate = taskView.findViewById(R.id.tvStartDate);
            TextView tvDueDate = taskView.findViewById(R.id.tvDueDate);
            TextView tvDuration = taskView.findViewById(R.id.tvDuration);
            ImageButton btnEdit = taskView.findViewById(R.id.btnEdit);
            ImageButton btnDelete = taskView.findViewById(R.id.btnDelete);

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

            btnEdit.setOnClickListener(v -> showEditTaskDialog(task));
            btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(task));

            return taskView;
        } catch (Exception e) {
            Log.e(TAG, "Error creating task view: " + e.getMessage());
            return null;
        }
    }

    private void showDeleteConfirmationDialog(Task task) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus Tugas")
                .setMessage("Apakah Anda yakin ingin menghapus tugas ini?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    taskDao.deleteTask(task.getId());
                    loadTasks();
                    Toast.makeText(requireContext(), "Tugas dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showEditTaskDialog(Task task) {
        try {
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.setContentView(R.layout.dialog_add_task);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.CENTER);
            }

            TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
            TextInputEditText etStartDate = dialog.findViewById(R.id.etStartDate);
            TextInputEditText etDueDate = dialog.findViewById(R.id.etDueDate);
            TextView tvDuration = dialog.findViewById(R.id.tvDuration);
            Button btnSave = dialog.findViewById(R.id.btnSave);
            Button btnCancel = dialog.findViewById(R.id.btnCancel);

            // Populate fields with existing task data
            etTitle.setText(task.getTitle());
            etStartDate.setText(task.getStartDate());
            etStartDate.setEnabled(false);
            etDueDate.setText(task.getDueDate());
            tvDuration.setText(calculateDurationText(task.getStartDate(), task.getDueDate()));

            etDueDate.setOnClickListener(v ->
                    showDateTimePicker(etDueDate, tvDuration, task.getStartDate()));

            btnSave.setOnClickListener(v -> {
                try {
                    String title = etTitle.getText() != null ?
                            etTitle.getText().toString().trim() : "";
                    String dueDateTime = etDueDate.getText() != null ?
                            etDueDate.getText().toString().trim() : "";

                    if (title.isEmpty() || dueDateTime.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Harap isi semua bidang", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isValidDueDate(task.getStartDate(), dueDateTime)) {
                        Toast.makeText(requireContext(),
                                "Waktu deadline harus setelah waktu mulai",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update task properties
                    task.setTitle(title);
                    task.setDueDate(dueDateTime);

                    int result = taskDao.updateTask(task);
                    if (result > 0) {
                        loadTasks();
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Tugas diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "Gagal memperbarui tugas", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating task: " + e.getMessage());
                    Toast.makeText(requireContext(),
                            "Terjadi kesalahan saat memperbarui", Toast.LENGTH_SHORT).show();
                }
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing edit task dialog: " + e.getMessage());
        }
    }

    private void showAddTaskDialog() {
        try {
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.setContentView(R.layout.dialog_add_task);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.WRAP_CONTENT);
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

            etDueDate.setOnClickListener(v ->
                    showDateTimePicker(etDueDate, tvDuration, currentDateTime));

            btnSave.setOnClickListener(v -> {
                try {
                    String title = etTitle.getText() != null ?
                            etTitle.getText().toString().trim() : "";
                    String dueDateTime = etDueDate.getText() != null ?
                            etDueDate.getText().toString().trim() : "";

                    if (title.isEmpty() || dueDateTime.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Harap isi semua bidang", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!isValidDueDate(currentDateTime, dueDateTime)) {
                        Toast.makeText(requireContext(),
                                "Waktu deadline harus setelah waktu mulai",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Task newTask = new Task(title, currentDateTime, dueDateTime);
                    long result = taskDao.insertTask(newTask);
                    if (result != -1) {
                        loadTasks();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(),
                                "Gagal menyimpan tugas", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving task: " + e.getMessage());
                    Toast.makeText(requireContext(),
                            "Terjadi kesalahan saat menyimpan", Toast.LENGTH_SHORT).show();
                }
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing add task dialog: " + e.getMessage());
        }
    }

    private void showDateTimePicker(TextInputEditText dueDateField,
                                    TextView durationField, String startDateTime) {
        try {
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
                Date minDate = TASK_DATE_FORMAT.parse(startDateTime);
                if (minDate != null) {
                    datePickerDialog.getDatePicker().setMinDate(minDate.getTime());
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing start date: " + e.getMessage());
            }

            datePickerDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing date time picker: " + e.getMessage());
        }
    }

    private String calculateDurationText(String startDateTime, String dueDateTime) {
        try {
            Date startDate = TASK_DATE_FORMAT.parse(startDateTime);
            Date dueDate = TASK_DATE_FORMAT.parse(dueDateTime);

            if (startDate != null && dueDate != null) {
                long diff = dueDate.getTime() - startDate.getTime();
                long seconds = diff / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                return String.format(Locale.getDefault(),
                        "Durasi: %d hari %d jam %d menit",
                        days, hours % 24, minutes % 60);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating duration: " + e.getMessage());
        }
        return "Durasi: -";
    }

    private String calculateRemainingTime(String dueDateTime) {
        if (dueDateTime == null || dueDateTime.isEmpty()) {
            return "-";
        }

        try {
            Date dueDate = TASK_DATE_FORMAT.parse(dueDateTime);
            Date now = new Date();

            if (dueDate != null) {
                long diffMillis = dueDate.getTime() - now.getTime();

                if (diffMillis <= 0) {
                    return "⏰ Waktu Habis";
                }

                long seconds = diffMillis / 1000;
                long minutes = seconds / 60;
                long hours = minutes / 60;
                long days = hours / 24;

                return String.format(Locale.getDefault(),
                        "Sisa: %d hari %d jam %d menit",
                        days, hours % 24, minutes % 60);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating remaining time: " + e.getMessage());
        }
        return "-";
    }

    private boolean isValidDueDate(String startDateTime, String dueDateTime) {
        try {
            Date startDate = TASK_DATE_FORMAT.parse(startDateTime);
            Date dueDate = TASK_DATE_FORMAT.parse(dueDateTime);
            return dueDate != null && startDate != null && dueDate.after(startDate);
        } catch (ParseException e) {
            Log.e(TAG, "Error validating due date: " + e.getMessage());
            return false;
        }
    }

    private String getCurrentDateTime() {
        return TASK_DATE_FORMAT.format(new Date());
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) {
            return "-";
        }

        try {
            Date date = TASK_DATE_FORMAT.parse(dateTime);
            return DISPLAY_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting date: " + dateTime, e);
            return dateTime;
        }
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
        try {
            View view = getView();
            if (view == null) return;

            LinearLayout container = view.findViewById(R.id.tasksContainer);
            for (int i = 0; i < container.getChildCount(); i++) {
                View taskView = container.getChildAt(i);
                TextView tvDuration = taskView.findViewById(R.id.tvDuration);
                Task task = taskList.get(i);
                tvDuration.setText(calculateRemainingTime(task.getDueDate()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating durations: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (taskDao != null) {
            taskDao.close();
        }
    }
}