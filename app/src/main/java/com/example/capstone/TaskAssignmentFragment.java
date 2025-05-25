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

    // Komponen untuk mengakses database tugas
    private TaskDao taskDao;
    // Daftar tugas yang akan ditampilkan
    private List<Task> taskList = new ArrayList<>();
    // Handler untuk update realtime durasi tugas
    private Handler handler = new Handler();
    // Runnable untuk menjalankan update berkala
    private Runnable updateRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inisialisasi database saat fragment dibuat
        taskDao = new TaskDao(requireContext());
        taskDao.open();  // Membuka koneksi database
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate layout fragment
        View view = inflater.inflate(R.layout.fragment_task_assignment, container, false);
        initUI(view);  // Setup tombol dan UI
        loadTasks();   // Memuat tugas dari database
        return view;
    }

    // Inisialisasi elemen UI dan event listener
    private void initUI(View view) {
        // Tombol untuk membuka dialog tambah tugas
        view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());
        // Tombol kembali ke halaman sebelumnya
        view.findViewById(R.id.backButton).setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    // Memuat tugas dari database dan menampilkannya
    private void loadTasks() {
        View view = getView();
        if (view == null) return;

        taskList.clear();
        taskList.addAll(taskDao.getAllTasks());  // Ambil semua tugas dari database

        LinearLayout container = view.findViewById(R.id.tasksContainer);
        container.removeAllViews();  // Hapus tampilan lama

        // Buat tampilan untuk setiap tugas
        for (Task task : taskList) {
            View taskView = createTaskView(task, container);
            container.addView(taskView);
        }
    }

    // Membuat view untuk menampilkan detail tugas
    private View createTaskView(Task task, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View taskView = inflater.inflate(R.layout.item_task, parent, false);

        // Hubungkan komponen UI dengan data tugas
        CheckBox cbTask = taskView.findViewById(R.id.cbTask);
        TextView tvStartDate = taskView.findViewById(R.id.tvStartDate);
        TextView tvDueDate = taskView.findViewById(R.id.tvDueDate);
        TextView tvDuration = taskView.findViewById(R.id.tvDuration);

        // Set data ke CheckBox dan tambahkan listener untuk update status
        if (cbTask != null) {
            cbTask.setText(task.getTitle());
            cbTask.setChecked(task.isCompleted());
            cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                taskDao.updateTask(task);  // Update status di database
            });
        }

        // Format tanggal dan tampilkan
        if (tvStartDate != null) tvStartDate.setText(formatDateTime(task.getStartDate()));
        if (tvDueDate != null) tvDueDate.setText(formatDateTime(task.getDueDate()));
        if (tvDuration != null) tvDuration.setText(calculateRemainingTime(task.getDueDate()));

        return taskView;
    }

    // Mengubah format tanggal untuk tampilan
    private String formatDateTime(String dateTime) {
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            Date date = originalFormat.parse(dateTime);
            return displayFormat.format(date);
        } catch (ParseException e) {
            return dateTime;  // Kembalikan format asli jika parsing gagal
        }
    }

    // Menampilkan dialog untuk menambah tugas baru
    private void showAddTaskDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_task);

        // Setup tampilan dialog
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
        }

        // Inisialisasi komponen UI dalam dialog
        TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
        TextInputEditText etStartDate = dialog.findViewById(R.id.etStartDate);
        TextInputEditText etDueDate = dialog.findViewById(R.id.etDueDate);
        TextView tvDuration = dialog.findViewById(R.id.tvDuration);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        // Set waktu mulai ke waktu sekarang
        String currentDateTime = getCurrentDateTime();
        etStartDate.setText(currentDateTime);
        etStartDate.setEnabled(false);  // Nonaktifkan edit waktu mulai

        // Tampilkan date-time picker saat field due date diklik
        etDueDate.setOnClickListener(v -> showDateTimePicker(etDueDate, tvDuration, currentDateTime));

        // Event listener untuk tombol simpan
        btnSave.setOnClickListener(v -> {
            // Validasi input
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

            // Simpan tugas baru ke database
            Task newTask = new Task(title, currentDateTime, dueDateTime);
            taskDao.insertTask(newTask);
            loadTasks();  // Refresh daftar tugas
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Menampilkan date dan time picker untuk memilih deadline
    private void showDateTimePicker(TextInputEditText dueDateField, TextView durationField, String startDateTime) {
        final Calendar calendar = Calendar.getInstance();

        // Date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Time picker dialog setelah tanggal dipilih
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                // Format tanggal yang dipilih
                                String selectedDateTime = String.format(Locale.getDefault(),
                                        "%02d/%02d/%02d %02d:%02d",
                                        month + 1, dayOfMonth, year % 100, hourOfDay, minute);

                                dueDateField.setText(selectedDateTime);
                                // Hitung dan tampilkan durasi
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

        // Set batas minimal tanggal ke waktu mulai
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

    // Menghitung durasi antara waktu mulai dan deadline
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

    // Menghitung sisa waktu sampai deadline
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

    // Validasi bahwa deadline harus setelah waktu mulai
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

    // Mendapatkan waktu sekarang dalam format tertentu
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Lifecycle method: Memulai update realtime saat fragment aktif
    @Override
    public void onResume() {
        super.onResume();
        loadTasks();
        startRealtimeUpdates();  // Mulai update durasi setiap menit
    }

    // Lifecycle method: Menghentikan update saat fragment tidak aktif
    @Override
    public void onPause() {
        super.onPause();
        stopRealtimeUpdates();  // Hentikan update
    }

    // Memulai pembaruan durasi secara realtime
    private void startRealtimeUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDurations();
                handler.postDelayed(this, 60000);  // Update setiap 60 detik
            }
        };
        handler.post(updateRunnable);
    }

    // Menghentikan pembaruan realtime
    private void stopRealtimeUpdates() {
        handler.removeCallbacks(updateRunnable);
    }

    // Memperbarui tampilan durasi untuk semua tugas
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

    // Lifecycle method: Membersihkan sumber daya saat fragment dihancurkan
    @Override
    public void onDestroy() {
        super.onDestroy();
        taskDao.close();  // Tutup koneksi database
    }
}