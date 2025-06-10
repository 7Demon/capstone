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
    // Tag untuk Logcat
    private static final String TAG = "TaskAssignmentFragment";
    // Format tanggal untuk disimpan di database (cth: 06/10/25 20:55).
    private static final SimpleDateFormat TASK_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
    // Format tanggal untuk ditampilkan ke pengguna (cth: 10 Jun 2025, 20:55).
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());


    private TaskDao taskDao;

    private List<Task> taskList = new ArrayList<>();
    private Handler handler = new Handler();
    // Runnable yang berisi kode untuk memperbarui sisa waktu.
    private Runnable updateRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Persiapan dan buka koneksi database.
        try {
            taskDao = new TaskDao(requireContext());
            taskDao.open();
        } catch (Exception e) {
            Log.e(TAG, "Error saat inisialisasi TaskDao: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Menghubungkan layout XML (fragment_task_assignment.xml) ke kode ini.
        View view = inflater.inflate(R.layout.fragment_task_assignment, container, false);
        initUI(view); // Inisialisasi komponen UI.
        loadTasks(); // Muat data tugas.
        return view;
    }

    // Mengatur aksi untuk tombol-tombol utama di UI.
    private void initUI(View view) {
        try {
            view.findViewById(R.id.btnAddTask).setOnClickListener(v -> showAddTaskDialog());
            view.findViewById(R.id.backButton).setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        } catch (Exception e) {
            Log.e(TAG, "Error saat inisialisasi UI: " + e.getMessage());
        }
    }

    // Mengambil semua tugas dari database dan menampilkannya di layar.
    private void loadTasks() {
        try {
            View view = getView();
            if (view == null) return; // Pastikan view sudah ada.

            taskList.clear(); // Kosongkan daftar lama.
            taskList.addAll(taskDao.getAllTasks()); // Ambil data baru dari DB.

            LinearLayout container = view.findViewById(R.id.tasksContainer);
            container.removeAllViews(); // Kosongkan tampilan sebelum diisi ulang.

            // Tampilkan setiap tugas satu per satu ke dalam container.
            for (Task task : taskList) {
                View taskView = createTaskView(task, container);
                if (taskView != null) {
                    container.addView(taskView);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saat memuat tugas: " + e.getMessage());
        }
    }

    // Membuat tampilan untuk satu item tugas (dari item_task.xml).
    private View createTaskView(Task task, ViewGroup parent) {
        try {
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            View taskView = inflater.inflate(R.layout.item_task, parent, false);

            // Kenali komponen di dalam satu item tugas.
            CheckBox cbTask = taskView.findViewById(R.id.cbTask);
            TextView tvStartDate = taskView.findViewById(R.id.tvStartDate);
            TextView tvDueDate = taskView.findViewById(R.id.tvDueDate);
            TextView tvDuration = taskView.findViewById(R.id.tvDuration);
            ImageButton btnEdit = taskView.findViewById(R.id.btnEdit);
            ImageButton btnDelete = taskView.findViewById(R.id.btnDelete);

            // Atur data ke komponen.
            cbTask.setText(task.getTitle());
            cbTask.setChecked(task.isCompleted());
            // Jika checkbox diubah, update status tugas di database.
            cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                taskDao.updateTask(task);
            });

            tvStartDate.setText(formatDateTime(task.getStartDate()));
            tvDueDate.setText(formatDateTime(task.getDueDate()));
            tvDuration.setText(calculateRemainingTime(task.getDueDate()));

            // Atur aksi untuk tombol edit dan hapus.
            btnEdit.setOnClickListener(v -> showEditTaskDialog(task));
            btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(task));

            return taskView;
        } catch (Exception e) {
            Log.e(TAG, "Error saat membuat tampilan tugas: " + e.getMessage());
            return null;
        }
    }

    // Menampilkan dialog konfirmasi sebelum menghapus tugas.
    private void showDeleteConfirmationDialog(Task task) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus Tugas")
                .setMessage("Apakah Anda yakin ingin menghapus tugas ini?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    taskDao.deleteTask(task.getId()); // Hapus dari DB.
                    loadTasks(); // Muat ulang daftar tugas.
                    Toast.makeText(requireContext(), "Tugas dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // Menampilkan dialog untuk mengedit tugas yang sudah ada.
    private void showEditTaskDialog(Task task) {
        try {
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.setContentView(R.layout.dialog_add_task);
            // Atur tampilan dialog.
            setupDialogWindow(dialog);

            // Kenali komponen di dalam dialog.
            TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
            TextInputEditText etStartDate = dialog.findViewById(R.id.etStartDate);
            TextInputEditText etDueDate = dialog.findViewById(R.id.etDueDate);
            TextView tvDuration = dialog.findViewById(R.id.tvDuration);
            Button btnSave = dialog.findViewById(R.id.btnSave);
            Button btnCancel = dialog.findViewById(R.id.btnCancel);

            // Isi kolom dengan data tugas yang ada.
            etTitle.setText(task.getTitle());
            etStartDate.setText(task.getStartDate());
            etStartDate.setEnabled(false); // Waktu mulai tidak bisa diubah.
            etDueDate.setText(task.getDueDate());
            tvDuration.setText(calculateDurationText(task.getStartDate(), task.getDueDate()));

            // Atur aksi saat kolom 'DueDate' diklik.
            etDueDate.setOnClickListener(v -> showDateTimePicker(etDueDate, tvDuration, task.getStartDate()));

            // Atur aksi tombol simpan.
            btnSave.setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String dueDateTime = etDueDate.getText().toString().trim();

                // Validasi input.
                if (title.isEmpty() || dueDateTime.isEmpty()) {
                    Toast.makeText(requireContext(), "Harap isi semua bidang", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidDueDate(task.getStartDate(), dueDateTime)) {
                    Toast.makeText(requireContext(), "Waktu deadline harus setelah waktu mulai", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update data tugas.
                task.setTitle(title);
                task.setDueDate(dueDateTime);
                taskDao.updateTask(task);
                loadTasks(); // Muat ulang daftar.
                dialog.dismiss();
                Toast.makeText(requireContext(), "Tugas diperbarui", Toast.LENGTH_SHORT).show();
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error saat menampilkan dialog edit: " + e.getMessage());
        }
    }

    // Menampilkan dialog untuk menambah tugas baru.
    private void showAddTaskDialog() {
        try {
            android.app.Dialog dialog = new android.app.Dialog(requireContext());
            dialog.setContentView(R.layout.dialog_add_task);
            // Atur tampilan dialog.
            setupDialogWindow(dialog);

            // Kenali komponen di dalam dialog.
            TextInputEditText etTitle = dialog.findViewById(R.id.etTitle);
            TextInputEditText etStartDate = dialog.findViewById(R.id.etStartDate);
            TextInputEditText etDueDate = dialog.findViewById(R.id.etDueDate);
            TextView tvDuration = dialog.findViewById(R.id.tvDuration);
            Button btnSave = dialog.findViewById(R.id.btnSave);
            Button btnCancel = dialog.findViewById(R.id.btnCancel);

            // Waktu mulai diatur otomatis ke waktu sekarang.
            String currentDateTime = getCurrentDateTime();
            etStartDate.setText(currentDateTime);
            etStartDate.setEnabled(false);

            etDueDate.setOnClickListener(v -> showDateTimePicker(etDueDate, tvDuration, currentDateTime));

            // Atur aksi tombol simpan.
            btnSave.setOnClickListener(v -> {
                String title = etTitle.getText().toString().trim();
                String dueDateTime = etDueDate.getText().toString().trim();

                // Validasi input.
                if (title.isEmpty() || dueDateTime.isEmpty()) {
                    Toast.makeText(requireContext(), "Harap isi semua bidang", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidDueDate(currentDateTime, dueDateTime)) {
                    Toast.makeText(requireContext(), "Waktu deadline harus setelah waktu mulai", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Buat tugas baru dan simpan ke database.
                Task newTask = new Task(title, currentDateTime, dueDateTime);
                taskDao.insertTask(newTask);
                loadTasks(); // Muat ulang daftar.
                dialog.dismiss();
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error saat menampilkan dialog tambah: " + e.getMessage());
        }
    }

    // Fungsi bantuan untuk mengatur tampilan window dialog.
    private void setupDialogWindow(android.app.Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
        }
    }

    // Menampilkan pilihan tanggal dan waktu untuk 'DueDate'.
    private void showDateTimePicker(TextInputEditText dueDateField, TextView durationField, String startDateTime) {
        final Calendar calendar = Calendar.getInstance();
        // Tampilkan dialog pilih tanggal.
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Setelah tanggal dipilih, tampilkan dialog pilih waktu.
                    TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                String selectedDateTime = String.format(Locale.getDefault(), "%02d/%02d/%02d %02d:%02d",
                                        month + 1, dayOfMonth, year % 100, hourOfDay, minute);
                                // Set teks pada kolom input dan durasi.
                                dueDateField.setText(selectedDateTime);
                                durationField.setText(calculateDurationText(startDateTime, selectedDateTime));
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                    timePickerDialog.show();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        // Atur tanggal minimum (tidak bisa memilih tanggal sebelum waktu mulai).
        try {
            Date minDate = TASK_DATE_FORMAT.parse(startDateTime);
            if (minDate != null) {
                datePickerDialog.getDatePicker().setMinDate(minDate.getTime());
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing tanggal mulai: " + e.getMessage());
        }
        datePickerDialog.show();
    }

    // Menghitung total durasi dari waktu mulai sampai selesai.
    private String calculateDurationText(String startDateTime, String dueDateTime) {
        try {
            Date startDate = TASK_DATE_FORMAT.parse(startDateTime);
            Date dueDate = TASK_DATE_FORMAT.parse(dueDateTime);
            if (startDate != null && dueDate != null) {
                long diff = dueDate.getTime() - startDate.getTime(); // Selisih dalam milidetik.
                long days = diff / (1000 * 60 * 60 * 24);
                long hours = (diff / (1000 * 60 * 60)) % 24;
                long minutes = (diff / (1000 * 60)) % 60;
                return String.format(Locale.getDefault(), "Durasi: %d hari %d jam %d menit", days, hours, minutes);
            }
        } catch (ParseException e) { /* Abaikan error parsing */ }
        return "Durasi: -";
    }

    // Menghitung sisa waktu dari sekarang sampai deadline.
    private String calculateRemainingTime(String dueDateTime) {
        if (dueDateTime == null || dueDateTime.isEmpty()) return "-";
        try {
            Date dueDate = TASK_DATE_FORMAT.parse(dueDateTime);
            if (dueDate != null) {
                long diffMillis = dueDate.getTime() - new Date().getTime(); // Selisih waktu.
                if (diffMillis <= 0) return "⏰ Waktu Habis"; // Jika sudah lewat.
                long days = diffMillis / (1000 * 60 * 60 * 24);
                long hours = (diffMillis / (1000 * 60 * 60)) % 24;
                long minutes = (diffMillis / (1000 * 60)) % 60;
                return String.format(Locale.getDefault(), "Sisa: %d hari %d jam %d menit", days, hours, minutes);
            }
        } catch (ParseException e) { /* Abaikan error parsing */ }
        return "-";
    }

    // Mengecek apakah tanggal selesai (due date) valid (setelah tanggal mulai).
    private boolean isValidDueDate(String startDateTime, String dueDateTime) {
        try {
            Date startDate = TASK_DATE_FORMAT.parse(startDateTime);
            Date dueDate = TASK_DATE_FORMAT.parse(dueDateTime);
            return dueDate != null && startDate != null && dueDate.after(startDate);
        } catch (ParseException e) {
            return false;
        }
    }

    // Mendapatkan waktu saat ini dalam format yang ditentukan.
    private String getCurrentDateTime() {
        return TASK_DATE_FORMAT.format(new Date());
    }

    // Mengubah format tanggal agar lebih mudah dibaca oleh pengguna.
    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return "-";
        try {
            Date date = TASK_DATE_FORMAT.parse(dateTime);
            return DISPLAY_DATE_FORMAT.format(date);
        } catch (ParseException e) {
            return dateTime; // Jika gagal format, tampilkan apa adanya.
        }
    }

    // --- Pembaruan Realtime untuk Sisa Waktu ---

    @Override
    public void onResume() {
        super.onResume();
        loadTasks(); // Muat ulang tugas saat kembali ke fragment.
        startRealtimeUpdates(); // Mulai pembaruan sisa waktu.
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRealtimeUpdates(); // Hentikan pembaruan saat fragment tidak terlihat.
    }

    // Memulai pembaruan sisa waktu setiap menit.
    private void startRealtimeUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDurations(); // Perbarui tampilan sisa waktu.
                handler.postDelayed(this, 60000); // Jadwalkan lagi dalam 1 menit.
            }
        };
        handler.post(updateRunnable); // Jalankan pertama kali.
    }

    // Menghentikan pembaruan sisa waktu.
    private void stopRealtimeUpdates() {
        handler.removeCallbacks(updateRunnable);
    }

    // Memperbarui tampilan sisa waktu untuk semua tugas yang terlihat.
    private void updateDurations() {
        try {
            View view = getView();
            if (view == null) return;
            LinearLayout container = view.findViewById(R.id.tasksContainer);
            // Loop melalui setiap item tugas yang ada di container.
            for (int i = 0; i < container.getChildCount(); i++) {
                View taskView = container.getChildAt(i);
                TextView tvDuration = taskView.findViewById(R.id.tvDuration);
                // Dapatkan tugas yang sesuai dari daftar.
                if (i < taskList.size()) {
                    Task task = taskList.get(i);
                    tvDuration.setText(calculateRemainingTime(task.getDueDate()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saat memperbarui durasi: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Tutup koneksi database saat fragment dihancurkan untuk mencegah memory leak.
        if (taskDao != null) {
            taskDao.close();
        }
    }
}