package com.example.capstone.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 1;

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tasks table
        final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "description TEXT," +
                "start_date TEXT NOT NULL," +
                "due_date TEXT NOT NULL," +
                "duration INTEGER," +
                "is_completed INTEGER DEFAULT 0," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // Create trigger for auto-updating timestamp
        final String SQL_CREATE_TRIGGER = "CREATE TRIGGER update_task_timestamp " +
                "AFTER UPDATE ON tasks " +
                "FOR EACH ROW " +
                "BEGIN " +
                "UPDATE tasks SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id; " +
                "END;";

        db.execSQL(SQL_CREATE_TASKS_TABLE);
        db.execSQL(SQL_CREATE_TRIGGER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tasks");
        db.execSQL("DROP TRIGGER IF EXISTS update_task_timestamp");
        onCreate(db);
    }
}