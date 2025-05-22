package com.example.capstone.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 3;

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
                "start_date TEXT NOT NULL," + // Format: MM/dd/yy HH:mm
                "due_date TEXT NOT NULL," +    // Format: MM/dd/yy HH:mm
                "is_completed INTEGER DEFAULT 0," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                ");";

        // Create trigger for auto-update timestamp
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
        // Handle migration to version 3
        if (oldVersion < 3) {
            // Step 1: Create temporary table
            db.execSQL("CREATE TABLE temp_tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "start_date TEXT NOT NULL," +
                    "due_date TEXT NOT NULL," +
                    "is_completed INTEGER DEFAULT 0," +
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");");

            // Step 2: Copy data to temporary table
            db.execSQL("INSERT INTO temp_tasks (id, title, description, start_date, due_date, is_completed, created_at, updated_at) " +
                    "SELECT id, title, description, start_date, due_date, is_completed, created_at, updated_at FROM tasks;");

            // Step 3: Drop old table
            db.execSQL("DROP TABLE tasks;");

            // Step 4: Rename temporary table
            db.execSQL("ALTER TABLE temp_tasks RENAME TO tasks;");

            // Step 5: Recreate trigger
            String SQL_CREATE_TRIGGER = "CREATE TRIGGER update_task_timestamp " +
                    "AFTER UPDATE ON tasks " +
                    "FOR EACH ROW " +
                    "BEGIN " +
                    "UPDATE tasks SET updated_at = CURRENT_TIMESTAMP WHERE id = OLD.id; " +
                    "END;";

            db.execSQL(SQL_CREATE_TRIGGER);
        }
    }
}