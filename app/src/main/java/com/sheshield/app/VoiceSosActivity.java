package com.sheshield.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VoiceSosActivity extends BaseActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private Button btnToggleVoiceSos;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ============================================================
        // SHOW OVER LOCK SCREEN & TURN ON SCREEN
        // ============================================================
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_fake_call);

        // Keep whatever existing code/methods were already inside your onCreate here!
    }

    private void checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        startVoiceService();
    }

    private void startVoiceService() {
        Intent intent = new Intent(this, VoiceSosService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        isServiceRunning = true;
        btnToggleVoiceSos.setText("Stop Voice Detection");
        Toast.makeText(this, "Voice SOS Active. Say 'call me' or 'help'", Toast.LENGTH_LONG).show();
    }

    private void stopVoiceService() {
        Intent intent = new Intent(this, VoiceSosService.class);
        stopService(intent);
        isServiceRunning = false;
        btnToggleVoiceSos.setText("Start Voice Detection");
        Toast.makeText(this, "Voice SOS Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceService();
            } else {
                Toast.makeText(this, "Microphone permission is required for Voice SOS", Toast.LENGTH_SHORT).show();
            }
        }
    }
}