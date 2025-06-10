package com.example.capstone.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.capstone.models.Schedule;

import java.util.ArrayList;
import java.util.List;

public class ScheduleDao {
    private SQLiteDatabase db;
    private ScheduleDbHelper dbHelper;

    public ScheduleDao(Context context) {
        dbHelper = new ScheduleDbHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertSchedule(Schedule schedule) {
        ContentValues values = new ContentValues();
        values.put(ScheduleContract.ScheduleEntry.COLUMN_DAY, schedule.getDay());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_TIME, schedule.getTime());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_ROOM, schedule.getRoom());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_COURSE_CODE, schedule.getCourseCode());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_COURSE_NAME, schedule.getCourseName());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_LECTURER, schedule.getLecturer());

        return db.insert(ScheduleContract.ScheduleEntry.TABLE_NAME, null, values);
    }

    public List<Schedule> getAllSchedules() {
        List<Schedule> schedules = new ArrayList<>();
        Cursor cursor = db.query(
                ScheduleContract.ScheduleEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                ScheduleContract.ScheduleEntry.COLUMN_DAY + " ASC"
        );

        if (cursor.moveToFirst()) {
            do {
                Schedule schedule = new Schedule();
                schedule.setId(cursor.getInt(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_ID)));
                schedule.setDay(cursor.getString(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_DAY)));
                schedule.setTime(cursor.getString(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_TIME)));
                schedule.setRoom(cursor.getString(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_ROOM)));
                schedule.setCourseCode(cursor.getString(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_COURSE_CODE)));
                schedule.setCourseName(cursor.getString(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_COURSE_NAME)));
                schedule.setLecturer(cursor.getString(cursor.getColumnIndexOrThrow(ScheduleContract.ScheduleEntry.COLUMN_LECTURER)));

                schedules.add(schedule);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return schedules;
    }

    public int updateSchedule(Schedule schedule) {
        ContentValues values = new ContentValues();
        values.put(ScheduleContract.ScheduleEntry.COLUMN_DAY, schedule.getDay());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_TIME, schedule.getTime());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_ROOM, schedule.getRoom());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_COURSE_CODE, schedule.getCourseCode());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_COURSE_NAME, schedule.getCourseName());
        values.put(ScheduleContract.ScheduleEntry.COLUMN_LECTURER, schedule.getLecturer());

        return db.update(
                ScheduleContract.ScheduleEntry.TABLE_NAME,
                values,
                ScheduleContract.ScheduleEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(schedule.getId())}
        );
    }

    public void deleteSchedule(int scheduleId) {
        db.delete(
                ScheduleContract.ScheduleEntry.TABLE_NAME,
                ScheduleContract.ScheduleEntry.COLUMN_ID + " = ?",
                new String[]{String.valueOf(scheduleId)}
        );
    }
}