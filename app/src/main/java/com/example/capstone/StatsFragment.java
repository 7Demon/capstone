package com.example.capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.capstone.R;
import com.example.capstone.models.StatsViewModel;
import com.example.capstone.models.Task;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class StatsFragment extends Fragment {

    private StatsViewModel viewModel;
    private TextView notYetPercent, finishedPercent;
    private LinearLayout recentTasksContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        // Bind views
        notYetPercent = view.findViewById(R.id.not_yet_percent);
        finishedPercent = view.findViewById(R.id.finished_percent);
        recentTasksContainer = view.findViewById(R.id.recent_tasks_container);

        // Observe LiveData
        viewModel.getNotYetProgress().observe(getViewLifecycleOwner(), progress -> {
            String formatted = new DecimalFormat("0.00").format(progress);
            notYetPercent.setText(formatted + "%");
        });

        viewModel.getFinishedProgress().observe(getViewLifecycleOwner(), progress -> {
            String formatted = new DecimalFormat("0.00").format(progress);
            finishedPercent.setText(formatted + "%");
        });

        viewModel.getRecentTasks().observe(getViewLifecycleOwner(), this::populateRecentTasks);
    }

    private void populateRecentTasks(List<Task> tasks) {
        recentTasksContainer.removeAllViews();

        if (tasks.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText("No completed tasks yet");
            emptyView.setTextSize(16);
            recentTasksContainer.addView(emptyView);
            return;
        }

        for (Task task : tasks) {
            TextView taskView = new TextView(requireContext());
            taskView.setText(task.getTitle());
            taskView.setTextSize(16);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);

            taskView.setLayoutParams(params);
            recentTasksContainer.addView(taskView);
        }
    }
}