package com.sheshield.app;
import android.util.Log;
import android.content.Intent;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaPlayer;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;
import java.util.Random;

public class FakeCallActivity extends BaseActivity {

    // ============================================================
    // RINGTONE
    // ============================================================

    private Ringtone ringtone;

    // ============================================================
    // FAKE CALL VIDEO AUDIO
    // ============================================================

    private MediaPlayer fakeCallAudio;

    // ============================================================
    // CONTACT
    // ============================================================

    private static final int PICK_CONTACT_REQUEST = 1001;

    // ============================================================
    // TEXT VIEWS
    // ============================================================

    private TextView tvCallerName;
    private TextView tvCallStatus;
    private TextView tvCallType;
    private TextView tvDialNumber;

    // ============================================================
    // CALLER IMAGE
    // ============================================================

    private ImageView imgCallerProfile;

    // ============================================================
    // SWIPE
    // ============================================================

    private RelativeLayout containerSwipeTrack;
    private CardView cardSwipeKnob;

    // ============================================================
    // LAYOUTS
    // ============================================================

    private LinearLayout incomingActions;
    private LinearLayout inCallControls;
    private LinearLayout keypadContainer;

    // ============================================================
    // END CALL BUTTON
    // ============================================================

    private FloatingActionButton fabEndCall;

    // ============================================================
    // CALL CONTROLS
    // ============================================================

    private LinearLayout btnMute;
    private LinearLayout btnKeypad;
    private LinearLayout btnSpeaker;
    private LinearLayout btnAddCall;
    private LinearLayout btnHold;
    private LinearLayout btnMore;

    // ============================================================
    // CONTROL TEXT
    // ============================================================

    private TextView tvMute;
    private TextView tvSpeaker;
    private TextView tvHold;

    // ============================================================
    // CALL STATE
    // ============================================================

    private boolean isCallActive = false;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isOnHold = false;
    private boolean isConference = false;

    // ============================================================
    // SWIPE VARIABLES
    // ============================================================

    private float downX;
    private float initialX;

    // ============================================================
    // TIMER
    // ============================================================

    private int secondsElapsed = 0;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable timerRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!isCallActive) {
                        return;
                    }

                    if (!isOnHold) {

                        secondsElapsed++;

                        int minutes =
                                secondsElapsed / 60;

                        int seconds =
                                secondsElapsed % 60;

                        tvCallStatus.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d",
                                        minutes,
                                        seconds
                                )
                        );
                    }

                    handler.postDelayed(
                            this,
                            1000
                    );
                }
            };

    // ============================================================
    // CALLERS
    // ============================================================

    private final String[] callers = {
            "Amma",
            "Appa",
            "Priya",
            "Rahul",
            "Ananya",
            "Sneha",
            "Kavya",
            "Rohan"
    };

    private final int[] callerIcons = {
            android.R.drawable.ic_menu_myplaces,
            android.R.drawable.ic_menu_call,
            android.R.drawable.ic_menu_gallery,
            android.R.drawable.ic_menu_camera
    };

    private String selectedCaller = "Amma";


    // ============================================================
    // ON CREATE
    // ============================================================

    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // ============================================================
        // SHOW OVER LOCK SCREEN & TURN ON DISPLAY
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

        setContentView(
                R.layout.activity_fake_call
        );

        // ============================================================
        // FORCE LOUDSPEAKER & MAX VOLUME
        // ============================================================
        forceMobileSpeakerOutput();

        initializeViews();

        setupCaller();

        setupSwipeGesture();

        setupControls();

        setupKeypad();

        startRingtone();
    }

    // ============================================================
// FORCE AUDIO TO PHONE LOUDSPEAKER AT MAX VOLUME
// ============================================================

    private void forceMobileSpeakerOutput() {
        try {
            AudioManager audioManager =
                    (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {

                // 1. Set all relevant audio streams to MAXIMUM volume
                int maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0);

                int maxVoice = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVoice, 0);

                int maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0);

                // 2. Force routing to Built-In Loudspeaker (bypasses Bluetooth/Pods)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                    for (android.media.AudioDeviceInfo device : audioManager.getAvailableCommunicationDevices()) {
                        if (device.getType() == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                            audioManager.setCommunicationDevice(device);
                            break;
                        }
                    }
                } else {
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    audioManager.setSpeakerphoneOn(true);
                }
            }

        } catch (Exception e) {
            Log.e("FakeCallActivity", "Unable to force mobile speaker output", e);
        }
    }


