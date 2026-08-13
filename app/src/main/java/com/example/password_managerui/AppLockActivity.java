package com.example.password_managerui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AppLockActivity extends AppCompatActivity {

    private EditText pinInput;
    private Button unlockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_app_lock);

        pinInput = findViewById(R.id.pinInput);
        unlockButton = findViewById(R.id.unlockButton);

        unlockButton.setOnClickListener(v -> {

            String enteredPin =
                    pinInput.getText().toString().trim();

            if (enteredPin.length() != 4) {

                Toast.makeText(
                        AppLockActivity.this,
                        "PIN must be exactly 4 digits",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (enteredPin.equals("1234")) {

                openHome();

            } else {

                Toast.makeText(
                        AppLockActivity.this,
                        "Incorrect PIN",
                        Toast.LENGTH_SHORT
                ).show();

                pinInput.setText("");
            }
        });
    }

    private void openHome() {

        Intent intent = new Intent(
                AppLockActivity.this,
                HomeActivity.class
        );

        startActivity(intent);
        finish();
    }
}