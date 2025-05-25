package com.example.flatuno_reviewer_app;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.example.flatuno_reviewer_app.models.Flashcard;

public class ViewFlashcardsActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private static final int AMPLITUDE_THRESHOLD = 3000; // Increased from 1000 to 5000 - only detect louder/closer sounds
    private static final int PRE_FLIP_DELAY = 1500; // Wait 2 seconds after sound before flipping
    private static final int POST_FLIP_DELAY = 1500; // Wait 1.5 seconds after flip before next card
    private static final int DEBUG_UPDATE_INTERVAL = 1000; // Show debug info every 1 second

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private ImageButton voiceButton;
    private long lastFlipTime = 0;
    private boolean canFlip = true;
    private boolean isWaitingForFlip = false;
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
    private Handler debugHandler = new Handler(Looper.getMainLooper());
    private double lastAmplitude = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_flashcards);

        // Initialize voice button first
        voiceButton = findViewById(R.id.voice_button);
        voiceButton.setOnClickListener(v -> {
            if (checkAndRequestPermission()) {
                toggleAudioRecording();
            }
        });

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
    protected void onPause() {
        super.onPause();
        // Stop recording when activity is paused
        stopAudioRecording();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop recording when activity is stopped
        stopAudioRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop recording and clean up
        stopAudioRecording();
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

        public class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
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
                
                // Initialize the tag to track flip state
                view.setTag(false);
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

    private boolean checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                    != PackageManager.PERMISSION_GRANTED) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Microphone Permission Required")
                    .setMessage("Sound detection needs microphone access to work. Please grant permission.")
                    .setPositiveButton("Grant Permission", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, 
                            new String[]{Manifest.permission.RECORD_AUDIO}, 
                            PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return false;
            }
        }
        return true;
    }

    private void toggleAudioRecording() {
        if (!isRecording) {
            // Only start if we have permission
            if (checkAndRequestPermission()) {
                startAudioRecording();
            }
        } else {
            stopAudioRecording();
        }
    }

    private void startAudioRecording() {
        // Don't start if already recording
        if (isRecording) return;

        if (audioRecord == null) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        }

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            isRecording = true;
            audioRecord.startRecording();
            voiceButton.setImageResource(R.drawable.ic_mic_active);
            Toast.makeText(this, "Voice recognition ON - Speak your answer", Toast.LENGTH_SHORT).show();

            // Start debug updates
            startDebugUpdates();

            recordingThread = new Thread(() -> {
                short[] buffer = new short[BUFFER_SIZE];
                while (isRecording) {
                    try {
                        int read = audioRecord.read(buffer, 0, BUFFER_SIZE);
                        if (read > 0 && isRecording) { // Double check isRecording
                            // Calculate average amplitude
                            double sum = 0;
                            for (int i = 0; i < read; i++) {
                                sum += Math.abs(buffer[i]);
                            }
                            lastAmplitude = sum / read;

                            // Check if sound level is above threshold and we're not in a waiting period
                            long currentTime = System.currentTimeMillis();
                            if (lastAmplitude > AMPLITUDE_THRESHOLD && canFlip && !isWaitingForFlip && 
                                (currentTime - lastFlipTime) > (PRE_FLIP_DELAY + POST_FLIP_DELAY)) {
                                
                                // Start the flip sequence
                                isWaitingForFlip = true;
                                canFlip = false;
                                
                                // Store current position before starting the sequence
                                final int currentPosition = viewPager.getCurrentItem();
                                
                                // Show that we heard something
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (isRecording) { // Check if still recording
                                        showCustomToast("Answer detected! Preparing to flip...");
                                    }
                                });

                                // Wait 2 seconds before flipping
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    if (!isRecording) return; // Stop if no longer recording
                                    
                                    // Get the current view and flip
                                    View currentView = ((ViewGroup) viewPager.getChildAt(0)).getChildAt(0);
                                    if (currentView != null) {
                                        currentView.performClick();
                                        showCustomToast("Flipping card...");
                                        lastFlipTime = System.currentTimeMillis();
                                        
                                        // Wait 1.5 seconds after flip before moving to next card
                                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                            if (!isRecording) return; // Stop if no longer recording
                                            
                                            // Use the stored position to check if we're at the last card
                                            if (currentPosition < flashcards.size() - 1) {
                                                // Move to next card
                                                viewPager.setCurrentItem(currentPosition + 1, true);
                                                showCustomToast("Moving to next card...");
                                            } else {
                                                // We're at the last card, finish
                                                showCustomToast("Last card reached - finishing...");
                                                finish();
                                            }
                                            
                                            // Reset state for next answer
                                            isWaitingForFlip = false;
                                            canFlip = true;
                                        }, POST_FLIP_DELAY);
                                    }
                                }, PRE_FLIP_DELAY);
                            }
                        }
                    } catch (Exception e) {
                        // If there's an error, stop recording
                        new Handler(Looper.getMainLooper()).post(() -> {
                            stopAudioRecording();
                            Toast.makeText(ViewFlashcardsActivity.this, 
                                "Error in voice recognition: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
                        break;
                    }
                }
            });
            recordingThread.start();
        } else {
            Toast.makeText(this, "Failed to initialize voice recognition. Error code: " + 
                audioRecord.getState(), Toast.LENGTH_LONG).show();
            stopAudioRecording();
        }
    }

    private void startDebugUpdates() {
        debugHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    String status = isWaitingForFlip ? "Waiting..." : "Listening...";
                    // Show the current amplitude as a percentage of the threshold
                    int percentage = (int)((lastAmplitude / AMPLITUDE_THRESHOLD) * 100);
                    String debugMsg = String.format("%s Sound level: %d%% (Need: 100%%)", 
                        status, Math.min(percentage, 100));
                    Toast.makeText(ViewFlashcardsActivity.this, debugMsg, Toast.LENGTH_SHORT).show();
                    
                    // Schedule next update
                    debugHandler.postDelayed(this, DEBUG_UPDATE_INTERVAL);
                }
            }
        }, DEBUG_UPDATE_INTERVAL);
    }

    private void stopAudioRecording() {
        isRecording = false;
        isWaitingForFlip = false;
        canFlip = true;
        debugHandler.removeCallbacksAndMessages(null); // Stop debug updates
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
            audioRecord = null;
        }
        
        if (recordingThread != null) {
            recordingThread.interrupt();
            recordingThread = null;
        }
        
        voiceButton.setImageResource(R.drawable.ic_mic);
        Toast.makeText(this, "Voice recognition OFF", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, can start recording
            } else {
                Toast.makeText(this, 
                    "Sound detection requires microphone permission. Please grant it in Settings.", 
                    Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }
} 