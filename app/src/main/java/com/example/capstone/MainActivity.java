package com.example.capstone;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mengaktifkan mode edge-to-edge untuk tampilan layar penuh (memanfaatkan seluruh layar)
        EdgeToEdge.enable(this);

        // Menetapkan layout XML activity_main sebagai tampilan utama activity ini
        setContentView(R.layout.activity_main);

        // Menyesuaikan padding layout dengan area sistem (status bar, navigation bar) agar tidak tertutup
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Jika activity baru saja dibuat (tidak direstore), tampilkan fragment awal (HomeFragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new HomeFragment()) // Mengganti container dengan HomeFragment
                    .commit();
        }

        // Inisialisasi BottomNavigationView untuk navigasi antar-fragment
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);

//        // Listener untuk menangani perubahan pilihan item di BottomNavigationView
//        bottomNav.setOnItemSelectedListener(item -> {
//            Fragment selectedFragment = null;
//            int itemId = item.getItemId(); // Ambil ID item yang diklik
//
//            // Tentukan fragment yang akan ditampilkan berdasarkan item yang dipilih
//            if (itemId == R.id.nav_home) {
//                selectedFragment = new HomeFragment();
//            } else if (itemId == R.id.nav_reminder) {
//                selectedFragment = new ReminderFragment();
//            }
//
//            // Jika fragment berhasil dipilih, tampilkan dalam container
//            if (selectedFragment != null) {
//                getSupportFragmentManager().beginTransaction()
//                        .replace(R.id.fragmentContainerView, selectedFragment)
//                        .commit();
//                return true; // Kembalikan true untuk menunjukkan event dikonsumsi
//            }
//            return false; // Jika tidak ada fragment yang dipilih, kembalikan false
//        });

        //  Listener untuk menangani perubahan pilihan item di BottomNavigationView
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_reminder) {
                selectedFragment = new ReminderFragment();
            } else if (itemId == R.id.nav_stats) {
                selectedFragment = new StatsFragment(); // Fragment tambahan untuk statistik
            }
            // Jika fragment berhasil dipilih, tampilkan dalam container
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainerView, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}
