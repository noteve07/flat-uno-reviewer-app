package com.example.flatuno_reviewer_app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.example.flatuno_reviewer_app.R;

public class InitializeData {
    private FlashcardDbHelper dbHelper;
    public SQLiteDatabase database;
    private Context context;

    private static final int[] TOPIC_COLOR_IDS = {
        R.color.topic_pink,
        R.color.topic_green,
        R.color.topic_blue,
        R.color.topic_yellow,
        R.color.topic_purple,
        R.color.topic_orange,
        R.color.topic_mint,
        R.color.topic_lavender,
        R.color.topic_lime,
        R.color.topic_peach
    };

    private int currentColorIndex = 0;

    private String getNextColor() {
        int colorId = TOPIC_COLOR_IDS[currentColorIndex];
        String color = String.format("#%06X", (0xFFFFFF & context.getResources().getColor(colorId)));
        currentColorIndex = (currentColorIndex + 1) % TOPIC_COLOR_IDS.length;
        return color;
    }

    public InitializeData(Context context) {
        this.context = context;
        dbHelper = new FlashcardDbHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public boolean isDatabaseEmpty() {
        Cursor cursor = database.query(FlashcardDbHelper.TABLE_TOPICS, null, null, null, null, null, null);
        boolean isEmpty = cursor.getCount() == 0;
        cursor.close();
        return isEmpty;
    }

    public void initializeIfEmpty() {
        if (isDatabaseEmpty()) {
            initializeDatabase();
        }
    }

    public void initializeDatabase() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        currentColorIndex = 0;  // Reset color index
        
        android.util.Log.d("InitializeData", "Starting database initialization");
        
        try {
            // Start transaction
            db.beginTransaction();
            
            // Delete tables in correct order to respect foreign key constraints
            db.delete("quiz_choices", null, null);
            db.delete("quiz_questions", null, null);
            db.delete("quizzes", null, null);
            db.delete("flashcards", null, null);
            db.delete("topics", null, null);
            
            android.util.Log.d("InitializeData", "Cleared all tables");
            
            // Insert sample topics with colors from resources
            long algoId = insertTopic(db, "Divide and Conquer", getNextColor());
            long greedyId = insertTopic(db, "Greedy Algorithm", getNextColor());
            long androidId = insertTopic(db, "Using Intents in Android Studio", getNextColor());
            long oopId = insertTopic(db, "OOP Principles", getNextColor());
            long rizalId = insertTopic(db, "Early Education of Rizal", getNextColor());
            long germanId = insertTopic(db, "German Verbs", getNextColor());
            
            if (algoId == -1 || androidId == -1 || oopId == -1 || rizalId == -1 || germanId == -1) {
                throw new RuntimeException("Failed to insert topics");
            }
            
            android.util.Log.d("InitializeData", "Inserted topics with IDs: " + algoId + ", " + androidId + ", " + oopId + ", " + rizalId + ", " + germanId);
            
            // Insert sample flashcards
            android.util.Log.d("InitializeData", "Starting flashcard insertion");
            
            // Insert flashcards for Divide and Conquer
            insertFlashcard(db, algoId, "Merge Sort", "A divide and conquer algorithm that recursively breaks down a problem into two or more sub-problems until they become simple enough to solve directly.");
            insertFlashcard(db, algoId, "Quick Sort", "A divide and conquer algorithm that picks an element as pivot and partitions the array around the pivot.");
            insertFlashcard(db, algoId, "Binary Search", "A search algorithm that finds the position of a target value within a sorted array by repeatedly dividing the search interval in half.");

            // Insert flashcards for Greedy Algorithm
            insertFlashcard(db, greedyId, "Activity Selection", "Selects the maximum number of activities that don't overlap, by always choosing the next activity that finishes earliest.");
            insertFlashcard(db, greedyId, "Fractional Knapsack", "Solves the knapsack problem by taking items with the highest value-to-weight ratio first, allowing fractions of items.");
            insertFlashcard(db, greedyId, "Huffman Coding", "A lossless data compression algorithm that builds an optimal prefix code using a greedy approach based on character frequencies.");

            // Insert flashcards for Android Intents
            insertFlashcard(db, androidId, "Explicit Intent", "An intent that explicitly specifies the component to start by name.");
            insertFlashcard(db, androidId, "Implicit Intent", "An intent that specifies an action that can be handled by any app installed on the device.");
            insertFlashcard(db, androidId, "Intent Filters", "Declarations in the manifest that specify the types of intents a component can handle.");
            
            // Insert flashcards for OOP
            insertFlashcard(db, oopId, "Encapsulation", "Bundling of data and methods that operate on that data within a single unit or object.");
            insertFlashcard(db, oopId, "Inheritance", "A mechanism that allows a class to inherit properties and methods from another class.");
            insertFlashcard(db, oopId, "Polymorphism", "The ability of an object to take many forms and behave differently based on the context.");
            
            // Insert flashcards for Rizal
            insertFlashcard(db, rizalId, "Ateneo Municipal", "Rizal's secondary education where he excelled in academics and won numerous awards.");
            insertFlashcard(db, rizalId, "University of Santo Tomas", "Where Rizal initially studied medicine before transferring to Spain.");
            insertFlashcard(db, rizalId, "Universidad Central de Madrid", "Where Rizal completed his medical degree and studied philosophy and letters.");
            
            // Insert flashcards for German
            insertFlashcard(db, germanId, "Sein", "To be - ich bin, du bist, er/sie/es ist");
            insertFlashcard(db, germanId, "Haben", "To have - ich habe, du hast, er/sie/es hat");
            insertFlashcard(db, germanId, "Werden", "To become - ich werde, du wirst, er/sie/es wird");
            
            android.util.Log.d("InitializeData", "Finished flashcard insertion");
            
            // Insert sample quizzes
            long algoQuizId = insertQuiz(db, algoId, "Divide and Conquer Basics");
            long androidQuizId = insertQuiz(db, androidId, "Android Intents Quiz");
            long oopQuizId = insertQuiz(db, oopId, "OOP Concepts");
            long rizalQuizId = insertQuiz(db, rizalId, "Rizal's Education");
            long germanQuizId = insertQuiz(db, germanId, "German Verb Conjugation");
            
            if (algoQuizId == -1 || androidQuizId == -1 || oopQuizId == -1 || rizalQuizId == -1 || germanQuizId == -1) {
                throw new RuntimeException("Failed to insert quizzes");
            }
            
            // Insert quiz questions and choices
            insertAlgoQuizQuestions(db, algoQuizId);
            insertAndroidQuizQuestions(db, androidQuizId);
            insertOOPQuizQuestions(db, oopQuizId);
            insertRizalQuizQuestions(db, rizalQuizId);
            insertGermanQuizQuestions(db, germanQuizId);
            
            // Verify that questions were inserted
            Cursor questionCursor = db.query(
                "quiz_questions",
                new String[]{"COUNT(*) as count"},
                null,
                null,
                null,
                null,
                null
            );
            
            if (questionCursor.moveToFirst()) {
                int questionCount = questionCursor.getInt(questionCursor.getColumnIndexOrThrow("count"));
                if (questionCount == 0) {
                    throw new RuntimeException("No quiz questions were inserted");
                }
                android.util.Log.d("InitializeData", "Successfully inserted " + questionCount + " quiz questions");
            }
            questionCursor.close();
            
            // Mark transaction as successful
            db.setTransactionSuccessful();
            android.util.Log.d("InitializeData", "Database initialization completed successfully");
            
        } catch (Exception e) {
            android.util.Log.e("InitializeData", "Error initializing database: " + e.getMessage());
            e.printStackTrace();
            // Transaction will be rolled back automatically
        } finally {
            // End transaction
            db.endTransaction();
        }
        
        db.close();
    }

