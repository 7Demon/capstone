package com.example.capstone.db;

public final class ScheduleContract {
    private ScheduleContract() {}

    public static class ScheduleEntry {
        public static final String TABLE_NAME = "schedules";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_DAY = "day";
        public static final String COLUMN_TIME = "time";
        public static final String COLUMN_ROOM = "room";
        public static final String COLUMN_COURSE_CODE = "course_code";
        public static final String COLUMN_COURSE_NAME = "course_name";
        public static final String COLUMN_LECTURER = "lecturer";
    }
}