// ============================================================
// RESET AUDIO ROUTING WHEN CALL ENDS
// ============================================================

    private void resetAudioRouting() {
        try {
            AudioManager audioManager =
                    (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioManager.clearCommunicationDevice();
                } else {
                    audioManager.setSpeakerphoneOn(false);
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                }
            }
        } catch (Exception e) {
            Log.e("FakeCallActivity", "Unable to reset audio routing", e);
        }
    }


    // ============================================================
    // INITIALIZE VIEWS
    // ============================================================

    private void initializeViews() {

        tvCallerName =
                findViewById(R.id.tvCallerName);

        tvCallStatus =
                findViewById(R.id.tvCallStatus);

        tvCallType =
                findViewById(R.id.tvCallType);

        tvDialNumber =
                findViewById(R.id.tvDialNumber);

        imgCallerProfile =
                findViewById(R.id.imgCallerProfile);

        containerSwipeTrack =
                findViewById(R.id.containerSwipeTrack);

        cardSwipeKnob =
                findViewById(R.id.cardSwipeKnob);

        incomingActions =
                findViewById(R.id.incomingActions);

        inCallControls =
                findViewById(R.id.inCallControls);

        keypadContainer =
                findViewById(R.id.keypadContainer);

        fabEndCall =
                findViewById(R.id.fabEndCall);

        btnMute =
                findViewById(R.id.btnMute);

        btnKeypad =
                findViewById(R.id.btnKeypad);

        btnSpeaker =
                findViewById(R.id.btnSpeaker);

        btnAddCall =
                findViewById(R.id.btnAddCall);

        btnHold =
                findViewById(R.id.btnHold);

        btnMore =
                findViewById(R.id.btnMore);

        tvMute =
                findViewById(R.id.tvMute);

        tvSpeaker =
                findViewById(R.id.tvSpeaker);

        tvHold =
                findViewById(R.id.tvHold);
    }


    // ============================================================
    // RANDOM CALLER
    // ============================================================

    private void setupCaller() {

        Random random =
                new Random();

        selectedCaller =
                callers[
                        random.nextInt(
                                callers.length
                        )
                        ];

        tvCallerName.setText(
                selectedCaller
        );

        imgCallerProfile.setImageResource(
                callerIcons[
                        random.nextInt(
                                callerIcons.length
                        )
                        ]
        );

        tvCallType.setText(
                "INCOMING CALL"
        );

        tvCallStatus.setText(
                "Mobile +91 98765 43210"
        );

        if (inCallControls != null) {

            inCallControls.setVisibility(
                    View.GONE
            );
        }

        if (keypadContainer != null) {

            keypadContainer.setVisibility(
                    View.GONE
            );
        }
    }


    // ============================================================
    // SWIPE GESTURE
    // ============================================================

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeGesture() {

        if (cardSwipeKnob == null ||
                containerSwipeTrack == null) {

            return;
        }

        cardSwipeKnob.setOnTouchListener(
                (view, event) -> {

                    if (isCallActive) {

                        return false;
                    }

                    switch (event.getAction()) {

                        case MotionEvent.ACTION_DOWN:

                            downX =
                                    event.getRawX();

                            initialX =
                                    view.getX();

                            return true;


                        case MotionEvent.ACTION_MOVE:

                            float delta =
                                    event.getRawX()
                                            - downX;

                            float newX =
                                    initialX + delta;

                            float minX = 4;

                            float maxX =
                                    containerSwipeTrack
                                            .getWidth()
                                            - view.getWidth()
                                            - 4;

                            if (newX < minX) {

                                newX = minX;
                            }

                            if (newX > maxX) {

                                newX = maxX;
                            }

                            view.setX(newX);

                            if (newX >
                                    maxX - 20) {

                                answerCall();

                            } else if (newX <
                                    minX + 20) {

                                endCall();
                            }

                            return true;


                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:

                            if (!isCallActive) {

                                view.animate()
                                        .x(initialX)
                                        .setDuration(200)
                                        .start();
                            }

                            return true;
                    }

                    return false;
                }
        );
    }


    // ============================================================
    // ANSWER CALL
    // ============================================================

    private void answerCall() {

        if (isCallActive) {

            return;
        }

        isCallActive = true;

        stopRingtone();

        containerSwipeTrack.setVisibility(
                View.GONE
        );

        if (incomingActions != null) {

            incomingActions.setVisibility(
                    View.GONE
            );
        }

        inCallControls.setVisibility(
                View.VISIBLE
        );

        tvCallType.setText(
                "CALLING..."
        );

        tvCallStatus.setText(
                "Connecting"
        );

        handler.postDelayed(
                () -> {

                    if (!isCallActive) {

                        return;
                    }

                    tvCallType.setText(
                            "CONNECTED"
                    );

                    secondsElapsed = 0;

                    tvCallStatus.setText(
                            "00:00"
                    );

                    handler.removeCallbacks(
                            timerRunnable
                    );

                    handler.postDelayed(
                            timerRunnable,
                            1000
                    );

                },
                1200
        );
    }


    // ============================================================
    // CONTROLS
    // ============================================================

    private void setupControls() {

        fabEndCall.setOnClickListener(
                v -> endCall()
        );

        btnMute.setOnClickListener(
                v -> toggleMute()
        );

        btnSpeaker.setOnClickListener(
                v -> toggleSpeaker()
        );

        btnKeypad.setOnClickListener(
                v -> toggleKeypad()
        );

        btnAddCall.setOnClickListener(
                v -> openContactPicker()
        );

        btnHold.setOnClickListener(
                v -> toggleHold()
        );

        btnMore.setOnClickListener(
                v -> {

                    Toast.makeText(
                            this,
                            "Contacts  •  Bluetooth  •  Record",
                            Toast.LENGTH_SHORT
                    ).show();

                }
        );
    }


    // ============================================================
    // CONTACT SELECTED
    // ============================================================

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode ==
                PICK_CONTACT_REQUEST &&
                resultCode ==
                        RESULT_OK &&
                data != null) {

            Uri contactUri =
                    data.getData();

            if (contactUri == null) {

                return;
            }

            Cursor cursor =
                    getContentResolver().query(
                            contactUri,
                            null,
                            null,
                            null,
                            null
                    );

            if (cursor != null) {

                if (cursor.moveToFirst()) {

                    int nameIndex =
                            cursor.getColumnIndex(
                                    ContactsContract
                                            .CommonDataKinds
                                            .Phone
                                            .DISPLAY_NAME
                            );

                    int numberIndex =
                            cursor.getColumnIndex(
                                    ContactsContract
                                            .CommonDataKinds
                                            .Phone
                                            .NUMBER
                            );

                    String contactName =
                            nameIndex >= 0
                                    ? cursor.getString(
                                    nameIndex
                            )
                                    : "Unknown";

                    String contactNumber =
                            numberIndex >= 0
                                    ? cursor.getString(
                                    numberIndex
                            )
                                    : "";

                    startConference(
                            contactName,
                            contactNumber
                    );
                }

                cursor.close();
            }
        }
    }


    // ============================================================
    // START CONFERENCE
    // ============================================================

    private void startConference(
            String contactName,
            String contactNumber
    ) {

        if (!isCallActive) {

            return;
        }

        isConference = true;

        tvCallType.setText(
                "CONFERENCE"
        );

        tvCallType.setTextSize(
                32
        );

        tvCallType.setTextColor(
                Color.BLACK
        );

        tvCallType.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        tvCallerName.setText(
                selectedCaller
                        + " + "
                        + contactName
        );

        tvCallerName.setTextSize(
                16
        );

        tvCallerName.setTextColor(
                Color.BLACK
        );

        tvCallerName.setTypeface(
                null,
                android.graphics.Typeface.NORMAL
        );

        tvCallStatus.setTextSize(
                14
        );

        tvCallStatus.setTextColor(
                Color.BLACK
        );

        Toast.makeText(
                this,
                contactName + " added to call",
                Toast.LENGTH_SHORT
        ).show();
    }


    // ============================================================
    // OPEN CONTACTS
    // ============================================================

    private void openContactPicker() {

        Intent intent =
                new Intent(
                        Intent.ACTION_PICK,
                        ContactsContract
                                .CommonDataKinds
                                .Phone
                                .CONTENT_URI
                );

        startActivityForResult(
                intent,
                PICK_CONTACT_REQUEST
        );
    }


    // ============================================================
    // MUTE
    // ============================================================

    private void toggleMute() {

        isMuted = !isMuted;

        if (isMuted) {

            tvMute.setText(
                    "Muted"
            );

            btnMute.setAlpha(
                    0.55f
            );

        } else {

            tvMute.setText(
                    "Mute"
            );

            btnMute.setAlpha(
                    1f
            );
        }
    }


    // ============================================================
    // SPEAKER
    // ============================================================

    private void toggleSpeaker() {

        isSpeakerOn = !isSpeakerOn;

        AudioManager audioManager =
                (AudioManager)
                        getSystemService(
                                Context.AUDIO_SERVICE
                        );

        if (audioManager != null) {

            audioManager.setMode(
                    AudioManager.MODE_IN_COMMUNICATION
            );

            audioManager.setSpeakerphoneOn(
                    isSpeakerOn
            );
        }

        if (isSpeakerOn) {

            tvSpeaker.setText(
                    "Speaker On"
            );

            btnSpeaker.setAlpha(
                    0.55f
            );

            // ====================================================
            // PLAY MALE VOICE FROM VIDEO
            // ====================================================

            playFakeCallAudio();

        } else {

            tvSpeaker.setText(
                    "Speaker"
            );

            btnSpeaker.setAlpha(
                    1f
            );

            // ====================================================
            // STOP MALE VOICE
            // ====================================================

            stopFakeCallAudio();
        }
    }


    // ============================================================
    // PLAY FAKE CALL VIDEO AUDIO
    // ============================================================

    private void playFakeCallAudio() {

        try {

            // Prevent multiple MediaPlayers
            if (fakeCallAudio != null) {

                if (fakeCallAudio.isPlaying()) {
                    return;
                }

                fakeCallAudio.release();
                fakeCallAudio = null;
            }


            // ====================================================
            // FORCE LOUDSPEAKER & MAX VOLUME
            // ====================================================
            forceMobileSpeakerOutput();


            // ====================================================
            // CREATE PLAYER FROM res/raw/fake_call.mp4
            // ====================================================
            fakeCallAudio =
                    MediaPlayer.create(
                            this,
                            R.raw.fake_call
                    );


            if (fakeCallAudio == null) {

                Toast.makeText(
                        this,
                        "Unable to play fake call audio.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // ====================================================
            // AUDIO SETTINGS
            // ====================================================
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                fakeCallAudio.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes.USAGE_VOICE_COMMUNICATION
                                )
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SPEECH
                                )
                                .build()
                );
            }


            // ====================================================
            // DIRECT OUTPUT TO INTERNAL SPEAKER (API 23+)
            // ====================================================
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                AudioManager audioManager =
                        (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (audioManager != null) {

                    for (android.media.AudioDeviceInfo device :
                            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {

                        if (device.getType() == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {

                            fakeCallAudio.setPreferredDevice(device);
                            break;
                        }
                    }
                }
            }


            // ====================================================
            // PLAY ONLY ONCE
            // ====================================================
            fakeCallAudio.setLooping(
                    false
            );


            // ====================================================
            // RELEASE AFTER AUDIO FINISHES
            // ====================================================
            fakeCallAudio.setOnCompletionListener(
                    mp -> {

                        mp.release();

                        fakeCallAudio = null;
                    }
            );


            // ====================================================
            // START
            // ====================================================
            fakeCallAudio.start();


            Log.d(
                    "FakeCallActivity",
                    "Fake call video audio started"
            );

        } catch (Exception e) {

            Log.e(
                    "FakeCallActivity",
                    "Unable to play fake call audio",
                    e
            );

            stopFakeCallAudio();
        }
    }


    // ============================================================
    // STOP FAKE CALL AUDIO
    // ============================================================

    private void stopFakeCallAudio() {

        try {

            if (fakeCallAudio != null) {

                if (fakeCallAudio.isPlaying()) {

                    fakeCallAudio.stop();
                }

                fakeCallAudio.release();

                fakeCallAudio = null;
            }

        } catch (Exception e) {

            fakeCallAudio = null;
        }
    }


    // ============================================================
    // HOLD
    // ============================================================

    private void toggleHold() {

        isOnHold = !isOnHold;

        if (isOnHold) {

            tvCallType.setText(
                    "ON HOLD"
            );

            tvHold.setText(
                    "Resume"
            );

            btnHold.setAlpha(
                    0.55f
            );

        } else {

            tvCallType.setText(
                    isConference
                            ? "CONFERENCE"
                            : "CONNECTED"
            );

            tvHold.setText(
                    "Hold"
            );

            btnHold.setAlpha(
                    1f
            );
        }
    }


    // ============================================================
    // CONFERENCE
    // ============================================================

    private void startConference() {

        if (!isCallActive) {

            return;
        }

        isConference = true;

        tvCallerName.setText(
                "CONF CALL"
        );

        tvCallType.setText(
                "CONFERENCE"
        );

        Toast.makeText(
                this,
                "Call added to conference",
                Toast.LENGTH_SHORT
        ).show();
    }


    // ============================================================
    // KEYPAD
    // ============================================================

    private void setupKeypad() {

        setupKey(R.id.key1, "1");
        setupKey(R.id.key2, "2");
        setupKey(R.id.key3, "3");

        setupKey(R.id.key4, "4");
        setupKey(R.id.key5, "5");
        setupKey(R.id.key6, "6");

        setupKey(R.id.key7, "7");
        setupKey(R.id.key8, "8");
        setupKey(R.id.key9, "9");

        setupKey(R.id.keyStar, "*");
        setupKey(R.id.key0, "0");
        setupKey(R.id.keyHash, "#");
    }


    private void setupKey(
            int id,
            String number
    ) {

        View key =
                findViewById(id);

        if (key == null) {

            return;
        }

        key.setOnClickListener(
                v -> {

                    String current =
                            tvDialNumber
                                    .getText()
                                    .toString();

                    tvDialNumber.setText(
                            current + number
                    );
                }
        );
    }


    private void toggleKeypad() {

        if (keypadContainer == null) {

            return;
        }

        if (keypadContainer.getVisibility()
                == View.VISIBLE) {

            keypadContainer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(
                            () -> keypadContainer
                                    .setVisibility(
                                            View.GONE
                                    )
                    )
                    .start();

        } else {

            keypadContainer.setAlpha(
                    0f
            );

            keypadContainer.setVisibility(
                    View.VISIBLE
            );

            keypadContainer.animate()
                    .alpha(1f)
                    .setDuration(180)
                    .start();
        }
    }


    // ============================================================
    // END CALL
    // ============================================================

    private void endCall() {

        stopRingtone();

        // ========================================================
        // STOP FAKE CALL VIDEO AUDIO
        // ========================================================

        stopFakeCallAudio();

        isCallActive = false;

        handler.removeCallbacks(
                timerRunnable
        );

        if (keypadContainer != null) {

            keypadContainer.setVisibility(
                    View.GONE
            );
        }

        tvCallType.setText(
                "CALL ENDED"
        );

        tvCallStatus.setText(
                "Call Ended"
        );

        handler.postDelayed(
                this::finish,
                800
        );
    }


    // ============================================================
    // RINGTONE
    // ============================================================

    private void startRingtone() {

        try {

            Uri uri =
                    RingtoneManager
                            .getDefaultUri(
                                    RingtoneManager
                                            .TYPE_RINGTONE
                            );

            if (uri == null) {

                uri =
                        RingtoneManager
                                .getDefaultUri(
                                        RingtoneManager
                                                .TYPE_NOTIFICATION
                                );
            }

            ringtone =
                    RingtoneManager
                            .getRingtone(
                                    getApplicationContext(),
                                    uri
                            );

            if (ringtone != null) {

                ringtone.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(
                                        AudioAttributes
                                                .USAGE_NOTIFICATION_RINGTONE
                                )
                                .build()
                );

                if (Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.P) {

                    ringtone.setLooping(
                            true
                    );
                }

                ringtone.play();
            }

        } catch (Exception ignored) {
        }
    }


    private void stopRingtone() {

        try {

            if (ringtone != null &&
                    ringtone.isPlaying()) {

                ringtone.stop();
            }

        } catch (Exception ignored) {
        }
    }


    // ============================================================
    // ACTIVITY DESTROY
    // ============================================================

    @Override
    protected void onDestroy() {

        stopRingtone();

        // ========================================================
        // RELEASE VIDEO AUDIO
        // ========================================================

        stopFakeCallAudio();

        // ========================================================
        // RESET AUDIO ROUTING (RESTORE BLUETOOTH / NORMAL MODE)
        // ========================================================

        resetAudioRouting();

        handler.removeCallbacksAndMessages(
                null
        );

        super.onDestroy();
    }
}