package com.example.flatuno_reviewer_app.models;

public class QuizChoice {
    private long id;
    private long questionId;
    private String choiceText;
    private boolean isCorrect;

    public QuizChoice(long questionId, String choiceText, boolean isCorrect) {
        this.questionId = questionId;
        this.choiceText = choiceText;
        this.isCorrect = isCorrect;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }
    
    public String getChoiceText() { return choiceText; }
    public void setChoiceText(String choiceText) { this.choiceText = choiceText; }
    
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
} 