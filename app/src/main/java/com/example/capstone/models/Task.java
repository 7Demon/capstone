package com.example.capstone.models;

public class Task {
    private int id;
    private String title;
    private String description;
    private String startDate;  // Format: "MM/dd/yy HH:mm"
    private String dueDate;    // Format: "MM/dd/yy HH:mm"
    private boolean isCompleted;

    // Constructor kosong (untuk database)
    public Task() {}

    // Constructor untuk membuat task baru (tanpa duration)
    public Task(String title, String startDate, String dueDate) {
        this.title = title;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.isCompleted = false;
    }

    // Getter & Setter
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

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}