package com.example.flatuno_reviewer_app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Transformation;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.example.flatuno_reviewer_app.models.Flashcard;

public class ViewFlashcardsActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private FlashcardAdapter adapter;
    private List<Flashcard> flashcards;
    private LinearLayout emptyState;
    private MaterialButton addCardButton;
    private MaterialButton addFirstCardButton;
    private String currentTopic;
    private String topicColor;
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_flashcards);

        // Initialize database
        dbHelper = new FlashcardDbHelper(this);
        database = dbHelper.getReadableDatabase();

        // Initialize views
        viewPager = findViewById(R.id.flashcards_viewpager);
        emptyState = findViewById(R.id.empty_state);
        addCardButton = findViewById(R.id.add_card_button);
        addFirstCardButton = findViewById(R.id.add_first_card_button);
        
        // Get topic and color from intent
        currentTopic = getIntent().getStringExtra("topic");
        topicColor = getIntent().getStringExtra("color");
        
        if (currentTopic != null) {
            setTitle(currentTopic);
        }

        // Apply topic color to UI elements
        if (topicColor != null) {
            int color = Color.parseColor(topicColor);
            // Make button color match the front card's darker shade
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] = Math.max(0.0f, hsv[2] * 0.85f); // Same brightness reduction as front card
            int darkerColor = Color.HSVToColor(hsv);
            
            addCardButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(darkerColor));
            addFirstCardButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(darkerColor));
        }

        // Load flashcards from database
        flashcards = getFlashcardsFromDatabase(currentTopic);
        
        // Set up adapter with topic color
        adapter = new FlashcardAdapter(flashcards, topicColor);
        viewPager.setAdapter(adapter);

        // Show empty state if no flashcards
        updateEmptyState();

        // Set up button listeners
        addCardButton.setOnClickListener(v -> showAddCardDialog());
        addFirstCardButton.setOnClickListener(v -> showAddCardDialog());
        
        Button finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(v -> finish());
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

    private void updateEmptyState() {
        if (flashcards.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
            addCardButton.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            viewPager.setVisibility(View.VISIBLE);
            addCardButton.setVisibility(View.VISIBLE);
        }
    }

    private void showCustomToast(String message) {
        View layout = getLayoutInflater().inflate(R.layout.custom_toast, null);
        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        android.widget.Toast toast = new android.widget.Toast(getApplicationContext());
        toast.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL, 0, 200);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    private void showAddCardDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_flashcard_card);

        // Initialize views
        com.google.android.material.textfield.TextInputEditText termInput = dialog.findViewById(R.id.term_input);
        com.google.android.material.textfield.TextInputEditText descriptionInput = dialog.findViewById(R.id.description_input);
        com.google.android.material.button.MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
        com.google.android.material.button.MaterialButton addButton = dialog.findViewById(R.id.add_button);

        // Set up button listeners
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        addButton.setOnClickListener(v -> {
            String term = termInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (term.isEmpty()) {
                termInput.setError("Please enter a term");
                return;
            }

            if (description.isEmpty()) {
                descriptionInput.setError("Please enter a description");
                return;
            }

            // Get topic ID
            long topicId = getTopicId(currentTopic);
            if (topicId == -1) {
                showCustomToast("Error: Topic not found");
                return;
            }

            // Add new flashcard to database
            com.example.flatuno_reviewer_app.database.InitializeData initializeData = 
                new com.example.flatuno_reviewer_app.database.InitializeData(this);
            long flashcardId = initializeData.insertFlashcard(initializeData.database, topicId, term, description);
            
            if (flashcardId != -1) {
                // Create new flashcard object
                Flashcard newCard = new Flashcard(topicId, term, description);
                newCard.setId(flashcardId);
                newCard.setCreatedAt(System.currentTimeMillis());
                newCard.setLastReviewed(System.currentTimeMillis());
                newCard.setColor(topicColor);

                // Add to list and update UI using the adapter's method
                adapter.addFlashcard(newCard);
                // Move to the last card (newly added card)
                viewPager.setCurrentItem(flashcards.size() - 1, true);
                updateEmptyState();
                dialog.dismiss();
                showCustomToast("Flashcard added successfully");
            } else {
                showCustomToast("Failed to add flashcard");
            }
        });

        dialog.show();
    }

    private long getTopicId(String topicName) {
        Cursor cursor = database.query(
            FlashcardDbHelper.TABLE_TOPICS,
            new String[]{"id"},
            "name = ?",
            new String[]{topicName},
            null,
            null,
            null
        );

        long topicId = -1;
        if (cursor.moveToFirst()) {
            topicId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();
        return topicId;
    }

    private List<Flashcard> getFlashcardsFromDatabase(String topicName) {
        List<Flashcard> flashcardList = new ArrayList<>();
        
        // First get the topic ID
        long topicId = getTopicId(topicName);
        if (topicId == -1) return flashcardList;

        // Query flashcards for this topic
        Cursor cursor = database.query(
            FlashcardDbHelper.TABLE_FLASHCARDS,
            null,
            "topic_id = ?",
            new String[]{String.valueOf(topicId)},
            null,
            null,
            "created_at DESC"
        );

        while (cursor.moveToNext()) {
            Flashcard flashcard = new Flashcard(
                cursor.getLong(cursor.getColumnIndexOrThrow("topic_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("term")),
                cursor.getString(cursor.getColumnIndexOrThrow("description"))
            );
            flashcard.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
            flashcard.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
            flashcard.setLastReviewed(cursor.getLong(cursor.getColumnIndexOrThrow("last_reviewed")));
            flashcard.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
            flashcardList.add(flashcard);
        }
        cursor.close();
        
        return flashcardList;
    }

    private class FlashcardAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<FlashcardAdapter.ViewHolder> {
        private List<Flashcard> flashcards;
        private boolean[] isFlipped;
        private String cardColor;

        FlashcardAdapter(List<Flashcard> flashcards, String color) {
            this.flashcards = flashcards;
            this.isFlipped = new boolean[flashcards.size()];
            this.cardColor = color;
        }

        public void addFlashcard(Flashcard flashcard) {
            int position = flashcards.size();
            flashcards.add(flashcard);
            // Create new array with increased size
            boolean[] newIsFlipped = new boolean[flashcards.size()];
            // Copy existing values
            System.arraycopy(isFlipped, 0, newIsFlipped, 0, isFlipped.length);
            isFlipped = newIsFlipped;
            notifyItemInserted(position);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_flashcard_view, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Flashcard card = flashcards.get(position);
            holder.frontText.setText(card.getDescription());
            holder.backText.setText(card.getTerm());

            // Set card background colors
            if (cardColor != null) {
                int color = Color.parseColor(cardColor);
                // Make front slightly darker for better contrast
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[2] = Math.max(0.0f, hsv[2] * 0.85f); // Slightly decrease brightness
                holder.frontView.setBackgroundColor(Color.HSVToColor(hsv));
                
                // Create a darker shade for the back
                hsv[1] = Math.min(1.0f, hsv[1] * 1.2f); // Increase saturation
                hsv[2] = Math.max(0.0f, hsv[2] * 0.85f); // Decrease brightness
                holder.backView.setBackgroundColor(Color.HSVToColor(hsv));
            }

            // Reset card state
            holder.frontView.setVisibility(View.VISIBLE);
            holder.backView.setVisibility(View.GONE);
            isFlipped[position] = false;

            // Set click listener for flip animation
            holder.itemView.setOnClickListener(v -> {
                if (isFlipped[position]) {
                    // Flip back
                    holder.itemView.startAnimation(new FlipAnimation(180, 0, holder.itemView));
                    holder.itemView.postDelayed(() -> {
                        holder.frontView.setVisibility(View.VISIBLE);
                        holder.backView.setVisibility(View.GONE);
                    }, 100);
                } else {
                    // Flip to back
                    holder.itemView.startAnimation(new FlipAnimation(0, 180, holder.itemView));
                    holder.itemView.postDelayed(() -> {
                        holder.frontView.setVisibility(View.GONE);
                        holder.backView.setVisibility(View.VISIBLE);
                    }, 100);
                }
                isFlipped[position] = !isFlipped[position];
            });

            // Set menu button click listener
            holder.menuButton.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.flashcard_menu, popup.getMenu());
                
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        showEditFlashcardDialog(card, position);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        showDeleteConfirmationDialog(card, position);
                        return true;
                    }
                    return false;
                });
                
                popup.show();
            });
        }

        private void showEditFlashcardDialog(Flashcard card, int position) {
            android.app.Dialog dialog = new android.app.Dialog(ViewFlashcardsActivity.this);
            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_add_flashcard_card);

            // Initialize views
            com.google.android.material.textfield.TextInputEditText termInput = dialog.findViewById(R.id.term_input);
            com.google.android.material.textfield.TextInputEditText descriptionInput = dialog.findViewById(R.id.description_input);
            com.google.android.material.button.MaterialButton cancelButton = dialog.findViewById(R.id.cancel_button);
            com.google.android.material.button.MaterialButton addButton = dialog.findViewById(R.id.add_button);

            // Set current values
            termInput.setText(card.getTerm());
            descriptionInput.setText(card.getDescription());
            addButton.setText("Update");

            // Set up button listeners
            cancelButton.setOnClickListener(v -> dialog.dismiss());

            addButton.setOnClickListener(v -> {
                String term = termInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();

                if (term.isEmpty()) {
                    termInput.setError("Please enter a term");
                    return;
                }

                if (description.isEmpty()) {
                    descriptionInput.setError("Please enter a description");
                    return;
                }

                // Update flashcard in database
                android.content.ContentValues values = new android.content.ContentValues();
                values.put("term", term);
                values.put("description", description);
                values.put("last_reviewed", System.currentTimeMillis());

                int rowsAffected = database.update(
                    FlashcardDbHelper.TABLE_FLASHCARDS,
                    values,
                    "id = ?",
                    new String[]{String.valueOf(card.getId())}
                );

                if (rowsAffected > 0) {
                    // Update local data
                    card.setTerm(term);
                    card.setDescription(description);
                    card.setLastReviewed(System.currentTimeMillis());
                    notifyItemChanged(position);
                    dialog.dismiss();
                    showCustomToast("Flashcard updated successfully");
                } else {
                    showCustomToast("Failed to update flashcard");
                }
            });

            dialog.show();
        }

        private void showDeleteConfirmationDialog(Flashcard card, int position) {
            new androidx.appcompat.app.AlertDialog.Builder(ViewFlashcardsActivity.this)
                .setTitle("Delete Flashcard")
                .setMessage("Are you sure you want to delete this flashcard?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from database
                    int rowsAffected = database.delete(
                        FlashcardDbHelper.TABLE_FLASHCARDS,
                        "id = ?",
                        new String[]{String.valueOf(card.getId())}
                    );

                    if (rowsAffected > 0) {
                        // Remove from list
                        flashcards.remove(position);
                        // Create new array with decreased size
                        boolean[] newIsFlipped = new boolean[flashcards.size()];
                        // Copy existing values
                        System.arraycopy(isFlipped, 0, newIsFlipped, 0, position);
                        System.arraycopy(isFlipped, position + 1, newIsFlipped, position, flashcards.size() - position);
                        isFlipped = newIsFlipped;
                        notifyItemRemoved(position);
                        updateEmptyState();
                        showCustomToast("Flashcard deleted successfully");
                    } else {
                        showCustomToast("Failed to delete flashcard");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        }

        @Override
        public int getItemCount() {
            return flashcards.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView frontText;
            TextView backText;
            View frontView;
            View backView;
            ImageButton menuButton;

            ViewHolder(View view) {
                super(view);
                frontText = view.findViewById(R.id.card_front_text);
                backText = view.findViewById(R.id.card_back_text);
                frontView = view.findViewById(R.id.card_front);
                backView = view.findViewById(R.id.card_back);
                menuButton = view.findViewById(R.id.menu_button);
            }
        }
    }

    private class FlipAnimation extends Animation {
        private final float fromDegrees;
        private final float toDegrees;
        private final View view;
        private Camera camera;

        FlipAnimation(float fromDegrees, float toDegrees, View view) {
            this.fromDegrees = fromDegrees;
            this.toDegrees = toDegrees;
            this.view = view;
            setDuration(300);
            setInterpolator(new DecelerateInterpolator());
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
            camera = new Camera();
            camera.setLocation(0, 0, -8 * getResources().getDisplayMetrics().density);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float degrees = fromDegrees + ((toDegrees - fromDegrees) * interpolatedTime);
            
            final Matrix matrix = t.getMatrix();
            camera.save();
            
            camera.rotateY(degrees);
            camera.getMatrix(matrix);
            camera.restore();
            
            matrix.preTranslate(-view.getWidth() / 2, -view.getHeight() / 2);
            matrix.postTranslate(view.getWidth() / 2, view.getHeight() / 2);
            matrix.preScale(1, 1, view.getWidth() / 2, view.getHeight() / 2);
            
            if (degrees > 90 || degrees < -90) {
                matrix.preScale(-1, 1, view.getWidth() / 2, view.getHeight() / 2);
            }
        }
    }
} 