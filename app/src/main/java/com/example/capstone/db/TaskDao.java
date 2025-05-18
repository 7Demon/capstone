package com.example.capstone.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.capstone.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskDao {
    private SQLiteDatabase db;
    private TaskDbHelper dbHelper;

    public TaskDao(Context context) {
        dbHelper = new TaskDbHelper(context);
    }

    // Buka koneksi ke database
    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    // Tutup koneksi ke database
    public void close() {
        dbHelper.close();
    }

    // Tambah data tugas (Create)
    public long insertTask(Task task) {
        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("start_date", task.getStartDate());
        values.put("due_date", task.getDueDate());
        values.put("duration", task.getDuration());
        values.put("is_completed", task.isCompleted() ? 1 : 0);

        return db.insert("tasks", null, values);
    }

    // Ambil semua data tugas (Read)
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
                task.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow("duration")));
                task.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow("is_completed")) == 1);

                tasks.add(task);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return tasks;
    }

    // Update data tugas
    public int updateTask(Task task) {
        ContentValues values = new ContentValues();
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("start_date", task.getStartDate());
        values.put("due_date", task.getDueDate());
        values.put("duration", task.getDuration());
        values.put("is_completed", task.isCompleted() ? 1 : 0);

        return db.update("tasks", values, "id = ?", new String[]{String.valueOf(task.getId())});
    }

    // Hapus data tugas berdasarkan ID
    public void deleteTask(int taskId) {
        db.delete("tasks", "id = ?", new String[]{String.valueOf(taskId)});
    }
}
