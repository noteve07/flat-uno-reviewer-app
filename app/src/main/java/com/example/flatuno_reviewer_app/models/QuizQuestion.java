package com.example.flatuno_reviewer_app.models;

public class QuizQuestion {
    private long id;
    private long quizId;
    private String question;
    private String correctAnswer;
    private long createdAt;

    public QuizQuestion(long quizId, String question, String correctAnswer) {
        this.quizId = quizId;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getQuizId() { return quizId; }
    public void setQuizId(long quizId) { this.quizId = quizId; }
    
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
} 