    private long insertTopic(SQLiteDatabase db, String name, String color) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        values.put("created_at", System.currentTimeMillis());
        values.put("last_modified", System.currentTimeMillis());
        long result = db.insert("topics", null, values);
        if (result == -1) {
            android.util.Log.e("InitializeData", "Failed to insert topic: " + name);
        } else {
            android.util.Log.d("InitializeData", "Successfully inserted topic: " + name + " with ID: " + result);
        }
        return result;
    }

    public long insertFlashcard(SQLiteDatabase db, long topicId, String term, String description) {
        ContentValues values = new ContentValues();
        values.put("topic_id", topicId);
        values.put("term", term);
        values.put("description", description);
        values.put("created_at", System.currentTimeMillis());
        values.put("last_reviewed", System.currentTimeMillis());
        
        // Get the topic color
        Cursor cursor = db.query(
            "topics",
            new String[]{"color"},
            "id = ?",
            new String[]{String.valueOf(topicId)},
            null,
            null,
            null
        );
        
        if (cursor.moveToFirst()) {
            String color = cursor.getString(cursor.getColumnIndexOrThrow("color"));
            values.put("color", color);
            android.util.Log.d("InitializeData", "Inserting flashcard for topic " + topicId + " with color " + color);
        } else {
            android.util.Log.e("InitializeData", "Failed to get color for topic " + topicId);
        }
        cursor.close();
        
        long result = db.insert("flashcards", null, values);
        if (result == -1) {
            android.util.Log.e("InitializeData", "Failed to insert flashcard: " + term);
        } else {
            android.util.Log.d("InitializeData", "Successfully inserted flashcard: " + term + " with ID: " + result);
        }
        return result;
    }

    private long insertQuiz(SQLiteDatabase db, long topicId, String title) {
        ContentValues values = new ContentValues();
        values.put("topic_id", topicId);
        values.put("title", title);
        values.put("created_at", System.currentTimeMillis());
        values.put("last_taken", System.currentTimeMillis());
        return db.insert("quizzes", null, values);
    }

    private void insertAlgoQuizQuestions(SQLiteDatabase db, long quizId) {
        // Question 1
        ContentValues q1Values = new ContentValues();
        q1Values.put("quiz_id", quizId);
        q1Values.put("question", "What is the time complexity of Merge Sort?");
        q1Values.put("correct_answer", "O(n log n)");
        q1Values.put("created_at", System.currentTimeMillis());
        long q1Id = db.insert("quiz_questions", null, q1Values);
        if (q1Id == -1) {
            throw new RuntimeException("Failed to insert question 1 for algo quiz");
        }

        // Choices for Question 1
        insertChoice(db, q1Id, "O(n log n)", true);
        insertChoice(db, q1Id, "O(n²)", false);
        insertChoice(db, q1Id, "O(log n)", false);
        insertChoice(db, q1Id, "O(n)", false);

        // Question 2
        ContentValues q2Values = new ContentValues();
        q2Values.put("quiz_id", quizId);
        q2Values.put("question", "Which algorithm uses a pivot element?");
        q2Values.put("correct_answer", "Quick Sort");
        q2Values.put("created_at", System.currentTimeMillis());
        long q2Id = db.insert("quiz_questions", null, q2Values);
        if (q2Id == -1) {
            throw new RuntimeException("Failed to insert question 2 for algo quiz");
        }

        // Choices for Question 2
        insertChoice(db, q2Id, "Quick Sort", true);
        insertChoice(db, q2Id, "Merge Sort", false);
        insertChoice(db, q2Id, "Binary Search", false);
        insertChoice(db, q2Id, "Bubble Sort", false);
    }

    private void insertAndroidQuizQuestions(SQLiteDatabase db, long quizId) {
        // Question 1
        ContentValues q1Values = new ContentValues();
        q1Values.put("quiz_id", quizId);
        q1Values.put("question", "What is an Explicit Intent?");
        q1Values.put("correct_answer", "An intent that explicitly specifies the component to start by name");
        q1Values.put("created_at", System.currentTimeMillis());
        long q1Id = db.insert("quiz_questions", null, q1Values);
        if (q1Id == -1) {
            throw new RuntimeException("Failed to insert question 1 for android quiz");
        }

        // Choices for Question 1
        insertChoice(db, q1Id, "An intent that explicitly specifies the component to start by name", true);
        insertChoice(db, q1Id, "An intent that specifies only the action", false);
        insertChoice(db, q1Id, "An intent that is used for system services", false);
        insertChoice(db, q1Id, "An intent that is used for background tasks", false);

        // Question 2
        ContentValues q2Values = new ContentValues();
        q2Values.put("quiz_id", quizId);
        q2Values.put("question", "What is the purpose of Intent Filters?");
        q2Values.put("correct_answer", "To specify the types of intents a component can handle");
        q2Values.put("created_at", System.currentTimeMillis());
        long q2Id = db.insert("quiz_questions", null, q2Values);
        if (q2Id == -1) {
            throw new RuntimeException("Failed to insert question 2 for android quiz");
        }

        // Choices for Question 2
        insertChoice(db, q2Id, "To specify the types of intents a component can handle", true);
        insertChoice(db, q2Id, "To filter out unwanted intents", false);
        insertChoice(db, q2Id, "To prioritize certain intents", false);
        insertChoice(db, q2Id, "To block malicious intents", false);
    }

    private void insertOOPQuizQuestions(SQLiteDatabase db, long quizId) {
        // Question 1
        ContentValues q1Values = new ContentValues();
        q1Values.put("quiz_id", quizId);
        q1Values.put("question", "What is Encapsulation?");
        q1Values.put("correct_answer", "Bundling of data and methods that operate on that data within a single unit");
        q1Values.put("created_at", System.currentTimeMillis());
        long q1Id = db.insert("quiz_questions", null, q1Values);
        if (q1Id == -1) {
            throw new RuntimeException("Failed to insert question 1 for OOP quiz");
        }

        // Choices for Question 1
        insertChoice(db, q1Id, "Bundling of data and methods that operate on that data within a single unit", true);
        insertChoice(db, q1Id, "Creating multiple instances of a class", false);
        insertChoice(db, q1Id, "Inheriting properties from a parent class", false);
        insertChoice(db, q1Id, "Converting one data type to another", false);

        // Question 2
        ContentValues q2Values = new ContentValues();
        q2Values.put("quiz_id", quizId);
        q2Values.put("question", "What is Polymorphism?");
        q2Values.put("correct_answer", "Ability of an object to take many forms and behave differently based on the context");
        q2Values.put("created_at", System.currentTimeMillis());
        long q2Id = db.insert("quiz_questions", null, q2Values);
        if (q2Id == -1) {
            throw new RuntimeException("Failed to insert question 2 for OOP quiz");
        }

        // Choices for Question 2
        insertChoice(db, q2Id, "Ability of an object to take many forms and behave differently based on the context", true);
        insertChoice(db, q2Id, "Creating multiple copies of an object", false);
        insertChoice(db, q2Id, "Converting objects to different types", false);
        insertChoice(db, q2Id, "Storing objects in different formats", false);
    }

    private void insertRizalQuizQuestions(SQLiteDatabase db, long quizId) {
        // Question 1
        ContentValues q1Values = new ContentValues();
        q1Values.put("quiz_id", quizId);
        q1Values.put("question", "Where did Rizal study from 1872-1877?");
        q1Values.put("correct_answer", "Ateneo Municipal");
        q1Values.put("created_at", System.currentTimeMillis());
        long q1Id = db.insert("quiz_questions", null, q1Values);
        if (q1Id == -1) {
            throw new RuntimeException("Failed to insert question 1 for Rizal quiz");
        }

        // Choices for Question 1
        insertChoice(db, q1Id, "Ateneo Municipal", true);
        insertChoice(db, q1Id, "University of Santo Tomas", false);
        insertChoice(db, q1Id, "University of Madrid", false);
        insertChoice(db, q1Id, "University of Heidelberg", false);

        // Question 2
        ContentValues q2Values = new ContentValues();
        q2Values.put("quiz_id", quizId);
        q2Values.put("question", "Who was Rizal's first teacher?");
        q2Values.put("correct_answer", "Doña Teodora");
        q2Values.put("created_at", System.currentTimeMillis());
        long q2Id = db.insert("quiz_questions", null, q2Values);
        if (q2Id == -1) {
            throw new RuntimeException("Failed to insert question 2 for Rizal quiz");
        }

        // Choices for Question 2
        insertChoice(db, q2Id, "Doña Teodora", true);
        insertChoice(db, q2Id, "Maestro Justiniano", false);
        insertChoice(db, q2Id, "Father Sanchez", false);
        insertChoice(db, q2Id, "Father Burgos", false);
    }

    private void insertGermanQuizQuestions(SQLiteDatabase db, long quizId) {
        // Question 1
        ContentValues q1Values = new ContentValues();
        q1Values.put("quiz_id", quizId);
        q1Values.put("question", "What is the conjugation of 'sein' for 'ich'?");
        q1Values.put("correct_answer", "bin");
        q1Values.put("created_at", System.currentTimeMillis());
        long q1Id = db.insert("quiz_questions", null, q1Values);
        if (q1Id == -1) {
            throw new RuntimeException("Failed to insert question 1 for German quiz");
        }

        // Choices for Question 1
        insertChoice(db, q1Id, "bin", true);
        insertChoice(db, q1Id, "bist", false);
        insertChoice(db, q1Id, "ist", false);
        insertChoice(db, q1Id, "sind", false);

        // Question 2
        ContentValues q2Values = new ContentValues();
        q2Values.put("quiz_id", quizId);
        q2Values.put("question", "What is the conjugation of 'haben' for 'du'?");
        q2Values.put("correct_answer", "hast");
        q2Values.put("created_at", System.currentTimeMillis());
        long q2Id = db.insert("quiz_questions", null, q2Values);
        if (q2Id == -1) {
            throw new RuntimeException("Failed to insert question 2 for German quiz");
        }

        // Choices for Question 2
        insertChoice(db, q2Id, "hast", true);
        insertChoice(db, q2Id, "habe", false);
        insertChoice(db, q2Id, "hat", false);
        insertChoice(db, q2Id, "haben", false);
    }

    private void insertChoice(SQLiteDatabase db, long questionId, String choiceText, boolean isCorrect) {
        ContentValues values = new ContentValues();
        values.put("question_id", questionId);
        values.put("choice_text", choiceText);
        values.put("is_correct", isCorrect ? 1 : 0);
        long result = db.insert("quiz_choices", null, values);
        if (result == -1) {
            android.util.Log.e("InitializeData", "Failed to insert choice: " + choiceText + " for question " + questionId);
            throw new RuntimeException("Failed to insert choice: " + choiceText + " for question " + questionId);
        } else {
            android.util.Log.d("InitializeData", "Successfully inserted choice: " + choiceText + " (correct: " + isCorrect + ") for question " + questionId);
        }
    }

    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
} 