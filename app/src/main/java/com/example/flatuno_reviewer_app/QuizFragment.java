package com.example.flatuno_reviewer_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class QuizFragment extends Fragment {
    private RecyclerView recyclerView;
    private QuizAdapter adapter;
    private List<QuizTopic> topics;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.quiz_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Sample data
        topics = new ArrayList<>();
        topics.add(new QuizTopic("Mathematics Quiz", 10, "Last score: 85%"));
        topics.add(new QuizTopic("Science Quiz", 15, "Last score: 90%"));
        topics.add(new QuizTopic("History Quiz", 12, "Not attempted yet"));
        topics.add(new QuizTopic("English Quiz", 8, "Last score: 75%"));
        topics.add(new QuizTopic("Programming Quiz", 20, "Not attempted yet"));

        // Set up adapter
        adapter = new QuizAdapter(topics);
        recyclerView.setAdapter(adapter);

        // FAB click listener
        FloatingActionButton fab = view.findViewById(R.id.fab_add_quiz);
        fab.setOnClickListener(v -> {
            // TODO: Implement add quiz functionality
        });

        return view;
    }

    // Sample data class
    private static class QuizTopic {
        String title;
        int questionCount;
        String lastScore;

        QuizTopic(String title, int questionCount, String lastScore) {
            this.title = title;
            this.questionCount = questionCount;
            this.lastScore = lastScore;
        }
    }

    // RecyclerView Adapter
    private class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.ViewHolder> {
        private List<QuizTopic> topics;

        QuizAdapter(List<QuizTopic> topics) {
            this.topics = topics;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            QuizTopic topic = topics.get(position);
            holder.quizTitle.setText(topic.title);
            holder.questionCount.setText(topic.questionCount + " questions");
            holder.lastScore.setText(topic.lastScore);
        }

        @Override
        public int getItemCount() {
            return topics.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView quizTitle;
            TextView questionCount;
            TextView lastScore;

            ViewHolder(View view) {
                super(view);
                quizTitle = view.findViewById(R.id.quiz_title);
                questionCount = view.findViewById(R.id.question_count);
                lastScore = view.findViewById(R.id.last_score);
            }
        }
    }
} 