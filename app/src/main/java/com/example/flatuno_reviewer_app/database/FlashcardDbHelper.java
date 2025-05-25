package com.example.flatuno_reviewer_app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FlashcardDbHelper extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = DatabaseConfig.DATABASE_VERSION;
    private static final String DATABASE_NAME = "flatuno.db";

    // Table Names
    public static final String TABLE_TOPICS = "topics";
    public static final String TABLE_FLASHCARDS = "flashcards";
    public static final String TABLE_QUIZZES = "quizzes";
    public static final String TABLE_QUIZ_QUESTIONS = "quiz_questions";
    public static final String TABLE_QUIZ_CHOICES = "quiz_choices";
    public static final String TABLE_QUIZ_SCORES = "quiz_scores";

    // Common column names
    public static final String KEY_ID = "id";
    public static final String KEY_CREATED_AT = "created_at";

    // TOPICS Table Columns
    public static final String KEY_TOPIC_NAME = "name";
    public static final String KEY_TOPIC_COLOR = "color";
    public static final String KEY_TOPIC_LAST_MODIFIED = "last_modified";

    // FLASHCARDS Table Columns
    public static final String KEY_FLASHCARD_TOPIC_ID = "topic_id";
    public static final String KEY_FLASHCARD_TERM = "term";
    public static final String KEY_FLASHCARD_DESCRIPTION = "description";
    public static final String KEY_FLASHCARD_LAST_REVIEWED = "last_reviewed";
    public static final String KEY_FLASHCARD_COLOR = "color";

    // QUIZZES Table Columns
    public static final String KEY_QUIZ_TOPIC_ID = "topic_id";
    public static final String KEY_QUIZ_TITLE = "title";
    public static final String KEY_QUIZ_LAST_TAKEN = "last_taken";

    // QUIZ_QUESTIONS Table Columns
    public static final String KEY_QUESTION_QUIZ_ID = "quiz_id";
    public static final String KEY_QUESTION_TEXT = "question";
    public static final String KEY_QUESTION_CORRECT_ANSWER = "correct_answer";

    // QUIZ_CHOICES Table Columns
    public static final String KEY_CHOICE_QUESTION_ID = "question_id";
    public static final String KEY_CHOICE_TEXT = "choice_text";
    public static final String KEY_CHOICE_IS_CORRECT = "is_correct";

    // QUIZ_SCORES Table Columns
    public static final String KEY_SCORE_QUIZ_ID = "quiz_id";
    public static final String KEY_SCORE_VALUE = "score";
    public static final String KEY_SCORE_TOTAL_QUESTIONS = "total_questions";
    public static final String KEY_SCORE_TAKEN_AT = "taken_at";
    public static final String KEY_SCORE_TIME_SPENT = "time_spent";

    // Create Table Statements
    private static final String CREATE_TABLE_TOPICS = "CREATE TABLE " + TABLE_TOPICS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_TOPIC_NAME + " TEXT NOT NULL,"
            + KEY_TOPIC_COLOR + " TEXT NOT NULL,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + KEY_TOPIC_LAST_MODIFIED + " INTEGER NOT NULL"
            + ")";

    private static final String CREATE_TABLE_FLASHCARDS = "CREATE TABLE " + TABLE_FLASHCARDS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_FLASHCARD_TOPIC_ID + " INTEGER NOT NULL,"
            + KEY_FLASHCARD_TERM + " TEXT NOT NULL,"
            + KEY_FLASHCARD_DESCRIPTION + " TEXT NOT NULL,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + KEY_FLASHCARD_LAST_REVIEWED + " INTEGER NOT NULL,"
            + KEY_FLASHCARD_COLOR + " TEXT NOT NULL,"
            + "FOREIGN KEY(" + KEY_FLASHCARD_TOPIC_ID + ") REFERENCES " + TABLE_TOPICS + "(" + KEY_ID + ")"
            + ")";

    private static final String CREATE_TABLE_QUIZZES = "CREATE TABLE " + TABLE_QUIZZES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_QUIZ_TOPIC_ID + " INTEGER NOT NULL,"
            + KEY_QUIZ_TITLE + " TEXT NOT NULL,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + KEY_QUIZ_LAST_TAKEN + " INTEGER NOT NULL,"
            + "FOREIGN KEY(" + KEY_QUIZ_TOPIC_ID + ") REFERENCES " + TABLE_TOPICS + "(" + KEY_ID + ")"
            + ")";

    private static final String CREATE_TABLE_QUIZ_QUESTIONS = "CREATE TABLE " + TABLE_QUIZ_QUESTIONS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_QUESTION_QUIZ_ID + " INTEGER NOT NULL,"
            + KEY_QUESTION_TEXT + " TEXT NOT NULL,"
            + KEY_QUESTION_CORRECT_ANSWER + " TEXT NOT NULL,"
            + KEY_CREATED_AT + " INTEGER NOT NULL,"
            + "FOREIGN KEY(" + KEY_QUESTION_QUIZ_ID + ") REFERENCES " + TABLE_QUIZZES + "(" + KEY_ID + ")"
            + ")";

    private static final String CREATE_TABLE_QUIZ_CHOICES = "CREATE TABLE " + TABLE_QUIZ_CHOICES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_CHOICE_QUESTION_ID + " INTEGER NOT NULL,"
            + KEY_CHOICE_TEXT + " TEXT NOT NULL,"
            + KEY_CHOICE_IS_CORRECT + " INTEGER NOT NULL,"
            + "FOREIGN KEY(" + KEY_CHOICE_QUESTION_ID + ") REFERENCES " + TABLE_QUIZ_QUESTIONS + "(" + KEY_ID + ")"
            + ")";

    private static final String CREATE_TABLE_QUIZ_SCORES = "CREATE TABLE " + TABLE_QUIZ_SCORES + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_SCORE_QUIZ_ID + " INTEGER NOT NULL,"
            + KEY_SCORE_VALUE + " INTEGER NOT NULL,"
            + KEY_SCORE_TOTAL_QUESTIONS + " INTEGER NOT NULL,"
            + KEY_SCORE_TAKEN_AT + " INTEGER NOT NULL,"
            + KEY_SCORE_TIME_SPENT + " INTEGER NOT NULL,"
            + "FOREIGN KEY(" + KEY_SCORE_QUIZ_ID + ") REFERENCES " + TABLE_QUIZZES + "(" + KEY_ID + ")"
            + ")";

    public FlashcardDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creating required tables
        db.execSQL(CREATE_TABLE_TOPICS);
        db.execSQL(CREATE_TABLE_FLASHCARDS);
        db.execSQL(CREATE_TABLE_QUIZZES);
        db.execSQL(CREATE_TABLE_QUIZ_QUESTIONS);
        db.execSQL(CREATE_TABLE_QUIZ_CHOICES);
        db.execSQL(CREATE_TABLE_QUIZ_SCORES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Only reset database if RESET_DATABASE flag is set to 1
        if (DatabaseConfig.RESET_DATABASE == 1) {
            // Drop older tables if existed
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ_SCORES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ_CHOICES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZ_QUESTIONS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUIZZES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FLASHCARDS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOPICS);

            // Create tables again
            onCreate(db);
        }
    }
} 