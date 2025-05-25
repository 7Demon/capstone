package com.example.capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.capstone.R;
import com.example.capstone.db.TaskDao;
import com.example.capstone.models.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderFragment extends Fragment {

    // DAO untuk mengakses database tugas
    private TaskDao taskDao;
    // Layout untuk menampilkan daftar reminder
    private LinearLayout reminderListLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout fragment
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);

        // Inisialisasi komponen UI
        reminderListLayout = view.findViewById(R.id.reminderList);

        // Memperbarui header tanggal (menampilkan hari dan tanggal)
        updateDateHeader(view);

        return view;
    }

    // Method untuk memperbarui header tanggal
    private void updateDateHeader(View view) {
        TextView dateHeader = view.findViewById(R.id.dateHeader);
        // Format tanggal: "Hari, Tanggal Bulan" (contoh: "Senin, 5 Juni")
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
        String currentDate = sdf.format(new Date());
        dateHeader.setText(currentDate);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Memuat tugas-tugas reminder saat fragment aktif
        loadReminderTasks();
    }

    // Memuat tugas-tugas yang akan dijadikan reminder
    private void loadReminderTasks() {
        // Inisialisasi TaskDao dan buka koneksi database
        taskDao = new TaskDao(requireContext());
        taskDao.open();

        // Ambil tugas yang deadline-nya dalam 3 hari ke depan
        List<Task> upcomingTasks = taskDao.getUpcomingTasks(3);

        // Hapus semua view yang ada sebelumnya
        reminderListLayout.removeAllViews();

        // Dapatkan tanggal sekarang untuk perhitungan
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Format untuk parsing tanggal dari string
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

        // Proses setiap tugas yang akan datang
        for (Task task : upcomingTasks) {
            try {
                // Parse tanggal deadline tugas
                Date dueDate = sdf.parse(task.getDueDate());

                // Normalisasi waktu ke 00:00:00 untuk perhitungan yang akurat
                Calendar cal = Calendar.getInstance();

                // Normalisasi tanggal sekarang (hilangkan jam, menit, detik)
                cal.setTime(new Date());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date normalizedCurrentDate = cal.getTime();

                // Normalisasi tanggal deadline
                cal.setTime(dueDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dueDate = cal.getTime();

                // Hitung selisih hari antara sekarang dan deadline
                long diffInMillis = dueDate.getTime() - normalizedCurrentDate.getTime();
                int daysUntilDue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                // Pastikan tidak negatif (jika deadline sudah lewat)
                daysUntilDue = Math.max(daysUntilDue, 0);

                // Tambahkan card reminder untuk tugas ini
                addReminderCard(task, daysUntilDue);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Tutup koneksi database
        taskDao.close();
    }

    // Menambahkan card reminder ke dalam layout
    private void addReminderCard(Task task, int daysUntilDue) {
        // Inflate layout card reminder
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_reminder_card, reminderListLayout, false);

        // Dapatkan referensi ke komponen dalam card
        TextView titleText = cardView.findViewById(R.id.reminderTitle);
        TextView dueDateText = cardView.findViewById(R.id.reminderDueDate);
        TextView daysLeftText = cardView.findViewById(R.id.reminderDaysLeft);

        // Set data tugas ke card
        titleText.setText(task.getTitle());
        dueDateText.setText("Due: " + task.getDueDate());

        // Styling untuk tugas yang mendekati deadline (<= 2 hari)
        if (daysUntilDue <= 2) {
            // Set background merah untuk tugas urgent
            cardView.setBackgroundResource(R.drawable.urgent_card_background);


            titleText.setTextColor(getResources().getColor(android.R.color.white));
            dueDateText.setTextColor(getResources().getColor(android.R.color.white));

            // Pesan khusus berdasarkan hari tersisa
            if (daysUntilDue <= 0) {
                daysLeftText.setText("HARI INI BATAS WAKTU!");
            } else if (daysUntilDue == 1) {
                daysLeftText.setText("BATAS WAKTU BESOK!");
            } else if (daysUntilDue == 2) {
                daysLeftText.setText("TINGGAL 2 HARI LAGI!");
            } else {
                daysLeftText.setText(daysUntilDue + " hari lagi");
            }
            daysLeftText.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            // Styling tugas yang masih punya waktu (> 2 hari)
            if (daysUntilDue <= 7) {
                daysLeftText.setText(daysUntilDue + " hari lagi");
                daysLeftText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
        // Set layout params untuk membuat card lebih kecil
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        );
//        params.setMargins(8, 4, 8, 4);
//        cardView.setLayoutParams(params);


        // Tambahkan card ke dalam layout reminder
        reminderListLayout.addView(cardView);
    }
}