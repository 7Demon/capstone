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
import android.util.Log;
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
    private static final String TAG = "ReminderFragment";
    private static final SimpleDateFormat REMINDER_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("E", new Locale("id", "ID"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d", Locale.getDefault());

    private TaskDao taskDao;
    private LinearLayout reminderListLayout;
    private Handler dateRefreshHandler = new Handler();
    private Runnable midnightRunnable;

    private static final String CHANNEL_ID = "task_reminder_channel";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);
        reminderListLayout = view.findViewById(R.id.reminderList);

        createNotificationChannel();
        updateDateHeader(view);
        populateDateRow(view);
        initUI(view);
        scheduleMidnightRefresh(view);

        return view;
    }

    private void initUI(View view) {
        view.findViewById(R.id.backButton).setOnClickListener(v -> {
            Fragment homeFragment = new HomeFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView, homeFragment)
                    .commit();
        });
    }

    private void updateDateHeader(View view) {
        try {
            TextView dateHeader = view.findViewById(R.id.dateHeader);
            String currentDate = DISPLAY_DATE_FORMAT.format(new Date());
            dateHeader.setText(currentDate);
        } catch (Exception e) {
            Log.e(TAG, "Error updating date header: " + e.getMessage());
        }
    }

    private void populateDateRow(View view) {
        LinearLayout calendarRow = view.findViewById(R.id.calendarRow);
        calendarRow.removeAllViews();

        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            try {
                Date date = calendar.getTime();
                String dayLabel = DAY_FORMAT.format(date);
                String dateLabel = DATE_FORMAT.format(date);

                TextView dateView = new TextView(getContext());
                dateView.setText(String.format("%s\n%s", dayLabel, dateLabel));
                dateView.setTextSize(14);
                dateView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                dateView.setPadding(39, 10, 40, 10);
                dateView.setBackgroundResource(R.drawable.calendar_day_background);
                dateView.setTextColor(getResources().getColor(android.R.color.black));

                calendarRow.addView(dateView);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            } catch (Exception e) {
                Log.e(TAG, "Error populating date row: " + e.getMessage());
            }
        }
    }

    private void scheduleMidnightRefresh(View view) {
        try {
            long now = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(now);
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 5);
            calendar.set(Calendar.MILLISECOND, 0);

            long midnight = calendar.getTimeInMillis();
            long delay = midnight - now;

            midnightRunnable = () -> {
                if (getView() != null) {
                    updateDateHeader(getView());
                    populateDateRow(getView());
                    loadReminderTasks();
                    scheduleMidnightRefresh(getView());
                }
            };

            dateRefreshHandler.postDelayed(midnightRunnable, delay);
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling midnight refresh: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (getView() != null) {
                updateDateHeader(getView());
                populateDateRow(getView());
            }
            loadReminderTasks();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dateRefreshHandler.removeCallbacks(midnightRunnable);
    }

    private void loadReminderTasks() {
        try {
            taskDao = new TaskDao(requireContext());
            taskDao.open();

            List<Task> upcomingTasks = taskDao.getUpcomingTasks(3);
            reminderListLayout.removeAllViews();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date normalizedCurrentDate = calendar.getTime();

            for (Task task : upcomingTasks) {
                try {
                    if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                        continue;
                    }

                    // Parse only date part (ignore time)
                    String dueDateOnly = task.getDueDate().substring(0, 8); // Get "MM/dd/yy"
                    Date dueDate = REMINDER_DATE_FORMAT.parse(dueDateOnly);

                    if (dueDate != null) {
                        long diffInMillis = dueDate.getTime() - normalizedCurrentDate.getTime();
                        int daysUntilDue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                        daysUntilDue = Math.max(daysUntilDue, 0);

                        addReminderCard(task, daysUntilDue);
                    }
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing task due date: " + task.getDueDate(), e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading reminder tasks: " + e.getMessage());
        } finally {
            if (taskDao != null) {
                taskDao.close();
            }
        }
    }

    private void addReminderCard(Task task, int daysUntilDue) {
        try {
            View cardView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_reminder_card, reminderListLayout, false);

            TextView titleText = cardView.findViewById(R.id.reminderTitle);
            TextView dueDateText = cardView.findViewById(R.id.reminderDueDate);
            TextView daysLeftText = cardView.findViewById(R.id.reminderDaysLeft);

            titleText.setText(task.getTitle());
            dueDateText.setText("Due: " + task.getDueDate());

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
                sendNotification(task, daysUntilDue);
            } else if (daysUntilDue <= 7) {
                daysLeftText.setText(daysUntilDue + " hari lagi");
                daysLeftText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            cardView.setOnClickListener(v -> {
                Fragment taskFragment = new TaskAssignmentFragment();
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainerView, taskFragment)
                        .addToBackStack(null)
                        .commit();
            });

            reminderListLayout.addView(cardView);
        } catch (Exception e) {
            Log.e(TAG, "Error adding reminder card: " + e.getMessage());
        }
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Task Reminder Channel";
                String description = "Channel for task reminders";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);

                NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage());
        }
    }

    private void sendNotification(Task task, int daysUntilDue) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_REQUEST_CODE);
                    return;
                }
            }

            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent,
                    PendingIntent.FLAG_IMMUTABLE);

            String title = "Pengingat Tugas: " + task.getTitle();
            String content = daysUntilDue <= 0 ? "Batas waktu hari ini!" :
                    daysUntilDue == 1 ? "Batas waktu besok!" :
                            "Tinggal 2 hari lagi!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager)
                    requireContext().getSystemService(NotificationManager.class);
            int notificationId = task.getId() > 0 ? task.getId() : (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadReminderTasks();
            }
        }
    }
}