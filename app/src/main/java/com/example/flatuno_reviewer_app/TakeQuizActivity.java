package com.example.flatuno_reviewer_app;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.airbnb.lottie.LottieAnimationView;
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.example.flatuno_reviewer_app.models.QuizChoice;
import com.example.flatuno_reviewer_app.models.QuizQuestion;
import com.example.flatuno_reviewer_app.models.QuizScore;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import androidx.core.content.res.ResourcesCompat;
import android.view.WindowManager;

public class TakeQuizActivity extends AppCompatActivity {
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;
    private long quizId;
    private List<QuizQuestion> questions;
    private List<List<QuizChoice>> choices;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private long startTime;
    private MediaPlayer correctSound;
    private MediaPlayer incorrectSound;
    private Handler handler;
    
    private TextView questionTextView;
    private LinearLayout choicesContainer;
    private TextView progressTextView;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_quiz);
        
        // Initialize database
        dbHelper = new FlashcardDbHelper(this);
        database = dbHelper.getReadableDatabase();
        
        // Initialize sound effects
        correctSound = MediaPlayer.create(this, R.raw.correct);
        incorrectSound = MediaPlayer.create(this, R.raw.incorrect);
        
        // Initialize handler for delayed actions
        handler = new Handler(Looper.getMainLooper());
        
        // Get quiz ID from intent
        quizId = getIntent().getLongExtra("quiz_id", -1);
        if (quizId == -1) {
            Toast.makeText(this, "Error: Quiz not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        questionTextView = findViewById(R.id.question_text);
        choicesContainer = findViewById(R.id.choices_container);
        progressTextView = findViewById(R.id.progress_text);
        
        // Set up toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        // Load questions and choices
        loadQuizData();
        
        // Start timer when quiz begins
        startTime = System.currentTimeMillis();
        
        // Display first question
        displayQuestion();
    }
    
    private void loadQuizData() {
        questions = new ArrayList<>();
        choices = new ArrayList<>();
        
        android.util.Log.d("TakeQuizActivity", "Loading quiz data for quiz ID: " + quizId);
        
        // Load questions
        Cursor questionCursor = database.query(
            FlashcardDbHelper.TABLE_QUIZ_QUESTIONS,
            null,
            "quiz_id = ?",
            new String[]{String.valueOf(quizId)},
            null,
            null,
            null
        );
        
        android.util.Log.d("TakeQuizActivity", "Found " + questionCursor.getCount() + " questions");
        
        if (questionCursor.getCount() == 0) {
            Toast.makeText(this, "No questions found for this quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        boolean hasValidQuestions = false;
        
        while (questionCursor.moveToNext()) {
            QuizQuestion question = new QuizQuestion(
                quizId,
                questionCursor.getString(questionCursor.getColumnIndexOrThrow("question")),
                questionCursor.getString(questionCursor.getColumnIndexOrThrow("correct_answer"))
            );
            question.setId(questionCursor.getLong(questionCursor.getColumnIndexOrThrow("id")));
            
            // Load choices for this question
            List<QuizChoice> questionChoices = new ArrayList<>();
            Cursor choiceCursor = database.query(
                FlashcardDbHelper.TABLE_QUIZ_CHOICES,
                null,
                "question_id = ?",
                new String[]{String.valueOf(question.getId())},
                null,
                null,
                null
            );
            
            android.util.Log.d("TakeQuizActivity", "Found " + choiceCursor.getCount() + " choices for question " + question.getId());
            
            if (choiceCursor.getCount() >= 2) {  // Only add questions that have at least 2 choices
            while (choiceCursor.moveToNext()) {
                QuizChoice choice = new QuizChoice(
                    question.getId(),
                    choiceCursor.getString(choiceCursor.getColumnIndexOrThrow("choice_text")),
                    choiceCursor.getInt(choiceCursor.getColumnIndexOrThrow("is_correct")) == 1
                );
                choice.setId(choiceCursor.getLong(choiceCursor.getColumnIndexOrThrow("id")));
                questionChoices.add(choice);
                }
                
                // Only add questions that have at least one correct choice
                boolean hasCorrectChoice = false;
                for (QuizChoice choice : questionChoices) {
                    if (choice.isCorrect()) {
                        hasCorrectChoice = true;
                        break;
                    }
                }
                
                if (hasCorrectChoice) {
                    questions.add(question);
                    Collections.shuffle(questionChoices);
                    choices.add(questionChoices);
                    hasValidQuestions = true;
                }
            }
            choiceCursor.close();
        }
        questionCursor.close();
        
        if (!hasValidQuestions) {
            Toast.makeText(this, "This quiz has no valid questions with choices. Please regenerate the quiz.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        android.util.Log.d("TakeQuizActivity", "Finished loading quiz data. Valid questions: " + questions.size());
    }
    
    private void displayQuestion() {
        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        List<QuizChoice> currentChoices = choices.get(currentQuestionIndex);
        
        // Update progress
        progressTextView.setText(String.format("Question %d of %d", currentQuestionIndex + 1, questions.size()));
        
        // Set question text with dark gray color and larger font
        questionTextView.setText(currentQuestion.getQuestion());
        questionTextView.setTextColor(getResources().getColor(R.color.dark_gray));
        questionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22); // Increased from 20sp
        
        // Clear previous choices
        choicesContainer.removeAllViews();
        
        // Add new choices
        for (QuizChoice choice : currentChoices) {
            MaterialButton choiceButton = new MaterialButton(this);
            choiceButton.setText(choice.getChoiceText());
            choiceButton.setTextColor(getResources().getColor(R.color.dark_gray));
            choiceButton.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.toolbar_color)));
            choiceButton.setStrokeWidth(4);
            choiceButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white)));
            choiceButton.setTypeface(ResourcesCompat.getFont(this, R.font.poppins_medium));
            choiceButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            choiceButton.setPadding(32, 32, 32, 32);
            choiceButton.setCornerRadius(12);
            choiceButton.setElevation(4);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            choiceButton.setLayoutParams(params);
            
            choiceButton.setOnClickListener(v -> {
                // Disable all buttons
                for (int i = 0; i < choicesContainer.getChildCount(); i++) {
                    View child = choicesContainer.getChildAt(i);
                    child.setEnabled(false);
                }
                
                // Show correct/incorrect feedback
                if (choice.getChoiceText().equals(currentQuestion.getCorrectAnswer())) {
                    choiceButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.correct_green)));
                    choiceButton.setTextColor(getResources().getColor(android.R.color.white));
                    choiceButton.setStrokeWidth(0);
                    score++; // Only increment score if the answer is correct
                    correctSound.start();
                } else {
                    choiceButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.incorrect_red)));
                    choiceButton.setTextColor(getResources().getColor(android.R.color.white));
                    choiceButton.setStrokeWidth(0);
                    incorrectSound.start();
                    
                    // Show correct answer after a short delay
                    handler.postDelayed(() -> {
                        for (int i = 0; i < choicesContainer.getChildCount(); i++) {
                            View child = choicesContainer.getChildAt(i);
                            if (child instanceof MaterialButton) {
                                MaterialButton button = (MaterialButton) child;
                                if (button.getText().toString().equals(currentQuestion.getCorrectAnswer())) {
                                    button.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.correct_green)));
                                    button.setTextColor(getResources().getColor(android.R.color.white));
                                    button.setStrokeWidth(0);
                                    break;
                                }
                            }
                        }
                    }, 500); // 0.5 second delay
                }
                
                // Wait 2 seconds then move to next question with animation
                handler.postDelayed(() -> {
                    if (currentQuestionIndex < questions.size() - 1) {
                        // Animate current view out to the left
                        choicesContainer.animate()
                            .translationX(-getResources().getDisplayMetrics().widthPixels)
                            .setDuration(300)
                            .withEndAction(() -> {
                                currentQuestionIndex++;
                                // Reset position before showing next question
                                choicesContainer.setTranslationX(getResources().getDisplayMetrics().widthPixels);
                                displayQuestion();
                                // Animate new question in from the right
                                choicesContainer.animate()
                                    .translationX(0)
                                    .setDuration(300)
                                    .start();
                            })
                            .start();
                    } else {
                        showResults();
                    }
                }, 2000);
            });
            
            choicesContainer.addView(choiceButton);
        }
    }
    
    private void checkAnswer() {
        // Answer is already checked in the choice button click listener
    }
    
    private void showResults() {
        // Calculate score
        double percentage = (double) score / questions.size() * 100;
        
        // Calculate time taken in seconds
        long timeInSeconds = (System.currentTimeMillis() - startTime) / 1000;
        
        // Save score to database
        ContentValues scoreValues = new ContentValues();
        scoreValues.put("quiz_id", quizId);
        scoreValues.put("score", score);
        scoreValues.put("total_questions", questions.size());
        scoreValues.put("taken_at", System.currentTimeMillis());
        scoreValues.put("time_spent", timeInSeconds * 1000); // Store in milliseconds
        
        long scoreId = database.insert(FlashcardDbHelper.TABLE_QUIZ_SCORES, null, scoreValues);
        
        // Update last taken time for the quiz
        ContentValues quizValues = new ContentValues();
        quizValues.put("last_taken", System.currentTimeMillis());
        database.update(
            FlashcardDbHelper.TABLE_QUIZZES,
            quizValues,
            "id = ?",
            new String[]{String.valueOf(quizId)}
        );
        
        // Create and show dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_quiz_results);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        // Set dialog width to 90% of screen width
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }
        
        // Set score and time
        TextView scoreTextView = dialog.findViewById(R.id.scoreTextView);
        TextView scoreFractionTextView = dialog.findViewById(R.id.scoreFractionTextView);
        TextView timeTextView = dialog.findViewById(R.id.timeTextView);
        
        // Set color based on score
        int colorResId;
        int buttonColorResId;
        if (percentage < 25) {
            colorResId = R.color.incorrect_red;
            buttonColorResId = R.color.error_red; // Darker red
        } else if (percentage < 75) {
            colorResId = R.color.warning_yellow;
            buttonColorResId = R.color.warning_yellow; // Same yellow
        } else {
            colorResId = R.color.correct_green;
            buttonColorResId = R.color.success_green; // Darker green
        }
        
        scoreTextView.setText(String.format("%.0f%%", percentage));
        scoreTextView.setTextColor(getResources().getColor(colorResId));
        scoreFractionTextView.setText(String.format("%d/%d", score, questions.size()));
        scoreFractionTextView.setTextColor(getResources().getColor(colorResId));
        
        // Format time taken in seconds
        String timeText = String.format("Time: %ds", timeInSeconds);
        timeTextView.setText(timeText);
        
        // Set up done button with matching color
        MaterialButton doneButton = dialog.findViewById(R.id.doneButton);
        doneButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(buttonColorResId)));
        doneButton.setOnClickListener(v -> {
            dialog.dismiss();
            // Set result to refresh quiz list
            setResult(RESULT_OK);
            finish();
        });
        
        dialog.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (correctSound != null) {
            correctSound.release();
        }
        if (incorrectSound != null) {
            incorrectSound.release();
        }
        handler.removeCallbacksAndMessages(null);
    }
} 