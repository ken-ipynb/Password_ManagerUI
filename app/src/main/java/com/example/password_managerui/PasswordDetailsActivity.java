package com.example.password_managerui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.json.JSONException;
import org.json.JSONObject;

public class PasswordDetailsActivity extends AppCompatActivity {

    private TextView websiteText;
    private TextView usernameText;
    private TextView passwordText;
    private TextView categoryText;

    private Button showPasswordButton;
    private Button editButton;
    private Button deleteButton;
    private Button generateQRButton;

    private String password;
    private String passwordId;
    private String website;
    private String username;
    private String category;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_password_details);

        websiteText = findViewById(R.id.websiteText);
        usernameText = findViewById(R.id.usernameText);
        passwordText = findViewById(R.id.passwordText);
        categoryText = findViewById(R.id.categoryText);

        showPasswordButton = findViewById(R.id.showPasswordButton);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        generateQRButton = findViewById(R.id.generateQRButton);

        passwordId = getIntent().getStringExtra("passwordId");
        website = getIntent().getStringExtra("website");
        username = getIntent().getStringExtra("username");
        password = getIntent().getStringExtra("password");
        category = getIntent().getStringExtra("category");

        websiteText.setText(website);
        usernameText.setText(username);
        passwordText.setText("••••••••");
        categoryText.setText(category);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String uid =
                FirebaseAuth.getInstance()
                        .getCurrentUser()
                        .getUid();

        databaseReference =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("passwords");

        showPasswordButton.setOnClickListener(v -> {

            if (passwordText.getText()
                    .toString()
                    .equals("••••••••")) {

                passwordText.setText(password);

            } else {

                passwordText.setText("••••••••");
            }
        });

        editButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            PasswordDetailsActivity.this,
                            AddPasswordActivity.class
                    );

            intent.putExtra("editMode", true);
            intent.putExtra("passwordId", passwordId);
            intent.putExtra("website", website);
            intent.putExtra("username", username);
            intent.putExtra("password", password);
            intent.putExtra("category", category);

            startActivity(intent);
        });

        deleteButton.setOnClickListener(v ->
                showDeleteConfirmation()
        );

        generateQRButton.setOnClickListener(v ->
                generateQRCode()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (databaseReference == null
                || passwordId == null
                || passwordId.isEmpty()) {
            return;
        }

        databaseReference
                .child(passwordId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        finish();
                        return;
                    }

                    website =
                            snapshot.child("website")
                                    .getValue(String.class);

                    username =
                            snapshot.child("username")
                                    .getValue(String.class);

                    password =
                            snapshot.child("password")
                                    .getValue(String.class);

                    category =
                            snapshot.child("category")
                                    .getValue(String.class);

                    websiteText.setText(website);
                    usernameText.setText(username);
                    passwordText.setText("••••••••");
                    categoryText.setText(category);
                });
    }

    private String createQRData() {

        try {

            JSONObject jsonObject =
                    new JSONObject();

            jsonObject.put("website", website);
            jsonObject.put("username", username);
            jsonObject.put("password", password);
            jsonObject.put("category", category);

            return jsonObject.toString();

        } catch (JSONException e) {

            return null;
        }
    }

    private void generateQRCode() {

        String qrData = createQRData();

        if (qrData == null) {

            Toast.makeText(
                    this,
                    "Failed to create QR data",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            BitMatrix bitMatrix =
                    new MultiFormatWriter()
                            .encode(
                                    qrData,
                                    BarcodeFormat.QR_CODE,
                                    800,
                                    800
                            );

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            800,
                            800,
                            Bitmap.Config.RGB_565
                    );

            for (int x = 0; x < 800; x++) {

                for (int y = 0; y < 800; y++) {

                    bitmap.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? android.graphics.Color.BLACK
                                    : android.graphics.Color.WHITE
                    );
                }
            }

            showQRCode(bitmap);

        } catch (WriterException e) {

            Toast.makeText(
                    this,
                    "Failed to generate QR",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showQRCode(Bitmap bitmap) {

        ImageView imageView =
                new ImageView(this);

        imageView.setImageBitmap(bitmap);

        imageView.setPadding(
                30,
                30,
                30,
                30
        );

        new AlertDialog.Builder(this)
                .setTitle("Wault QR Code")
                .setView(imageView)
                .setPositiveButton(
                        "Done",
                        null
                )
                .show();
    }

    private void showDeleteConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Delete Password")
                .setMessage(
                        "Are you sure you want to delete this password?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Delete",
                        (dialog, which) ->
                                deletePassword()
                )
                .show();
    }

    private void deletePassword() {

        if (passwordId == null
                || passwordId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Password ID not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        databaseReference
                .child(passwordId)
                .removeValue()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            PasswordDetailsActivity.this,
                            "Password deleted",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            PasswordDetailsActivity.this,
                            "Failed to delete password",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}