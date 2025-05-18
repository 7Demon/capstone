package com.example.capstone.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "tasks.db"; // Nama database
    private static final int DATABASE_VERSION = 1; // Versi database

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Buat tabel tasks
        final String SQL_CREATE_TASKS_TABLE = "CREATE TABLE tasks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," + // ID auto increment
                "title TEXT NOT NULL," + // Judul tugas
                "description TEXT," + // Deskripsi (opsional)
                "start_date TEXT NOT NULL," + // Tanggal mulai
                "due_date TEXT NOT NULL," + // Tanggal selesai
                "duration INTEGER," + // Durasi (dalam hari)
                "is_completed INTEGER DEFAULT 0," + // Status selesai
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," + // Timestamp dibuat
                "updated_at TEXT DEFAULT CURRENT_TIMESTAMP" + // Timestamp diupdate
                ");";

        // Buat trigger untuk update kolom updated_at setiap data diubah
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
        // Hapus tabel & trigger lama saat upgrade
        db.execSQL("DROP TABLE IF EXISTS tasks");
        db.execSQL("DROP TRIGGER IF EXISTS update_task_timestamp");
        onCreate(db); // Buat ulang tabel
    }
}
