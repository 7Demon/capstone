package com.example.capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReminderFragment extends Fragment {

    public ReminderFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reminder, container, false);

        // 1. Setup Calendar
        setupCalendar(view, inflater);

        // 2. Setup Reminder List
        setupReminders(view);

        return view;
    }

    private void setupCalendar(View view, LayoutInflater inflater) {
        LinearLayout calendarRow = view.findViewById(R.id.calendarRow);
        calendarRow.removeAllViews();

        TextView dateHeader = view.findViewById(R.id.dateHeader);
        SimpleDateFormat headerFormat = new SimpleDateFormat("EEEE, d", Locale.getDefault());
        dateHeader.setText(headerFormat.format(Calendar.getInstance().getTime()));

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DATE, -1);
        }

        for (int i = 0; i < 7; i++) {
            View dayView = createDayView(calendar, inflater);
            calendarRow.addView(dayView);
            calendar.add(Calendar.DATE, 1);
        }
    }

    private void setupReminders(View view) {
        LinearLayout reminderList = view.findViewById(R.id.reminderList);
        reminderList.removeAllViews(); // Clear existing views if any

        for (int i = 1; i <= 5; i++) {
            TextView reminderItem = new TextView(getContext());
            reminderItem.setText("Reminder " + i);
            reminderItem.setPadding(24, 24, 24, 24);
            reminderItem.setBackgroundColor(0xFFD9D9D9);
            reminderItem.setTextColor(0xFF000000);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 24);
            reminderItem.setLayoutParams(params);

            reminderList.addView(reminderItem);
        }
    }

    private View createDayView(Calendar calendar, LayoutInflater inflater) {
        View dayView = inflater.inflate(R.layout.item_day, null);

        TextView dayName = dayView.findViewById(R.id.dayName);
        TextView dayNumber = dayView.findViewById(R.id.dayNumber);

        SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.getDefault());
        dayName.setText(dayFormat.format(calendar.getTime()));

        SimpleDateFormat numberFormat = new SimpleDateFormat("d", Locale.getDefault());
        dayNumber.setText(numberFormat.format(calendar.getTime()));

        return dayView;
    }
}