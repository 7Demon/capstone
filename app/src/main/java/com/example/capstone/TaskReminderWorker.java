package com.example.capstone;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.capstone.db.TaskDao;
import com.example.capstone.models.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Kelas Worker untuk menjalankan tugas di latar belakang (background task).
 */
public class TaskReminderWorker extends Worker {

    // ID unik yang diperlukan untuk channel notifikasi.
    private static final String CHANNEL_ID = "task_reminder_channel";

    // Constructor bawaan untuk Worker.
    public TaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Metode utama yang akan dijalankan oleh WorkManager di background.
     */
    @NonNull
    @Override
    public Result doWork() {
        // Buat channel notifikasi (wajib untuk Android 8+).
        createNotificationChannel();

        // Buka koneksi ke database.
        TaskDao taskDao = new TaskDao(getApplicationContext());
        taskDao.open();


        // mengambil semua tugas yang belum selesai.
        List<Task> tasks = taskDao.getUpcomingTasks(3);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());

        // Dapatkan tanggal hari ini tanpa informasi waktu (jam, menit, dll).
        Date today = normalize(new Date());

        // Periksa setiap tugas satu per satu.
        for (Task task : tasks) {
            try {
                // Ubah string tanggal deadline dari tugas menjadi objek Date.
                Date dueDate = normalize(sdf.parse(task.getDueDate()));

                // Hitung selisih hari antara hari ini dan tanggal deadline.
                long diff = dueDate.getTime() - today.getTime();
                int daysUntilDue = (int) (diff / (1000 * 60 * 60 * 24));

                // Jika deadline dalam 2 hari, hari ini, atau besok, kirim notifikasi.
                if (daysUntilDue >= 0 && daysUntilDue <= 2) {
                    sendNotification(task, daysUntilDue);
                }

            } catch (ParseException e) {
                // Catat jika ada error saat parsing tanggal.
                e.printStackTrace();
            }
        }
        Log.d("Worker", "Pemeriksaan pengingat tugas selesai!");

        // Tutup koneksi database.
        taskDao.close();

        return Result.success();
    }

    /**
     * Fungsi bantuan untuk menghapus informasi waktu (jam, menit, dll.) dari sebuah tanggal.
     */
    private Date normalize(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Fungsi untuk membuat dan mengirim notifikasi ke pengguna.
     * @param task Objek tugas yang akan diingatkan.
     * @param daysUntilDue Sisa hari menuju deadline.
     */
    private void sendNotification(Task task, int daysUntilDue) {
        String title = "Pengingat Tugas: " + task.getTitle();
        //isi notifikasi berdasarkan sisa hari.
        String content;
        if (daysUntilDue <= 0) {
            content = "Batas waktu hari ini!";
        } else if (daysUntilDue == 1) {
            content = "Batas waktu besok!";
        } else {
            content = "Tinggal " + daysUntilDue + " hari lagi!";
        }

        // Buat notifikasi menggunakan builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Cek izin untuk mengirim notifikasi (wajib untuk Android 13+).
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Kirim notifikasi. ID notifikasi dibuat unik berdasarkan ID tugas.
        NotificationManagerCompat.from(getApplicationContext()).notify(task.getId(), builder.build());
    }

    /**
     * Membuat Channel Notifikasi. Ini diperlukan untuk Android Oreo (API 26) ke atas.
     */
    private void createNotificationChannel() {
        // Kode ini hanya berjalan jika versi Android adalah Oreo atau lebih baru.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Reminder Channel";
            String description = "Channel for task reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Daftarkan channel ke sistem.
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}