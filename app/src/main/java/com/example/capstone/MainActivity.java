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

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1;

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

        // Tampilkan fragment awal
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new HomeFragment())
                    .commit();
        }

        // Setup bottom navigation
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

        // Minta izin notifikasi jika perlu
        requestNotificationPermission();

        // Jadwalkan background worker harian
        scheduleDailyReminderWorker();
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

    private void scheduleDailyReminderWorker() {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, 7); // waktu harian untuk reminder
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long delay = target.getTimeInMillis() - now.getTimeInMillis();
        if (delay < 0) {
            delay += TimeUnit.DAYS.toMillis(1); // jika sudah lewat jam 7 pagi, jadwalkan besok
        }

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(TaskReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DailyTaskReminder",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}
