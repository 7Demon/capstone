package com.example.capstone.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.capstone.models.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDao {
    private static final String TAG = "TaskDao";
    private static final SimpleDateFormat TASK_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_ONLY_FORMAT = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

    private SQLiteDatabase db;
    private TaskDbHelper dbHelper;

    public TaskDao(Context context) {
        dbHelper = new TaskDbHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertTask(Task task) {
        if (task == null) {
            Log.e(TAG, "Attempt to insert null task");
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());

        try {
            // Validate and set start date
            if (task.getStartDate() == null || task.getStartDate().isEmpty()) {
                task.setStartDate(TASK_DATE_FORMAT.format(new Date()));
            } else {
                // Validate existing start date format
                TASK_DATE_FORMAT.parse(task.getStartDate());
            }

            // Validate due date
            if (task.getDueDate() == null || task.getDueDate().isEmpty()) {
                Log.e(TAG, "Due date cannot be empty");
                return -1;
            }
            TASK_DATE_FORMAT.parse(task.getDueDate());

        } catch (ParseException e) {
            Log.e(TAG, "Invalid date format in task: " + e.getMessage());
            return -1;
        }

        values.put("start_date", task.getStartDate());
        values.put("due_date", task.getDueDate());
        values.put("is_completed", task.isCompleted() ? 1 : 0);

        try {
            return db.insert("tasks", null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error inserting task: " + e.getMessage());
            return -1;
        }
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        Cursor cursor = null;

        try {
            cursor = db.query("tasks", null, null, null, null, null, "due_date ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Task task = new Task();
                    task.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    task.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow("start_date")));
                    task.setDueDate(cursor.getString(cursor.getColumnIndexOrThrow("due_date")));
                    task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1);

                    tasks.add(task);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tasks: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tasks;
    }
    public List<Task> getTodayTasks() {
        List<Task> tasks = new ArrayList<>();
        Cursor cursor = null;

        try {
            String todayStart = DATE_ONLY_FORMAT.format(new Date()) + " 00:00";
            String todayEnd = DATE_ONLY_FORMAT.format(new Date()) + " 23:59";

            // Hapus filter is_completed untuk mengambil semua task
            String query = "SELECT * FROM tasks " +
                    "WHERE due_date BETWEEN ? AND ? " +
                    "ORDER BY is_completed ASC, due_date ASC"; // Urutkan berdasarkan status

            cursor = db.rawQuery(query, new String[]{todayStart, todayEnd});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Task task = new Task();
                    task.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    task.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow("start_date")));
                    task.setDueDate(cursor.getString(cursor.getColumnIndexOrThrow("due_date")));
                    task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1);

                    tasks.add(task);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting today's tasks: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tasks;
    }

    public int updateTask(Task task) {
        if (task == null) {
            Log.e(TAG, "Attempt to update null task");
            return 0;
        }

        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());

        try {
            // Validate dates before updating
            if (task.getStartDate() != null) {
                TASK_DATE_FORMAT.parse(task.getStartDate());
            }
            if (task.getDueDate() != null) {
                TASK_DATE_FORMAT.parse(task.getDueDate());
            }
        } catch (ParseException e) {
            Log.e(TAG, "Invalid date format in task update: " + e.getMessage());
            return 0;
        }

        values.put("start_date", task.getStartDate());
        values.put("due_date", task.getDueDate());
        values.put("is_completed", task.isCompleted() ? 1 : 0);

        try {
            return db.update("tasks", values, "id = ?", new String[]{String.valueOf(task.getId())});
        } catch (Exception e) {
            Log.e(TAG, "Error updating task: " + e.getMessage());
            return 0;
        }
    }

    public void deleteTask(int taskId) {
        try {
            db.delete("tasks", "id = ?", new String[]{String.valueOf(taskId)});
        } catch (Exception e) {
            Log.e(TAG, "Error deleting task: " + e.getMessage());
        }
    }

    public List<Task> getUpcomingTasks(int daysAhead) {
        List<Task> tasks = new ArrayList<>();
        Cursor cursor = null;

        try {
            String currentDate = DATE_ONLY_FORMAT.format(new Date()) + " 00:00";
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, daysAhead);
            String futureDate = DATE_ONLY_FORMAT.format(calendar.getTime()) + " 23:59";

            String query = "SELECT *, " +
                    "CASE " +
                    "WHEN date(due_date) = date('now') THEN 0 " +
                    "WHEN date(due_date) = date('now', '+1 day') THEN 1 " +
                    "WHEN date(due_date) = date('now', '+2 days') THEN 2 " +
                    "ELSE 3 END AS priority " +
                    "FROM tasks " +
                    "WHERE is_completed = 0 AND due_date BETWEEN ? AND ? " +
                    "ORDER BY priority ASC, due_date ASC";

            cursor = db.rawQuery(query, new String[]{currentDate, futureDate});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Task task = new Task();
                    task.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                    task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
                    task.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow("start_date")));
                    task.setDueDate(cursor.getString(cursor.getColumnIndexOrThrow("due_date")));
                    task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1);

                    tasks.add(task);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting upcoming tasks: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tasks;
    }
}