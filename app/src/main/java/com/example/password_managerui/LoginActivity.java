package com.example.password_managerui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput;
    private EditText passwordInput;

    private TextView forgotPasswordText;
    private TextView signupText;

    private Button loginButton;

    private FirebaseAuth firebaseAuth;

    private boolean isSignUpMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        signupText = findViewById(R.id.signupText);

        loginButton = findViewById(R.id.loginButton);

        firebaseAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(v -> {

            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                        LoginActivity.this,
                        "Please enter email and password",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (isSignUpMode) {
                createAccount(email, password);
            } else {
                loginUser(email, password);
            }
        });

        forgotPasswordText.setOnClickListener(v -> {

            String email = emailInput.getText().toString().trim();

            if (email.isEmpty()) {

                Toast.makeText(
                        LoginActivity.this,
                        "Enter your email first",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Password reset email sent",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    LoginActivity.this,
                                    "Unable to send reset email",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
        });

        signupText.setOnClickListener(v -> {

            if (!isSignUpMode) {

                isSignUpMode = true;

                loginButton.setText("Create Account");

                signupText.setText("Already have an account? Login");

                forgotPasswordText.setVisibility(TextView.GONE);

            } else {

                isSignUpMode = false;

                loginButton.setText("Login");

                signupText.setText("Don't have an account? Sign Up");

                forgotPasswordText.setVisibility(TextView.VISIBLE);
            }
        });
    }

    private void createAccount(String email, String password) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "Account created successfully",
                                Toast.LENGTH_SHORT
                        ).show();

                        openHome();

                    } else {

                        Toast.makeText(
                                LoginActivity.this,
                                "Account creation failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void loginUser(String email, String password) {

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                LoginActivity.this,
                                "Login successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        openHome();

                    } else {

                        Toast.makeText(
                                LoginActivity.this,
                                "Invalid email or password",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private void openHome() {

        Intent intent = new Intent(
                LoginActivity.this,
                HomeActivity.class
        );

        startActivity(intent);

        finish();
    }
}