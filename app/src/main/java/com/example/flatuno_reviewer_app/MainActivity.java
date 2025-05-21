package com.example.flatuno_reviewer_app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup toolbar
        setupToolbar();

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_flashcards) {
                selectedFragment = new FlashcardsFragment();
            } else if (itemId == R.id.navigation_quiz) {
                selectedFragment = new QuizFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.navigation_home);
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // set logo on left, resize to 24dp
        Drawable logo = ContextCompat.getDrawable(this, R.drawable.flatuno_logo);
        if (logo != null) {
            int size = (int) (getResources().getDisplayMetrics().density * 24);
            logo.setBounds(0, 0, size, size);
            DrawableCompat.setTint(logo, ContextCompat.getColor(this, android.R.color.white));
            toolbar.setNavigationIcon(logo);
            toolbar.setTitleMarginStart((int) (getResources().getDisplayMetrics().density * 0));
        }

        // apply Poppins Bold font to toolbar title
        TextView titleTextView = null;
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View v = toolbar.getChildAt(i);
            if (v instanceof TextView) {
                titleTextView = (TextView) v;
                break;
            }
        }

        if (titleTextView != null) {
            Typeface typeface = ResourcesCompat.getFont(this, R.font.poppins_bold);
            titleTextView.setTypeface(typeface);

            int paddingTopDp = 7;
            int paddingTopPx = (int) (paddingTopDp * getResources().getDisplayMetrics().density);
            titleTextView.setPadding(
                    titleTextView.getPaddingLeft(),
                    paddingTopPx,
                    titleTextView.getPaddingRight(),
                    titleTextView.getPaddingBottom()
            );
        }
    }
}
