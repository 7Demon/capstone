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
    private TextView notYetPercent;
    private TextView finishedPercent;
    private LinearLayout recentTasksContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Hubungkan kode dengan file layoutnya (fragment_stats.xml).
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi ViewModel.
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        // Kenali semua komponen UI dari layout.
        notYetPercent = view.findViewById(R.id.not_yet_percent);
        finishedPercent = view.findViewById(R.id.finished_percent);
        recentTasksContainer = view.findViewById(R.id.recent_tasks_container);

        // Amati perubahan data 'notYetProgress' dari ViewModel.
        viewModel.getNotYetProgress().observe(getViewLifecycleOwner(), progress -> {
            // Format angka menjadi dua desimal (contoh: 85.00).
            String formatted = new DecimalFormat("0.00").format(progress);
            // Atur teks persentase.
            notYetPercent.setText(formatted + "%");
        });

        // Amati perubahan data 'finishedProgress' dari ViewModel.
        viewModel.getFinishedProgress().observe(getViewLifecycleOwner(), progress -> {
            String formatted = new DecimalFormat("0.00").format(progress);
            finishedPercent.setText(formatted + "%");
        });

        // Amati perubahan pada daftar tugas terbaru. Jika data berubah, panggil 'populateRecentTasks'.
        viewModel.getRecentTasks().observe(getViewLifecycleOwner(), this::populateRecentTasks);
    }

    // Fungsi untuk menampilkan daftar tugas terbaru ke layar.
    private void populateRecentTasks(List<Task> tasks) {
        // Kosongkan daftar lama sebelum menampilkan yang baru.
        recentTasksContainer.removeAllViews();

        // Jika tidak ada tugas, tampilkan pesan.
        if (tasks.isEmpty()) {
            TextView emptyView = new TextView(requireContext());
            emptyView.setText("No completed tasks yet"); // "Belum ada tugas yang selesai"
            emptyView.setTextSize(16);
            recentTasksContainer.addView(emptyView);
            return; // Hentikan fungsi di sini.
        }

        // Untuk setiap tugas dalam daftar, buat TextView dan tambahkan ke wadah.
        for (Task task : tasks) {
            TextView taskView = new TextView(requireContext());
            taskView.setText(task.getTitle()); // Tampilkan judul tugas.
            taskView.setTextSize(16);

            // Atur jarak bawah untuk setiap item tugas.
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp

            taskView.setLayoutParams(params);
            recentTasksContainer.addView(taskView); // Tambahkan item tugas ke layar.
        }
    }
}