package com.example.password_managerui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.RGBLuminanceSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.security.SecureRandom;

public class AddPasswordActivity extends AppCompatActivity {

    private static final int QR_SCAN_REQUEST = 200;
    private static final int QR_UPLOAD_REQUEST = 201;

    private EditText websiteInput;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText categoryInput;

    private Button saveButton;
    private Button generateButton;
    private Button scanQRButton;
    private Button uploadQRButton;

    private DatabaseReference databaseReference;

    private boolean editMode;
    private String passwordId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_password);

        websiteInput = findViewById(R.id.websiteInput);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        categoryInput = findViewById(R.id.categoryInput);

        saveButton = findViewById(R.id.saveButton);
        generateButton = findViewById(R.id.generateButton);
        scanQRButton = findViewById(R.id.scanQRButton);
        uploadQRButton = findViewById(R.id.uploadQRButton);

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

        editMode =
                getIntent().getBooleanExtra(
                        "editMode",
                        false
                );

        if (editMode) {

            passwordId =
                    getIntent().getStringExtra(
                            "passwordId"
                    );

            websiteInput.setText(
                    getIntent().getStringExtra("website")
            );

            usernameInput.setText(
                    getIntent().getStringExtra("username")
            );

            passwordInput.setText(
                    getIntent().getStringExtra("password")
            );

            categoryInput.setText(
                    getIntent().getStringExtra("category")
            );

            saveButton.setText("Update Password");
        }

        generateButton.setOnClickListener(v -> {

            String generatedPassword =
                    generatePassword(16);

            passwordInput.setText(
                    generatedPassword
            );

            passwordInput.setSelection(
                    passwordInput.length()
            );

            Toast.makeText(
                    AddPasswordActivity.this,
                    "Password generated",
                    Toast.LENGTH_SHORT
            ).show();
        });

        scanQRButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            AddPasswordActivity.this,
                            QRScannerActivity.class
                    );

            startActivityForResult(
                    intent,
                    QR_SCAN_REQUEST
            );
        });

        uploadQRButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            intent.setType("image/*");

            startActivityForResult(
                    intent,
                    QR_UPLOAD_REQUEST
            );
        });

        saveButton.setOnClickListener(v -> {

            String website =
                    websiteInput.getText()
                            .toString()
                            .trim();

            String username =
                    usernameInput.getText()
                            .toString()
                            .trim();

            String password =
                    passwordInput.getText()
                            .toString()
                            .trim();

            String category =
                    categoryInput.getText()
                            .toString()
                            .trim();

            if (website.isEmpty()
                    || username.isEmpty()
                    || password.isEmpty()
                    || category.isEmpty()) {

                Toast.makeText(
                        AddPasswordActivity.this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (editMode) {

                updatePassword(
                        website,
                        username,
                        password,
                        category
                );

            }

            else {

                addNewPassword(
                        website,
                        username,
                        password,
                        category
                );
            }
        });
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == QR_SCAN_REQUEST) {

            if (resultCode != RESULT_OK
                    || data == null) {

                return;
            }

            String qrResult =
                    data.getStringExtra(
                            "qr_result"
                    );

            if (qrResult == null
                    || qrResult.trim().isEmpty()) {

                Toast.makeText(
                        this,
                        "Invalid QR data",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            processQRData(qrResult);

            return;
        }

        if (requestCode == QR_UPLOAD_REQUEST) {

            if (resultCode != RESULT_OK
                    || data == null
                    || data.getData() == null) {

                return;
            }

            Uri imageUri =
                    data.getData();

            decodeUploadedQR(imageUri);
        }
    }

    private void decodeUploadedQR(Uri imageUri) {

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(imageUri);

            if (inputStream == null) {

                Toast.makeText(
                        this,
                        "Unable to open image",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Bitmap bitmap =
                    BitmapFactory.decodeStream(
                            inputStream
                    );

            inputStream.close();

            if (bitmap == null) {

                Toast.makeText(
                        this,
                        "Unable to read image",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            int width =
                    bitmap.getWidth();

            int height =
                    bitmap.getHeight();

            int[] pixels =
                    new int[width * height];

            bitmap.getPixels(
                    pixels,
                    0,
                    width,
                    0,
                    0,
                    width,
                    height
            );

            RGBLuminanceSource source =
                    new RGBLuminanceSource(
                            width,
                            height,
                            pixels
                    );

            BinaryBitmap binaryBitmap =
                    new BinaryBitmap(
                            new HybridBinarizer(source)
                    );

            Result result =
                    new MultiFormatReader()
                            .decode(binaryBitmap);

            String qrResult =
                    result.getText();

            if (qrResult == null
                    || qrResult.trim().isEmpty()) {

                Toast.makeText(
                        this,
                        "QR code is empty",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            Toast.makeText(
                    this,
                    "QR image scanned successfully",
                    Toast.LENGTH_SHORT
            ).show();

            processQRData(qrResult);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Could not read QR code from image",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void processQRData(String qrResult) {

        try {

            JSONObject jsonObject =
                    new JSONObject(qrResult);

            String website =
                    jsonObject.optString(
                            "website",
                            ""
                    ).trim();

            String username =
                    jsonObject.optString(
                            "username",
                            ""
                    ).trim();

            String password =
                    jsonObject.optString(
                            "password",
                            ""
                    ).trim();

            String category =
                    jsonObject.optString(
                            "category",
                            ""
                    ).trim();

            if (website.isEmpty()
                    || username.isEmpty()
                    || password.isEmpty()
                    || category.isEmpty()) {

                Toast.makeText(
                        this,
                        "QR data is incomplete",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            websiteInput.setText(website);
            usernameInput.setText(username);
            passwordInput.setText(password);
            categoryInput.setText(category);

            if (editMode) {

                saveButton.setText(
                        "Update Password"
                );

            } else {

                saveButton.setText(
                        "Save Password"
                );
            }

            Toast.makeText(
                    this,
                    "QR data loaded. Review and save.",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (JSONException e) {

            Toast.makeText(
                    this,
                    "Invalid QR format",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String generatePassword(int length) {

        String characters =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        + "abcdefghijklmnopqrstuvwxyz"
                        + "0123456789"
                        + "!@#$%^&*";

        SecureRandom random =
                new SecureRandom();

        StringBuilder password =
                new StringBuilder();

        for (int i = 0; i < length; i++) {

            int index =
                    random.nextInt(
                            characters.length()
                    );

            password.append(
                    characters.charAt(index)
            );
        }

        return password.toString();
    }

    private void addNewPassword(
            String website,
            String username,
            String password,
            String category) {

        String id =
                databaseReference
                        .push()
                        .getKey();

        if (id == null) {

            Toast.makeText(
                    AddPasswordActivity.this,
                    "Failed to generate ID",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PasswordModel passwordModel =
                new PasswordModel(
                        id,
                        website,
                        username,
                        password,
                        category
                );

        databaseReference
                .child(id)
                .setValue(passwordModel)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            AddPasswordActivity.this,
                            "Password saved successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AddPasswordActivity.this,
                            "Failed to save password",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void updatePassword(
            String website,
            String username,
            String password,
            String category) {

        if (passwordId == null
                || passwordId.isEmpty()) {

            Toast.makeText(
                    AddPasswordActivity.this,
                    "Password ID not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PasswordModel passwordModel =
                new PasswordModel(
                        passwordId,
                        website,
                        username,
                        password,
                        category
                );

        databaseReference
                .child(passwordId)
                .setValue(passwordModel)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            AddPasswordActivity.this,
                            "Password updated successfully",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AddPasswordActivity.this,
                            "Failed to update password",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }
}