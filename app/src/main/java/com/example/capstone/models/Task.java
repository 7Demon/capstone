package com.example.capstone.models;

public class Task {
    private int id;
    private String title;
    private String description;
    private String startDate;
    private String dueDate;
    private int duration;
    private boolean isCompleted;

    // Constructors
    public Task() {}

    public Task(String title, String startDate, String dueDate, int duration) {
        this.title = title;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.duration = duration;
        this.isCompleted = false;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}