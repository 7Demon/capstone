package com.example.capstone;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
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

    private TaskDao taskDao; // Akses database task
    private LinearLayout reminderListLayout; // Layout untuk menampilkan list pengingat

    private Handler dateRefreshHandler = new Handler(); // Handler untuk refresh otomatis
    private Runnable midnightRunnable; // Runnable untuk trigger di tengah malam

    private static final String CHANNEL_ID = "task_reminder_channel"; // ID channel notifikasi
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101; // Kode request permission

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout fragment
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);
        reminderListLayout = view.findViewById(R.id.reminderList);

        createNotificationChannel(); // Buat channel notifikasi jika perlu (Android 8+)

        updateDateHeader(view);      // Perbarui header tanggal
        populateDateRow(view);       // Isi baris kalender kecil
        scheduleMidnightRefresh(view); // Jadwalkan refresh tengah malam

        return view;
    }

    private void updateDateHeader(View view) {
        // Format tanggal: Senin, 27 Mei
        TextView dateHeader = view.findViewById(R.id.dateHeader);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
        String currentDate = sdf.format(new Date());
        dateHeader.setText(currentDate);
    }

    private void populateDateRow(View view) {
        // Tampilkan 7 hari ke depan dalam bentuk singkat (baris kalender)
        LinearLayout calendarRow = view.findViewById(R.id.calendarRow);
        calendarRow.removeAllViews();

        SimpleDateFormat dayFormat = new SimpleDateFormat("E", new Locale("id", "ID"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("d", Locale.getDefault());

        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            Date date = calendar.getTime();
            String dayLabel = dayFormat.format(date); // Hari (Sen, Sel, ...)
            String dateLabel = dateFormat.format(date); // Tanggal (1, 2, ...)

            String fullLabel = dayLabel + "\n" + dateLabel;

            // Buat tampilan tanggal kecil
            TextView dateView = new TextView(getContext());
            dateView.setText(fullLabel);
            dateView.setTextSize(14);
            dateView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            dateView.setPadding(12, 8, 12, 8);
            dateView.setBackgroundResource(R.drawable.calendar_day_background);
            dateView.setTextColor(getResources().getColor(android.R.color.black));

            calendarRow.addView(dateView);
            calendar.add(Calendar.DAY_OF_MONTH, 1); // Lanjut ke hari berikutnya
        }
    }

    private void scheduleMidnightRefresh(View view) {
        // Hitung waktu menuju tengah malam hari berikutnya
        long now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        calendar.add(Calendar.DAY_OF_YEAR, 1); // Besok
        calendar.set(Calendar.HOUR_OF_DAY, 0); // Jam 00:00:05
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 5);
        calendar.set(Calendar.MILLISECOND, 0);

        long midnight = calendar.getTimeInMillis();
        long delay = midnight - now;

        // Runnable yang dijalankan saat tengah malam
        midnightRunnable = () -> {
            if (getView() != null) {
                updateDateHeader(getView());
                populateDateRow(getView());
                loadReminderTasks(); // Muat ulang pengingat
                scheduleMidnightRefresh(getView()); // Jadwalkan ulang
            }
        };

        // Jalankan runnable setelah delay
        dateRefreshHandler.postDelayed(midnightRunnable, delay);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            updateDateHeader(getView());
            populateDateRow(getView());
        }
        loadReminderTasks(); // Muat ulang saat kembali ke fragment
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dateRefreshHandler.removeCallbacks(midnightRunnable); // Stop refresh saat view dihancurkan
    }

    private void loadReminderTasks() {
        taskDao = new TaskDao(requireContext());
        taskDao.open();

        List<Task> upcomingTasks = taskDao.getUpcomingTasks(3); // Ambil tugas 3 hari ke depan
        reminderListLayout.removeAllViews(); // Bersihkan tampilan lama

        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

        for (Task task : upcomingTasks) {
            try {
                Date dueDate = sdf.parse(task.getDueDate());

                // Normalisasi waktu ke 00:00:00
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

                // Hitung selisih hari dari sekarang ke due date
                long diffInMillis = dueDate.getTime() - normalizedCurrentDate.getTime();
                int daysUntilDue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                daysUntilDue = Math.max(daysUntilDue, 0); // Minimal 0

                addReminderCard(task, daysUntilDue);

                // Kirim notifikasi kalau tinggal <= 2 hari
                if (daysUntilDue <= 2) {
                    sendNotification(task, daysUntilDue);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        taskDao.close(); // Tutup koneksi DB
    }

    private void addReminderCard(Task task, int daysUntilDue) {
        // Buat kartu pengingat
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_reminder_card, reminderListLayout, false);

        TextView titleText = cardView.findViewById(R.id.reminderTitle);
        TextView dueDateText = cardView.findViewById(R.id.reminderDueDate);
        TextView daysLeftText = cardView.findViewById(R.id.reminderDaysLeft);

        titleText.setText(task.getTitle());
        dueDateText.setText("Due: " + task.getDueDate());

        if (daysUntilDue <= 2) {
            // Tampilan card jika urgent
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
            if (daysUntilDue <= 7) {
                daysLeftText.setText(daysUntilDue + " hari lagi");
                daysLeftText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }

        reminderListLayout.addView(cardView); // Tambahkan ke layout
    }

    private void createNotificationChannel() {
        // Untuk Android 8+ (Oreo), notifikasi butuh channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Reminder Channel";
            String description = "Channel for task reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification(Task task, int daysUntilDue) {
        // Android 13+ harus cek izin terlebih dahulu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                return;
            }
        }

        // Intent untuk buka app saat klik notifikasi
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        String title = "Pengingat Tugas: " + task.getTitle();
        String content = daysUntilDue <= 0 ? "Batas waktu hari ini!" :
                daysUntilDue == 1 ? "Batas waktu besok!" :
                        "Tinggal 2 hari lagi!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ikon notifikasi
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Hilang saat diklik

        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(NotificationManager.class);

        // Gunakan ID unik (pakai ID task atau waktu saat ini)
        int notificationId = task.getId() > 0 ? task.getId() : (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Cek jika izin notifikasi disetujui
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadReminderTasks(); // Muat ulang untuk mengirim notifikasi
            }
        }
    }
}
