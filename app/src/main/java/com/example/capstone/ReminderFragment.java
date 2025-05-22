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

    private TaskDao taskDao;
    private LinearLayout reminderListLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);

        // Initialize UI components
        reminderListLayout = view.findViewById(R.id.reminderList);

        // Update date header (No. 5)
        updateDateHeader(view); // Panggil method dengan parameter view

        return view;
    }

    // Method untuk update tanggal header
    private void updateDateHeader(View view) {
        TextView dateHeader = view.findViewById(R.id.dateHeader); // Ambil TextView dari view
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
        String currentDate = sdf.format(new Date());
        dateHeader.setText(currentDate);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReminderTasks();
    }

    //    private void loadReminderTasks() {
//        // Initialize TaskDao
//        taskDao = new TaskDao(requireContext());
//        taskDao.open();
//
//        // Get all tasks from database
//        List<Task> allTasks = taskDao.getAllTasks();
//
//        // Clear existing views
//        reminderListLayout.removeAllViews();
//
//        // Get current date
//        Calendar calendar = Calendar.getInstance();
//        Date currentDate = calendar.getTime();
//
//        // Format for parsing dates
//        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
//
//        for (Task task : allTasks) {
//            try {
//                // Parse task due date
//                Date dueDate = sdf.parse(task.getDueDate());
//
//                // Calculate days until due date
//                long diffInMillis = dueDate.getTime() - currentDate.getTime();
//                int daysUntilDue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
//
//                // Only show tasks that are not completed and due in the next 7 days
//                if (!task.isCompleted() && daysUntilDue >= 0 && daysUntilDue <= 7) {
//                    addReminderCard(task, daysUntilDue);
//                }
//
//            } catch (ParseException e) {
//                e.printStackTrace();
//            }
//        }
//
//        taskDao.close();
//    }
    private void loadReminderTasks() {
        // Initialize TaskDao
        taskDao = new TaskDao(requireContext());
        taskDao.open();

        // Get upcoming tasks (next 7 days)
        List<Task> upcomingTasks = taskDao.getUpcomingTasks(3);

        // Clear existing views
        reminderListLayout.removeAllViews();

        // Get current date
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Format for parsing dates
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

        for (Task task : upcomingTasks) {
            try {
                Date dueDate = sdf.parse(task.getDueDate());

                // Normalisasi waktu ke 00:00:00 untuk kedua tanggal
                Calendar cal = Calendar.getInstance();

                // Normalisasi currentDate (hari ini)
                cal.setTime(new Date());
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Date currentDa = cal.getTime();

                // Normalisasi dueDate
                cal.setTime(dueDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dueDate = cal.getTime();

                // Hitung selisih hari
                long diffInMillis = dueDate.getTime() - currentDa.getTime();
                int daysUntilDue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                daysUntilDue = Math.max(daysUntilDue, 0);

                addReminderCard(task, daysUntilDue);

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        taskDao.close();
    }

    private void addReminderCard(Task task, int daysUntilDue) {
        // Inflate reminder card layout
        View cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_reminder_card, reminderListLayout, false);

        TextView titleText = cardView.findViewById(R.id.reminderTitle);
        TextView dueDateText = cardView.findViewById(R.id.reminderDueDate);
        TextView daysLeftText = cardView.findViewById(R.id.reminderDaysLeft);

        // Set task data to the card
        titleText.setText(task.getTitle());
        dueDateText.setText("Due: " + task.getDueDate());

        // Custom styling for tasks due in <= 2 days
        if (daysUntilDue <= 2) {
            // Change background color for urgent tasks
            cardView.setBackgroundResource(R.drawable.urgent_card_background);

            // Change text colors for better contrast
            titleText.setTextColor(getResources().getColor(android.R.color.white));
            dueDateText.setTextColor(getResources().getColor(android.R.color.white));

            // Set urgent text
            if (daysUntilDue <= 0) { // Deadline hari ini atau sudah lewat
                daysLeftText.setText("HARI INI BATAS WAKTU!");
            } else if (daysUntilDue == 1) {
                daysLeftText.setText("BATAS WAKTU BESOK!");
            } else if (daysUntilDue == 2) { // Tambah kondisi khusus untuk 2 hari
                daysLeftText.setText("TINGGAL 2 HARI LAGI!");
            } else {
                daysLeftText.setText(daysUntilDue + " hari lagi");
            }
            daysLeftText.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            // Normal styling for tasks with > 2 days left
            if (daysUntilDue <= 7) {
                daysLeftText.setText(daysUntilDue + " hari lagi");
                daysLeftText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }

        // Add card to the reminder list
        reminderListLayout.addView(cardView);
    }
}