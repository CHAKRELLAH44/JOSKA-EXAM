package com.example.joska_exam;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Result extends AppCompatActivity {

    private Question[] data;
    private String quizID;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultat);

        quizID = getIntent().getStringExtra("Quiz ID");
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (getIntent().hasExtra("User ID")) {
            uid = getIntent().getStringExtra("User ID");
        }

        TextView title = findViewById(R.id.title);
        ListView listView = findViewById(R.id.listview);
        TextView total = findViewById(R.id.total);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                try {
                    if (snapshot.child("Quizzes").hasChild(quizID)) {
                        DataSnapshot ansref = snapshot.child("Quizzes").child(quizID).child("Answers").child(uid);
                        DataSnapshot qref = snapshot.child("Quizzes").child(quizID);

                        // Récupération du titre
                        String titleText = getSafeString(qref.child("Title"));
                        title.setText(titleText.isEmpty() ? "Quiz Results" : titleText);

                        // Récupération du nombre de questions
                        int num = 0;
                        try {
                            num = Integer.parseInt(getSafeString(qref.child("Total Questions")));
                        } catch (NumberFormatException e) {
                            Log.e("Result", "Error parsing total questions", e);
                        }

                        data = new Question[num];
                        int correctAns = 0;

                        for (int i = 0; i < num; i++) {
                            DataSnapshot qRef2 = qref.child("Questions").child(String.valueOf(i));
                            Question question = new Question();

                            // Récupération des données de la question
                            question.setQuestion(getSafeString(qRef2.child("Question")));
                            question.setOption1(getSafeString(qRef2.child("Option 1")));
                            question.setOption2(getSafeString(qRef2.child("Option 2")));
                            question.setOption3(getSafeString(qRef2.child("Option 3")));
                            question.setOption4(getSafeString(qRef2.child("Option 4")));

                            // Récupération des réponses
                            int selectedAnswer = 0;
                            int correctAnswer = 0;

                            try {
                                // Réponse sélectionnée par l'utilisateur
                                if (ansref.hasChild(String.valueOf(i))) {
                                    selectedAnswer = Integer.parseInt(getSafeString(ansref.child(String.valueOf(i))));
                                }

                                // Bonne réponse
                                correctAnswer = Integer.parseInt(getSafeString(qRef2.child("Correct Answer")));

                                // Vérification
                                if (selectedAnswer == correctAnswer) {
                                    correctAns++;
                                }

                                // Log pour vérifier les réponses
                                Log.d("Result", "Question " + i + ": Selected Answer = " + selectedAnswer + ", Correct Answer = " + correctAnswer);
                            } catch (NumberFormatException e) {
                                Log.e("Result", "Error parsing answers", e);
                            }

                            question.setSelectedAnswer(selectedAnswer);
                            question.setCorrectAnswer(correctAnswer);
                            data[i] = question;
                        }

                        total.setText(String.format("Total %d/%d", correctAns, data.length));
                        ListAdapter listAdapter = new ListAdapter(Result.this, data);
                        listView.setAdapter(listAdapter);
                    } else {
                        Toast.makeText(Result.this, "Quiz not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e("Result", "Error in onDataChange", e);
                    Toast.makeText(Result.this, "Error loading results", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Result.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("Result", "Database error", error.toException());
            }

            private String getSafeString(DataSnapshot snapshot) {
                Object value = snapshot.getValue();
                return value != null ? value.toString() : "";
            }
        };
        database.addValueEventListener(listener);
    }

    public class ListAdapter extends BaseAdapter {
        private final Result context;
        Question[] arr;

        ListAdapter(Result context, Question[] arr2) {
            this.context = context;
            this.arr = arr2;
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            View v = view;
            if (v == null) {
                LayoutInflater inflater = context.getLayoutInflater();
                v = inflater.inflate(R.layout.question, viewGroup, false);
            }

            TextView question = v.findViewById(R.id.question);
            RadioButton option1 = v.findViewById(R.id.option1);
            RadioButton option2 = v.findViewById(R.id.option2);
            RadioButton option3 = v.findViewById(R.id.option3);
            RadioButton option4 = v.findViewById(R.id.option4);
            TextView result = v.findViewById(R.id.result);

            // Réinitialiser les styles
            option1.setBackgroundResource(0);
            option2.setBackgroundResource(0);
            option3.setBackgroundResource(0);
            option4.setBackgroundResource(0);

            // Remplir les données
            question.setText(arr[i].getQuestion());
            option1.setText(arr[i].getOption1());
            option2.setText(arr[i].getOption2());
            option3.setText(arr[i].getOption3());
            option4.setText(arr[i].getOption4());

            // Désactiver les RadioButtons
            option1.setEnabled(false);
            option2.setEnabled(false);
            option3.setEnabled(false);
            option4.setEnabled(false);

            // Cocher la réponse sélectionnée
            switch (arr[i].getSelectedAnswer()) {
                case 1: option1.setChecked(true); break;
                case 2: option2.setChecked(true); break;
                case 3: option3.setChecked(true); break;
                case 4: option4.setChecked(true); break;
            }

            // Gestion des résultats
            result.setVisibility(View.VISIBLE);
            if (arr[i].getSelectedAnswer() == arr[i].getCorrectAnswer()) {
                // Réponse correcte
                result.setText("Correct Answer");
                result.setBackgroundResource(R.drawable.green_background);
                result.setTextColor(ContextCompat.getColor(context, R.color.green_dark));

                // Mettre en vert la réponse sélectionnée
                switch (arr[i].getSelectedAnswer()) {
                    case 1: option1.setBackgroundResource(R.drawable.green_background); break;
                    case 2: option2.setBackgroundResource(R.drawable.green_background); break;
                    case 3: option3.setBackgroundResource(R.drawable.green_background); break;
                    case 4: option4.setBackgroundResource(R.drawable.green_background); break;
                }
            } else {
                // Réponse incorrecte
                result.setText("Wrong Answer");
                result.setBackgroundResource(R.drawable.red_background);
                result.setTextColor(ContextCompat.getColor(context, R.color.red_dark));

                // Mettre en rouge la réponse sélectionnée (si une réponse a été donnée)
                if (arr[i].getSelectedAnswer() != 0) {
                    switch (arr[i].getSelectedAnswer()) {
                        case 1: option1.setBackgroundResource(R.drawable.red_background); break;
                        case 2: option2.setBackgroundResource(R.drawable.red_background); break;
                        case 3: option3.setBackgroundResource(R.drawable.red_background); break;
                        case 4: option4.setBackgroundResource(R.drawable.red_background); break;
                    }
                }

                // Mettre en vert la bonne réponse
                switch (arr[i].getCorrectAnswer()) {
                    case 1: option1.setBackgroundResource(R.drawable.green_background); break;
                    case 2: option2.setBackgroundResource(R.drawable.green_background); break;
                    case 3: option3.setBackgroundResource(R.drawable.green_background); break;
                    case 4: option4.setBackgroundResource(R.drawable.green_background); break;
                }
            }

            return v;
        }
    }
}
