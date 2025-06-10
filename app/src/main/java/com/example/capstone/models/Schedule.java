package com.example.capstone.models;

public class Schedule {
    private int id;
    private String day;
    private String time;
    private String room;
    private String courseCode;
    private String courseName;
    private String lecturer;

    public Schedule() {}

    public Schedule(String day, String time, String room, String courseCode, String courseName, String lecturer) {
        this.day = day;
        this.time = time;
        this.room = room;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.lecturer = lecturer;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getLecturer() { return lecturer; }
    public void setLecturer(String lecturer) { this.lecturer = lecturer; }
}