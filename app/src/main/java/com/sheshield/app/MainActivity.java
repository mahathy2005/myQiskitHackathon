package com.sheshield.app;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import android.content.Intent;
import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    // ============================================================
    // SHARED PREFERENCES
    // ============================================================

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USER_NAME = "USER_NAME";


    // ============================================================
    // VOICE SOS PERMISSION
    // ============================================================

    private static final int PERMISSION_REQUEST_CODE = 2002;

    private boolean isVoiceSosRunning = false;


    // ============================================================
    // EMERGENCY SOS STATE
    // ============================================================

    private boolean isSosActive = false;


    // ============================================================
    // EMERGENCY ALERT SOUND
    // ============================================================

    private ToneGenerator alertToneGenerator;


    // ============================================================
    // ROOT LAYOUT
    // ============================================================

    private View rootLayout;


    // ============================================================
    // DRAWER
    // ============================================================

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    // ============================================================
    // FIREBASE
    // ============================================================

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    // ============================================================
    // SOS BUTTON
    // ============================================================

    private Handler longPressHandler;
    private Runnable longPressRunnable;
    private ProgressBar longPressProgress;

    private boolean isLongPressed = false;

    private static final int LONG_PRESS_DURATION = 3000;


    // ============================================================
    // DOUBLE TAP SETTINGS
    // ============================================================

    private static final int DOUBLE_TAP_TIMEOUT = 400;

    private boolean isWaitingForSecondTap = false;

    private Handler doubleTapHandler;

    private Runnable singleTapTimeoutRunnable;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // ========================================================
        // FIREBASE INITIALIZATION
        // ========================================================

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // ========================================================
        // USER GREETING
        // ========================================================

        TextView tvUserGreeting =
                findViewById(R.id.tvUserGreeting);

        SharedPreferences sharedPreferences =
                getSharedPreferences(
                        PREF_NAME,
                        MODE_PRIVATE
                );

        String userName =
                sharedPreferences.getString(
                        KEY_USER_NAME,
                        "User"
                );

        if (tvUserGreeting != null) {

            if (userName != null &&
                    !userName.trim().isEmpty()) {

                tvUserGreeting.setText(
                        "Hello, " +
                                userName.trim() +
                                "! 👋"
                );

            } else {

                tvUserGreeting.setText(
                        "Hello, User! 👋"
                );
            }
        }


        // ========================================================
        // ROOT LAYOUT
        // ========================================================

        rootLayout =
                findViewById(R.id.main);


        // ========================================================
        // DRAWER
        // ========================================================

        drawerLayout =
                findViewById(R.id.drawerLayout);

        navigationView =
                findViewById(R.id.navigationView);


        // ========================================================
        // DOUBLE TAP HANDLER
        // ========================================================

        doubleTapHandler =
                new Handler(
                        Looper.getMainLooper()
                );


        singleTapTimeoutRunnable = () -> {

            isWaitingForSecondTap = false;

            android.util.Log.d(
                    "MainActivity",
                    "Second tap not detected"
            );
        };


        // ========================================================
        // NAVIGATION DRAWER
        // ========================================================

        setupNavigationDrawer();


        // ========================================================
        // MENU BUTTON
        // ========================================================

        ImageButton btnMenu =
                findViewById(R.id.imageView6);

        if (btnMenu != null) {

            btnMenu.setOnClickListener(v -> {

                if (drawerLayout == null) {
                    return;
                }

                if (drawerLayout.isDrawerOpen(
                        GravityCompat.START
                )) {

                    drawerLayout.closeDrawer(
                            GravityCompat.START
                    );

                } else {

                    drawerLayout.openDrawer(
                            GravityCompat.START
                    );
                }
            });
        }


        // ========================================================
        // NOTIFICATION BUTTON
        // ========================================================

        ImageButton btnNotifications =
                findViewById(R.id.btnNotifications);

        if (btnNotifications != null) {

            btnNotifications.setOnClickListener(v ->

                    Toast.makeText(
                            MainActivity.this,
                            "Notifications clicked",
                            Toast.LENGTH_SHORT
                    ).show()
            );
        }


        // ========================================================
        // KNOW MORE BUTTON
        // ========================================================

        MaterialButton btnKnowMore =
                findViewById(R.id.btnKnowMore);

        if (btnKnowMore != null) {

            btnKnowMore.setOnClickListener(v -> {

                try {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    KnowMoreActivity.class
                            );

                    startActivity(intent);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open Know More page",
                            Toast.LENGTH_SHORT
                    ).show();

                    e.printStackTrace();
                }
            });
        }


        // ========================================================
        // PULSE ANIMATION
        // ========================================================

        View pulseCircle1 =
                findViewById(R.id.pulseCircle1);

        View pulseCircle2 =
                findViewById(R.id.pulseCircle2);

        if (pulseCircle1 != null) {

            startPulseAnimation(
                    pulseCircle1,
                    0
            );
        }

        if (pulseCircle2 != null) {

            startPulseAnimation(
                    pulseCircle2,
                    600
            );
        }


        // ========================================================
        // SOS BUTTON
        // ========================================================

        setupSOSButton();


        // ========================================================
        // QUICK ACCESS
        // ========================================================

        setupQuickAccessClickListeners();


        // ========================================================
        // LOAD SAVED LANGUAGE
        // ========================================================

        String savedLang =
                getSharedPreferences(
                        "AppSettings",
                        MODE_PRIVATE
                ).getString(
                        "SELECTED_LANGUAGE",
                        "en"
                );

        if (savedLang != null &&
                !savedLang.equals("en")) {

            applyLanguageTranslation(savedLang);
        }


        // ========================================================
        // HANDLE SOS INTENT
        // ========================================================

        handleSosIntent(getIntent());


        // ========================================================
        // BACK BUTTON
        // ========================================================

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (drawerLayout != null &&
                                drawerLayout.isDrawerOpen(
                                        GravityCompat.START
                                )) {

                            drawerLayout.closeDrawer(
                                    GravityCompat.START
                            );

                        } else {

                            setEnabled(false);

                            getOnBackPressedDispatcher()
                                    .onBackPressed();

                            setEnabled(true);
                        }
                    }
                }
        );
    }


    // ============================================================
    // HANDLE NEW INTENT
    // ============================================================

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        setIntent(intent);

        handleSosIntent(intent);
    }


    // ============================================================
    // HANDLE SOS INTENT
    // ============================================================

    private void handleSosIntent(Intent intent) {

        if (intent == null) {
            return;
        }

        if (intent.getBooleanExtra(
                "TRIGGER_SOS",
                false
        )) {

            triggerEmergencyAlert();

            intent.removeExtra(
                    "TRIGGER_SOS"
            );
        }

        if (intent.getBooleanExtra(
                "DEACTIVATE_SOS",
                false
        )) {

            deactivateEmergencyAlert();

            isVoiceSosRunning = false;

            intent.removeExtra(
                    "DEACTIVATE_SOS"
            );
        }
    }


    // ============================================================
    // EMERGENCY ALERT
    // ============================================================

    private void triggerEmergencyAlert() {

        // Prevent duplicate activation
        if (isSosActive) {
            return;
        }

        isSosActive = true;

        isWaitingForSecondTap = false;

        if (doubleTapHandler != null &&
                singleTapTimeoutRunnable != null) {

            doubleTapHandler.removeCallbacks(
                    singleTapTimeoutRunnable
            );
        }


        // ========================================================
        // MESSAGE
        // ========================================================

        Toast.makeText(
                MainActivity.this,
                "EMERGENCY ALERT TRIGGERED!",
                Toast.LENGTH_LONG
        ).show();


        // ========================================================
        // PLAY ALERT SOUND
        // ========================================================

        playAlertSound();


        // ========================================================
        // CHANGE BACKGROUND
        // ========================================================

        animateBackgroundColor(
                ContextCompat.getColor(
                        this,
                        R.color.bg_normal
                ),

                ContextCompat.getColor(
                        this,
                        R.color.bg_sos_alert
                )
        );
    }


    // ============================================================
    // DEACTIVATE EMERGENCY ALERT
    // ============================================================

    private void deactivateEmergencyAlert() {

        if (!isSosActive) {
            return;
        }

        isSosActive = false;

        isWaitingForSecondTap = false;

        if (doubleTapHandler != null &&
                singleTapTimeoutRunnable != null) {

            doubleTapHandler.removeCallbacks(
                    singleTapTimeoutRunnable
            );
        }


        // ========================================================
        // STOP SOUND
        // ========================================================

        stopAlertSound();


        // ========================================================
        // MESSAGE
        // ========================================================

        Toast.makeText(
                MainActivity.this,
                "Safety Confirmed - Resetting UI",
                Toast.LENGTH_SHORT
        ).show();


        // ========================================================
        // RESET BACKGROUND
        // ========================================================

        animateBackgroundColor(
                ContextCompat.getColor(
                        this,
                        R.color.bg_sos_alert
                ),

                ContextCompat.getColor(
                        this,
                        R.color.bg_normal
                )
        );
    }


    // ============================================================
    // PLAY EMERGENCY ALERT SOUND
    // ============================================================

    private void playAlertSound() {

        try {

            // Stop previous sound if one exists
            stopAlertSound();


            // ====================================================
            // CREATE TONE GENERATOR
            // ====================================================

            alertToneGenerator =
                    new ToneGenerator(
                            AudioManager.STREAM_ALARM,
                            70
                    );


            // ====================================================
            // PLAY WARNING BEEP
            // ====================================================

            alertToneGenerator.startTone(
                    ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
            );

        } catch (Exception e) {

            android.util.Log.e(
                    "MainActivity",
                    "Unable to play emergency alert sound",
                    e
            );
        }
    }


    // ============================================================
    // STOP EMERGENCY ALERT SOUND
    // ============================================================

    private void stopAlertSound() {

        if (alertToneGenerator != null) {

            try {

                alertToneGenerator.stopTone();

                alertToneGenerator.release();

            } catch (Exception e) {

                android.util.Log.e(
                        "MainActivity",
                        "Error stopping alert sound",
                        e
                );
            }

            alertToneGenerator = null;
        }
    }


    // ============================================================
    // CHECK PERMISSIONS AND START VOICE SOS
    // ============================================================

    private void checkPermissionsAndStartVoiceService() {

        List<String> permissionsNeeded =
                new ArrayList<>();


        // ========================================================
        // MICROPHONE
        // ========================================================

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            permissionsNeeded.add(
                    Manifest.permission.RECORD_AUDIO
            );
        }


        // ========================================================
        // NOTIFICATIONS ANDROID 13+
        // ========================================================

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                permissionsNeeded.add(
                        Manifest.permission.POST_NOTIFICATIONS
                );
            }
        }


        // ========================================================
        // REQUEST PERMISSIONS
        // ========================================================

        if (!permissionsNeeded.isEmpty()) {

            ActivityCompat.requestPermissions(
                    this,
                    permissionsNeeded.toArray(
                            new String[0]
                    ),
                    PERMISSION_REQUEST_CODE
            );

        } else {

            startVoiceService();
        }
    }


    // ============================================================
