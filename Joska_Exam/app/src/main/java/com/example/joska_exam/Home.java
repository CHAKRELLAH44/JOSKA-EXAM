package com.example.joska_exam;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Home extends AppCompatActivity {

    private String userID;
    private String firstname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ProgressDialog progressDialog = new ProgressDialog(Home.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        // Récupération du UserID depuis l'intent ou FirebaseAuth
        Bundle b = getIntent().getExtras();
        if (b != null) {
            userID = b.getString("UserID");
            Log.d("HomeDebug", "UserID from intent: " + userID);
        }

        // Solution de secours si UserID n'est pas dans l'intent
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && userID == null) {
            userID = currentUser.getUid();
            Log.d("HomeDebug", "UserID from Auth: " + userID);
        }

        TextView name = findViewById(R.id.User_name);
        TextView total_questions = findViewById(R.id.total_questions);
        TextView total_points = findViewById(R.id.total_points);
        Button startQuiz = findViewById(R.id.startQuiz);
        Button createQuiz = findViewById(R.id.createQuiz);
        RelativeLayout solvedQuizzes = findViewById(R.id.solvedQuizzes);
        RelativeLayout yourQuizzes = findViewById(R.id.your_quizzes);
        EditText start_quiz_id = findViewById(R.id.start_quiz_id);
        EditText quiz_title = findViewById(R.id.quiz_title);
        ImageView signout = findViewById(R.id.signout);

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (userID == null) {
                    progressDialog.dismiss();
                    Log.d("UserDebug", "UserID is null");
                    return;
                }

                // Vérifie si le nœud "users" existe
                if (!snapshot.hasChild("users")) {
                    progressDialog.dismiss();
                    Log.d("UserDebug", "users node doesn't exist");
                    return;
                }

                DataSnapshot user = snapshot.child("users").child(userID);
                if (!user.exists()) {
                    progressDialog.dismiss();
                    Log.d("UserDebug", "User data doesn't exist for ID: " + userID);
                    return;
                }

                // Récupération du nom
                firstname = user.child("Name").getValue(String.class);
                if (firstname == null) {
                    firstname = "User";
                    Log.d("UserDebug", "Name is null, using default");
                }

                Log.d("UserDebug", "Name value: " + firstname);

                // Gestion des points
                if (user.hasChild("Total Points")) {
                    String totalPoints = user.child("Total Points").getValue(String.class);
                    if (totalPoints != null) {
                        try {
                            int Points = Integer.parseInt(totalPoints);
                            total_points.setText(String.format("%03d", Points));
                        } catch (NumberFormatException e) {
                            total_points.setText("000");
                            Log.e("UserDebug", "Error parsing Total Points", e);
                        }
                    } else {
                        total_points.setText("000");
                    }
                } else {
                    total_points.setText("000");
                }

                // Gestion des questions
                if (user.hasChild("Total Questions")) {
                    String totalQuestions = user.child("Total Questions").getValue(String.class);
                    if (totalQuestions != null) {
                        try {
                            int Questions = Integer.parseInt(totalQuestions);
                            total_questions.setText(String.format("%03d", Questions));
                        } catch (NumberFormatException e) {
                            total_questions.setText("000");
                            Log.e("UserDebug", "Error parsing Total Questions", e);
                        }
                    } else {
                        total_questions.setText("000");
                    }
                } else {
                    total_questions.setText("000");
                }

                // Affichage du nom
                name.setText("Welcome " + firstname);
                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Home.this, "Can't Connect", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                Log.e("UserDebug", "Database error: " + error.getMessage());
            }
        };
        database.addValueEventListener(listener);

        // Gestion de la déconnexion
        signout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(Home.this, MainActivity.class);
            startActivity(i);
            finish();
        });

        // Gestion de la création de quiz
        createQuiz.setOnClickListener(v -> {
            if(quiz_title.getText().toString().equals("")) {
                quiz_title.setError("Quiz title cannot be empty");
                return;
            }
            Intent i = new Intent(Home.this, ExamEditor.class);
            i.putExtra("QuizTitle", quiz_title.getText().toString());
            startActivity(i);
        });

        // Gestion du démarrage de quiz
        startQuiz.setOnClickListener(v -> {
            if(start_quiz_id.getText().toString().equals("")) {
                start_quiz_id.setError("Quiz ID cannot be empty");
                return;
            }
            Intent i = new Intent(Home.this, Exam.class);
            i.putExtra("QuizID", start_quiz_id.getText().toString());
            start_quiz_id.setText("");
            startActivity(i);
        });

        // Liste des quiz résolus
        solvedQuizzes.setOnClickListener(v -> {
            Intent i = new Intent(Home.this, ListQuizzes.class);
            i.putExtra("Operation", "List Solved Quizzes");
            startActivity(i);
        });

        // Liste des quiz créés
        yourQuizzes.setOnClickListener(v -> {
            Intent i = new Intent(Home.this, ListQuizzes.class);
            i.putExtra("Operation", "List Your Quizzes");
            startActivity(i);
        });
    }
}