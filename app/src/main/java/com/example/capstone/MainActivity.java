package com.example.capstone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;


    // 3 Untuk 8 kali sehari
    // REPEAT_INTERVAL untuk mengatur jeda antar notifikasi.
    private static final long REPEAT_INTERVAL = 3;
    private static final TimeUnit TIME_UNIT = TimeUnit.HOURS; // TimeUnit.MINUTES



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new HomeFragment())
                    .commit();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_reminder) {
                selectedFragment = new ReminderFragment();
            } else if (itemId == R.id.nav_stats) {
                selectedFragment = new StatsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        requestNotificationPermission();

        // Panggil fungsi penjadwalan yang baru
        scheduleRepeatingReminderWorker();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Fungsi untuk menjadwalkan pengingat berulang dengan interval yang fleksibel.
     */
    private void scheduleRepeatingReminderWorker() {
        // Buat permintaan kerja WorkManager yang berjalan secara periodik
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(TaskReminderWorker.class, REPEAT_INTERVAL, TIME_UNIT)
                .build();

        // Enqueue pekerjaan unik dengan nama "RepeatingTaskReminder".
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "RepeatingTaskReminder",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );
    }
}