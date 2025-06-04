package com.example.joska_exam;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;

public class signup extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference database;

    private static final int CAMERA_REQUEST = 100;
    private ImageView imgProfile;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        EditText user_name = findViewById(R.id.User_name);
        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password);
        EditText Cpassword = findViewById(R.id.C_password);
        Button signup = findViewById(R.id.signup);
        TextView login = findViewById(R.id.login);

        imgProfile = findViewById(R.id.img_profile);
        Button takePhotoButton = findViewById(R.id.btn_take_photo);

        // Ouvre la caméra
        takePhotoButton.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_REQUEST);
            }
        });

        signup.setOnClickListener(v -> {
            ProgressDialog progressDialog = new ProgressDialog(signup.this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            Thread thread = new Thread(() -> {
                String Confirmpass = Cpassword.getText().toString();
                String pass = password.getText().toString();
                String em = email.getText().toString();
                String username = user_name.getText().toString();

                if (!pass.equals(Confirmpass)) {
                    runOnUiThread(() -> {
                        Toast.makeText(signup.this, "Password doesn't match", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    });
                    return;
                }

                auth.createUserWithEmailAndPassword(em, pass).addOnCompleteListener(signup.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        DatabaseReference ref = database.child("users").child(user.getUid());
                        ref.child("Name").setValue(username);

                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            auth.signOut(); // Déconnexion automatique après inscription
                            Intent i = new Intent(signup.this, MainActivity.class); // Redirige vers login
                            i.putExtra("signup_success", true); // (optionnel) pour afficher un message dans login
                            startActivity(i);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(signup.this, "Operation failed", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        });
                    }
                });
            });
            thread.start();
        });

        login.setOnClickListener(v -> {
            Intent i = new Intent(signup.this, MainActivity.class);
            startActivity(i);
            finish();
        });
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile;
        try {
            photoFile = File.createTempFile("IMG_", ".jpg", getExternalCacheDir());
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error while creating file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            imgProfile.setImageURI(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