// START VOICE SOS SERVICE
// ============================================================

    private void startVoiceService() {

        Intent serviceIntent =
                new Intent(
                        getApplicationContext(),
                        VoiceSosService.class
                );

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(
                        this,
                        serviceIntent
                );

            } else {

                startService(serviceIntent);
            }

            isVoiceSosRunning = true;

            Toast.makeText(
                    this,
                    "Voice SOS Activated",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            isVoiceSosRunning = false;

            android.util.Log.e(
                    "MainActivity",
                    "Unable to start Voice SOS",
                    e
            );

            Toast.makeText(
                    this,
                    "Unable to start Voice SOS",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


// ============================================================
// STOP VOICE SOS SERVICE
// ============================================================

    private void stopVoiceService() {

        Intent serviceIntent =
                new Intent(
                        getApplicationContext(),
                        VoiceSosService.class
                );

        try {

            stopService(serviceIntent);

            isVoiceSosRunning = false;

            if (isSosActive) {

                deactivateEmergencyAlert();

            } else {

                resetBackgroundToNormal();
            }

            Toast.makeText(
                    this,
                    "Voice SOS Deactivated",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            android.util.Log.e(
                    "MainActivity",
                    "Unable to stop Voice SOS",
                    e
            );
        }
    }

    // ============================================================
    // PERMISSION RESULT
    // ============================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                PERMISSION_REQUEST_CODE) {

            boolean allGranted = true;

            for (int result : grantResults) {

                if (result !=
                        PackageManager.PERMISSION_GRANTED) {

                    allGranted = false;

                    break;
                }
            }

            if (allGranted) {

                startVoiceService();

            } else {

                isVoiceSosRunning = false;

                Toast.makeText(
                        this,
                        "Microphone permission required for Voice SOS",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    // ============================================================
    // NAVIGATION DRAWER
    // ============================================================

    private void setupNavigationDrawer() {

        if (drawerLayout == null ||
                navigationView == null) {

            return;
        }

        navigationView.setNavigationItemSelectedListener(
                item -> {

                    int id = item.getItemId();


                    // HOME
                    if (id == R.id.nav_home) {

                        drawerLayout.closeDrawer(
                                GravityCompat.START
                        );
                    }


                    // TRUSTED CONTACTS
                    else if (id ==
                            R.id.nav_trusted_contacts) {

                        openTrustedContacts();
                    }


                    // LIVE LOCATION
                    else if (id ==
                            R.id.nav_live_location) {

                        Toast.makeText(
                                MainActivity.this,
                                "Live Location page coming soon",
                                Toast.LENGTH_SHORT
                        ).show();
                    }


                    // SAFE ZONES
                    else if (id ==
                            R.id.nav_safe_zones) {

                        Toast.makeText(
                                MainActivity.this,
                                "Safe Zones page coming soon",
                                Toast.LENGTH_SHORT
                        ).show();
                    }



                    // INCIDENT HISTORY
                    else if (id ==
                            R.id.nav_incident_history) {

                        Toast.makeText(
                                MainActivity.this,
                                "Incident History page coming soon",
                                Toast.LENGTH_SHORT
                        ).show();
                    }


                    // PROFILE
                    else if (id ==
                            R.id.nav_profile) {

                        Toast.makeText(
                                MainActivity.this,
                                "Profile page coming soon",
                                Toast.LENGTH_SHORT
                        ).show();
                    }


                    // LANGUAGE
                    else if (id ==
                            R.id.nav_language) {

                        showLanguageSelectionDialog();
                    }


                    // SETTINGS
                    else if (id ==
                            R.id.nav_settings) {

                        try {

                            Intent intent =
                                    new Intent(
                                            android.provider.Settings
                                                    .ACTION_APPLICATION_DETAILS_SETTINGS
                                    );

                            Uri uri =
                                    Uri.fromParts(
                                            "package",
                                            getPackageName(),
                                            null
                                    );

                            intent.setData(uri);

                            startActivity(intent);

                        } catch (Exception e) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Unable to open App Settings",
                                    Toast.LENGTH_SHORT
                            ).show();

                            e.printStackTrace();
                        }
                    }


                    // ABOUT
                    else if (id ==
                            R.id.nav_about) {

                        try {

                            Intent intent =
                                    new Intent(
                                            MainActivity.this,
                                            AboutActivity.class
                                    );

                            startActivity(intent);

                        } catch (Exception e) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Unable to open About page",
                                    Toast.LENGTH_SHORT
                            ).show();

                            e.printStackTrace();
                        }
                    }


                    drawerLayout.closeDrawer(
                            GravityCompat.START
                    );

                    return true;
                }
        );
    }


    // ============================================================
    // LANGUAGE SELECTION
    // ============================================================

    private void showLanguageSelectionDialog() {

        String[] languages = {
                "English",
                "Hindi (हिंदी)",
                "Telugu (తెలుగు)",
                "Tamil (தமிழ்)",
                "Kannada (ಕನ್ನಡ)",

        };

        String[] langCodes = {
                "en",
                "hi",
                "te",
                "ta",
                "kn",

        };

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(
                        this
                );

        builder.setTitle(
                "Select App Language"
        );

        builder.setItems(
                languages,
                (dialog, which) -> {

                    String selectedCode =
                            langCodes[which];

                    LocaleHelper.setLocale(
                            this,
                            selectedCode
                    );

                    Intent intent =
                            getIntent();

                    finish();

                    startActivity(intent);
                }
        );

        builder.create().show();
    }


    // ============================================================
    // APPLY LANGUAGE TRANSLATION
    // ============================================================

    private void applyLanguageTranslation(
            String langCode
    ) {

        if (langCode == null ||
                langCode.equals("en")) {

            return;
        }

        View rootView =
                findViewById(
                        android.R.id.content
                );

        if (rootView != null) {

            translateAllViews(
                    rootView,
                    langCode
            );
        }


        if (navigationView != null) {

            android.view.Menu menu =
                    navigationView.getMenu();

            for (int i = 0;
                 i < menu.size();
                 i++) {

                android.view.MenuItem menuItem =
                        menu.getItem(i);

                CharSequence title =
                        menuItem.getTitle();

                if (title != null &&
                        title.length() > 0) {

                    LanguageManager.translateMenuItem(
                            this,
                            menuItem,
                            langCode
                    );
                }
            }
        }
    }


    // ============================================================
    // TRANSLATE ALL VIEWS
    // ============================================================

    private void translateAllViews(
            View view,
            String langCode
    ) {

        if (view instanceof TextView) {

            TextView textView =
                    (TextView) view;

            CharSequence text =
                    textView.getText();

            if (text != null &&
                    text.length() > 0) {

                LanguageManager.translateTextView(
                        this,
                        textView,
                        langCode
                );
            }
        }


        if (view instanceof android.view.ViewGroup) {

            android.view.ViewGroup viewGroup =
                    (android.view.ViewGroup) view;

            for (int i = 0;
                 i < viewGroup.getChildCount();
                 i++) {

                View child =
                        viewGroup.getChildAt(i);

                translateAllViews(
                        child,
                        langCode
                );
            }
        }
    }


    // ============================================================
    // OPEN TRUSTED CONTACTS
    // ============================================================

    private void openTrustedContacts() {

        try {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            TrustedContactsActivity.class
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Unable to open Trusted Contacts",
                    Toast.LENGTH_SHORT
            ).show();

            e.printStackTrace();
        }
    }


    // ============================================================
    // QUICK ACCESS
    // ============================================================

    private void setupQuickAccessClickListeners() {

        // Trusted Contacts
        View cardTrustedContacts =
                findViewById(
                        R.id.cardTrustedContacts
                );

        if (cardTrustedContacts != null) {

            cardTrustedContacts.setOnClickListener(
                    v -> openTrustedContacts()
            );
        }


        // Live Location
        View cardLiveLocation =
                findViewById(R.id.cardLiveLocation);

        if (cardLiveLocation != null) {

            cardLiveLocation.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                OfflineMapActivity.class
                        );

                startActivity(intent);

            });
        }


        // Safe Zone
        View cardSafeZone = findViewById(R.id.cardSafeZone);

        if (cardSafeZone != null) {

            cardSafeZone.setOnClickListener(v -> {

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                SafeZonesActivity.class
                        );

                startActivity(intent);
            });
        }


        // Fake Call
        View cardFakeCall =
                findViewById(
                        R.id.cardFakeCall
                );

        if (cardFakeCall != null) {

            cardFakeCall.setOnClickListener(v -> {

                try {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    FakeCallActivity.class
                            );

                    startActivity(intent);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open Fake Call",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }

        View cardQuantumDashboard =
                findViewById(R.id.cardQuantumDashboard);

        if (cardQuantumDashboard != null) {
            cardQuantumDashboard.setOnClickListener(v ->
                    openQuantumDashboard()
            );
        }

        // Safety Tips
        View cardSafetyTips =
                findViewById(
                        R.id.cardSafetyTips
                );

        if (cardSafetyTips != null) {

            cardSafetyTips.setOnClickListener(v -> {

                try {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    SafetyTipsActivity.class
                            );

                    startActivity(intent);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open Safety Tips",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        }


        // Report Incident
        // Report Incident
        View cardReportIncident = findViewById(R.id.cardReportIncident);

        if (cardReportIncident != null) {
            cardReportIncident.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(
                            MainActivity.this,
                            IncidentReportActivity.class
                    );
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open Report Incident page",
                            Toast.LENGTH_SHORT
                    ).show();
                    e.printStackTrace();
                }
            });
        }


        // ========================================================
// EMERGENCY SERVICES
// ========================================================

        View cardEmergencyServices =
                findViewById(R.id.cardEmergencyServices);

        if (cardEmergencyServices != null) {

            cardEmergencyServices.setOnClickListener(v -> {

                try {

                    Intent intent =
                            new Intent(
                                    MainActivity.this,
                                    EmergencyServicesActivity.class
                            );

                    startActivity(intent);

                } catch (Exception e) {

                    Toast.makeText(
                            MainActivity.this,
                            "Unable to open Emergency Services",
                            Toast.LENGTH_SHORT
                    ).show();

                    e.printStackTrace();
                }
            });
        }


        // Voice SOS
        View cardVoiceSos =
                findViewById(
                        R.id.cardVoiceSos
                );

        if (cardVoiceSos != null) {

            cardVoiceSos.setOnClickListener(v -> {

                if (!isVoiceSosRunning) {

                    checkPermissionsAndStartVoiceService();

                } else {

                    stopVoiceService();
                }
            });
        }

        // Quantum Dashboard
        cardQuantumDashboard = findViewById(R.id.cardQuantumDashboard);
        if (cardQuantumDashboard != null) {
            cardQuantumDashboard.setOnClickListener(v -> openQuantumDashboard());
        }
    }



    // ============================================================
    // SOS BUTTON SETUP
    // ============================================================

    private void setupSOSButton() {

        View btnSOS =
                findViewById(
                        R.id.button
                );

        longPressProgress =
                findViewById(
                        R.id.longPressProgress
                );

        longPressHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        if (btnSOS == null) {
            return;
        }


        // ========================================================
        // 3 SECOND LONG PRESS
        // ========================================================

        longPressRunnable = () -> {

            isLongPressed = true;

            if (!isSosActive) {

                triggerEmergencyAlert();

            } else {

                android.util.Log.d(
                        "MainActivity",
                        "SOS already active"
                );
            }

            resetLongPressUI();
        };


        // ========================================================
        // TOUCH LISTENER
        // ========================================================

        btnSOS.setOnTouchListener(
                (v, event) -> {

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            isLongPressed = false;

                            if (longPressProgress != null) {

                                longPressProgress.setVisibility(
                                        View.VISIBLE
                                );

                                longPressProgress.setProgress(
                                        0
                                );

                                animateProgress();
                            }

                            longPressHandler.postDelayed(
                                    longPressRunnable,
                                    LONG_PRESS_DURATION
                            );

                            return true;


                        case MotionEvent.ACTION_UP:

                            longPressHandler.removeCallbacks(
                                    longPressRunnable
                            );

                            resetLongPressUI();

                            if (isLongPressed) {

                                isLongPressed = false;

                                return true;
                            }

                            handleShortTap();

                            return true;


                        case MotionEvent.ACTION_CANCEL:

                            longPressHandler.removeCallbacks(
                                    longPressRunnable
                            );

                            resetLongPressUI();

                            isLongPressed = false;

                            return true;
                    }

                    return true;
                }
        );
    }


    // ============================================================
    // HANDLE SHORT TAP / DOUBLE TAP
    // ============================================================

    private void handleShortTap() {

        // No SOS = nothing to deactivate
        if (!isSosActive) {
            return;
        }


        // First tap
        if (!isWaitingForSecondTap) {

            isWaitingForSecondTap = true;

            android.util.Log.d(
                    "MainActivity",
                    "First tap detected - waiting for second tap"
            );

            doubleTapHandler.postDelayed(
                    singleTapTimeoutRunnable,
                    DOUBLE_TAP_TIMEOUT
            );

            return;
        }


        // Second tap
        isWaitingForSecondTap = false;

        doubleTapHandler.removeCallbacks(
                singleTapTimeoutRunnable
        );

        android.util.Log.d(
                "MainActivity",
                "DOUBLE TAP detected - deactivating SOS"
        );

        deactivateEmergencyAlert();
    }


    // ============================================================
    // RESET SOS PROGRESS
    // ============================================================

    private void resetLongPressUI() {

        if (longPressProgress != null) {

            longPressProgress.clearAnimation();

            longPressProgress.setVisibility(
                    View.GONE
            );

            longPressProgress.setProgress(
                    0
            );
        }
    }


    // ============================================================
    // SOS PROGRESS ANIMATION
    // ============================================================

    private void animateProgress() {

        if (longPressProgress == null) {
            return;
        }

        ObjectAnimator animation =
                ObjectAnimator.ofInt(
                        longPressProgress,
                        "progress",
                        0,
                        100
                );

        animation.setDuration(
                LONG_PRESS_DURATION
        );

        animation.start();
    }


    // ============================================================
    // RESET BACKGROUND
    // ============================================================

    private void resetBackgroundToNormal() {

        if (rootLayout == null) {
            return;
        }

        rootLayout.setBackgroundColor(
                ContextCompat.getColor(
                        this,
                        R.color.bg_normal
                )
        );
    }


    // ============================================================
    // BACKGROUND COLOR ANIMATION
    // ============================================================

    private void animateBackgroundColor(
            int colorFrom,
            int colorTo
    ) {

        if (rootLayout == null) {
            return;
        }

        ValueAnimator colorAnimation =
                ValueAnimator.ofObject(
                        new ArgbEvaluator(),
                        colorFrom,
                        colorTo
                );

        colorAnimation.setDuration(600);

        colorAnimation.addUpdateListener(
                animator -> {

                    int animatedColor =
                            (int) animator.getAnimatedValue();

                    rootLayout.setBackgroundColor(
                            animatedColor
                    );
                }
        );

        colorAnimation.start();
    }


    // ============================================================
    // PULSE ANIMATION
    // ============================================================

    private void startPulseAnimation(
            View view,
            long startDelay
    ) {

        ObjectAnimator scaleX =
                ObjectAnimator.ofFloat(
                        view,
                        "scaleX",
                        1.0f,
                        1.8f
                );

        ObjectAnimator scaleY =
                ObjectAnimator.ofFloat(
                        view,
                        "scaleY",
                        1.0f,
                        1.8f
                );

        ObjectAnimator alpha =
                ObjectAnimator.ofFloat(
                        view,
                        "alpha",
                        0.8f,
                        0.0f
                );

        AnimatorSet animatorSet =
                new AnimatorSet();

        animatorSet.playTogether(
                scaleX,
                scaleY,
                alpha
        );

        animatorSet.setDuration(1200);

        animatorSet.setStartDelay(startDelay);

        animatorSet.addListener(
                new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(
                            Animator animation
                    ) {

                        animatorSet.setStartDelay(0);

                        animatorSet.start();
                    }
                }
        );

        animatorSet.start();
    }


    // ============================================================
    // ACTIVITY DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        // ========================================================
        // STOP EMERGENCY ALERT SOUND
        // ========================================================

        stopAlertSound();


        // ========================================================
        // REMOVE LONG PRESS CALLBACK
        // ========================================================

        if (longPressHandler != null &&
                longPressRunnable != null) {

            longPressHandler.removeCallbacks(
                    longPressRunnable
            );
        }


        // ========================================================
        // REMOVE DOUBLE TAP CALLBACK
        // ========================================================

        if (doubleTapHandler != null &&
                singleTapTimeoutRunnable != null) {

            doubleTapHandler.removeCallbacks(
                    singleTapTimeoutRunnable
            );
        }


        // IMPORTANT:
        // DO NOT call stopVoiceService() here.
        // VoiceSosService must continue running outside MainActivity.

        super.onDestroy();

    }

    private void openQuantumDashboard() {
        Intent intent = new Intent(
                OfflineMapActivity.this,
                QuantumDashboardActivity.class
        );
        startActivity(intent);
    }

}
