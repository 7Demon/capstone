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
        scheduleDao = new ScheduleDao(requireContext());
        scheduleDao.open();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scheduleDao.close();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        scheduleContainer = view.findViewById(R.id.scheduleContainer);

        // Tombol Add
        Button btnAddSchedule = view.findViewById(R.id.btnAddSchedule);
        btnAddSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddScheduleDialog();
            }
        });

        // Tombol Back
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().onBackPressed();
            }
        });

        loadSchedules();
        return view;
    }



    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Tambah Jadwal");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);

        TextInputEditText etDay = dialogView.findViewById(R.id.etDay);
        TextInputEditText etTime = dialogView.findViewById(R.id.etTime);
        TextInputEditText etRoom = dialogView.findViewById(R.id.etRoom);
        TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        TextInputEditText etLecturer = dialogView.findViewById(R.id.etLecturer);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String day = etDay.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String courseCode = etCourseCode.getText().toString().trim();
            String courseName = etCourseName.getText().toString().trim();
            String lecturer = etLecturer.getText().toString().trim();

            if (!day.isEmpty() && !time.isEmpty() && !room.isEmpty() &&
                    !courseCode.isEmpty() && !courseName.isEmpty() && !lecturer.isEmpty()) {
                Schedule newSchedule = new Schedule();
                newSchedule.setDay(day);
                newSchedule.setTime(time);
                newSchedule.setRoom(room);
                newSchedule.setCourseCode(courseCode);
                newSchedule.setCourseName(courseName);
                newSchedule.setLecturer(lecturer);
                scheduleDao.insertSchedule(newSchedule);
                loadSchedules();
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.create().show();
    }

    private void loadSchedules() {
        scheduleContainer.removeViews(1, scheduleContainer.getChildCount() - 1);
        List<Schedule> schedules = scheduleDao.getAllSchedules();
        scheduleCount = 0;

        // Tentukan urutan hari
        List<String> dayOrder = List.of("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu");

        // Kelompokkan jadwal berdasarkan hari
        Map<String, List<Schedule>> scheduleMap = new HashMap<>();
        for (Schedule schedule : schedules) {
            String day = schedule.getDay();
            if (!scheduleMap.containsKey(day)) {
                scheduleMap.put(day, new ArrayList<>());
            }
            scheduleMap.get(day).add(schedule);
        }

        // Tampilkan jadwal sesuai urutan hari yang telah ditentukan
        for (String day : dayOrder) {
            if (scheduleMap.containsKey(day)) {
                addDayHeader(day);
                addTableHeader();
                for (Schedule schedule : scheduleMap.get(day)) {
                    addScheduleItem(schedule);
                }
            }
        }
    }


    private void addScheduleItem(Schedule schedule) {
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

        scheduleItem.addView(createTextView("0.5", String.valueOf(++scheduleCount) + "."));
        scheduleItem.addView(createTextView("1.5", schedule.getTime()));
        scheduleItem.addView(createTextView("1.5", schedule.getRoom()));
        scheduleItem.addView(createTextView("1.5", schedule.getCourseCode()));
        scheduleItem.addView(createTextView("1.5", schedule.getCourseName()));
        scheduleItem.addView(createTextView("1.5", schedule.getLecturer()));

        LinearLayout actionLayout = new LinearLayout(requireContext());
        actionLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.5f
        );
        actionLayout.setLayoutParams(actionParams);

        // Tombol Edit (Text)
        TextView btnEdit = new TextView(requireContext());
        btnEdit.setText("EDIT");
        btnEdit.setTextColor(Color.BLUE);
        btnEdit.setTextSize(14);
        btnEdit.setPadding(0, dpToPx(5), 0, dpToPx(5));
        btnEdit.setOnClickListener(v -> showEditScheduleDialog(schedule));
        actionLayout.addView(btnEdit);

        // Tombol Delete (Icon)
        TextView btnDelete = new TextView(requireContext());
        btnDelete.setText("DELETE");
        btnDelete.setTextColor(Color.RED);
        btnDelete.setTextSize(14);
        btnDelete.setPadding(0, dpToPx(5), 0, dpToPx(5));
//        ImageButton btnDelete = new ImageButton(requireContext());
//        btnDelete.setImageResource(android.R.drawable.ic_delete);
//        btnDelete.setBackgroundColor(Color.TRANSPARENT);
//        btnDelete.setColorFilter(Color.RED);
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(schedule));
        actionLayout.addView(btnDelete);

        scheduleItem.addView(actionLayout);
        scheduleContainer.addView(scheduleItem);
    }

    private void showEditScheduleDialog(Schedule schedule) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Jadwal");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);

        TextInputEditText etDay = dialogView.findViewById(R.id.etDay);
        TextInputEditText etTime = dialogView.findViewById(R.id.etTime);
        TextInputEditText etRoom = dialogView.findViewById(R.id.etRoom);
        TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        TextInputEditText etLecturer = dialogView.findViewById(R.id.etLecturer);

        etDay.setText(schedule.getDay());
        etTime.setText(schedule.getTime());
        etRoom.setText(schedule.getRoom());
        etCourseCode.setText(schedule.getCourseCode());
        etCourseName.setText(schedule.getCourseName());
        etLecturer.setText(schedule.getLecturer());

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String day = etDay.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String courseCode = etCourseCode.getText().toString().trim();
            String courseName = etCourseName.getText().toString().trim();
            String lecturer = etLecturer.getText().toString().trim();

            if (!day.isEmpty() && !time.isEmpty() && !room.isEmpty() &&
                    !courseCode.isEmpty() && !courseName.isEmpty() && !lecturer.isEmpty()) {
                schedule.setDay(day);
                schedule.setTime(time);
                schedule.setRoom(room);
                schedule.setCourseCode(courseCode);
                schedule.setCourseName(courseName);
                schedule.setLecturer(lecturer);
                scheduleDao.updateSchedule(schedule);
                loadSchedules();
            }
        });

        builder.setNegativeButton("Batal", null);
        builder.create().show();
    }

    private void showDeleteConfirmationDialog(Schedule schedule) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Hapus")
                .setMessage("Apakah Anda yakin ingin menghapus jadwal ini?")
                .setPositiveButton("Hapus", (dialog, which) -> {
                    scheduleDao.deleteSchedule(schedule.getId());
                    loadSchedules();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private TextView createTextView(String weight, String text) {
        TextView textView = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                Float.parseFloat(weight)
        );
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        textView.setTextColor(Color.BLACK);
        return textView;
    }

    private void addDayHeader(String day) {
        TextView dayHeader = new TextView(requireContext());
        dayHeader.setText(day);
        dayHeader.setTextSize(18);
        dayHeader.setTypeface(null, Typeface.BOLD);
        dayHeader.setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(4));
        scheduleContainer.addView(dayHeader);
    }

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

    private TextView createHeaderTextView(String weight, String text) {
        TextView textView = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                Float.parseFloat(weight)
        );
        textView.setLayoutParams(params);
        textView.setText(text);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        textView.setTextColor(Color.BLACK);
        return textView;
    }
}
