package com.example.capstone.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScheduleDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "schedules.db";
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ScheduleContract.ScheduleEntry.TABLE_NAME + " (" +
                    ScheduleContract.ScheduleEntry.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    ScheduleContract.ScheduleEntry.COLUMN_DAY + " TEXT NOT NULL," +
                    ScheduleContract.ScheduleEntry.COLUMN_TIME + " TEXT NOT NULL," +
                    ScheduleContract.ScheduleEntry.COLUMN_ROOM + " TEXT NOT NULL," +
                    ScheduleContract.ScheduleEntry.COLUMN_COURSE_CODE + " TEXT NOT NULL," +
                    ScheduleContract.ScheduleEntry.COLUMN_COURSE_NAME + " TEXT NOT NULL," +
                    ScheduleContract.ScheduleEntry.COLUMN_LECTURER + " TEXT NOT NULL)";

    public ScheduleDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ScheduleContract.ScheduleEntry.TABLE_NAME);
        onCreate(db);
    }
}