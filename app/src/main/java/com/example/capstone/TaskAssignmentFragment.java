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

    // Data Access Object untuk task
    private TaskDao taskDao;

    // Menyimpan daftar tugas yang diambil dari database
    private List<Task> taskList = new ArrayList<>();

    // Membuka koneksi ke database saat fragment dibuat
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        taskDao = new TaskDao(requireContext());
        taskDao.open();
    }

    // Menampilkan tampilan UI fragment dan inisialisasi
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_assignment, container, false);
        initUI(view);     // Inisialisasi tombol
        loadTasks();      // Memuat tugas dari database
        return view;
    }

    // Inisialisasi elemen UI dan event handler
    private void initUI(View view) {
        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());
        view.findViewById(R.id.backButton).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    // Memuat semua tugas dari database dan menampilkannya di layout
    private void loadTasks() {
        View view = getView();
        if (view == null) return;

        taskList.clear();  // Kosongkan list
        taskList.addAll(taskDao.getAllTasks()); // Ambil dari database

        LinearLayout container = view.findViewById(R.id.tasksContainer);
        container.removeAllViews(); // Bersihkan container

        // Tambahkan setiap task sebagai view
        for (Task task : taskList) {
            View taskView = createTaskView(task, container);
            container.addView(taskView);
        }
    }

    // Saat fragment kembali aktif, muat ulang data
    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
    }

    // Membuat view untuk satu item task
    private View createTaskView(Task task, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View taskView = inflater.inflate(R.layout.item_task, parent, false);

        CheckBox cbTask = taskView.findViewById(R.id.cbTask);
        TextView tvStartDate = taskView.findViewById(R.id.tvStartDate);
        TextView tvDueDate = taskView.findViewById(R.id.tvDueDate);
        TextView tvDuration = taskView.findViewById(R.id.tvDuration);

        // Set nilai dan listener checkbox
        if (cbTask != null) {
            cbTask.setText(task.getTitle());
            cbTask.setChecked(task.isCompleted());
            cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                taskDao.updateTask(task);  // Update status di DB
            });
        }

        if (tvStartDate != null) tvStartDate.setText(task.getStartDate());
        if (tvDueDate != null) tvDueDate.setText(task.getDueDate());
        if (tvDuration != null) tvDuration.setText(String.format(Locale.getDefault(), "%d days", task.getDuration()));

        return taskView;
    }

    // Menampilkan dialog untuk menambahkan tugas baru
    private void showAddTaskDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_task);

        // Set ukuran dan posisi dialog
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
        }

        // Ambil referensi ke inputan dalam dialog
        TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
        TextInputEditText etStartDate = dialog.findViewById(R.id.etStartDate);
        TextInputEditText etDueDate = dialog.findViewById(R.id.etDueDate);
        TextView tvDuration = dialog.findViewById(R.id.tvDuration);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Setup date picker untuk kedua tanggal
        setupDatePicker(etStartDate, etDueDate, tvDuration);
        setupDatePicker(etDueDate, etStartDate, tvDuration);

        // Tombol simpan
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
            String startDate = etStartDate.getText() != null ? etStartDate.getText().toString().trim() : "";
            String dueDate = etDueDate.getText() != null ? etDueDate.getText().toString().trim() : "";

            // Validasi input
            if (title.isEmpty() || startDate.isEmpty() || dueDate.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hitung durasi dan simpan ke database
            int duration = calculateDuration(startDate, dueDate);
            Task newTask = new Task(title, startDate, dueDate, duration);
            taskDao.insertTask(newTask);
            loadTasks(); // Refresh list
            dialog.dismiss();
        });

        // Tombol batal
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Setup date picker untuk field tanggal
    private void setupDatePicker(TextInputEditText dateField, TextInputEditText otherDateField, TextView durationField) {
        dateField.setOnClickListener(v -> showDatePicker(dateField, otherDateField, durationField));
    }

    // Tampilkan date picker dialog
    private void showDatePicker(TextInputEditText targetField, TextInputEditText otherField, TextView durationField) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%02d",
                            month + 1, dayOfMonth, year % 100);
                    targetField.setText(selectedDate);

                    // Jika tanggal lain sudah dipilih, hitung durasi
                    if (otherField.getText() != null && !otherField.getText().toString().isEmpty()) {
                        int duration = calculateDuration(
                                targetField.getId() == R.id.etStartDate ? selectedDate : otherField.getText().toString(),
                                targetField.getId() == R.id.etDueDate ? selectedDate : otherField.getText().toString()
                        );
                        durationField.setText(String.format(Locale.getDefault(), "Duration: %d days", duration));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    // Menghitung selisih hari antara dua tanggal
    private int calculateDuration(String startDateStr, String dueDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
            Date startDate = sdf.parse(startDateStr);
            Date dueDate = sdf.parse(dueDateStr);

            if (startDate != null && dueDate != null) {
                long diff = dueDate.getTime() - startDate.getTime();
                return (int) (diff / (1000 * 60 * 60 * 24)) + 1; // Tambah 1 agar inklusif
            }
        } catch (ParseException e) {
            Log.e("TaskFragment", "Error parsing date", e);
        }
        return 0;
    }

    // Tutup koneksi ke database saat fragment dihancurkan
    @Override
    public void onDestroy() {
        super.onDestroy();
        taskDao.close();
    }
}
