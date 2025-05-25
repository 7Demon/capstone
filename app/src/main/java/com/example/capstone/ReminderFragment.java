package com.example.capstone;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.capstone.db.TaskDao;
import com.example.capstone.models.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderFragment extends Fragment {

    private TaskDao taskDao;
    private LinearLayout reminderListLayout;

    // Handler dan Runnable untuk menjadwalkan update saat tengah malam
    private Handler dateRefreshHandler = new Handler();
    private Runnable midnightRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout fragment
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);

        // Inisialisasi layout untuk menampung card reminder
        reminderListLayout = view.findViewById(R.id.reminderList);

        // Tampilkan tanggal hari ini sebagai header
        updateDateHeader(view);

        // Tambahkan baris kalender 7 hari ke depan secara dinamis
        populateDateRow(view);

        // Jadwalkan pembaruan otomatis saat hari berganti (jam 00:00)
        scheduleMidnightRefresh(view);

        return view;
    }

    // Mengatur teks header tanggal dengan format "Sabtu, 25 Mei"
    private void updateDateHeader(View view) {
        TextView dateHeader = view.findViewById(R.id.dateHeader);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
        String currentDate = sdf.format(new Date());
        dateHeader.setText(currentDate);
    }

    // Menampilkan baris kalender (7 hari ke depan) di atas daftar reminder
    private void populateDateRow(View view) {
        LinearLayout calendarRow = view.findViewById(R.id.calendarRow);
        calendarRow.removeAllViews(); // Hapus elemen sebelumnya

        SimpleDateFormat dayFormat = new SimpleDateFormat("E", new Locale("id", "ID")); // Sen, Sel, Rab
        SimpleDateFormat dateFormat = new SimpleDateFormat("d", Locale.getDefault());   // 25, 26, dst

        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Date date = calendar.getTime();
            String dayLabel = dayFormat.format(date);
            String dateLabel = dateFormat.format(date);
            String fullLabel = dayLabel + "\n" + dateLabel;

            // Buat TextView untuk satu hari kalender
            TextView dateView = new TextView(getContext());
            dateView.setText(fullLabel);
            dateView.setTextSize(14);
            dateView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            dateView.setPadding(12, 8, 12, 8);
            dateView.setBackgroundResource(R.drawable.calendar_day_background);
            dateView.setTextColor(getResources().getColor(android.R.color.black));

            // Tambahkan ke baris kalender
            calendarRow.addView(dateView);
            calendar.add(Calendar.DAY_OF_MONTH, 1); // Tambah 1 hari
        }
    }

    // Menjadwalkan pembaruan otomatis saat tengah malam agar tampilan tetap realtime
    private void scheduleMidnightRefresh(View view) {
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, 1); // Tambah 1 hari
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 5); // Tambahkan sedikit delay agar aman
        calendar.set(Calendar.MILLISECOND, 0);

        long midnight = calendar.getTimeInMillis();
        long delay = midnight - now;

        // Runnable akan dijalankan saat tengah malam
        midnightRunnable = () -> {
            if (getView() != null) {
                updateDateHeader(getView());
                populateDateRow(getView());
                loadReminderTasks();
                scheduleMidnightRefresh(getView()); // Jadwalkan ulang untuk hari berikutnya
            }
        };

        dateRefreshHandler.postDelayed(midnightRunnable, delay);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Pastikan UI diperbarui ketika fragment diaktifkan kembali
        if (getView() != null) {
            updateDateHeader(getView());
            populateDateRow(getView());
        }

        loadReminderTasks(); // Muat ulang daftar tugas
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Hindari memory leak dengan menghapus Runnable saat fragment dihancurkan
        dateRefreshHandler.removeCallbacks(midnightRunnable);
    }

    // Memuat data tugas yang akan datang dari database
    private void loadReminderTasks() {
        taskDao = new TaskDao(requireContext());
        taskDao.open();

        // Ambil maksimal 3 tugas mendatang
        List<Task> upcomingTasks = taskDao.getUpcomingTasks(3);

        // Hapus card sebelumnya
        reminderListLayout.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

        for (Task task : upcomingTasks) {
            try {
                Date dueDate = sdf.parse(task.getDueDate());

                // Normalisasi tanggal agar hanya tanggalnya saja yang dibandingkan (tanpa jam)
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date normalizedCurrentDate = cal.getTime();

                cal.setTime(dueDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dueDate = cal.getTime();

                // Hitung selisih hari
                long diffInMillis = dueDate.getTime() - normalizedCurrentDate.getTime();
                int daysUntilDue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                daysUntilDue = Math.max(daysUntilDue, 0); // Jangan biarkan negatif

                addReminderCard(task, daysUntilDue); // Tambahkan ke tampilan

            } catch (ParseException e) {
                e.printStackTrace(); // Tangani error parsing
            }
        }

        taskDao.close(); // Tutup koneksi DB
    }

    // Menambahkan satu kartu pengingat ke dalam layout
    private void addReminderCard(Task task, int daysUntilDue) {
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_reminder_card, reminderListLayout, false);

        TextView titleText = cardView.findViewById(R.id.reminderTitle);
        TextView dueDateText = cardView.findViewById(R.id.reminderDueDate);
        TextView daysLeftText = cardView.findViewById(R.id.reminderDaysLeft);

        titleText.setText(task.getTitle());
        dueDateText.setText("Due: " + task.getDueDate());

        // Tampilkan card berwarna merah untuk task yang hampir atau sudah jatuh tempo
        if (daysUntilDue <= 2) {
            cardView.setBackgroundResource(R.drawable.urgent_card_background);
            titleText.setTextColor(getResources().getColor(android.R.color.white));
            dueDateText.setTextColor(getResources().getColor(android.R.color.white));

            if (daysUntilDue <= 0) {
                daysLeftText.setText("HARI INI BATAS WAKTU!");
            } else if (daysUntilDue == 1) {
                daysLeftText.setText("BATAS WAKTU BESOK!");
            } else {
                daysLeftText.setText("TINGGAL 2 HARI LAGI!");
            }
            daysLeftText.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            // Warna biasa untuk tugas yang masih cukup waktu
            if (daysUntilDue <= 7) {
                daysLeftText.setText(daysUntilDue + " hari lagi");
                daysLeftText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }

        // Tambahkan card ke dalam layout
        reminderListLayout.addView(cardView);
    }
}
