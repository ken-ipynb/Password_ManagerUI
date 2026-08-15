package com.example.password_managerui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton backButton;
    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private TextView changeProfileText;
    private TextView profileInfoName;
    private TextView profileInfoEmail;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<String> imagePicker;
    private ActivityResultLauncher<String> backupFilePicker;
    private ActivityResultLauncher<String[]> restoreFilePicker;
    private boolean dialogOnlyMode = false;

    private static final String PREFS_NAME = "WaultPrefs";
    private static final String PROFILE_IMAGE_URI = "profile_image_uri";
    private static final String APP_LOCK_ENABLED = "app_lock_enabled";
    private static final String APP_PIN = "app_pin";
    private static final String AUTO_LOCK_ENABLED = "auto_lock_enabled";
    private static final String AUTO_LOCK_TIME = "auto_lock_time";
    private static final String BACKUP_FILE_NAME = "Wault_Backup.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        String requestedSection = getIntent().getStringExtra("open_section");

        if ("profile".equals(requestedSection)) {
            dialogOnlyMode = false;
            setContentView(R.layout.activity_settings);
            initializeViews();
            initializeActivityLaunchers();
            loadUserProfile();
            setupListeners();
            return;
        }

        if (requestedSection != null && !requestedSection.trim().isEmpty()) {
            dialogOnlyMode = true;
            initializeActivityLaunchers();
            Window window = getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                WindowManager.LayoutParams params = window.getAttributes();
                params.dimAmount = 0.45f;
                window.setAttributes(params);
            }
            getWindow().getDecorView().post(() -> openRequestedSection(requestedSection));
            return;
        }

        dialogOnlyMode = false;
        setContentView(R.layout.activity_settings);
        initializeViews();
        initializeActivityLaunchers();
        loadUserProfile();
        setupListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);
        profileName = findViewById(R.id.profileName);
        profileEmail = findViewById(R.id.profileEmail);
        changeProfileText = findViewById(R.id.changeProfileText);
        profileInfoName = findViewById(R.id.profileInfoName);
        profileInfoEmail = findViewById(R.id.profileInfoEmail);
    }

    private void initializeActivityLaunchers() {
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) saveProfileImage(uri);
        });
        backupFilePicker = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null) createBackup(uri);
            else if (dialogOnlyMode) exitDialogMode();
        });
        restoreFilePicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) readRestoreFile(uri);
            else if (dialogOnlyMode) exitDialogMode();
        });
    }

    private void setupListeners() {
        if (backButton != null) backButton.setOnClickListener(v -> finish());
        if (changeProfileText != null) {
            changeProfileText.setOnClickListener(v -> {
                if (imagePicker != null) imagePicker.launch("image/*");
            });
        }
        if (profileImage != null) profileImage.setOnClickListener(v -> showEditProfileDialog());
        if (profileName != null) profileName.setOnClickListener(v -> showEditProfileDialog());
        if (profileEmail != null) profileEmail.setOnClickListener(v -> showEditProfileDialog());
    }

    private void exitDialogMode() {
        if (!dialogOnlyMode) {
            finish();
            return;
        }
        Intent intent = new Intent(SettingsActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void openRequestedSection(String section) {
        switch (section) {
            case "app_lock": showAppLockDialog(); break;
            case "change_pin": showChangePinDialog(); break;
            case "auto_lock": showAutoLockDialog(); break;
            case "backup": showBackupRestoreDialog(); break;
            case "delete_account": showDeleteAccountDialog(); break;
            default: exitDialogMode(); break;
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            if (profileName != null) profileName.setText("User");
            if (profileEmail != null) profileEmail.setText("No email available");
            if (profileInfoName != null) profileInfoName.setText("User");
            if (profileInfoEmail != null) profileInfoEmail.setText("No email available");
            if (profileImage != null) profileImage.setImageResource(android.R.drawable.ic_menu_myplaces);
            return;
        }

        String name = user.getDisplayName();
        String displayName = (name != null && !name.trim().isEmpty()) ? name : "User";
        if (profileName != null) profileName.setText(displayName);
        if (profileInfoName != null) profileInfoName.setText(displayName);

        String email = user.getEmail();
        String displayEmail = (email != null && !email.trim().isEmpty()) ? email : "No email available";
        if (profileEmail != null) profileEmail.setText(displayEmail);
        if (profileInfoEmail != null) profileInfoEmail.setText(displayEmail);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedPath = prefs.getString(PROFILE_IMAGE_URI, null);
        if (savedPath != null && !savedPath.isEmpty()) {
            File profileFile = new File(savedPath);
            if (profileFile.exists()) {
                if (profileImage != null) Glide.with(this).load(profileFile).circleCrop().into(profileImage);
                return;
            }
            prefs.edit().remove(PROFILE_IMAGE_URI).apply();
        }

        Uri firebasePhoto = user.getPhotoUrl();
        if (profileImage != null) {
            if (firebasePhoto != null) Glide.with(this).load(firebasePhoto).circleCrop().into(profileImage);
            else profileImage.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    private void saveProfileImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                showToast("Unable to open image");
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (bitmap == null) {
                showToast("Unable to load image");
                return;
            }
            File profileDir = new File(getFilesDir(), "profile");
            if (!profileDir.exists()) profileDir.mkdirs();
            File profileFile = new File(profileDir, "profile_picture.jpg");
            FileOutputStream outputStream = new FileOutputStream(profileFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            outputStream.close();
            bitmap.recycle();

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PROFILE_IMAGE_URI, profileFile.getAbsolutePath()).apply();
            if (profileImage != null) Glide.with(this).load(profileFile).circleCrop().into(profileImage);
            showToast("Profile picture updated");
        } catch (Exception e) {
            showToast("Failed to save profile picture");
        }
    }

    private void showEditProfileDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showToast("No user is currently signed in");
            return;
        }
        EditText nameInput = new EditText(this);
        nameInput.setHint("Enter your name");
        nameInput.setSingleLine(true);
        String currentName = user.getDisplayName();
        if (currentName != null) {
            nameInput.setText(currentName);
            nameInput.setSelection(nameInput.length());
        }
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email");
        emailInput.setSingleLine(true);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        String currentEmail = user.getEmail();
        if (currentEmail != null) emailInput.setText(currentEmail);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 0, 40, 0);
        layout.addView(nameInput);
        layout.addView(emailInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newEmail = emailInput.getText().toString().trim();
            if (newName.isEmpty()) {
                nameInput.setError("Name cannot be empty");
                return;
            }
            if (newEmail.isEmpty()) {
                emailInput.setError("Email cannot be empty");
                return;
            }
            updateProfile(user, newName, newEmail, dialog);
        }));
        dialog.show();
    }

    private void updateProfile(FirebaseUser user, String newName, String newEmail, AlertDialog dialog) {
        UserProfileChangeRequest updates = new UserProfileChangeRequest.Builder().setDisplayName(newName).build();
        user.updateProfile(updates).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                showToast("Failed to update name");
                return;
            }
            updateEmail(user, newEmail, dialog);
        });
    }

    private void updateEmail(FirebaseUser user, String newEmail, AlertDialog dialog) {
        String oldEmail = user.getEmail();
        if (oldEmail != null && oldEmail.equalsIgnoreCase(newEmail)) {
            user.reload().addOnCompleteListener(task -> {
                loadUserProfile();
                dialog.dismiss();
                showToast("Profile updated");
            });
            return;
        }
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToastLong("Verification email sent. Verify it to complete the email change.");
                loadUserProfile();
                dialog.dismiss();
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                showToastLong("Email update failed: " + error);
            }
        });
    }

    private void showAppLockDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(APP_LOCK_ENABLED, false);
        String savedPin = prefs.getString(APP_PIN, "");
        AlertDialog dialog;

        if (enabled && !savedPin.isEmpty()) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle("App Lock")
                    .setMessage("App Lock is currently enabled.\n\nWault.io requires your PIN when the app is locked.")
                    .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                    .setPositiveButton("Disable", (d, which) -> disableAppLock())
                    .create();
        } else {
            dialog = new AlertDialog.Builder(this)
                    .setTitle("App Lock")
                    .setMessage("Protect Wault.io with a 4-digit PIN.")
                    .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                    .setPositiveButton("Enable", (d, which) -> enableAppLock())
                    .create();
        }
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.show();
    }

    private void enableAppLock() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedPin = prefs.getString(APP_PIN, "");
        if (!savedPin.isEmpty()) {
            prefs.edit().putBoolean(APP_LOCK_ENABLED, true).apply();
            showToast("App Lock enabled");
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        Intent intent = new Intent(SettingsActivity.this, PinSetupActivity.class);
        startActivity(intent);
        if (dialogOnlyMode) finish();
    }

    private void disableAppLock() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedPin = prefs.getString(APP_PIN, "");
        if (savedPin.isEmpty()) {
            prefs.edit().putBoolean(APP_LOCK_ENABLED, false).apply();
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        EditText pinInput = createPinInput("Enter 4-digit PIN");
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Disable App Lock")
                .setMessage("Enter your current PIN to disable App Lock.")
                .setView(pinInput)
                .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                .setPositiveButton("Disable", null)
                .create();
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String enteredPin = pinInput.getText().toString().trim();
            if (enteredPin.length() != 4) {
                pinInput.setError("PIN must be exactly 4 digits");
                return;
            }
            if (!enteredPin.equals(savedPin)) {
                pinInput.setError("Incorrect PIN");
                return;
            }
            prefs.edit().putBoolean(APP_LOCK_ENABLED, false).remove(APP_PIN).putBoolean(AUTO_LOCK_ENABLED, false).remove(AUTO_LOCK_TIME).apply();
            dialog.dismiss();
            showToast("App Lock disabled");
            if (dialogOnlyMode) exitDialogMode();
        }));
        dialog.show();
    }

    private void showChangePinDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean appLockEnabled = prefs.getBoolean(APP_LOCK_ENABLED, false);
        String savedPin = prefs.getString(APP_PIN, "");
        if (!appLockEnabled || savedPin.isEmpty()) {
            showToast("Enable App Lock first");
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        EditText currentPin = createPinInput("Current 4-digit PIN");
        EditText newPin = createPinInput("New 4-digit PIN");
        EditText confirmPin = createPinInput("Confirm new PIN");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 0, 40, 0);
        layout.addView(currentPin);
        layout.addView(newPin);
        layout.addView(confirmPin);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Change PIN")
                .setView(layout)
                .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                .setPositiveButton("Change", null)
                .create();
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = currentPin.getText().toString().trim();
            String newValue = newPin.getText().toString().trim();
            String confirm = confirmPin.getText().toString().trim();
            if (current.length() != 4) {
                currentPin.setError("Enter your current 4-digit PIN");
                return;
            }
            if (!current.equals(savedPin)) {
                currentPin.setError("Incorrect PIN");
                return;
            }
            if (newValue.length() != 4) {
                newPin.setError("PIN must be exactly 4 digits");
                return;
            }
            if (!newValue.equals(confirm)) {
                confirmPin.setError("PINs do not match");
                return;
            }
            if (newValue.equals(current)) {
                newPin.setError("New PIN must be different");
                return;
            }
            prefs.edit().putString(APP_PIN, newValue).apply();
            dialog.dismiss();
            showToast("PIN changed successfully");
            if (dialogOnlyMode) exitDialogMode();
        }));
        dialog.show();
    }

    private EditText createPinInput(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        input.setPadding(0, 20, 0, 20);
        return input;
    }

    private void showAutoLockDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(AUTO_LOCK_ENABLED, false);
        AlertDialog dialog;
        if (enabled) {
            dialog = new AlertDialog.Builder(this)
                    .setTitle("Auto Lock")
                    .setMessage("Auto Lock is currently enabled.\n\nWault.io will lock after 30 seconds of inactivity.")
                    .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                    .setPositiveButton("Disable", null)
                    .create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                prefs.edit().putBoolean(AUTO_LOCK_ENABLED, false).remove(AUTO_LOCK_TIME).apply();
                dialog.dismiss();
                showToast("Auto Lock disabled");
                if (dialogOnlyMode) exitDialogMode();
            }));
        } else {
            dialog = new AlertDialog.Builder(this)
                    .setTitle("Auto Lock")
                    .setMessage("Automatically lock Wault.io after 30 seconds of inactivity.")
                    .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                    .setPositiveButton("Enable", null)
                    .create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                boolean appLockEnabled = prefs.getBoolean(APP_LOCK_ENABLED, false);
                String savedPin = prefs.getString(APP_PIN, "");
                if (!appLockEnabled || savedPin.isEmpty()) {
                    showToast("Enable App Lock first");
                    dialog.dismiss();
                    if (dialogOnlyMode) exitDialogMode();
                    return;
                }
                prefs.edit().putBoolean(AUTO_LOCK_ENABLED, true).putLong(AUTO_LOCK_TIME, 30000).apply();
                dialog.dismiss();
                showToast("Auto Lock enabled for 30 seconds");
                if (dialogOnlyMode) exitDialogMode();
            }));
        }
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.show();
    }

    private void showDeleteAccountDialog() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showToast("No user is currently signed in");
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Delete Account")
                        .setMessage("This will permanently delete your account and all saved passwords.\n\nThis action cannot be undone.")
                        .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                        .setPositiveButton("Continue", (d, which) -> showDeletePasswordDialog(user))
                        .create();
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.show();
    }

    private void showDeletePasswordDialog(FirebaseUser user) {
        String email = user.getEmail();
        if (email == null || email.trim().isEmpty()) {
            showToastLong("Unable to verify this account");
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your account password");
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setPadding(40, 20, 40, 20);
        AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Confirm Account Deletion")
                        .setMessage("Enter your current password to continue.")
                        .setView(passwordInput)
                        .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                        .setPositiveButton("Delete Account", null)
                        .create();
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = passwordInput.getText().toString();
            if (password.isEmpty()) {
                passwordInput.setError("Password cannot be empty");
                return;
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            passwordInput.setEnabled(false);
            reauthenticateAndDelete(user, email, password, dialog, passwordInput);
        }));
        dialog.show();
    }

    private void reauthenticateAndDelete(FirebaseUser user, String email, String password, AlertDialog dialog, EditText passwordInput) {
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (!reauthTask.isSuccessful()) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                passwordInput.setEnabled(true);
                passwordInput.setError("Incorrect password");
                return;
            }
            deleteUserDataAndAccount(user, dialog);
        });
    }

    private void deleteUserDataAndAccount(FirebaseUser user, AlertDialog dialog) {
        String uid = user.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
        userRef.removeValue().addOnCompleteListener(databaseTask -> {
            if (!databaseTask.isSuccessful()) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                showToastLong("Failed to delete account data");
                return;
            }
            user.delete().addOnCompleteListener(deleteTask -> {
                if (deleteTask.isSuccessful()) {
                    deleteLocalProfileImage();
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
                    firebaseAuth.signOut();
                    dialog.dismiss();
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String error = deleteTask.getException() != null ? deleteTask.getException().getMessage() : "Unable to delete account";
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    showToastLong("Delete failed: " + error);
                }
            });
        });
    }

    private void deleteLocalProfileImage() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedPath = prefs.getString(PROFILE_IMAGE_URI, null);
        if (savedPath != null && !savedPath.isEmpty()) {
            File profileFile = new File(savedPath);
            if (profileFile.exists()) profileFile.delete();
        }
        File profileDir = new File(getFilesDir(), "profile");
        if (profileDir.exists()) profileDir.delete();
    }

    private void showBackupRestoreDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Backup & Restore")
                        .setItems(new String[]{"Create Backup", "Restore Backup"}, (d, which) -> {
                            if (which == 0) backupFilePicker.launch(BACKUP_FILE_NAME);
                            else restoreFilePicker.launch(new String[]{"application/json", "text/plain"});
                        })
                        .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                        .create();
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.show();
    }

    private void createBackup(Uri fileUri) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showToast("No user is currently signed in");
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        DatabaseReference passwordsRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("passwords");
        passwordsRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                showToastLong("Failed to read passwords");
                if (dialogOnlyMode) exitDialogMode();
                return;
            }
            try {
                JSONArray passwordArray = new JSONArray();
                DataSnapshot snapshot = task.getResult();
                for (DataSnapshot child : snapshot.getChildren()) {
                    PasswordModel password = child.getValue(PasswordModel.class);
                    if (password == null) continue;
                    JSONObject object = new JSONObject();
                    object.put("id", password.getId());
                    object.put("website", password.getWebsite());
                    object.put("username", password.getUsername());
                    object.put("password", password.getPassword());
                    object.put("category", password.getCategory());
                    passwordArray.put(object);
                }
                JSONObject backup = new JSONObject();
                backup.put("app", "Wault.io");
                backup.put("version", 1);
                backup.put("passwords", passwordArray);
                OutputStream outputStream = getContentResolver().openOutputStream(fileUri);
                if (outputStream == null) {
                    showToastLong("Unable to create backup file");
                    if (dialogOnlyMode) exitDialogMode();
                    return;
                }
                outputStream.write(backup.toString(4).getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
                showToastLong("Backup created successfully");
                if (dialogOnlyMode) exitDialogMode();
            } catch (Exception e) {
                showToastLong("Backup failed: " + e.getMessage());
                if (dialogOnlyMode) exitDialogMode();
            }
        });
    }

    private void readRestoreFile(Uri fileUri) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showToast("No user is currently signed in");
            if (dialogOnlyMode) exitDialogMode();
            return;
        }
        try {
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                showToastLong("Unable to open backup file");
                if (dialogOnlyMode) exitDialogMode();
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
            reader.close();

            JSONObject backup = new JSONObject(builder.toString());
            if (!"Wault.io".equals(backup.optString("app"))) {
                showToastLong("Invalid Wault.io backup file");
                if (dialogOnlyMode) exitDialogMode();
                return;
            }
            if (!backup.has("passwords")) {
                showToastLong("Backup contains no passwords");
                if (dialogOnlyMode) exitDialogMode();
                return;
            }
            JSONArray passwordArray = backup.getJSONArray("passwords");
            showRestoreConfirmation(user, passwordArray);
        } catch (Exception e) {
            showToastLong("Unable to read backup file");
            if (dialogOnlyMode) exitDialogMode();
        }
    }

    private void showRestoreConfirmation(FirebaseUser user, JSONArray passwordArray) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Confirm Restore")
                        .setMessage(passwordArray.length() + " password entries will replace your current passwords.")
                        .setNegativeButton("Cancel", (d, which) -> exitDialogMode())
                        .setPositiveButton("Restore", (d, which) -> replacePasswords(user, passwordArray))
                        .create();
        dialog.setOnCancelListener(d -> exitDialogMode());
        dialog.show();
    }

    private void replacePasswords(FirebaseUser user, JSONArray passwordArray) {
        String uid = user.getUid();
        DatabaseReference passwordsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("passwords");
        passwordsRef.removeValue().addOnCompleteListener(deleteTask -> {
            if (!deleteTask.isSuccessful()) {
                showToastLong("Failed to clear existing passwords");
                if (dialogOnlyMode) exitDialogMode();
                return;
            }
            Map<String, Object> restoredData = new HashMap<>();
            try {
                for (int i = 0; i < passwordArray.length(); i++) {
                    JSONObject object = passwordArray.getJSONObject(i);
                    String id = object.optString("id");
                    String website = object.optString("website");
                    String username = object.optString("username");
                    String password = object.optString("password");
                    String category = object.optString("category");
                    if (id.isEmpty()) id = passwordsRef.push().getKey();
                    if (id == null) continue;
                    PasswordModel model = new PasswordModel(id, website, username, password, category);
                    restoredData.put(id, model);
                }
                passwordsRef.setValue(restoredData).addOnCompleteListener(restoreTask -> {
                    if (restoreTask.isSuccessful()) showToastLong("Backup restored successfully");
                    else showToastLong("Restore failed");
                    if (dialogOnlyMode) exitDialogMode();
                });
            } catch (Exception e) {
                showToastLong("Restore failed: " + e.getMessage());
                if (dialogOnlyMode) exitDialogMode();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dialogOnlyMode) return;
        if (firebaseAuth != null) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) user.reload().addOnCompleteListener(task -> loadUserProfile());
            else loadUserProfile();
        }
    }

    @Override
    public void onBackPressed() {
        if (dialogOnlyMode) exitDialogMode();
        else finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToastLong(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}