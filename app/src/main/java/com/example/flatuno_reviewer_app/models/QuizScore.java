package com.example.flatuno_reviewer_app.models;

public class QuizScore {
    private long id;
    private long quizId;
    private int score;
    private int totalQuestions;
    private long takenAt;
    private long timeSpent; // in milliseconds

    public QuizScore(long quizId, int score, int totalQuestions, long timeSpent) {
        this.quizId = quizId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.takenAt = System.currentTimeMillis();
        this.timeSpent = timeSpent;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getQuizId() { return quizId; }
    public void setQuizId(long quizId) { this.quizId = quizId; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public long getTakenAt() { return takenAt; }
    public void setTakenAt(long takenAt) { this.takenAt = takenAt; }
    
    public long getTimeSpent() { return timeSpent; }
    public void setTimeSpent(long timeSpent) { this.timeSpent = timeSpent; }

    // Helper method to get score percentage
    public double getScorePercentage() {
        return (double) score / totalQuestions * 100;
    }
} 