package com.example.flatuno_reviewer_app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.flatuno_reviewer_app.database.FlashcardDbHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.InputStream;

public class GenerateQuizActivity extends AppCompatActivity {
    private static final int PICK_PDF_FILE = 1;

    private TextInputEditText lessonTextInput;
    private TextView pdfFileName;
    private MaterialButton selectPdfButton;
    private MaterialButton generateButton;
    private Uri selectedPdfUri;
    private FlashcardDbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_quiz);

        // Initialize database
        dbHelper = new FlashcardDbHelper(this);
        database = dbHelper.getWritableDatabase();

        // Initialize views
        lessonTextInput = findViewById(R.id.lesson_text_input);
        pdfFileName = findViewById(R.id.pdf_file_name);
        selectPdfButton = findViewById(R.id.select_pdf_button);
        generateButton = findViewById(R.id.generate_button);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Set up click listeners
        selectPdfButton.setOnClickListener(v -> openPdfPicker());
        generateButton.setOnClickListener(v -> generateQuiz());
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, PICK_PDF_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                selectedPdfUri = data.getData();
                String fileName = getFileName(selectedPdfUri);
                pdfFileName.setText(fileName);
                
                // Extract text from PDF
                try {
                    String pdfText = extractTextFromPdf(selectedPdfUri);
                    lessonTextInput.setText(pdfText);
                } catch (Exception e) {
                    Toast.makeText(this, "Error reading PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf(File.separator);
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String extractTextFromPdf(Uri uri) throws Exception {
        InputStream inputStream = null;
        PdfReader reader = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new Exception("Could not open PDF file");
            }
            
            reader = new PdfReader(inputStream);
            PdfDocument pdfDoc = new PdfDocument(reader);
            StringBuilder text = new StringBuilder();
            
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                text.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i)));
                text.append("\n");
            }
            
            pdfDoc.close();
            return text.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void generateQuiz() {
        String lessonText = lessonTextInput.getText().toString().trim();
        if (lessonText.isEmpty() && selectedPdfUri == null) {
            Toast.makeText(this, "Please provide lesson text or select a PDF file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        generateButton.setEnabled(false);
        generateButton.setText("Generating...");

        // TODO: Implement quiz generation
        Toast.makeText(this, "Quiz generation is currently disabled. Please check back later.", Toast.LENGTH_LONG).show();
        generateButton.setEnabled(true);
        generateButton.setText("Generate Quiz");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }
} 