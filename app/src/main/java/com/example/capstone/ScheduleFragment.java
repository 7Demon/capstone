

// ScheduleFragment.java
package com.example.capstone;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import android.util.Log;

public class ScheduleFragment extends Fragment {

    private static final String TAG = "ScheduleFragment";
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        initUI(view);
        loadSchedules();
        return view;
    }

    private void initUI(View view) {
        try {
            scheduleContainer = view.findViewById(R.id.scheduleContainer);
            view.findViewById(R.id.btnAddSchedule).setOnClickListener(v -> showAddScheduleDialog());

            // Tombol back menggunakan OnBackPressedDispatcher
            view.findViewById(R.id.backButton).setOnClickListener(v ->
                    requireActivity().getOnBackPressedDispatcher().onBackPressed());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI: " + e.getMessage());
        }
    }

    private void loadSchedules() {
        scheduleContainer.removeViews(1, scheduleContainer.getChildCount() - 1); // Simpan header utama
        List<Schedule> schedules = scheduleDao.getAllSchedules();
        scheduleCount = schedules.size();

        // Kelompokkan jadwal berdasarkan hari
        Map<String, List<Schedule>> scheduleMap = new HashMap<>();
        for (Schedule schedule : schedules) {
            String day = schedule.getDay();
            if (!scheduleMap.containsKey(day)) {
                scheduleMap.put(day, new ArrayList<>());
            }
            scheduleMap.get(day).add(schedule);
        }

        // Tampilkan jadwal per hari
        for (Map.Entry<String, List<Schedule>> entry : scheduleMap.entrySet()) {
            addDayHeader(entry.getKey());
            addTableHeader();
            for (Schedule schedule : entry.getValue()) {
                addScheduleItem(schedule);
            }
        }
    }

    private void addDayHeader(String day) {
        TextView dayHeader = new TextView(requireContext());
        dayHeader.setText(day);
        dayHeader.setTextSize(16);
        dayHeader.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(16), 0, dpToPx(8));
        scheduleContainer.addView(dayHeader);
    }

    private void addTableHeader() {
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(getResources().getColor(R.color.light_gray));
        header.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        header.setLayoutParams(params);

        header.addView(createHeaderTextView("0.5", "No."));
        header.addView(createHeaderTextView("1.5", "Jam-Ke"));
        header.addView(createHeaderTextView("1.5", "Ruang"));
        header.addView(createHeaderTextView("1.5", "MK"));
        header.addView(createHeaderTextView("1.5", "Kelas"));
        header.addView(createHeaderTextView("2", "Dosen Pengampu"));

        scheduleContainer.addView(header);
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
        scheduleItem.addView(createTextView("2", schedule.getLecturer()));

        scheduleContainer.addView(scheduleItem);
    }

    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Schedule");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);

        TextInputEditText etDay = dialogView.findViewById(R.id.etDay);
        TextInputEditText etTime = dialogView.findViewById(R.id.etTime);
        TextInputEditText etRoom = dialogView.findViewById(R.id.etRoom);
        TextInputEditText etCourseCode = dialogView.findViewById(R.id.etCourseCode);
        TextInputEditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        TextInputEditText etLecturer = dialogView.findViewById(R.id.etLecturer);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String day = etDay.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String courseCode = etCourseCode.getText().toString().trim();
            String courseName = etCourseName.getText().toString().trim();
            String lecturer = etLecturer.getText().toString().trim();

            if (!day.isEmpty() && !time.isEmpty() && !room.isEmpty() &&
                    !courseCode.isEmpty() && !courseName.isEmpty() && !lecturer.isEmpty()) {

                Schedule newSchedule = new Schedule(day, time, room, courseCode, courseName, lecturer);
                scheduleDao.insertSchedule(newSchedule);
                loadSchedules(); // Refresh tampilan
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
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
        return textView;
    }

    private TextView createHeaderTextView(String weight, String text) {
        TextView textView = createTextView(weight, text);
        textView.setTypeface(null, Typeface.BOLD);
        return textView;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
