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

    public void onItemClick(Topic topic) {
        Intent intent = new Intent(getActivity(), ViewFlashcardsActivity.class);
        intent.putExtra("topic", topic.getName());
        intent.putExtra("color", topic.getColor());
        startActivity(intent);
    }
} 