    package com.example.password_managerui;

    import android.content.Intent;
    import android.os.Bundle;
    import android.text.InputFilter;
    import android.text.InputType;
    import android.widget.EditText;
    import android.widget.Toast;

    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.splashscreen.SplashScreen;

    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;

    public class MainActivity extends AppCompatActivity {

        private FirebaseAuth firebaseAuth;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            SplashScreen.installSplashScreen(this);
            super.onCreate(savedInstanceState);

            firebaseAuth = FirebaseAuth.getInstance();

            FirebaseUser currentUser =
                    firebaseAuth.getCurrentUser();

            if (currentUser != null) {
                checkAppLock();
            } else {
                openLogin();
            }
        }

        private void checkAppLock() {

            boolean appLockEnabled =
                    getSharedPreferences(
                            "WaultPrefs",
                            MODE_PRIVATE
                    ).getBoolean(
                            "app_lock_enabled",
                            false
                    );

            if (appLockEnabled) {
                showPinDialog();
            } else {
                openHome();
            }
        }

        private void showPinDialog() {

            EditText pinInput = new EditText(this);

            pinInput.setHint("Enter 4-digit PIN");

            pinInput.setInputType(
                    InputType.TYPE_CLASS_NUMBER |
                            InputType.TYPE_NUMBER_VARIATION_PASSWORD
            );

            pinInput.setFilters(
                    new InputFilter[]{
                            new InputFilter.LengthFilter(4)
                    }
            );

            pinInput.setPadding(
                    40,
                    20,
                    40,
                    20
            );

            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setTitle("App Locked")
                            .setMessage(
                                    "Enter your PIN to continue."
                            )
                            .setView(pinInput)
                            .setCancelable(false)
                            .setPositiveButton(
                                    "Unlock",
                                    null
                            )
                            .create();

            dialog.setOnShowListener(d -> {

                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(v -> {

                    String enteredPin =
                            pinInput.getText()
                                    .toString()
                                    .trim();

                    String savedPin =
                            getSharedPreferences(
                                    "WaultPrefs",
                                    MODE_PRIVATE
                            ).getString(
                                    "app_pin",
                                    ""
                            );

                    if (enteredPin.length() != 4) {

                        pinInput.setError(
                                "PIN must be 4 digits"
                        );

                        return;
                    }

                    if (!enteredPin.equals(savedPin)) {

                        pinInput.setError(
                                "Incorrect PIN"
                        );

                        return;
                    }

                    Toast.makeText(
                            MainActivity.this,
                            "App unlocked",
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();

                    openHome();
                });
            });

            dialog.show();
        }

        private void openHome() {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            HomeActivity.class
                    );

            startActivity(intent);
            finish();
        }

        private void openLogin() {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            LoginActivity.class
                    );

            startActivity(intent);
            finish();
        }
    }