package com.example.capstone;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.capstone.db.ScheduleDao;
import com.example.capstone.models.Schedule;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ScheduleFragment extends Fragment {


    private LinearLayout scheduleContainer;
    private ScheduleDao scheduleDao;
    private int scheduleCount = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Persiapkan akses ke database.
        scheduleDao = new ScheduleDao(requireContext());
        scheduleDao.open();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Tutup koneksi database untuk menghindari error.
        scheduleDao.close();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Hubungkan kode ini dengan file layoutnya (fragment_schedule.xml).
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        // Kenali komponen UI dari layout.
        scheduleContainer = view.findViewById(R.id.scheduleContainer);

        // Atur aksi untuk tombol "Tambah Jadwal".
        Button btnAddSchedule = view.findViewById(R.id.btnAddSchedule);
        btnAddSchedule.setOnClickListener(v -> showAddScheduleDialog());

        // Atur aksi untuk tombol "Kembali".
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Langsung tampilkan data jadwal saat fragment dibuka.
        loadSchedules();
        return view;
    }


    // Menampilkan pop-up untuk menambah jadwal baru.
    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Tambah Jadwal");

        // Pakai layout custom untuk isi pop-up.
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);

        // Kenali semua kolom input di pop-up.
        final TextInputEditText etDay = dialogView.findViewById(R.id.etDay);
        final TextInputEditText etTime = dialogView.findViewById(R.id.etTime);
        final TextInputEditText etRoom = dialogView.findViewById(R.id.etRoom);
        final TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        final TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        final TextInputEditText etLecturer = dialogView.findViewById(R.id.etLecturer);

        // Aksi saat tombol "Simpan" di pop-up ditekan.
        builder.setPositiveButton("Simpan", (dialog, which) -> {
            // Ambil semua teks dari kolom input.
            String day = etDay.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String courseCode = etCourseCode.getText().toString().trim();
            String courseName = etCourseName.getText().toString().trim();
            String lecturer = etLecturer.getText().toString().trim();

            // Pastikan semua kolom sudah diisi.
            if (!day.isEmpty() && !time.isEmpty() && !room.isEmpty() &&
                    !courseCode.isEmpty() && !courseName.isEmpty() && !lecturer.isEmpty()) {

                // Buat objek jadwal baru dari data input.
                Schedule newSchedule = new Schedule();
                newSchedule.setDay(day);
                newSchedule.setTime(time);
                newSchedule.setRoom(room);
                newSchedule.setCourseCode(courseCode);
                newSchedule.setCourseName(courseName);
                newSchedule.setLecturer(lecturer);

                // Simpan jadwal baru ke database.
                scheduleDao.insertSchedule(newSchedule);

                // Tampilkan ulang daftar jadwal agar data baru muncul.
                loadSchedules();
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.create().show();
    }

    // Mengambil data dari database dan menampilkannya ke layar.
    private void loadSchedules() {
        // Hapus daftar lama sebelum menampilkan yang baru.
        if (scheduleContainer.getChildCount() > 1) {
            scheduleContainer.removeViews(1, scheduleContainer.getChildCount() - 1);
        }

        // Ambil semua data jadwal dari database.
        List<Schedule> schedules = scheduleDao.getAllSchedules();
        scheduleCount = 0; // Reset nomor urut.

        // Tentukan urutan hari yang benar.
        List<String> dayOrder = List.of("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu");

        // Kelompokkan jadwal berdasarkan harinya.
        Map<String, List<Schedule>> scheduleMap = new HashMap<>();
        for (Schedule schedule : schedules) {
            String day = schedule.getDay();
            if (!scheduleMap.containsKey(day)) {
                scheduleMap.put(day, new ArrayList<>());
            }
            scheduleMap.get(day).add(schedule);
        }

        // Tampilkan jadwal per hari sesuai urutan yang benar.
        for (String day : dayOrder) {
            if (scheduleMap.containsKey(day)) {
                addDayHeader(day); // Judul hari, cth: "Senin"
                addTableHeader();  // Judul kolom, cth: "Waktu", "Ruangan"
                for (Schedule schedule : scheduleMap.get(day)) {
                    addScheduleItem(schedule); // Tambah satu baris jadwal
                }
            }
        }
    }


    // Membuat satu baris tampilan untuk satu jadwal.
    private void addScheduleItem(Schedule schedule) {
        // Buat layout untuk satu baris.
        LinearLayout scheduleItem = new LinearLayout(requireContext());
        scheduleItem.setOrientation(LinearLayout.HORIZONTAL);
        scheduleItem.setBackgroundColor(getResources().getColor(android.R.color.white));
        scheduleItem.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(1), 0, 0);
        scheduleItem.setLayoutParams(params);

        // Tambah teks untuk setiap kolom di baris itu.
        scheduleItem.addView(createTextView("0.5", String.valueOf(++scheduleCount) + "."));
        scheduleItem.addView(createTextView("1.5", schedule.getTime()));
        scheduleItem.addView(createTextView("1.5", schedule.getRoom()));
        scheduleItem.addView(createTextView("1.5", schedule.getCourseCode()));
        scheduleItem.addView(createTextView("1.5", schedule.getCourseName()));
        scheduleItem.addView(createTextView("1.5", schedule.getLecturer()));

        // Buat wadah untuk tombol "EDIT" dan "DELETE".
        LinearLayout actionLayout = new LinearLayout(requireContext());
        actionLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f
        );
        actionLayout.setLayoutParams(actionParams);

        // Tombol Edit.
        TextView btnEdit = new TextView(requireContext());
        btnEdit.setText("EDIT");
        btnEdit.setTextColor(Color.BLUE);
        btnEdit.setOnClickListener(v -> showEditScheduleDialog(schedule));
        actionLayout.addView(btnEdit);

        // Tombol Delete.
        TextView btnDelete = new TextView(requireContext());
        btnDelete.setText("DELETE");
        btnDelete.setTextColor(Color.RED);
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(schedule));
        actionLayout.addView(btnDelete);

        // Masukkan tombol-tombol ke dalam baris.
        scheduleItem.addView(actionLayout);

        // Masukkan baris yang sudah lengkap ke daftar utama.
        scheduleContainer.addView(scheduleItem);
    }

    // Menampilkan pop-up untuk mengedit jadwal yang sudah ada.
    private void showEditScheduleDialog(Schedule schedule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Jadwal");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);

        final TextInputEditText etDay = dialogView.findViewById(R.id.etDay);
        final TextInputEditText etTime = dialogView.findViewById(R.id.etTime);
        final TextInputEditText etRoom = dialogView.findViewById(R.id.etRoom);
        final TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        final TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        final TextInputEditText etLecturer = dialogView.findViewById(R.id.etLecturer);

        // Isi kolom input dengan data yang sudah ada.
        etDay.setText(schedule.getDay());
        etTime.setText(schedule.getTime());
        etRoom.setText(schedule.getRoom());
        etCourseCode.setText(schedule.getCourseCode());
        etCourseName.setText(schedule.getCourseName());
        etLecturer.setText(schedule.getLecturer());

        // Aksi saat tombol "Simpan" ditekan.
        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String day = etDay.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String courseCode = etCourseCode.getText().toString().trim();
            String courseName = etCourseName.getText().toString().trim();
            String lecturer = etLecturer.getText().toString().trim();

            if (!day.isEmpty() && !time.isEmpty() && !room.isEmpty() &&
                    !courseCode.isEmpty() && !courseName.isEmpty() && !lecturer.isEmpty()) {

                // Perbarui data di objek jadwal yang ada.
                schedule.setDay(day);
                schedule.setTime(time);
                schedule.setRoom(room);
                schedule.setCourseCode(courseCode);
                schedule.setCourseName(courseName);
                schedule.setLecturer(lecturer);

                // Simpan perubahan ke database.
                scheduleDao.updateSchedule(schedule);

                // Refresh tampilan.
                loadSchedules();
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.create().show();
    }

    // Menampilkan pop-up konfirmasi sebelum menghapus.
    private void showDeleteConfirmationDialog(Schedule schedule) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus jadwal ini?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    // Hapus jadwal dari database.
                    scheduleDao.deleteSchedule(schedule.getId());
                    // Refresh tampilan.
                    loadSchedules();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // --- FUNGSI BANTU ---

    // Mengubah satuan dp ke pixel.
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Membuat komponen teks (TextView) untuk sel tabel.
    private TextView createTextView(String weight, String text) {
        TextView textView = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, Float.parseFloat(weight)
        );
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        textView.setTextColor(Color.BLACK);
        return textView;
    }

    // Menambah judul hari (cth: "Senin").
    private void addDayHeader(String day) {
        TextView dayHeader = new TextView(requireContext());
        dayHeader.setText(day);
        dayHeader.setTextSize(18);
        dayHeader.setTypeface(null, Typeface.BOLD);
        dayHeader.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(4));
        scheduleContainer.addView(dayHeader);
    }

    // Menambah baris judul kolom tabel.
    private void addTableHeader() {
        LinearLayout headerLayout = new LinearLayout(requireContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(Color.LTGRAY);
        headerLayout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        headerLayout.addView(createHeaderTextView("0.5", "No."));
        headerLayout.addView(createHeaderTextView("1.5", "Waktu"));
        headerLayout.addView(createHeaderTextView("1.5", "Ruangan"));
        headerLayout.addView(createHeaderTextView("1.5", "Kode"));
        headerLayout.addView(createHeaderTextView("1.5", "Matkul"));
        headerLayout.addView(createHeaderTextView("1.5", "Dosen"));
        headerLayout.addView(createHeaderTextView("1.5", "Aksi"));

        scheduleContainer.addView(headerLayout);
    }

    // Membuat komponen teks (TextView) untuk judul kolom (dibuat tebal).
    private TextView createHeaderTextView(String weight, String text) {
        TextView textView = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, Float.parseFloat(weight)
        );
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        textView.setTextColor(Color.BLACK);
        return textView;
    }
}