package com.example.flatuno_reviewer_app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.example.flatuno_reviewer_app.models.Quiz;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;

public class CreateQuizActivity extends AppCompatActivity {
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;
    private LinearLayout questionsContainer;
    private EditText quizTitleInput;
    private Button addQuestionButton;
    private Button saveQuizButton;
    private List<QuestionItem> questionItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_quiz);

        // Initialize database
        dbHelper = new FlashcardDbHelper(this);
        database = dbHelper.getWritableDatabase();

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Create Quiz");

        // Initialize views
        questionsContainer = findViewById(R.id.questions_container);
        quizTitleInput = findViewById(R.id.quiz_title_input);
        addQuestionButton = findViewById(R.id.add_question_button);
        saveQuizButton = findViewById(R.id.save_quiz_button);
        questionItems = new ArrayList<>();

        // Add first question by default
        addQuestionItem();

        // Set up click listeners
        addQuestionButton.setOnClickListener(v -> addQuestionItem());
        saveQuizButton.setOnClickListener(v -> saveQuiz());
    }

    private void addQuestionItem() {
        View questionView = getLayoutInflater().inflate(R.layout.item_question_edit, questionsContainer, false);
        QuestionItem questionItem = new QuestionItem(questionView);
        questionItems.add(questionItem);
        questionsContainer.addView(questionView);
    }

    private void saveQuiz() {
        String title = quizTitleInput.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a quiz title", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate questions
        for (QuestionItem item : questionItems) {
            if (!item.validate()) {
                Toast.makeText(this, "Please fill in all question fields", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Save quiz to database
        database.beginTransaction();
        try {
            // First create a new topic
            ContentValues topicValues = new ContentValues();
            topicValues.put("name", title); // Use quiz title as topic name
            topicValues.put("color", "#FF9AA2"); // Default color
            topicValues.put("created_at", System.currentTimeMillis());
            topicValues.put("last_modified", System.currentTimeMillis());
            
            long topicId = database.insert(FlashcardDbHelper.TABLE_TOPICS, null, topicValues);
            
            if (topicId != -1) {
                // Then insert quiz
                ContentValues quizValues = new ContentValues();
                quizValues.put("title", title);
                quizValues.put("topic_id", topicId);
                quizValues.put("created_at", System.currentTimeMillis());
                quizValues.put("last_taken", System.currentTimeMillis());
                
                long quizId = database.insert(FlashcardDbHelper.TABLE_QUIZZES, null, quizValues);
                
                if (quizId != -1) {
                    // Insert questions and choices
                    for (QuestionItem item : questionItems) {
                        item.saveToDatabase(database, quizId);
                    }
                    
                    database.setTransactionSuccessful();
                    Toast.makeText(this, "Quiz created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error creating quiz", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error creating topic", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error creating quiz", Toast.LENGTH_SHORT).show();
        } finally {
            database.endTransaction();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    }

    // Helper class to manage question items
    private class QuestionItem {
        private EditText questionInput;
        private EditText[] choiceInputs;
        private Spinner correctAnswerSpinner;

        QuestionItem(View view) {
            questionInput = view.findViewById(R.id.question_input);
            choiceInputs = new EditText[] {
                view.findViewById(R.id.choice1_input),
                view.findViewById(R.id.choice2_input),
                view.findViewById(R.id.choice3_input),
                view.findViewById(R.id.choice4_input)
            };
            correctAnswerSpinner = view.findViewById(R.id.correct_answer_spinner);
        }

        boolean validate() {
            if (questionInput.getText().toString().trim().isEmpty()) {
                return false;
            }
            for (EditText choice : choiceInputs) {
                if (choice.getText().toString().trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        void saveToDatabase(SQLiteDatabase db, long quizId) {
            // Insert question
            ContentValues questionValues = new ContentValues();
            questionValues.put("quiz_id", quizId);
            questionValues.put("question", questionInput.getText().toString().trim());
            questionValues.put("correct_answer", choiceInputs[correctAnswerSpinner.getSelectedItemPosition()].getText().toString().trim());
            questionValues.put("created_at", System.currentTimeMillis());
            
            long questionId = db.insert(FlashcardDbHelper.TABLE_QUIZ_QUESTIONS, null, questionValues);
            
            if (questionId != -1) {
                // Insert choices
                int correctAnswerIndex = correctAnswerSpinner.getSelectedItemPosition();
                for (int i = 0; i < choiceInputs.length; i++) {
                    ContentValues choiceValues = new ContentValues();
                    choiceValues.put("question_id", questionId);
                    choiceValues.put("choice_text", choiceInputs[i].getText().toString().trim());
                    choiceValues.put("is_correct", i == correctAnswerIndex ? 1 : 0);
                    db.insert(FlashcardDbHelper.TABLE_QUIZ_CHOICES, null, choiceValues);
                }
            }
        }
    }

    // Helper class for topic items in spinner
    private static class TopicItem {
        private long id;
        private String name;

        TopicItem(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }
    }
} 