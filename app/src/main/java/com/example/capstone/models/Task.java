package com.example.capstone.models; // Package tempat class ini berada

// Kelas model Task untuk merepresentasikan entitas tugas dalam aplikasi
public class Task {
    // Properti / Atribut
    private int id; // ID unik untuk setiap task
    private String title; // Judul task
    private String description; // Deskripsi task
    private String startDate; // Tanggal mulai task (format string "05/18/25")
    private String dueDate; // batas waktu task
    private int duration; // Durasi task dalam jumlah hari
    private boolean isCompleted; // Status apakah task sudah selesai

    // Konstruktor

    // Konstruktor kosong (untuk membuat objek tanpa parameter: mengambil dari database)
    public Task() {}

    // Konstruktor untuk membuat task baru (tanpa id dan deskripsi)
    public Task(String title, String startDate, String dueDate, int duration) {
        this.title = title;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.duration = duration;
        this.isCompleted = false; // Default task
    }

    // Getter dan Setter

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
