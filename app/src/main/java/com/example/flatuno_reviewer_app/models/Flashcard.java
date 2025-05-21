package com.example.flatuno_reviewer_app.models;

public class Flashcard {
    private long id;
    private long topicId;
    private String term;
    private String description;
    private long createdAt;
    private long lastReviewed;
    private String color;  // Color inherited from topic

    public Flashcard(long topicId, String term, String description) {
        this.topicId = topicId;
        this.term = term;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.lastReviewed = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getTopicId() { return topicId; }
    public void setTopicId(long topicId) { this.topicId = topicId; }
    
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(long lastReviewed) { this.lastReviewed = lastReviewed; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
} 