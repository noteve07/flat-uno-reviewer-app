package com.example.flatuno_reviewer_app;

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

public class HomeFragment extends Fragment {
    private TextView flashcardsCount;
    private TextView quizzesCount;
    private LinearLayout recentActivityContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        flashcardsCount = view.findViewById(R.id.flashcards_count);
        quizzesCount = view.findViewById(R.id.quizzes_count);
        recentActivityContainer = view.findViewById(R.id.recent_activity_container);

        // Set sample stats
        flashcardsCount.setText("24");
        quizzesCount.setText("8");

        // Add recent activities
        addRecentActivity("Completed German Verbs Quiz", "Score: 85%", "2 hours ago");
        addRecentActivity("Added new OOP Concepts flashcards", "12 new cards", "5 hours ago");
        addRecentActivity("Reviewed Rizal's Poems", "15 cards reviewed", "Yesterday");
        addRecentActivity("Created new Android Basics quiz", "20 questions", "2 days ago");

        return view;
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
} 