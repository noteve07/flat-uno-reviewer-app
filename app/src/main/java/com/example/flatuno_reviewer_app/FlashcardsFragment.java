package com.example.flatuno_reviewer_app;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
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
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.example.flatuno_reviewer_app.models.Topic;
import com.google.android.material.snackbar.Snackbar;

public class FlashcardsFragment extends Fragment {
    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private List<Topic> topics;
    private String selectedColor = "#FF9AA2"; // Default color
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flashcards, container, false);

        // Initialize database
        dbHelper = new FlashcardDbHelper(requireContext());
        database = dbHelper.getReadableDatabase();

        // Initialize RecyclerView with GridLayoutManager
        recyclerView = view.findViewById(R.id.flashcards_recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        // Load topics from database
        topics = getTopicsFromDatabase();

        // Set up adapter
        adapter = new FlashcardAdapter(topics);
        recyclerView.setAdapter(adapter);

        // FAB click listener
        FloatingActionButton fab = view.findViewById(R.id.fab_add_flashcard);
        fab.setOnClickListener(v -> showAddFlashcardDialog());

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

    private List<Topic> getTopicsFromDatabase() {
        List<Topic> topicList = new ArrayList<>();
        
        Cursor cursor = database.query(
            FlashcardDbHelper.TABLE_TOPICS,
            null,
            null,
            null,
            null,
            null,
            "name ASC"
        );

        while (cursor.moveToNext()) {
            Topic topic = new Topic(
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("color"))
            );
            topic.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            topic.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
            topic.setLastModified(cursor.getLong(cursor.getColumnIndexOrThrow("last_modified")));
            
            // Get flashcard count for this topic
            Cursor countCursor = database.query(
                FlashcardDbHelper.TABLE_FLASHCARDS,
                new String[]{"COUNT(*) as count"},
                "topic_id = ?",
                new String[]{String.valueOf(topic.getId())},
                null,
                null,
                null
            );
            
            if (countCursor.moveToFirst()) {
                topic.setCardCount(countCursor.getInt(countCursor.getColumnIndexOrThrow("count")));
            }
            countCursor.close();
            
            topicList.add(topic);
        }
        cursor.close();
        
        return topicList;
    }

    private void showAddFlashcardDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_flashcard);

        // Initialize views
        TextInputEditText titleInput = dialog.findViewById(R.id.term_input);
        TextInputEditText subjectInput = dialog.findViewById(R.id.description_input);
        MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
        MaterialButton createButton = dialog.findViewById(R.id.add_button);

        // Set up color selection
        int[] colorIds = {
            R.id.color_pink, R.id.color_green, R.id.color_blue, R.id.color_yellow,
            R.id.color_purple, R.id.color_orange, R.id.color_mint, R.id.color_lavender,
            R.id.color_lime, R.id.color_peach
        };

        int[] colors = {
            R.color.color_pink,
            R.color.color_green,
            R.color.color_blue,
            R.color.color_yellow,
            R.color.color_purple,
            R.color.color_orange,
            R.color.color_mint,
            R.color.color_lavender,
            R.color.color_lime,
            R.color.color_peach
        };

        int[] selectedColors = {
            R.color.color_pink_selected,
            R.color.color_green_selected,
            R.color.color_blue_selected,
            R.color.color_yellow_selected,
            R.color.color_purple_selected,
            R.color.color_orange_selected,
            R.color.color_mint_selected,
            R.color.color_lavender_selected,
            R.color.color_lime_selected,
            R.color.color_peach_selected
        };

        // Set initial selected color
        selectedColor = getString(colors[0]);
        ImageView firstColor = dialog.findViewById(colorIds[0]);
        firstColor.setColorFilter(getResources().getColor(selectedColors[0], null));

        for (int i = 0; i < colorIds.length; i++) {
            final int colorIndex = i;
            final String color = getString(colors[colorIndex]);
            ImageView colorView = dialog.findViewById(colorIds[colorIndex]);
            
            colorView.setOnClickListener(v -> {
                // Update selected color
                selectedColor = color;
                
                // Update selection UI
                for (int j = 0; j < colorIds.length; j++) {
                    ImageView view = dialog.findViewById(colorIds[j]);
                    if (j == colorIndex) {
                        view.setColorFilter(getResources().getColor(selectedColors[j], null));
                    } else {
                        view.setColorFilter(getResources().getColor(colors[j], null));
                    }
                }
            });
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

            // Create new topic in database
            Topic newTopic = new Topic(title, selectedColor);
            long topicId = insertTopic(newTopic);
            if (topicId != -1) {
                newTopic.setId(topicId);
                topics.add(0, newTopic); // Add to beginning of list
                adapter.notifyItemInserted(0);
                recyclerView.smoothScrollToPosition(0);
                dialog.dismiss();
                Toast.makeText(requireContext(), "Topic created successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to create topic", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private long insertTopic(Topic topic) {
        android.content.ContentValues values = new android.content.ContentValues();
        values.put("name", topic.getName());
        values.put("color", topic.getColor());
        values.put("created_at", topic.getCreatedAt());
        values.put("last_modified", topic.getLastModified());
        return database.insert(FlashcardDbHelper.TABLE_TOPICS, null, values);
    }

    // RecyclerView Adapter
    private class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.ViewHolder> {
        private List<Topic> topics;

        FlashcardAdapter(List<Topic> topics) {
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
            Topic topic = topics.get(position);
            holder.topicTitle.setText(topic.getName());
            holder.cardCount.setText(topic.getCardCount() + " cards");
            
            // Set the background color of the LinearLayout inside CardView
            View cardContent = holder.itemView.findViewById(R.id.card_content);
            if (cardContent != null) {
                cardContent.setBackgroundColor(Color.parseColor(topic.getColor()));
            }

            // Set click listener for normal click
            holder.itemView.setOnClickListener(v -> onItemClick(topic));

            // Set long click listener for update/delete options
            holder.itemView.setOnLongClickListener(v -> {
                showTopicOptionsDialog(topic, position);
                return true;
            });
        }

        private void showTopicOptionsDialog(Topic topic, int position) {
            Dialog dialog = new Dialog(requireContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_topic_options);

            MaterialButton updateButton = dialog.findViewById(R.id.update_button);
            MaterialButton deleteButton = dialog.findViewById(R.id.delete_button);
            MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);

            updateButton.setOnClickListener(v -> {
                dialog.dismiss();
                showUpdateTopicDialog(topic, position);
            });

            deleteButton.setOnClickListener(v -> {
                dialog.dismiss();
                showDeleteConfirmationDialog(topic, position);
            });

            cancelButton.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        private void showUpdateTopicDialog(Topic topic, int position) {
            Dialog dialog = new Dialog(requireContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_update_topic);

            TextInputEditText titleInput = dialog.findViewById(R.id.topic_name_input);
            MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
            MaterialButton updateButton = dialog.findViewById(R.id.update_button);

            // Set current topic name
            titleInput.setText(topic.getName());

            cancelButton.setOnClickListener(v -> dialog.dismiss());

            updateButton.setOnClickListener(v -> {
                String newName = titleInput.getText().toString().trim();
                if (newName.isEmpty()) {
                    titleInput.setError("Please enter a topic name");
                    return;
                }

                // Update topic in database
                android.content.ContentValues values = new android.content.ContentValues();
                values.put("name", newName);
                values.put("last_modified", System.currentTimeMillis());

                int rowsAffected = database.update(
                    FlashcardDbHelper.TABLE_TOPICS,
                    values,
                    "id = ?",
                    new String[]{String.valueOf(topic.getId())}
                );

                if (rowsAffected > 0) {
                    // Update local data
                    topic.setName(newName);
                    topic.setLastModified(System.currentTimeMillis());
                    notifyItemChanged(position);
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Topic updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update topic", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();
        }

        private void showDeleteConfirmationDialog(Topic topic, int position) {
            Dialog dialog = new Dialog(requireContext());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_delete_topic);

            TextView messageText = dialog.findViewById(R.id.message_text);
            MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
            MaterialButton deleteButton = dialog.findViewById(R.id.delete_button);

            messageText.setText("Are you sure you want to delete \"" + topic.getName() + "\"? This will also delete all flashcards and quizzes in this topic.");

            cancelButton.setOnClickListener(v -> dialog.dismiss());

            deleteButton.setOnClickListener(v -> {
                // Delete topic and all related data from database
                database.beginTransaction();
                try {
                    // Get all quiz IDs for this topic
                    Cursor quizCursor = database.query(
                        FlashcardDbHelper.TABLE_QUIZZES,
                        new String[]{"id"},
                        "topic_id = ?",
                        new String[]{String.valueOf(topic.getId())},
                        null,
                        null,
                        null
                    );

                    List<Long> quizIds = new ArrayList<>();
                    while (quizCursor.moveToNext()) {
                        quizIds.add(quizCursor.getLong(quizCursor.getColumnIndexOrThrow("id")));
                    }
                    quizCursor.close();

                    // For each quiz, delete its choices and questions
                    for (Long quizId : quizIds) {
                        // Get all question IDs for this quiz
                        Cursor questionCursor = database.query(
                            FlashcardDbHelper.TABLE_QUIZ_QUESTIONS,
                            new String[]{"id"},
                            "quiz_id = ?",
                            new String[]{String.valueOf(quizId)},
                            null,
                            null,
                            null
                        );

                        List<Long> questionIds = new ArrayList<>();
                        while (questionCursor.moveToNext()) {
                            questionIds.add(questionCursor.getLong(questionCursor.getColumnIndexOrThrow("id")));
                        }
                        questionCursor.close();

                        // Delete choices for each question
                        for (Long questionId : questionIds) {
                            database.delete(
                                FlashcardDbHelper.TABLE_QUIZ_CHOICES,
                                "question_id = ?",
                                new String[]{String.valueOf(questionId)}
                            );
                        }

                        // Delete questions for this quiz
                        database.delete(
                            FlashcardDbHelper.TABLE_QUIZ_QUESTIONS,
                            "quiz_id = ?",
                            new String[]{String.valueOf(quizId)}
                        );

                        // Delete quiz scores
                        database.delete(
                            FlashcardDbHelper.TABLE_QUIZ_SCORES,
                            "quiz_id = ?",
                            new String[]{String.valueOf(quizId)}
                        );
                    }

                    // Delete quizzes for this topic
                    database.delete(
                        FlashcardDbHelper.TABLE_QUIZZES,
                        "topic_id = ?",
                        new String[]{String.valueOf(topic.getId())}
                    );

                    // Delete flashcards for this topic
                    database.delete(
                        FlashcardDbHelper.TABLE_FLASHCARDS,
                        "topic_id = ?",
                        new String[]{String.valueOf(topic.getId())}
                    );

                    // Finally, delete the topic
                    int topicDeleted = database.delete(
                        FlashcardDbHelper.TABLE_TOPICS,
                        "id = ?",
                        new String[]{String.valueOf(topic.getId())}
                    );

                    if (topicDeleted > 0) {
                        database.setTransactionSuccessful();
                        // Update local data
                        topics.remove(position);
                        notifyItemRemoved(position);
                        dialog.dismiss();
                        
                        // Show undo snackbar
                        View rootView = requireActivity().findViewById(android.R.id.content);
                        Snackbar snackbar = Snackbar.make(rootView, "Topic deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", view -> {
                                // Restore topic and flashcards
                                restoreTopic(topic, position);
                            });
                        snackbar.show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete topic", Toast.LENGTH_SHORT).show();
                    }
                } finally {
                    database.endTransaction();
                }
            });

            dialog.show();
        }

        private void restoreTopic(Topic topic, int position) {
            database.beginTransaction();
            try {
                // Restore topic
                android.content.ContentValues topicValues = new android.content.ContentValues();
                topicValues.put("name", topic.getName());
                topicValues.put("color", topic.getColor());
                topicValues.put("created_at", topic.getCreatedAt());
                topicValues.put("last_modified", System.currentTimeMillis());

                long topicId = database.insert(FlashcardDbHelper.TABLE_TOPICS, null, topicValues);
                
                if (topicId != -1) {
                    database.setTransactionSuccessful();
                    // Update local data
                    topic.setId(topicId);
                    topics.add(position, topic);
                    notifyItemInserted(position);
                    Toast.makeText(requireContext(), "Topic restored", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to restore topic", Toast.LENGTH_SHORT).show();
                }
            } finally {
                database.endTransaction();
            }
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

    public void onItemClick(Topic topic) {
        Intent intent = new Intent(getActivity(), ViewFlashcardsActivity.class);
        intent.putExtra("topic", topic.getName());
        intent.putExtra("color", topic.getColor());
        startActivity(intent);
    }
} 