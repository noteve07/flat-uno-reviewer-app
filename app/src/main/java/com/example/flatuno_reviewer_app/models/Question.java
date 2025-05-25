package com.example.flatuno_reviewer_app.models;

import java.util.List;

public class Question {
    private long id;
    private long quizId;
    private String question;
    private String correctAnswer;
    private long createdAt;
    private List<Choice> choices;

    public Question(long quizId, String question, String correctAnswer) {
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

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public static class Choice {
        private long id;
        private long questionId;
        private String choiceText;
        private boolean isCorrect;

        public Choice(long questionId, String choiceText, boolean isCorrect) {
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
} 