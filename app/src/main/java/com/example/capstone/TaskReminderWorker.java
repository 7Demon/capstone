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

public class TaskReminderWorker extends Worker {

    private static final String CHANNEL_ID = "task_reminder_channel";

    public TaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        createNotificationChannel();

        TaskDao taskDao = new TaskDao(getApplicationContext());
        taskDao.open();

        List<Task> tasks = taskDao.getUpcomingTasks(3);
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        Date today = normalize(cal.getTime());

        for (Task task : tasks) {
            try {
                Date dueDate = normalize(sdf.parse(task.getDueDate()));
                long diff = dueDate.getTime() - today.getTime();
                int daysUntilDue = (int) (diff / (1000 * 60 * 60 * 24));

                if (daysUntilDue <= 2) {
                    sendNotification(task, daysUntilDue);
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        Log.d("Worker", "Notifikasi dikirim!");

        taskDao.close();
        return Result.success();
    }

    private Date normalize(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private void sendNotification(Task task, int daysUntilDue) {
        String title = "Pengingat Tugas: " + task.getTitle();
        String content = daysUntilDue <= 0 ? "Batas waktu hari ini!" :
                daysUntilDue == 1 ? "Batas waktu besok!" :
                        "Tinggal 2 hari lagi!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(getApplicationContext()).notify(task.getId(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Task Reminder Channel";
            String description = "Channel for task reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
