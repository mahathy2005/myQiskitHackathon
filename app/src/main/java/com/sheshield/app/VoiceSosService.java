package com.sheshield.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import android.content.Intent;
import android.content.pm.ServiceInfo;

import android.media.AudioManager;
import android.media.ToneGenerator;

import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;


public class VoiceSosService extends Service
        implements RecognitionListener {


    // ============================================================
    // CONSTANTS
    // ============================================================

    private static final String TAG =
            "VoiceSosService";


    // ============================================================
    // FOREGROUND SERVICE NOTIFICATION
    // ============================================================

    private static final String CHANNEL_ID =
            "VoiceSosChannel";

    private static final int NOTIFICATION_ID =
            101;


    // ============================================================
    // SOS NOTIFICATION
    // ============================================================

    private static final String SOS_CHANNEL_ID =
            "SOS_ALERT_CHANNEL";

    private static final int SOS_NOTIFICATION_ID =
            2026;


    // ============================================================
    // VOSK
    // ============================================================

    private SpeechService speechService;


    // ============================================================
    // SOS STATE
    // ============================================================

    private boolean isSosTriggered =
            false;


    // ============================================================
    // EMERGENCY SOUND
    // ============================================================

    private ToneGenerator alertToneGenerator;


    // ============================================================
    // SERVICE CREATE
    // ============================================================

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(
                TAG,
                "Voice SOS Service Created"
        );


        // ========================================================
        // CREATE NOTIFICATION CHANNELS
        // ========================================================

        createNotificationChannel();

        createSosNotificationChannel();


        // ========================================================
        // FOREGROUND SERVICE NOTIFICATION
        // ========================================================

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "SheShield Protection Active"
                        )
                        .setContentText(
                                "Listening for emergency keyword..."
                        )
                        .setSmallIcon(
                                R.mipmap.ic_launcher
                        )
                        .setPriority(
                                NotificationCompat.PRIORITY_LOW
                        )
                        .setOngoing(true)
                        .setCategory(
                                NotificationCompat.CATEGORY_SERVICE
                        )
                        .build();


        // ========================================================
        // START FOREGROUND SERVICE
        // ========================================================

        try {

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R) {

                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                );

            } else {

                startForeground(
                        NOTIFICATION_ID,
                        notification
                );
            }

            Log.d(
                    TAG,
                    "Foreground Voice SOS service started"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to start foreground service",
                    e
            );
        }


        // ========================================================
        // INITIALIZE VOSK
        // ========================================================

        initVoskModel();
    }


    // ============================================================
    // INITIALIZE VOSK MODEL
    // ============================================================

    private void initVoskModel() {

        /*
         * Required asset structure:
         *
         * app/
         * └── src/
         *     └── main/
         *         └── assets/
         *             └── model-en-us/
         *                 ├── am/
         *                 ├── conf/
         *                 ├── graph/
         *                 └── ivector/
         *
         * Folder name:
         *
         * model-en-us
         */


        StorageService.unpack(
                this,
                "model-en-us",
                "model",

                model -> {

                    try {

                        // ====================================================
                        // VOICE COMMAND GRAMMAR
                        // ====================================================

                        String grammar =
                                "["
                                        + "\"help\", "
                                        + "\"help me\", "
                                        + "\"save me\", "
                                        + "\"emergency\", "
                                        + "\"call me\", "
                                        + "\"safe\", "
                                        + "\"i am safe\", "
                                        + "\"[unk]\""
                                        + "]";


                        // ====================================================
                        // CREATE RECOGNIZER
                        // ====================================================

                        Recognizer recognizer =
                                new Recognizer(
                                        model,
                                        16000.0f,
                                        grammar
                                );


                        // ====================================================
                        // CREATE SPEECH SERVICE
                        // ====================================================

                        speechService =
                                new SpeechService(
                                        recognizer,
                                        16000.0f
                                );


                        // ====================================================
                        // START LISTENING
                        // ====================================================

                        speechService.startListening(
                                VoiceSosService.this
                        );


                        Log.d(
                                TAG,
                                "Vosk Speech Recognition Started"
                        );

                    } catch (IOException e) {

                        Log.e(
                                TAG,
                                "Failed to initialize Vosk recognizer",
                                e
                        );
                    }
                },

                error -> {

                    Log.e(
                            TAG,
                            "Failed to unpack model from assets. " +
                                    "Check model-en-us folder.",
                            error
                    );
                }
        );
    }


    // ============================================================
    // VOSK PARTIAL RESULT
    // ============================================================

    // ============================================================
