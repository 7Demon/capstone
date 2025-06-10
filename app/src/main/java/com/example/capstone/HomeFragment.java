package com.example.capstone;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.capstone.db.TaskDao;
import com.example.capstone.models.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // Format tanggal untuk menentukan hari saat ini
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

    private TaskDao taskDao; // DAO untuk mengakses data tugas
    private Handler handler = new Handler(); // Handler untuk pengecekan perubahan hari
    private Runnable updateRunnable;
    private LinearLayout todayTasksContainer; // Container untuk menampilkan tugas-tugas hari ini
    private String currentDay; // Menyimpan hari saat ini

    public HomeFragment() {
        // Konstruktor kosong diperlukan oleh Fragment
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Inisialisasi DAO dan buka koneksi database
            taskDao = new TaskDao(requireContext());
            taskDao.open();
            currentDay = DATE_FORMAT.format(new Date()); // Simpan hari saat ini
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TaskDao: " + e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout untuk fragment ini
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        // Inisialisasi tombol dan layout dari XML
        Button btnTaskAssignment = view.findViewById(R.id.btnTaskAssignment);
        Button btnSetSchedule = view.findViewById(R.id.btnSetSchedule);
        todayTasksContainer = view.findViewById(R.id.todayTasksContainer);

        // Navigasi ke TaskAssignmentFragment saat tombol ditekan
        btnTaskAssignment.setOnClickListener(v -> {
            Fragment taskFragment = new TaskAssignmentFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, taskFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Navigasi ke ScheduleFragment saat tombol ditekan
        btnSetSchedule.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new ScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTodayTasks(); // Muat ulang tugas hari ini saat fragment aktif kembali
        startDailyUpdateChecker(); // Mulai pengecekan perubahan hari
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDailyUpdateChecker(); // Hentikan pengecekan saat fragment tidak aktif
    }

    // Memuat dan menampilkan daftar tugas untuk hari ini
    private void loadTodayTasks() {
        try {
            todayTasksContainer.removeAllViews();

            List<Task> todayTasks = taskDao.getTodayTasks(); // Ambil data tugas hari ini dari DB

            if (todayTasks.isEmpty()) {
                TextView emptyView = new TextView(getContext());
                emptyView.setText("Tidak ada tugas untuk hari ini");
                emptyView.setTextSize(16);
                emptyView.setGravity(Gravity.CENTER);
                todayTasksContainer.addView(emptyView);
                return;
            }

            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (Task task : todayTasks) {
                // Inflate layout untuk setiap item tugas
                View taskView = inflater.inflate(R.layout.item_today_task, todayTasksContainer, false);

                CheckBox cbTask = taskView.findViewById(R.id.cbTask);
                TextView tvStatus = taskView.findViewById(R.id.tvStatus);

                cbTask.setText(task.getTitle());
                cbTask.setChecked(task.isCompleted()); // Tampilkan apakah tugas sudah selesai

                // Atur teks dan warna status berdasarkan status tugas
                String status = task.isCompleted() ? "Sudah dikerjakan" : "Belum dikerjakan";
                int color = task.isCompleted() ?
                        ContextCompat.getColor(requireContext(), R.color.green) :
                        ContextCompat.getColor(requireContext(), R.color.red);

                tvStatus.setText(status);
                tvStatus.setTextColor(color);

                // Listener jika checkbox diubah
                cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    task.setCompleted(isChecked); // Perbarui status tugas
                    taskDao.updateTask(task);

                    // Perbarui status tampilan
                    String newStatus = isChecked ? "Sudah dikerjakan" : "Belum dikerjakan";
                    int newColor = isChecked ?
                            ContextCompat.getColor(requireContext(), R.color.green) :
                            ContextCompat.getColor(requireContext(), R.color.red);

                    tvStatus.setText(newStatus);
                    tvStatus.setTextColor(newColor);
                });

                todayTasksContainer.addView(taskView); // Tambahkan ke tampilan
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading today's tasks: " + e.getMessage());
        }
    }

    // Mulai pengecekan otomatis untuk perubahan hari
    private void startDailyUpdateChecker() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                checkForDayChange(); // Cek apakah hari sudah berganti
                handler.postDelayed(this, 60000); // Jalankan lagi setiap 1 menit
            }
        };
        handler.post(updateRunnable);
    }

    // Hentikan pengecekan otomatis
    private void stopDailyUpdateChecker() {
        handler.removeCallbacks(updateRunnable);
    }

    // Cek apakah hari sudah berganti
    private void checkForDayChange() {
        String newDay = DATE_FORMAT.format(new Date());
        if (!newDay.equals(currentDay)) {
            currentDay = newDay;
            loadTodayTasks(); // Reload tugas baru untuk hari baru
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Tutup koneksi database
        if (taskDao != null) {
            taskDao.close();
        }
    }
}
