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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

    private TaskDao taskDao;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private LinearLayout todayTasksContainer;
    private String currentDay;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            taskDao = new TaskDao(requireContext());
            taskDao.open();
            currentDay = DATE_FORMAT.format(new Date());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TaskDao: " + e.getMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);

        // Initialize UI components
        Button btnTaskAssignment = view.findViewById(R.id.btnTaskAssignment);
        Button btnSetSchedule = view.findViewById(R.id.btnSetSchedule);
        todayTasksContainer = view.findViewById(R.id.todayTasksContainer);

        btnTaskAssignment.setOnClickListener(v -> {
            Fragment taskFragment = new TaskAssignmentFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, taskFragment)
                    .addToBackStack(null)
                    .commit();
        });

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
        loadTodayTasks();
        startDailyUpdateChecker();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDailyUpdateChecker();
    }

    private void loadTodayTasks() {
        try {
            todayTasksContainer.removeAllViews();

            List<Task> todayTasks = taskDao.getTodayTasks();

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
                View taskView = inflater.inflate(R.layout.item_today_task, todayTasksContainer, false);

                CheckBox cbTask = taskView.findViewById(R.id.cbTask);
                TextView tvStatus = taskView.findViewById(R.id.tvStatus);

                cbTask.setText(task.getTitle());
                cbTask.setChecked(task.isCompleted());

                String status = task.isCompleted() ? "Sudah dikerjakan" : "Belum dikerjakan";
                int color = task.isCompleted() ?
                        ContextCompat.getColor(requireContext(), R.color.green) :
                        ContextCompat.getColor(requireContext(), R.color.red);

                tvStatus.setText(status);
                tvStatus.setTextColor(color);

                cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    task.setCompleted(isChecked);
                    taskDao.updateTask(task);

                    String newStatus = isChecked ? "Sudah dikerjakan" : "Belum dikerjakan";
                    int newColor = isChecked ?
                            ContextCompat.getColor(requireContext(), R.color.green) :
                            ContextCompat.getColor(requireContext(), R.color.red);

                    tvStatus.setText(newStatus);
                    tvStatus.setTextColor(newColor);
                });

                todayTasksContainer.addView(taskView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading today's tasks: " + e.getMessage());
        }
    }

    private void startDailyUpdateChecker() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                checkForDayChange();
                handler.postDelayed(this, 60000); // Check every minute
            }
        };
        handler.post(updateRunnable);
    }

    private void stopDailyUpdateChecker() {
        handler.removeCallbacks(updateRunnable);
    }

    private void checkForDayChange() {
        String newDay = DATE_FORMAT.format(new Date());
        if (!newDay.equals(currentDay)) {
            currentDay = newDay;
            loadTodayTasks();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (taskDao != null) {
            taskDao.close();
        }
    }
}