package com.example.flatuno_reviewer_app;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Window;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class FlashcardsFragment extends Fragment {
    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<FlashcardTopic> topics;
    private String selectedColor = "#FF9AA2"; // Default color

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcards, container, false);

        // Initialize RecyclerView with GridLayoutManager
        recyclerView = view.findViewById(R.id.flashcards_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        // Sample data with diverse topics and pastel colors
        topics = new ArrayList<>();
        topics.add(new FlashcardTopic("OOP Concepts", 12, "#FF9AA2"));     // Deeper Pink
        topics.add(new FlashcardTopic("German Verbs", 15, "#B5EAD7"));     // Muted Teal
        topics.add(new FlashcardTopic("Rizal's Poems", 8, "#A2D2FF"));     // Softer Blue
        topics.add(new FlashcardTopic("Sorting Algorithms", 10, "#FFDAC1")); // Warm Orange
        topics.add(new FlashcardTopic("Android Basics", 14, "#E2F0CB"));   // Sage Green
        topics.add(new FlashcardTopic("German Grammar", 12, "#C7CEEA"));   // Soft Purple
        topics.add(new FlashcardTopic("Data Structures", 16, "#FFB7B2"));  // Coral Pink
        topics.add(new FlashcardTopic("Mobile UI/UX", 13, "#B5EAD7"));     // Mint Green

        // Set up adapter
        adapter = new FlashcardAdapter(topics);
        recyclerView.setAdapter(adapter);

        // FAB click listener
        FloatingActionButton fab = view.findViewById(R.id.fab_add_flashcard);
        fab.setOnClickListener(v -> showAddFlashcardDialog());

        return view;
    }

    private void showAddFlashcardDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_flashcard);

        // Initialize views
        TextInputEditText titleInput = dialog.findViewById(R.id.topic_title_input);
        TextInputEditText subjectInput = dialog.findViewById(R.id.topic_subject_input);
        LinearLayout colorPicker = dialog.findViewById(R.id.color_picker);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
        MaterialButton createButton = dialog.findViewById(R.id.create_button);

        // Set up color picker
        String[] colors = {
            "#FF9AA2", // Deeper Pink
            "#B5EAD7", // Muted Teal
            "#A2D2FF", // Softer Blue
            "#FFDAC1", // Warm Orange
            "#E2F0CB", // Sage Green
            "#C7CEEA", // Soft Purple
            "#FFB7B2", // Coral Pink
            "#B5EAD7"  // Mint Green
        };

        for (String color : colors) {
            View colorView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_color_picker, colorPicker, false);
            
            View colorIndicator = colorView.findViewById(R.id.color_view);
            colorIndicator.setBackgroundColor(Color.parseColor(color));
            
            if (color.equals(selectedColor)) {
                colorView.findViewById(R.id.check_icon).setVisibility(View.VISIBLE);
            }

            colorView.setOnClickListener(v -> {
                // Update selected color
                selectedColor = color;
                
                // Update check icons
                for (int i = 0; i < colorPicker.getChildCount(); i++) {
                    View child = colorPicker.getChildAt(i);
                    child.findViewById(R.id.check_icon).setVisibility(
                        child == v ? View.VISIBLE : View.GONE
                    );
                }
            });

            colorPicker.addView(colorView);
        }

        // Set up button listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        createButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String subject = subjectInput.getText().toString().trim();

            if (title.isEmpty()) {
                titleInput.setError("Please enter a title");
                return;
            }

            // Create new topic
            FlashcardTopic newTopic = new FlashcardTopic(title, 0, selectedColor);
            topics.add(0, newTopic); // Add to beginning of list
            adapter.notifyItemInserted(0);
            recyclerView.smoothScrollToPosition(0);

            dialog.dismiss();
            Toast.makeText(requireContext(), "Topic created successfully", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // Sample data class
    private static class FlashcardTopic {
        String title;
        int cardCount;
        String color;

        FlashcardTopic(String title, int cardCount, String color) {
            this.title = title;
            this.cardCount = cardCount;
            this.color = color;
        }

        String getTitle() {
            return title;
        }
    }

    // RecyclerView Adapter
    private class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.ViewHolder> {
        private List<FlashcardTopic> topics;

        FlashcardAdapter(List<FlashcardTopic> topics) {
            this.topics = topics;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_flashcard, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            FlashcardTopic topic = topics.get(position);
            holder.topicTitle.setText(topic.title);
            holder.cardCount.setText(topic.cardCount + " cards");
            
            // Set the background color of the LinearLayout inside CardView
            View cardContent = holder.itemView.findViewById(R.id.card_content);
            if (cardContent != null) {
                cardContent.setBackgroundColor(Color.parseColor(topic.color));
            }

            // Set click listener
            holder.itemView.setOnClickListener(v -> onItemClick(topic));
        }

        @Override
        public int getItemCount() {
            return topics.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView topicTitle;
            TextView cardCount;
            ImageView topicIcon;

            ViewHolder(View view) {
                super(view);
                topicTitle = view.findViewById(R.id.topic_title);
                cardCount = view.findViewById(R.id.card_count);
                topicIcon = view.findViewById(R.id.topic_icon);
            }
        }
    }

    public void onItemClick(FlashcardTopic topic) {
        Intent intent = new Intent(getActivity(), ViewFlashcardsActivity.class);
        intent.putExtra("topic", topic.getTitle());
        intent.putExtra("color", topic.color);
        startActivity(intent);
    }
} 