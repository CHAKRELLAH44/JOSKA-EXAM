package com.example.joska_exam;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

public class ExamEditor extends AppCompatActivity {

    private ArrayList<Question> data;
    private RecyclerView listview;
    private int quizID;
    private ItemTouchHelper itemTouchHelper;

    private final ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder dragged,
                              @NonNull RecyclerView.ViewHolder target) {
            int positionDragged = dragged.getAdapterPosition();
            int positionTarget = target.getAdapterPosition();

            Collections.swap(data, positionDragged, positionTarget);
            listview.getAdapter().notifyItemMoved(positionDragged, positionTarget);
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exam_editor);

        Bundle b = getIntent().getExtras();
        String quizTitle = b.getString("Quiz Title");

        TextView title = findViewById(R.id.title);
        title.setText(quizTitle);

        Button submit = findViewById(R.id.submit);

        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("Quizzes").hasChild("Last ID")) {
                    String ID = snapshot.child("Quizzes").child("Last ID").getValue(String.class);
                    quizID = Integer.parseInt(ID) + 1;
                } else {
                    quizID = 100000;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ExamEditor.this, "Can't Connect", Toast.LENGTH_SHORT).show();
            }
        };
        database.addValueEventListener(listener);

        data = new ArrayList<>();
        data.add(new Question());
        listview = findViewById(R.id.listview);

        listview.setLayoutManager(new LinearLayoutManager(this));
        CustomAdapter customAdapter = new CustomAdapter(data);
        listview.setAdapter(customAdapter);

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(listview);

        submit.setOnClickListener(v -> {
            DatabaseReference ref = database.child("Quizzes");
            ref.child(String.valueOf(quizID)).child("Title").setValue(quizTitle);
            ref.child(String.valueOf(quizID)).child("Total Questions").setValue(data.size());
            DatabaseReference qRef = ref.child(String.valueOf(quizID)).child("Questions");

            for (int i = 0; i < data.size(); i++) {
                String p = String.valueOf(i);
                qRef.child(p).child("Question").setValue(data.get(i).getQuestion());
                qRef.child(p).child("Option 1").setValue(data.get(i).getOption1());
                qRef.child(p).child("Option 2").setValue(data.get(i).getOption2());
                qRef.child(p).child("Option 3").setValue(data.get(i).getOption3());
                qRef.child(p).child("Option 4").setValue(data.get(i).getOption4());
                qRef.child(p).child("Correct Answer").setValue(data.get(i).getCorrectAnswer());
            }

            database.child("Users").child(
                            FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("Quizzes").child(String.valueOf(quizID))
                    .setValue("");

            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Quiz ID", String.valueOf(quizID));
            clipboardManager.setPrimaryClip(clip);
            Toast.makeText(this, "Your Quiz ID:" + quizID + " has been copied to clipboard", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    public static class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
        private final ArrayList<Question> arr;

        public CustomAdapter(ArrayList<Question> arr) {
            this.arr = arr;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.question_edit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setIsRecyclable(false);

            Question currentQuestion = arr.get(position);
            holder.question.setText(currentQuestion.getQuestion());
            holder.option1.setText(currentQuestion.getOption1());
            holder.option2.setText(currentQuestion.getOption2());
            holder.option3.setText(currentQuestion.getOption3());
            holder.option4.setText(currentQuestion.getOption4());

            // Reset radio buttons
            holder.option1rb.setChecked(false);
            holder.option2rb.setChecked(false);
            holder.option3rb.setChecked(false);
            holder.option4rb.setChecked(false);

            // Set correct radio button
            switch (currentQuestion.getSelectedAnswer()) {
                case 1: holder.option1rb.setChecked(true); break;
                case 2: holder.option2rb.setChecked(true); break;
                case 3: holder.option3rb.setChecked(true); break;
                case 4: holder.option4rb.setChecked(true); break;
            }

            // Set text change listeners
            setTextChangeListener(holder.question, position, (q, text) -> q.setQuestion(text));
            setTextChangeListener(holder.option1, position, (q, text) -> q.setOption1(text));
            setTextChangeListener(holder.option2, position, (q, text) -> q.setOption2(text));
            setTextChangeListener(holder.option3, position, (q, text) -> q.setOption3(text));
            setTextChangeListener(holder.option4, position, (q, text) -> q.setOption4(text));

            holder.radio_group.setOnCheckedChangeListener((radioGroup, i) -> {
                if (holder.option1rb.isChecked()) currentQuestion.setSelectedAnswer(1);
                else if (holder.option2rb.isChecked()) currentQuestion.setSelectedAnswer(2);
                else if (holder.option3rb.isChecked()) currentQuestion.setSelectedAnswer(3);
                else if (holder.option4rb.isChecked()) currentQuestion.setSelectedAnswer(4);
            });

            holder.new_question.setVisibility(position == (arr.size() - 1) ? View.VISIBLE : View.GONE);
            holder.new_question.setOnClickListener(view -> {
                arr.add(new Question());
                notifyDataSetChanged();
            });
        }

        private void setTextChangeListener(EditText editText, int position, TextUpdater updater) {
            editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    updater.update(arr.get(position), editable.toString());
                }
            });
        }

        @Override
        public int getItemCount() {
            return arr.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final EditText question;
            final EditText option1;
            final EditText option2;
            final EditText option3;
            final EditText option4;
            final RadioButton option1rb;
            final RadioButton option2rb;
            final RadioButton option3rb;
            final RadioButton option4rb;
            final LinearLayout new_question;
            final RadioGroup radio_group;

            public ViewHolder(View view) {
                super(view);
                question = view.findViewById(R.id.question);
                option1 = view.findViewById(R.id.option1et);
                option2 = view.findViewById(R.id.option2et);
                option3 = view.findViewById(R.id.option3et);
                option4 = view.findViewById(R.id.option4et);
                option1rb = view.findViewById(R.id.option1rb);
                option2rb = view.findViewById(R.id.option2rb);
                option3rb = view.findViewById(R.id.option3rb);
                option4rb = view.findViewById(R.id.option4rb);
                new_question = view.findViewById(R.id.new_question);
                radio_group = view.findViewById(R.id.radio_group);
            }
        }

        interface TextUpdater {
            void update(Question question, String text);
        }
    }
}