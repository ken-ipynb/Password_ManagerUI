package com.example.password_managerui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinSetupActivity extends AppCompatActivity {

    private EditText pinInput;
    private EditText confirmPinInput;
    private Button savePinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pin_setup);

        pinInput = findViewById(R.id.pinInput);
        confirmPinInput = findViewById(R.id.confirmPinInput);
        savePinButton = findViewById(R.id.savePinButton);

        savePinButton.setOnClickListener(v -> {

            String pin =
                    pinInput.getText().toString().trim();

            String confirmPin =
                    confirmPinInput.getText().toString().trim();

            if (pin.length() != 4 ||
                    confirmPin.length() != 4) {

                Toast.makeText(
                        this,
                        "PIN must be exactly 4 digits",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (!pin.equals(confirmPin)) {

                Toast.makeText(
                        this,
                        "PINs do not match",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            getSharedPreferences(
                    "WaultPrefs",
                    MODE_PRIVATE
            )
                    .edit()
                    .putBoolean("app_lock_enabled", true)
                    .putString("app_pin", pin)
                    .apply();

            Toast.makeText(
                    this,
                    "App Lock enabled",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });
    }
}