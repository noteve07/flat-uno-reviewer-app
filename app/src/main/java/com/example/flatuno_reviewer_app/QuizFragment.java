package com.example.flatuno_reviewer_app;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.PopupMenu;
import java.util.ArrayList;
import java.util.List;
import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.example.flatuno_reviewer_app.models.Quiz;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class QuizFragment extends Fragment {
    private RecyclerView recyclerView;
    private QuizAdapter adapter;
    private List<Quiz> quizzes;
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;
    private ActivityResultLauncher<Intent> quizLauncher;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize ActivityResultLauncher
        quizLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Refresh quiz list when returning from quiz
                refreshQuizList();
            }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quiz, container, false);

        // Initialize database
        dbHelper = new FlashcardDbHelper(requireContext());
        database = dbHelper.getReadableDatabase();

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshQuizList);

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.quiz_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load quizzes from database
        quizzes = getQuizzesFromDatabase();

        // Set up adapter
        adapter = new QuizAdapter(quizzes);
        recyclerView.setAdapter(adapter);

        // Set up swipe functionality
        setupSwipeActions();

        // FAB click listener with popup menu
        FloatingActionButton fab = view.findViewById(R.id.fab_add_quiz);
        fab.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), fab, android.view.Gravity.TOP);
            popup.getMenuInflater().inflate(R.menu.menu_add_quiz, popup.getMenu());
            
            // Show the popup
            popup.show();
            
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_add_manual) {
                    Intent intent = new Intent(getActivity(), CreateQuizActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.action_generate_ai) {
                    Intent intent = new Intent(getActivity(), GenerateQuizActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        });

        return view;
    }

    private void refreshQuizList() {
        // Clear existing list
        quizzes.clear();
        // Get fresh data from database
        quizzes.addAll(getQuizzesFromDatabase());
        // Notify adapter that all data has changed
        adapter.notifyDataSetChanged();
        // Stop refresh animation
        swipeRefreshLayout.setRefreshing(false);
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

    private List<Quiz> getQuizzesFromDatabase() {
        List<Quiz> quizList = new ArrayList<>();
        
        Cursor cursor = database.query(
            FlashcardDbHelper.TABLE_QUIZZES,
            null,
            null,
            null,
            null,
            null,
            "created_at DESC"
        );

        while (cursor.moveToNext()) {
            Quiz quiz = new Quiz(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getLong(cursor.getColumnIndexOrThrow("topic_id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("last_taken"))
            );
            
            // Get question count for this quiz
            Cursor countCursor = database.query(
                FlashcardDbHelper.TABLE_QUIZ_QUESTIONS,
                new String[]{"COUNT(*) as count"},
                "quiz_id = ?",
                new String[]{String.valueOf(quiz.getId())},
                null,
                null,
                null
            );
            
            if (countCursor.moveToFirst()) {
                quiz.setQuestionCount(countCursor.getInt(countCursor.getColumnIndexOrThrow("count")));
            }
            countCursor.close();
            
            // Get most recent score for this quiz
            Cursor scoreCursor = database.query(
                FlashcardDbHelper.TABLE_QUIZ_SCORES,
                new String[]{"score", "total_questions", "taken_at"},
                "quiz_id = ?",
                new String[]{String.valueOf(quiz.getId())},
                null,
                null,
                "taken_at DESC",  // Order by taken_at DESC to get most recent
                "1"  // Limit to 1 to get only the most recent score
            );
            
            if (scoreCursor.moveToFirst()) {
                int score = scoreCursor.getInt(scoreCursor.getColumnIndexOrThrow("score"));
                int totalQuestions = scoreCursor.getInt(scoreCursor.getColumnIndexOrThrow("total_questions"));
                int percentage = (score * 100) / totalQuestions;
                quiz.setLastScore("Last score: " + percentage + "%");
            } else {
                quiz.setLastScore("Not attempted yet");
            }
            scoreCursor.close();
            
            // Get topic name
            Cursor topicCursor = database.query(
                FlashcardDbHelper.TABLE_TOPICS,
                new String[]{"name"},
                "id = ?",
                new String[]{String.valueOf(quiz.getTopicId())},
                null,
                null,
                null
            );
            
            if (topicCursor.moveToFirst()) {
                quiz.setTopicName(topicCursor.getString(topicCursor.getColumnIndexOrThrow("name")));
            }
            topicCursor.close();
            
            quizList.add(quiz);
        }
        cursor.close();
        
        return quizList;
    }

    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, 
            ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            
            private static final float SWIPE_THRESHOLD = 0.25f; // 25% of item width
            private static final float SWIPE_VELOCITY_THRESHOLD = 0.5f;
            
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, 
                                RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Quiz quiz = quizzes.get(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // Delete quiz
                    showDeleteConfirmationDialog(quiz);
                } else if (direction == ItemTouchHelper.LEFT) {
                    // Edit quiz
                    showEditDialog(quiz);
                }
            }

            @Override
            public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
                return SWIPE_THRESHOLD;
            }

            @Override
            public float getSwipeVelocityThreshold(float defaultValue) {
                return defaultValue * SWIPE_VELOCITY_THRESHOLD;
            }

            @Override
            public void onChildDraw(android.graphics.Canvas c, RecyclerView recyclerView, 
                                  RecyclerView.ViewHolder viewHolder, float dX, float dY, 
                                  int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float maxSwipeDistance = itemView.getWidth() * 0.3f; // 30% of item width
                
                // Limit swipe distance
                if (dX > 0) { // Swiping right (delete)
                    dX = Math.min(dX, maxSwipeDistance);
                } else if (dX < 0) { // Swiping left (edit)
                    dX = Math.max(dX, -maxSwipeDistance);
                }
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmationDialog(Quiz quiz) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Quiz")
            .setMessage("Are you sure you want to delete this quiz? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteQuiz(quiz);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                adapter.notifyItemChanged(quizzes.indexOf(quiz));
            })
            .setCancelable(false)
            .show();
    }

    private void deleteQuiz(Quiz quiz) {
        database.beginTransaction();
        try {
            // Get all question IDs for this quiz
            Cursor questionCursor = database.query(
                FlashcardDbHelper.TABLE_QUIZ_QUESTIONS,
                new String[]{"id"},
                "quiz_id = ?",
                new String[]{String.valueOf(quiz.getId())},
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

            // Delete questions
            database.delete(
                FlashcardDbHelper.TABLE_QUIZ_QUESTIONS,
                "quiz_id = ?",
                new String[]{String.valueOf(quiz.getId())}
            );

            // Delete quiz scores
            database.delete(
                FlashcardDbHelper.TABLE_QUIZ_SCORES,
                "quiz_id = ?",
                new String[]{String.valueOf(quiz.getId())}
            );

            // Delete quiz
            database.delete(
                FlashcardDbHelper.TABLE_QUIZZES,
                "id = ?",
                new String[]{String.valueOf(quiz.getId())}
            );

            database.setTransactionSuccessful();
            
            // Update UI
            int position = quizzes.indexOf(quiz);
            quizzes.remove(position);
            adapter.notifyItemRemoved(position);
            
            Toast.makeText(requireContext(), "Quiz deleted successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error deleting quiz", Toast.LENGTH_SHORT).show();
        } finally {
            database.endTransaction();
        }
    }

    private void showEditDialog(Quiz quiz) {
        // Create dialog for editing quiz title
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_quiz, null);
        
        TextView titleInput = dialogView.findViewById(R.id.quiz_title_input);
        titleInput.setText(quiz.getTitle());
        
        builder.setView(dialogView)
            .setTitle("Edit Quiz")
            .setPositiveButton("Save", (dialog, which) -> {
                String newTitle = titleInput.getText().toString().trim();
                if (!newTitle.isEmpty()) {
                    updateQuizTitle(quiz, newTitle);
                } else {
                    Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    adapter.notifyItemChanged(quizzes.indexOf(quiz));
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                adapter.notifyItemChanged(quizzes.indexOf(quiz));
            })
            .setCancelable(false)
            .show();
    }

    private void updateQuizTitle(Quiz quiz, String newTitle) {
        database.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("title", newTitle);
            
            int rowsAffected = database.update(
                FlashcardDbHelper.TABLE_QUIZZES,
                values,
                "id = ?",
                new String[]{String.valueOf(quiz.getId())}
            );
            
            if (rowsAffected > 0) {
                quiz.setTitle(newTitle);
                adapter.notifyItemChanged(quizzes.indexOf(quiz));
                Toast.makeText(requireContext(), "Quiz updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Error updating quiz", Toast.LENGTH_SHORT).show();
                adapter.notifyItemChanged(quizzes.indexOf(quiz));
            }
            
            database.setTransactionSuccessful();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error updating quiz", Toast.LENGTH_SHORT).show();
            adapter.notifyItemChanged(quizzes.indexOf(quiz));
        } finally {
            database.endTransaction();
        }
    }

    // RecyclerView Adapter
    private class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.ViewHolder> {
        private List<Quiz> quizzes;

        QuizAdapter(List<Quiz> quizzes) {
            this.quizzes = quizzes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quiz, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Quiz quiz = quizzes.get(position);
            holder.quizTitle.setText(quiz.getTitle());
            holder.questionCount.setText(quiz.getQuestionCount() + " questions");
            
            // Set last score with color coding
            String lastScore = quiz.getLastScore();
            if (lastScore.startsWith("Last score:")) {
                int percentage = Integer.parseInt(lastScore.replaceAll("[^0-9]", ""));
                int colorResId;
                if (percentage < 25) {
                    colorResId = R.color.error_red;
                } else if (percentage < 75) {
                    colorResId = R.color.warning_yellow;
                } else {
                    colorResId = R.color.success_green;
                }
                holder.lastScore.setTextColor(getResources().getColor(colorResId, null));
            } else {
                holder.lastScore.setTextColor(getResources().getColor(R.color.toolbar_color, null));
            }
            holder.lastScore.setText(lastScore);
            
            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TakeQuizActivity.class);
                intent.putExtra("quiz_id", quiz.getId());
                quizLauncher.launch(intent);
            });
        }

        @Override
        public int getItemCount() {
            return quizzes.size();
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