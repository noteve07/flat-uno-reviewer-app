package com.example.flatuno_reviewer_app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
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

public class ViewFlashcardsActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private FlashcardAdapter adapter;
    private List<Flashcard> flashcards;
    private LinearLayout emptyState;
    private MaterialButton addCardButton;
    private MaterialButton addFirstCardButton;
    private String currentTopic;
    private String topicColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_flashcards);

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
            addCardButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            addFirstCardButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        }

        // Load sample flashcards based on topic
        flashcards = getSampleFlashcards(currentTopic);
        
        // Set up adapter with topic color
        adapter = new FlashcardAdapter(flashcards, topicColor);
        viewPager.setAdapter(adapter);

        // Show empty state if no flashcards
        updateEmptyState();

        // Set up button listeners
        addCardButton.setOnClickListener(v -> showAddCardDialog());
        addFirstCardButton.setOnClickListener(v -> showAddCardDialog());
        
        MaterialButton finishButton = findViewById(R.id.finish_button);
        finishButton.setOnClickListener(v -> finish());
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

    private void showAddCardDialog() {
        // TODO: Implement add card dialog
        // For now, just add a sample card
        flashcards.add(new Flashcard(
            "Sample description for " + currentTopic,
            "Sample term"
        ));
        adapter.notifyItemInserted(flashcards.size() - 1);
        updateEmptyState();
    }

    private List<Flashcard> getSampleFlashcards(String topic) {
        List<Flashcard> cards = new ArrayList<>();
        
        switch (topic) {
            case "OOP Concepts":
                cards.add(new Flashcard(
                    "The bundling of data and methods that operate on that data within a single unit (class)",
                    "Encapsulation"
                ));
                cards.add(new Flashcard(
                    "A mechanism that allows a class to inherit properties and methods from another class",
                    "Inheritance"
                ));
                cards.add(new Flashcard(
                    "The ability of an object to take many forms and behave differently based on the context",
                    "Polymorphism"
                ));
                cards.add(new Flashcard(
                    "Hiding complex implementation details and showing only necessary features",
                    "Abstraction"
                ));
                break;

            case "German Verbs":
                cards.add(new Flashcard(
                    "The German verb meaning 'to be'",
                    "sein"
                ));
                cards.add(new Flashcard(
                    "The German verb meaning 'to have'",
                    "haben"
                ));
                cards.add(new Flashcard(
                    "The German verb meaning 'to go'",
                    "gehen"
                ));
                cards.add(new Flashcard(
                    "The German verb meaning 'to come'",
                    "kommen"
                ));
                break;

            case "Rizal's Poems":
                cards.add(new Flashcard(
                    "The title of Rizal's poem about his love for his country",
                    "Mi Ultimo Adios"
                ));
                cards.add(new Flashcard(
                    "The meaning of 'Mi Ultimo Adios' in English",
                    "My Last Farewell"
                ));
                cards.add(new Flashcard(
                    "The place where 'Mi Ultimo Adios' was written",
                    "Fort Santiago"
                ));
                cards.add(new Flashcard(
                    "The date when 'Mi Ultimo Adios' was written",
                    "December 30, 1896"
                ));
                break;

            default:
                // No sample cards for other topics
                break;
        }
        
        return cards;
    }

    private static class Flashcard {
        String description;
        String term;

        Flashcard(String description, String term) {
            this.description = description;
            this.term = term;
        }
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

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_flashcard_view, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Flashcard card = flashcards.get(position);
            holder.frontText.setText(card.description);
            holder.backText.setText(card.term);

            // Set card background colors
            if (cardColor != null) {
                int color = Color.parseColor(cardColor);
                // Make front slightly darker for better contrast
                float[] hsv = new float[3];
                Color.colorToHSV(color, hsv);
                hsv[2] = Math.max(0.0f, hsv[2] * 0.95f); // Slightly decrease brightness
                holder.frontView.setBackgroundColor(Color.HSVToColor(hsv));
                
                // Create a darker shade for the back
                hsv[1] = Math.min(1.0f, hsv[1] * 1.2f); // Increase saturation
                hsv[2] = Math.max(0.0f, hsv[2] * 0.8f); // Decrease brightness
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
                    holder.frontView.setVisibility(View.VISIBLE);
                    holder.backView.setVisibility(View.GONE);
                    holder.itemView.startAnimation(new FlipAnimation(180, 0, holder.itemView));
                } else {
                    // Flip to back
                    holder.frontView.setVisibility(View.GONE);
                    holder.backView.setVisibility(View.VISIBLE);
                    holder.itemView.startAnimation(new FlipAnimation(0, 180, holder.itemView));
                }
                isFlipped[position] = !isFlipped[position];
            });
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

            ViewHolder(View view) {
                super(view);
                frontText = view.findViewById(R.id.card_front_text);
                backText = view.findViewById(R.id.card_back_text);
                frontView = view.findViewById(R.id.card_front);
                backView = view.findViewById(R.id.card_back);
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
            // Set a smaller distance for less perspective distortion
            camera.setLocation(0, 0, -8 * getResources().getDisplayMetrics().density);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float degrees = fromDegrees + ((toDegrees - fromDegrees) * interpolatedTime);
            
            final Matrix matrix = t.getMatrix();
            camera.save();
            
            // Apply 3D rotation
            camera.rotateY(degrees);
            camera.getMatrix(matrix);
            camera.restore();
            
            // Center the rotation
            matrix.preTranslate(-view.getWidth() / 2, -view.getHeight() / 2);
            matrix.postTranslate(view.getWidth() / 2, view.getHeight() / 2);
            
            // Apply perspective with reduced scaling
            matrix.preScale(1, 1, view.getWidth() / 2, view.getHeight() / 2);
        }
    }
} 