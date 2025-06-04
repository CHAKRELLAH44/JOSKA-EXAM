package com.example.joska_exam;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Affiche un message si l'utilisateur vient de s'inscrire
        boolean fromSignup = getIntent().getBooleanExtra("signup_success", false);
        if (fromSignup) {
            Toast.makeText(this, "Compte créé avec succès. Veuillez vous connecter.", Toast.LENGTH_LONG).show();
        }

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        Button login = findViewById(R.id.login);
        TextView signup = findViewById(R.id.signup);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            Intent i = new Intent(MainActivity.this, Home.class);
            i.putExtra("user UID", user.getUid());
            startActivity(i);
            finish();
        }

        login.setOnClickListener(v -> {
            String em = email.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (em.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Connexion en cours...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            auth.signInWithEmailAndPassword(em, pass)
                    .addOnCompleteListener(MainActivity.this, task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = auth.getCurrentUser();
                            Intent i = new Intent(MainActivity.this, Home.class);
                            i.putExtra("user UID", currentUser.getUid());
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Erreur de connexion", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        signup.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, signup.class);
            startActivity(i);
            finish();
        });
    }
}