// VOSK PARTIAL RESULT
// ============================================================

    @Override
    public void onPartialResult(
            String hypothesis
    ) {

        // IMPORTANT:
        // Do NOT trigger SOS from partial speech.
        // Partial results can sometimes contain
        // incorrect/intermediate words.

        Log.d(
                TAG,
                "Vosk partial: " + hypothesis
        );
    }


    // ============================================================
    // VOSK NORMAL RESULT
    // ============================================================

    @Override
    public void onResult(
            String hypothesis
    ) {

        checkKeyword(hypothesis);
    }


    // ============================================================
    // VOSK FINAL RESULT
    // ============================================================

    @Override
    public void onFinalResult(
            String hypothesis
    ) {

        checkKeyword(hypothesis);
    }


    // ============================================================
    // CHECK VOICE KEYWORDS
    // ============================================================

    private void checkKeyword(
            String jsonResult
    ) {

        // ========================================================
        // BASIC VALIDATION
        // ========================================================

        if (jsonResult == null ||
                jsonResult.trim().isEmpty()) {

            return;
        }


        // ========================================================
        // DEBUG RAW OUTPUT
        // ========================================================

        Log.d(
                TAG,
                "Vosk RAW Output: " + jsonResult
        );


        try {

            JSONObject obj =
                    new JSONObject(jsonResult);


            // ====================================================
            // FINAL TEXT
            // ====================================================

            String text =
                    obj.optString(
                                    "text",
                                    ""
                            )
                            .toLowerCase()
                            .trim();


            // ====================================================
            // PARTIAL TEXT
            // ====================================================

            String partial =
                    obj.optString(
                                    "partial",
                                    ""
                            )
                            .toLowerCase()
                            .trim();


            // ====================================================
            // DEBUG
            // ====================================================

            Log.d(
                    TAG,
                    "Recognized text: " + text
            );

            Log.d(
                    TAG,
                    "Recognized partial: " + partial
            );


            // ====================================================
            // IGNORE EMPTY / UNKNOWN
            // ====================================================

            if ((text.isEmpty() ||
                    text.equals("[unk]"))
                    &&
                    (partial.isEmpty() ||
                            partial.equals("[unk]"))) {

                return;
            }


            // ====================================================
            // SAFETY / DEACTIVATION
            //
            // SAY:
            //
            // SAFE
            //
            // OR:
            //
            // I AM SAFE
            // ====================================================

            boolean safeDetected =
                    text.contains("safe")
                            || partial.contains("safe");


            if (safeDetected) {

                Log.d(
                        TAG,
                        "Safety keyword detected - " +
                                "deactivating SOS"
                );


                // =================================================
                // SHOW DEACTIVATED NOTIFICATION
                // =================================================

                showSosNotification(
                        "SOS DEACTIVATED",
                        "Safety confirmed. SheShield SOS protection has been deactivated.",
                        false
                );


                // =================================================
                // STOP SOS SOUND
                // =================================================

                stopEmergencySound();


                // =================================================
                // STOP VIBRATION
                // =================================================

                stopSosVibration();


                // =================================================
                // OPEN MAIN ACTIVITY
                // =================================================

                Intent deactivateIntent =
                        new Intent(
                                this,
                                MainActivity.class
                        );

                deactivateIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                );


                // =================================================
                // SEND DEACTIVATE COMMAND
                // =================================================

                deactivateIntent.putExtra(
                        "DEACTIVATE_SOS",
                        true
                );


                try {

                    startActivity(
                            deactivateIntent
                    );

                } catch (Exception e) {

                    Log.e(
                            TAG,
                            "Unable to open MainActivity",
                            e
                    );
                }


                // =================================================
                // RESET SOS STATE
                // =================================================

                isSosTriggered = false;


                // =================================================
                // CONFIRMATION
                // =================================================

                Toast.makeText(
                        getApplicationContext(),
                        "Voice SOS Deactivated " +
                                "(Safety Confirmed)",
                        Toast.LENGTH_SHORT
                ).show();


                // =================================================
                // STOP VOICE SERVICE
                // =================================================

                stopSelf();

                return;
            }


            // ====================================================
            // PREVENT REPEATED SOS
            // ====================================================

            if (isSosTriggered) {

                return;
            }


            // ====================================================
            // FAKE CALL TRIGGER ("call me")
            // ====================================================

            boolean callMeDetected =
                    text.contains("call me")
                            || partial.contains("call me");

            if (callMeDetected) {

                Log.d(
                        TAG,
                        "Fake Call trigger keyword detected!"
                );

                Intent fakeCallIntent =
                        new Intent(
                                this,
                                FakeCallActivity.class
                        );

                fakeCallIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                );

                try {

                    startActivity(
                            fakeCallIntent
                    );

                } catch (Exception e) {

                    Log.e(
                            TAG,
                            "Unable to open FakeCallActivity from Voice Service",
                            e
                    );
                }

                return;
            }


            // ====================================================
            // EMERGENCY KEYWORDS
            // ====================================================

            boolean helpDetected =
                    text.equals("help")
                            || text.equals("help me");

            boolean saveDetected =
                    text.equals("save me");

            boolean emergencyDetected =
                    text.equals("emergency");

            // ====================================================
            // TRIGGER SOS
            // ====================================================

            if (helpDetected ||
                    saveDetected ||
                    emergencyDetected) {

                isSosTriggered = true;


                Log.d(
                        TAG,
                        "Emergency keyword detected!"
                );


                triggerSos();
            }


        } catch (JSONException e) {

            Log.e(
                    TAG,
                    "Error parsing Vosk result JSON",
                    e
            );
        }
    }


    // ============================================================
    // PLAY EMERGENCY ALERT SOUND
    // ============================================================

    private void playEmergencySound() {

        try {

            // ====================================================
            // STOP PREVIOUS SOUND
            // ====================================================

            stopEmergencySound();


            // ====================================================
            // CREATE TONE GENERATOR
            // ====================================================

            alertToneGenerator =
                    new ToneGenerator(
                            AudioManager.STREAM_ALARM,
                            100
                    );


            // ====================================================
            // PLAY SAME EMERGENCY BEEP
            // ====================================================

            alertToneGenerator.startTone(
                    ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
            );


            Log.d(
                    TAG,
                    "Emergency beep started"
            );


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to play emergency beep",
                    e
            );
        }
    }


    // ============================================================
    // STOP EMERGENCY ALERT SOUND
    // ============================================================

    private void stopEmergencySound() {

        if (alertToneGenerator != null) {

            try {

                alertToneGenerator.stopTone();

                alertToneGenerator.release();

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Error stopping emergency sound",
                        e
                );
            }

            alertToneGenerator = null;
        }
    }


    // ============================================================
    // VIBRATE FOR SOS
    // ============================================================

    private void vibrateForSos() {

        try {

            Vibrator vibrator =
                    (Vibrator) getSystemService(
                            VIBRATOR_SERVICE
                    );

            if (vibrator == null ||
                    !vibrator.hasVibrator()) {

                Log.d(
                        TAG,
                        "Device has no vibrator"
                );

                return;
            }

            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.O) {

                VibrationEffect effect =
                        VibrationEffect.createWaveform(
                                new long[]{
                                        0,
                                        500,
                                        200,
                                        500,
                                        200,
                                        800
                                },
                                -1
                        );

                vibrator.vibrate(effect);

            } else {

                vibrator.vibrate(
                        new long[]{
                                0,
                                500,
                                200,
                                500,
                                200,
                                800
                        },
                        -1
                );
            }

            Log.d(
                    TAG,
                    "SOS vibration started"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to vibrate for SOS",
                    e
            );
        }
    }


    // ============================================================
    // STOP SOS VIBRATION
    // ============================================================

    private void stopSosVibration() {

        try {

            Vibrator vibrator =
                    (Vibrator) getSystemService(
                            VIBRATOR_SERVICE
                    );

            if (vibrator != null) {

                vibrator.cancel();
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Unable to stop SOS vibration",
                    e
            );
        }
    }


    // ============================================================
    // TRIGGER SOS
    // ============================================================

    private void triggerSos() {

        Log.d(TAG, "Triggering SOS from Voice Service!");

        // 1. SAME EMERGENCY BEEP
        playEmergencySound();

        // 2. VIBRATION
        vibrateForSos();

        // 3. SOS NOTIFICATION
        showSosNotification(
                "SOS ACTIVATED",
                "Emergency detected. SheShield protection has been activated.",
                true
        );

        // 4. UPDATE MAIN ACTIVITY → RED SOS BACKGROUND
        Intent sosIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        sosIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        sosIntent.putExtra(
                "TRIGGER_SOS",
                true
        );

        try {

            startActivity(sosIntent);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Could not open MainActivity. SOS still active.",
                    e
            );
        }

        // Allow another voice SOS after 5 seconds
        new Handler(
                Looper.getMainLooper()
        ).postDelayed(
                () -> {

                    isSosTriggered = false;

                    Log.d(
                            TAG,
                            "SOS trigger reset"
                    );

                },
                5000
        );
    }


    // ============================================================
    // CREATE FOREGROUND NOTIFICATION CHANNEL
    // ============================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Voice SOS Protection",
                            NotificationManager.IMPORTANCE_LOW
                    );


            channel.setDescription(
                    "Monitors voice triggers for emergency SOS"
            );


            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );


            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }


    // ============================================================
    // CREATE SOS NOTIFICATION CHANNEL
    // ============================================================

    private void createSosNotificationChannel() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            SOS_CHANNEL_ID,
                            "SheShield SOS Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                    );


            channel.setDescription(
                    "Emergency SOS activation and deactivation alerts"
            );


            // ====================================================
            // ENABLE VIBRATION
            // ====================================================

            channel.enableVibration(true);


            // ====================================================
            // LOCK SCREEN
            // ====================================================

            channel.setLockscreenVisibility(
                    Notification.VISIBILITY_PUBLIC
            );


            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );


            if (manager != null) {

                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }


    // ============================================================
    // SHOW SOS NOTIFICATION
    // ============================================================

    private void showSosNotification(
            String title,
            String message,
            boolean activated
    ) {

        try {

            // ====================================================
            // MAIN ACTIVITY INTENT
            // ====================================================

            Intent intent =
                    new Intent(
                            this,
                            MainActivity.class
                    );


            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP
            );


            // ====================================================
            // COMMAND
            // ====================================================

            if (activated) {

                intent.putExtra(
                        "TRIGGER_SOS",
                        true
                );

            } else {

                intent.putExtra(
                        "DEACTIVATE_SOS",
                        true
                );
            }


            // ====================================================
            // PENDING INTENT FLAGS
            // ====================================================

            int pendingIntentFlags =
                    PendingIntent.FLAG_UPDATE_CURRENT;


            if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.M) {

                pendingIntentFlags |=
                        PendingIntent.FLAG_IMMUTABLE;
            }


            // ====================================================
            // PENDING INTENT
            // ====================================================

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(
                            this,
                            activated ? 1 : 2,
                            intent,
                            pendingIntentFlags
                    );


            // ====================================================
            // NOTIFICATION BUILDER
            // ====================================================

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(
                            this,
                            SOS_CHANNEL_ID
                    )
                            .setSmallIcon(
                                    R.mipmap.ic_launcher
                            )
                            .setContentTitle(
                                    title
                            )
                            .setContentText(
                                    message
                            )
                            .setStyle(
                                    new NotificationCompat.BigTextStyle()
                                            .bigText(message)
                            )
                            .setPriority(
                                    NotificationCompat.PRIORITY_HIGH
                            )
                            .setCategory(
                                    NotificationCompat.CATEGORY_ALARM
                            )
                            .setVisibility(
                                    NotificationCompat.VISIBILITY_PUBLIC
                            )
                            .setAutoCancel(true)
                            .setContentIntent(
                                    pendingIntent
                            );


            // ====================================================
            // SHOW NOTIFICATION
            // ====================================================

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    NOTIFICATION_SERVICE
                            );


            if (manager != null) {

                manager.notify(
                        activated
                                ? SOS_NOTIFICATION_ID
                                : SOS_NOTIFICATION_ID + 1,
                        builder.build()
                );


                Log.d(
                        TAG,
                        "SOS notification shown: " +
                                title
                );
            }


        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to show SOS notification",
                    e
            );
        }
    }


    // ============================================================
    // SERVICE START COMMAND
    // ============================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        Log.d(
                TAG,
                "Voice SOS Service onStartCommand"
        );


        // ========================================================
        // KEEP SERVICE ALIVE
        // ========================================================

        return START_STICKY;
    }


    // ============================================================
    // SERVICE DESTROY
    // ============================================================

    @Override
    public void onDestroy() {

        Log.d(
                TAG,
                "Voice SOS Service Stopped"
        );


        // ========================================================
        // STOP VOSK
        // ========================================================

        if (speechService != null) {

            try {

                speechService.stop();

                speechService.shutdown();

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Error shutting down Vosk",
                        e
                );
            }

            speechService = null;
        }


        // ========================================================
        // STOP EMERGENCY SOUND
        // ========================================================

        stopEmergencySound();


        // ========================================================
        // STOP VIBRATION
        // ========================================================

        stopSosVibration();


        // ========================================================
        // REMOVE FOREGROUND SERVICE
        // ========================================================

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.N) {

            stopForeground(
                    STOP_FOREGROUND_REMOVE
            );

        } else {

            stopForeground(true);
        }


        super.onDestroy();
    }


    // ============================================================
    // SERVICE BIND
    // ============================================================

    @Nullable
    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }


    // ============================================================
    // VOSK ERROR
    // ============================================================

    @Override
    public void onError(
            Exception exception
    ) {

        Log.e(
                TAG,
                "Vosk recognition error",
                exception
        );
    }


    // ============================================================
    // VOSK TIMEOUT
    // ============================================================

    @Override
    public void onTimeout() {

        Log.d(
                TAG,
                "Vosk recognition timeout"
        );
    }
}