package com.example.joska_exam;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
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

public class Exam extends AppCompatActivity {

    private Question[] data;
    private String quizID;
    private String uid;
    private int oldTotalPoints = 0;
    private int oldTotalQuestions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam);

        // Initialisation des vues
        ListView listView = findViewById(R.id.listview);
        Button submit = findViewById(R.id.submit);
        TextView title = findViewById(R.id.title);

        // Récupération du quizID depuis l'intent
        quizID = getIntent().getStringExtra("QuizID");
        Log.d("ExamDebug", "QuizID reçu: " + quizID);

        // Initialisation Firebase
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        // Test temporaire avec des données statiques (à commenter après test)
        // initializeTestData(listView);

        // Écouteur Firebase
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.child("Quizzes").hasChild(quizID)) {
                        DataSnapshot ref = snapshot.child("Quizzes").child(quizID);

                        // Récupération du titre
                        String quizTitle = ref.child("Title").getValue(String.class);
                        title.setText(quizTitle != null ? quizTitle : "Quiz");
                        Log.d("ExamDebug", "Titre du quiz: " + quizTitle);

                        // Récupération du nombre de questions
                        int numQuestions = 0;
                        try {
                            Long totalQuestions = ref.child("Total Questions").getValue(Long.class);
                            numQuestions = totalQuestions != null ? totalQuestions.intValue() : 0;
                        } catch (Exception e) {
                            Log.e("ExamError", "Erreur conversion Total Questions", e);
                        }

                        Log.d("ExamDebug", "Nombre de questions: " + numQuestions);

                        // Initialisation du tableau de questions
                        data = new Question[numQuestions];

                        // Chargement des questions
                        for (int i = 0; i < numQuestions; i++) {
                            DataSnapshot qRef = ref.child("Questions").child(String.valueOf(i));
                            Question question = new Question();

                            if (qRef.exists()) {
                                question.setQuestion(qRef.child("Question").getValue(String.class));
                                question.setOption1(qRef.child("Option 1").getValue(String.class));
                                question.setOption2(qRef.child("Option 2").getValue(String.class));
                                question.setOption3(qRef.child("Option 3").getValue(String.class));
                                question.setOption4(qRef.child("Option 4").getValue(String.class));

                                try {
                                    Long correctAns = qRef.child("Correct Answer").getValue(Long.class);
                                    question.setCorrectAnswer(correctAns != null ? correctAns.intValue() : 0);
                                } catch (Exception e) {
                                    question.setCorrectAnswer(0);
                                    Log.e("ExamError", "Erreur conversion Correct Answer", e);
                                }

                                data[i] = question;
                                Log.d("ExamDebug", "Question " + i + " chargée: " + question.getQuestion());
                            }
                        }

                        // Création et configuration de l'adaptateur
                        ListAdapter listAdapter = new ListAdapter(Exam.this, data);
                        listView.setAdapter(listAdapter);

                        // Debug: vérification de l'affichage
                        listView.post(() -> {
                            Log.d("ExamDebug", "ListView height: " + listView.getHeight());
                            Log.d("ExamDebug", "Adapter count: " + listAdapter.getCount());
                        });

                        // Récupération des statistiques utilisateur
                        DataSnapshot userRef = snapshot.child("Users").child(uid);
                        if (userRef.hasChild("Total Points")) {
                            try {
                                Long points = userRef.child("Total Points").getValue(Long.class);
                                oldTotalPoints = points != null ? points.intValue() : 0;
                            } catch (Exception e) {
                                Log.e("ExamError", "Erreur conversion Total Points", e);
                            }
                        }
                        if (userRef.hasChild("Total Questions")) {
                            try {
                                Long questions = userRef.child("Total Questions").getValue(Long.class);
                                oldTotalQuestions = questions != null ? questions.intValue() : 0;
                            } catch (Exception e) {
                                Log.e("ExamError", "Erreur conversion Total Questions", e);
                            }
                        }
                    } else {
                        Toast.makeText(Exam.this, "Quiz non trouvé", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("ExamError", "Erreur dans onDataChange", e);
                    Toast.makeText(Exam.this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Exam.this, "Erreur de connexion: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ExamError", "DatabaseError: " + error.getMessage());
            }
        };

        database.addValueEventListener(listener);

        // Gestion du clic sur le bouton Submit
        submit.setOnClickListener(v -> {
            try {
                if (data == null || data.length == 0) {
                    Toast.makeText(Exam.this, "Aucune question à soumettre", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference ref = database.child("Quizzes").child("Answers").child(uid);
                int totalPoints = oldTotalPoints;
                int points = 0;

                // Calcul des points
                for (int i = 0; i < data.length; i++) {
                    ref.child(String.valueOf(i)).setValue(data[i].getSelectedAnswer());
                    if (data[i].getSelectedAnswer() == data[i].getCorrectAnswer()) {
                        points++;
                        totalPoints++;
                    }
                }

                // Mise à jour des statistiques
                ref.child("Total Points").setValue(points);
                int totalQuestions = oldTotalQuestions + data.length;

                database.child("Users").child(uid).child("Total Points").setValue(totalPoints);
                database.child("Users").child(uid).child("Total Questions").setValue(totalQuestions);
                database.child("Users").child(uid).child("Quizzes Questions").child(quizID).setValue("");

                // Redirection vers les résultats
                Intent i = new Intent(Exam.this, Result.class);
                i.putExtra("Quiz ID", quizID);
                startActivity(i);
                finish();
            } catch (Exception e) {
                Log.e("ExamError", "Erreur lors de la soumission", e);
                Toast.makeText(Exam.this, "Erreur de soumission", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour tester avec des données statiques (à commenter en production)
    private void initializeTestData(ListView listView) {
        Question[] testData = new Question[3];
        for (int i = 0; i < testData.length; i++) {
            Question q = new Question();
            q.setQuestion("Question test " + (i+1));
            q.setOption1("Option A");
            q.setOption2("Option B");
            q.setOption3("Option C");
            q.setOption4("Option D");
            q.setCorrectAnswer(1);
            testData[i] = q;
        }
        data = testData;
        ListAdapter listAdapter = new ListAdapter(this, data);
        listView.setAdapter(listAdapter);
        Log.d("ExamDebug", "Test data initialized with " + testData.length + " questions");
    }

    // Adaptateur personnalisé
    public class ListAdapter extends BaseAdapter {
        private final Exam context;
        private final Question[] arr;

        ListAdapter(Exam context, Question[] arr) {
            this.context = context;
            this.arr = arr;
        }

        @Override
        public int getCount() {
            return arr.length;
        }

        @Override
        public Object getItem(int i) {
            return arr[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                convertView = inflater.inflate(R.layout.question, parent, false);

                holder = new ViewHolder();
                holder.question = convertView.findViewById(R.id.question);
                holder.option1 = convertView.findViewById(R.id.option1);
                holder.option2 = convertView.findViewById(R.id.option2);
                holder.option3 = convertView.findViewById(R.id.option3);
                holder.option4 = convertView.findViewById(R.id.option4);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            try {
                // Configuration de la vue
                Question current = arr[position];

                holder.question.setText(current.getQuestion());
                holder.option1.setText(current.getOption1());
                holder.option2.setText(current.getOption2());
                holder.option3.setText(current.getOption3());
                holder.option4.setText(current.getOption4());

                // Réinitialisation des états
                holder.option1.setOnCheckedChangeListener(null);
                holder.option2.setOnCheckedChangeListener(null);
                holder.option3.setOnCheckedChangeListener(null);
                holder.option4.setOnCheckedChangeListener(null);

                holder.option1.setChecked(false);
                holder.option2.setChecked(false);
                holder.option3.setChecked(false);
                holder.option4.setChecked(false);

                // Écouteurs pour les RadioButtons
                holder.option1.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) current.setSelectedAnswer(1);
                });

                holder.option2.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) current.setSelectedAnswer(2);
                });

                holder.option3.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) current.setSelectedAnswer(3);
                });

                holder.option4.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) current.setSelectedAnswer(4);
                });

                // Restauration de la sélection
                switch (current.getSelectedAnswer()) {
                    case 1: holder.option1.setChecked(true); break;
                    case 2: holder.option2.setChecked(true); break;
                    case 3: holder.option3.setChecked(true); break;
                    case 4: holder.option4.setChecked(true); break;
                }
            } catch (Exception e) {
                Log.e("ListAdapter", "Erreur dans getView", e);
            }

            return convertView;
        }

        class ViewHolder {
            TextView question;
            RadioButton option1;
            RadioButton option2;
            RadioButton option3;
            RadioButton option4;
        }
    }
}