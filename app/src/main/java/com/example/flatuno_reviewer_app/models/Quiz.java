package com.example.flatuno_reviewer_app.models;

public class Quiz {
    private long id;
    private String title;
    private long topicId;
    private long createdAt;
    private long lastModified;
    private int questionCount;
    private String lastScore;
    private String topicName;

    public Quiz(long id, String title, long topicId, long createdAt, long lastModified) {
        this.id = id;
        this.title = title;
        this.topicId = topicId;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
        this.questionCount = 0;
        this.lastScore = "Not attempted yet";
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getTopicId() { return topicId; }
    public void setTopicId(long topicId) { this.topicId = topicId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public String getLastScore() { return lastScore; }
    public void setLastScore(String lastScore) { this.lastScore = lastScore; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }
} 