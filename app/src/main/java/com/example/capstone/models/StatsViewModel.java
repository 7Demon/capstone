package com.example.capstone.models;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.capstone.db.TaskDao;
import com.example.capstone.models.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsViewModel extends AndroidViewModel {
    private final TaskDao taskDao;
    private final MutableLiveData<Double> notYetProgress = new MutableLiveData<>();
    private final MutableLiveData<Double> finishedProgress = new MutableLiveData<>();
    private final MutableLiveData<List<Task>> recentTasks = new MutableLiveData<>();
    private final MutableLiveData<Map<String, List<String>>> projects = new MutableLiveData<>();

    public StatsViewModel(Application application) {
        super(application);
        taskDao = new TaskDao(application);
        taskDao.open();
        loadStatsData();
    }

    private void loadStatsData() {
        new Thread(() -> {
            // Get all tasks
            List<Task> allTasks = taskDao.getAllTasks();

            // Calculate progress
            int totalTasks = allTasks.size();
            int completedCount = 0;

            // Separate completed and recent tasks
            List<Task> completedTasks = new ArrayList<>();
            List<Task> notCompletedTasks = new ArrayList<>();

            for (Task task : allTasks) {
                if (task.isCompleted()) {
                    completedCount++;
                    completedTasks.add(task);
                } else {
                    notCompletedTasks.add(task);
                }
            }

            // Calculate percentages
            double completedPercentage = totalTasks > 0 ?
                    (completedCount * 100.0) / totalTasks : 0.0;
            double notCompletedPercentage = totalTasks > 0 ?
                    (notCompletedTasks.size() * 100.0) / totalTasks : 0.0;

            // Get recent completed tasks (last 4 completed)
            List<Task> recentCompleted = new ArrayList<>();
            int count = Math.min(completedTasks.size(), 6);
            for (int i = 0; i < count; i++) {
                recentCompleted.add(completedTasks.get(completedTasks.size() - 1 - i));
            }

            // Group tasks by project




            // Post values to LiveData
            notYetProgress.postValue(notCompletedPercentage);
            finishedProgress.postValue(completedPercentage);
            recentTasks.postValue(recentCompleted);


            taskDao.close();
        }).start();
    }

    public LiveData<Double> getNotYetProgress() { return notYetProgress; }
    public LiveData<Double> getFinishedProgress() { return finishedProgress; }
    public LiveData<List<Task>> getRecentTasks() { return recentTasks; }
    public LiveData<Map<String, List<String>>> getProjects() { return projects; }
}
