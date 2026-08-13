package com.example.password_managerui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class AutoLockActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WaultPrefs";

    private static final String APP_LOCK_ENABLED =
            "app_lock_enabled";

    private static final String AUTO_LOCK_ENABLED =
            "auto_lock_enabled";

    private static final long AUTO_LOCK_DELAY =
            30 * 1000L;

    private final Handler autoLockHandler =
            new Handler(Looper.getMainLooper());

    private boolean activityVisible = false;

    private final Runnable autoLockRunnable = () -> {

        if (!activityVisible) {
            lockApp();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {

        super.onResume();

        activityVisible = true;

        stopAutoLockTimer();
    }

    @Override
    protected void onPause() {

        super.onPause();

        activityVisible = false;

        startAutoLockTimer();
    }

    private void startAutoLockTimer() {

        stopAutoLockTimer();

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        boolean appLockEnabled =
                prefs.getBoolean(
                        APP_LOCK_ENABLED,
                        false
                );

        boolean autoLockEnabled =
                prefs.getBoolean(
                        AUTO_LOCK_ENABLED,
                        false
                );

        if (!appLockEnabled || !autoLockEnabled) {
            return;
        }

        autoLockHandler.postDelayed(
                autoLockRunnable,
                AUTO_LOCK_DELAY
        );
    }

    private void stopAutoLockTimer() {

        autoLockHandler.removeCallbacks(
                autoLockRunnable
        );
    }

    private void lockApp() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                );

        boolean appLockEnabled =
                prefs.getBoolean(
                        APP_LOCK_ENABLED,
                        false
                );

        boolean autoLockEnabled =
                prefs.getBoolean(
                        AUTO_LOCK_ENABLED,
                        false
                );

        if (!appLockEnabled || !autoLockEnabled) {
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.putExtra(
                "auto_lock",
                true
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    @Override
    protected void onDestroy() {

        stopAutoLockTimer();

        super.onDestroy();
    }
}