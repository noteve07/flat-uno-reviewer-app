package com.example.flatuno_reviewer_app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;

public class HomeFragment extends Fragment {
    private TextView flashcardsCount;
    private TextView quizzesCount;
    private LinearLayout recentActivityContainer;
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize database
        dbHelper = new FlashcardDbHelper(requireContext());
        database = dbHelper.getReadableDatabase();

        // Initialize views
        flashcardsCount = view.findViewById(R.id.flashcards_count);
        quizzesCount = view.findViewById(R.id.quizzes_count);
        recentActivityContainer = view.findViewById(R.id.recent_activity_container);

        // Load stats from database
        loadStats();
        
        // Load recent activities
        loadRecentActivities();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    private void loadStats() {
        // Get total flashcards count
        Cursor flashcardCursor = database.query(
            FlashcardDbHelper.TABLE_FLASHCARDS,
            new String[]{"COUNT(*) as count"},
            null,
            null,
            null,
            null,
            null
        );
        
        if (flashcardCursor.moveToFirst()) {
            int count = flashcardCursor.getInt(flashcardCursor.getColumnIndex("count"));
            flashcardsCount.setText(String.valueOf(count));
        }
        flashcardCursor.close();

        // Get total quizzes count
        Cursor quizCursor = database.query(
            FlashcardDbHelper.TABLE_QUIZZES,
            new String[]{"COUNT(*) as count"},
            null,
            null,
            null,
            null,
            null
        );
        
        if (quizCursor.moveToFirst()) {
            int count = quizCursor.getInt(quizCursor.getColumnIndex("count"));
            quizzesCount.setText(String.valueOf(count));
        }
        quizCursor.close();
    }

    private void loadRecentActivities() {
        // Get recent quiz scores
        Cursor quizScoreCursor = database.query(
            FlashcardDbHelper.TABLE_QUIZ_SCORES,
            new String[]{"score", "total_questions", "taken_at", "quiz_id"},
            null,
            null,
            null,
            null,
            "taken_at DESC",
            "3"
        );

        while (quizScoreCursor.moveToNext()) {
            int score = quizScoreCursor.getInt(quizScoreCursor.getColumnIndex("score"));
            int totalQuestions = quizScoreCursor.getInt(quizScoreCursor.getColumnIndex("total_questions"));
            long takenAt = quizScoreCursor.getLong(quizScoreCursor.getColumnIndex("taken_at"));
            long quizId = quizScoreCursor.getLong(quizScoreCursor.getColumnIndex("quiz_id"));

            // Get quiz title
            Cursor quizCursor = database.query(
                FlashcardDbHelper.TABLE_QUIZZES,
                new String[]{"title"},
                "id = ?",
                new String[]{String.valueOf(quizId)},
                null,
                null,
                null
            );

            if (quizCursor.moveToFirst()) {
                String quizTitle = quizCursor.getString(quizCursor.getColumnIndex("title"));
                int percentage = (score * 100) / totalQuestions;
                String timeAgo = getTimeAgo(takenAt);
                
                addRecentActivity(
                    "Completed " + quizTitle,
                    "Score: " + percentage + "%",
                    timeAgo
                );
            }
            quizCursor.close();
        }
        quizScoreCursor.close();

        // Get recently added flashcards
        Cursor flashcardCursor = database.query(
            FlashcardDbHelper.TABLE_FLASHCARDS,
            new String[]{"term", "created_at", "topic_id"},
            null,
            null,
            null,
            null,
            "created_at DESC",
            "3"
        );

        while (flashcardCursor.moveToNext()) {
            String term = flashcardCursor.getString(flashcardCursor.getColumnIndex("term"));
            long createdAt = flashcardCursor.getLong(flashcardCursor.getColumnIndex("created_at"));
            long topicId = flashcardCursor.getLong(flashcardCursor.getColumnIndex("topic_id"));

            // Get topic name
            Cursor topicCursor = database.query(
                FlashcardDbHelper.TABLE_TOPICS,
                new String[]{"name"},
                "id = ?",
                new String[]{String.valueOf(topicId)},
                null,
                null,
                null
            );

            if (topicCursor.moveToFirst()) {
                String topicName = topicCursor.getString(topicCursor.getColumnIndex("name"));
                String timeAgo = getTimeAgo(createdAt);
                
                addRecentActivity(
                    "Added new " + topicName + " flashcard",
                    term,
                    timeAgo
                );
            }
            topicCursor.close();
        }
        flashcardCursor.close();
    }

    private void addRecentActivity(String title, String subtitle, String time) {
        View activityView = getLayoutInflater().inflate(R.layout.item_recent_activity, recentActivityContainer, false);
        
        TextView titleView = activityView.findViewById(R.id.activity_title);
        TextView subtitleView = activityView.findViewById(R.id.activity_subtitle);
        TextView timeView = activityView.findViewById(R.id.activity_time);

        titleView.setText(title);
        subtitleView.setText(subtitle);
        timeView.setText(time);

        recentActivityContainer.addView(activityView);
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days == 1 ? "Yesterday" : days + " days ago";
        } else if (hours > 0) {
            return hours + " hours ago";
        } else if (minutes > 0) {
            return minutes + " minutes ago";
        } else {
            return "Just now";
        }
    }
} 