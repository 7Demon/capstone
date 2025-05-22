package com.example.capstone.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.capstone.models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskDao {
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
        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());

        // Default start date dengan format yang sesuai
        if(task.getStartDate() == null || task.getStartDate().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            task.setStartDate(sdf.format(new Date()));
        }

        values.put("start_date", task.getStartDate());
        values.put("due_date", task.getDueDate());
        values.put("is_completed", task.isCompleted() ? 1 : 0);

        return db.insert("tasks", null, values);
    }

    public List<Task> getAllTasks() {
        List<Task> tasks = new ArrayList<>();
        Cursor cursor = db.query("tasks", null, null, null, null, null, "due_date ASC");

        if (cursor.moveToFirst()) {
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
        cursor.close();
        return tasks;
    }

    public int updateTask(Task task) {
        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("start_date", task.getStartDate());
        values.put("due_date", task.getDueDate());
        values.put("is_completed", task.isCompleted() ? 1 : 0);

        return db.update("tasks", values, "id = ?", new String[]{String.valueOf(task.getId())});
    }

    public void deleteTask(int taskId) {
        db.delete("tasks", "id = ?", new String[]{String.valueOf(taskId)});
    }

    public List<Task> getUpcomingTasks(int daysAhead) {
        List<Task> tasks = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());

        // Set rentang waktu
        String currentDate = sdf.format(new Date()) + " 00:00";

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysAhead);
        String futureDate = sdf.format(calendar.getTime()) + " 23:59";

        String query = "SELECT *, " +
                "CASE " +
                "WHEN date(due_date) = date('now') THEN 0 " +
                "WHEN date(due_date) = date('now', '+1 day') THEN 1 " +
                "WHEN date(due_date) = date('now', '+2 days') THEN 2 " +
                "ELSE 3 END AS priority " +
                "FROM tasks " +
                "WHERE is_completed = 0 AND due_date BETWEEN ? AND ? " +
                "ORDER BY priority ASC, due_date ASC";

        Cursor cursor = db.rawQuery(query, new String[]{currentDate, futureDate});

        if (cursor.moveToFirst()) {
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
        cursor.close();
        return tasks;
    }
}