package com.example.flatuno_reviewer_app.models;

public class Topic {
    private long id;
    private String name;
    private String color;
    private long createdAt;
    private long lastModified;
    private int cardCount;

    public Topic(String name, String color) {
        this.name = name;
        this.color = color;
        this.createdAt = System.currentTimeMillis();
        this.lastModified = System.currentTimeMillis();
        this.cardCount = 0;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public int getCardCount() { return cardCount; }
    public void setCardCount(int cardCount) { this.cardCount = cardCount; }
} 