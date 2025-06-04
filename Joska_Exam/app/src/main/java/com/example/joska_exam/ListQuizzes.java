package com.example.joska_exam;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ListQuizzes extends AppCompatActivity {

    private static final String TAG = "ListQuizzes";
    private ListView listView;
    private TextView emptyView;
    private DatabaseReference databaseRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_quizzes);

        // Initialisation des vues
        listView = findViewById(R.id.listview);
        emptyView = findViewById(R.id.emptyView);
        TextView title = findViewById(R.id.title);

        // Initialisation Firebase
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseRef = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG, "Chemin Firebase: Users/" + userId + "/Quizzes");

        // Configuration de l'interface
        String operationType = getIntent().getStringExtra("Operation");
        if ("List Your Quizzes".equals(operationType)) {
            title.setText("Vos Quiz");
            loadQuizzes();
        } else {
            title.setText("Quiz");
            loadQuizzes(); // Charge tous les quiz par défaut
        }

        // Gestion du clic sur un élément
        listView.setOnItemClickListener((parent, view, position, id) -> {
            QuizItem item = (QuizItem) parent.getItemAtPosition(position);
            Intent intent = new Intent(ListQuizzes.this, Exam.class);
            intent.putExtra("QuizID", item.getId());
            startActivity(intent);
        });
    }

    private void loadQuizzes() {
        databaseRef.child("Users").child(userId).child("Quizzes")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<QuizItem> quizzes = new ArrayList<>();

                        Log.d(TAG, "Nombre de quiz trouvés: " + snapshot.getChildrenCount());

                        if (!snapshot.exists()) {
                            showEmptyView("Aucun quiz trouvé");
                            return;
                        }

                        for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                            String quizId = quizSnapshot.getKey();
                            // Ignore les champs spéciaux comme "Total Points"
                            if (quizId != null && !quizId.equals("Total Points")) {
                                quizzes.add(new QuizItem(quizId, "Quiz " + quizId));
                                Log.d(TAG, "Quiz ajouté: " + quizId);
                            }
                        }

                        updateListView(quizzes);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Erreur Firebase: " + error.getMessage());
                        showEmptyView("Erreur de chargement");
                    }
                });
    }

    private void updateListView(ArrayList<QuizItem> quizzes) {
        runOnUiThread(() -> {
            if (quizzes.isEmpty()) {
                showEmptyView("Aucun quiz disponible");
                return;
            }

            QuizAdapter adapter = new QuizAdapter(ListQuizzes.this, quizzes);
            listView.setAdapter(adapter);
            listView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            Toast.makeText(ListQuizzes.this,
                    quizzes.size() + " quiz chargés",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showEmptyView(String message) {
        runOnUiThread(() -> {
            emptyView.setText(message);
            emptyView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        });
    }

    // Classe pour représenter un quiz
    private static class QuizItem {
        private final String id;
        private final String title;

        public QuizItem(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }

        @Override
        public String toString() { return title; }
    }

    // Adapter personnalisé
    private static class QuizAdapter extends ArrayAdapter<QuizItem> {
        public QuizAdapter(Context context, ArrayList<QuizItem> quizzes) {
            super(context, R.layout.quizzes_listitem, quizzes);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            QuizItem quiz = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.quizzes_listitem, parent, false);
            }

            TextView title = convertView.findViewById(R.id.quiz);
            TextView grade = convertView.findViewById(R.id.grade);

            title.setText(quiz.getTitle());
            grade.setVisibility(View.GONE);

            return convertView;
        }
    }
}