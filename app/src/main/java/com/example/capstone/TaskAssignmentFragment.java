package com.example.capstone;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
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

        if (tvStartDate != null) tvStartDate.setText(task.getStartDate());
        if (tvDueDate != null) tvDueDate.setText(task.getDueDate());
        if (tvDuration != null) tvDuration.setText(String.format(Locale.getDefault(), "%d days", task.getDuration()));

        return taskView;
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

        // Set tanggal mulai otomatis ke hari ini
        String currentDate = getCurrentDate();
        etStartDate.setText(currentDate);
        etStartDate.setEnabled(false);
        etStartDate.setFocusable(false);

        // Setup date picker hanya untuk due date
        etDueDate.setOnClickListener(v -> showDueDatePicker(etDueDate, currentDate, tvDuration));

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String dueDate = etDueDate.getText() != null ? etDueDate.getText().toString().trim() : "";

            if (title.isEmpty() || dueDate.isEmpty()) {
                Toast.makeText(requireContext(), "Harap isi semua bidang", Toast.LENGTH_SHORT).show();
                return;
            }

            int duration = calculateDuration(currentDate, dueDate);
            if (duration <= 0) {
                Toast.makeText(requireContext(), "Tanggal deadline tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }

            Task newTask = new Task(title, currentDate, dueDate, duration);
            taskDao.insertTask(newTask);
            loadTasks();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDueDatePicker(TextInputEditText dueDateField, String startDate, TextView durationField) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%02d",
                            month + 1, dayOfMonth, year % 100);
                    dueDateField.setText(selectedDate);

                    int duration = calculateDuration(startDate, selectedDate);
                    durationField.setText(String.format(Locale.getDefault(), "Durasi: %d hari", duration));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
            Date minDate = sdf.parse(startDate);
            if (minDate != null) {
                datePicker.getDatePicker().setMinDate(minDate.getTime());
            }
        } catch (ParseException e) {
            Log.e("DatePicker", "Error parsing date", e);
        }

        datePicker.show();
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private int calculateDuration(String startDateStr, String dueDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
            Date startDate = sdf.parse(startDateStr);
            Date dueDate = sdf.parse(dueDateStr);

            if (startDate != null && dueDate != null) {
                long diff = dueDate.getTime() - startDate.getTime();
                return (int) (diff / (1000 * 60 * 60 * 24)) + 1;
            }
        } catch (ParseException e) {
            Log.e("TaskFragment", "Error parsing date", e);
        }
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskDao.close();
    }
}