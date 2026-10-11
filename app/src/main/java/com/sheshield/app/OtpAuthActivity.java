package com.sheshield.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OtpAuthActivity extends AppCompatActivity {

    // ============================================================
    // VIEWS
    // ============================================================

    private TextInputEditText etMobile;
    private TextInputEditText etOtp;

    private MaterialButton btnSendOtp;
    private MaterialButton btnVerifyOtp;

    private LinearLayout layoutOtpContainer;


    // ============================================================
    // FIREBASE
    // ============================================================

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    // ============================================================
    // OTP
    // ============================================================

    private String verificationId;


    // ============================================================
    // USER SIGNUP DATA
    // ============================================================

    private String fullName;
    private String dob;
    private String age;
    private String bloodGroup;
    private String email;
    private String userMobile;
    private String address;
    private String about;


    // ============================================================
    // GUARDIAN DATA
    // ============================================================

    private String guardianName;
    private String guardianRelation;
    private String guardianMobile;
    private String guardianBloodGroup;
    private String guardianEmail;
    private String guardianAddress;


    // ============================================================
    // PREVENT DOUBLE VERIFICATION / PROFILE SAVING
    // ============================================================

    private boolean profileSaving = false;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_otp_auth);


        // ========================================================
        // FIREBASE
        // ========================================================

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();


        // ========================================================
        // GET USER DETAILS FROM SIGNUP
        // ========================================================

        Intent intent = getIntent();

        fullName =
                intent.getStringExtra("fullName");

        dob =
                intent.getStringExtra("dob");

        age =
                intent.getStringExtra("age");

        bloodGroup =
                intent.getStringExtra("bloodGroup");

        email =
                intent.getStringExtra("email");

        userMobile =
                intent.getStringExtra("userMobile");

        address =
                intent.getStringExtra("address");

        about =
                intent.getStringExtra("about");


        // ========================================================
        // GET GUARDIAN DETAILS
        // ========================================================

        guardianName =
                intent.getStringExtra("guardianName");

        guardianRelation =
                intent.getStringExtra("guardianRelation");

        guardianMobile =
                intent.getStringExtra("guardianMobile");

        guardianBloodGroup =
                intent.getStringExtra("guardianBloodGroup");

        guardianEmail =
                intent.getStringExtra("guardianEmail");

        guardianAddress =
                intent.getStringExtra("guardianAddress");


        // ========================================================
        // INITIALIZE VIEWS
        // ========================================================

        etMobile =
                findViewById(R.id.etMobileNumber);

        etOtp =
                findViewById(R.id.etOtpCode);

        btnSendOtp =
                findViewById(R.id.btnSendOtp);

        btnVerifyOtp =
                findViewById(R.id.btnVerifyOtp);

        layoutOtpContainer =
                findViewById(R.id.layoutOtpContainer);


        // ========================================================
        // CHECK USER MOBILE
        // ========================================================

        if (userMobile == null ||
                userMobile.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "User mobile number is missing",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }


        // ========================================================
        // SHOW USER MOBILE
        // ========================================================

        if (etMobile != null) {

            etMobile.setText(userMobile);

            // User cannot change the mobile number here.
            etMobile.setEnabled(false);
        }


        // ========================================================
        // SEND OTP BUTTON
        // ========================================================

        if (btnSendOtp != null) {

            btnSendOtp.setOnClickListener(
                    v -> sendOtpToUserMobile()
            );
        }


        // ========================================================
        // VERIFY OTP BUTTON
        // ========================================================

        if (btnVerifyOtp != null) {

            btnVerifyOtp.setOnClickListener(
                    v -> verifyOtp()
            );
        }


        // ========================================================
        // AUTOMATIC VERIFICATION WHEN 6 DIGITS ARE ENTERED
        // ========================================================

        if (etOtp != null) {

            etOtp.addTextChangedListener(
                    new TextWatcher() {

                        @Override
                        public void beforeTextChanged(
                                CharSequence s,
                                int start,
                                int count,
                                int after
                        ) {
                        }


                        @Override
                        public void onTextChanged(
                                CharSequence s,
                                int start,
                                int before,
                                int count
                        ) {

                            if (s != null
                                    && s.length() == 6
                                    && verificationId != null
                                    && !profileSaving) {

                                verifyOtp();
                            }
                        }


                        @Override
                        public void afterTextChanged(
                                Editable s
                        ) {
                        }
                    }
            );
        }


        // ========================================================
        // AUTOMATICALLY SEND OTP
        // ========================================================

        sendOtpToUserMobile();
    }


    // ============================================================
    // SEND OTP ONLY TO USER MOBILE
    // ============================================================

    private void sendOtpToUserMobile() {

        String phone =
                userMobile != null
                        ? userMobile.trim()
                        : "";


        // ========================================================
        // CHECK USER MOBILE
        // ========================================================

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    "User mobile number is missing",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        // ========================================================
        // ADD INDIA COUNTRY CODE
        // ========================================================

        if (!phone.startsWith("+")) {

            phone = "+91" + phone;
        }


        // ========================================================
        // DISABLE SEND BUTTON
        // ========================================================

        if (btnSendOtp != null) {

            btnSendOtp.setEnabled(false);
        }


        // ========================================================
        // FIREBASE PHONE AUTH
        // ========================================================
        //
        // IMPORTANT:
        // ONLY USER MOBILE IS USED FOR OTP.
        //
        // guardianMobile is NEVER used here.
        //
        // ========================================================

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)

                        .setPhoneNumber(phone)

                        .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                        )

                        .setActivity(this)

                        .setCallbacks(
                                mCallbacks
                        )

                        .build();


        PhoneAuthProvider.verifyPhoneNumber(
                options
        );
    }


    // ============================================================
    // FIREBASE OTP CALLBACKS
    // ============================================================

    private final PhoneAuthProvider
            .OnVerificationStateChangedCallbacks mCallbacks =

            new PhoneAuthProvider
                    .OnVerificationStateChangedCallbacks() {


                // =================================================
                // AUTOMATIC VERIFICATION
                // =================================================

                @Override
                public void onVerificationCompleted(
                        @NonNull PhoneAuthCredential credential
                ) {

                    signInWithPhoneCredential(
                            credential
                    );
                }


                // =================================================
                // VERIFICATION FAILED
                // =================================================

                @Override
                public void onVerificationFailed(
                        @NonNull FirebaseException e
                ) {

                    enableButtons();

                    String errorCode = "UNKNOWN";

                    if (e instanceof FirebaseAuthException) {

                        errorCode =
                                ((FirebaseAuthException) e)
                                        .getErrorCode();
                    }


                    String errorMessage =
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "No error message";


                    String fullError =
                            "OTP FAILED\n\n"
                                    + "CODE: "
                                    + errorCode
                                    + "\n\n"
                                    + "MESSAGE:\n"
                                    + errorMessage;


                    Toast.makeText(
                            OtpAuthActivity.this,
                            fullError,
                            Toast.LENGTH_LONG
                    ).show();


                    Log.e(
                            "SHE_SHIELD_OTP",
                            fullError,
                            e
                    );
                }


                // =================================================
                // OTP SENT
                // =================================================

                @Override
                public void onCodeSent(
                        @NonNull String sVerificationId,
                        @NonNull PhoneAuthProvider
                                .ForceResendingToken token
                ) {

                    verificationId =
                            sVerificationId;


                    if (btnSendOtp != null) {

                        btnSendOtp.setEnabled(true);
                    }


                    // Show OTP section

                    if (layoutOtpContainer != null) {

                        layoutOtpContainer.setVisibility(
                                View.VISIBLE
                        );
                    }


                    if (etOtp != null) {

                        etOtp.requestFocus();
                    }


                    Toast.makeText(
                            OtpAuthActivity.this,
                            "OTP sent to your mobile number",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            };


    // ============================================================
    // VERIFY OTP
    // ============================================================

    private void verifyOtp() {

        if (profileSaving) {

            return;
        }


        if (etOtp == null) {

            return;
        }


        String code =
                etOtp.getText() != null
                        ? etOtp.getText()
                        .toString()
                        .trim()
                        : "";


        // ========================================================
        // CHECK OTP
        // ========================================================

        if (code.isEmpty()
                || code.length() != 6) {

            etOtp.setError(
                    "Enter 6-digit OTP"
            );

            return;
        }


        // ========================================================
        // CHECK VERIFICATION ID
        // ========================================================

        if (verificationId == null
                || verificationId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please request OTP first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // ========================================================
        // CREATE PHONE CREDENTIAL
        // ========================================================

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(
                        verificationId,
                        code
                );


        // ========================================================
        // AUTHENTICATE
        // ========================================================

        signInWithPhoneCredential(
                credential
        );
    }


    // ============================================================
    // FIREBASE PHONE AUTHENTICATION
    // ============================================================

    private void signInWithPhoneCredential(
            PhoneAuthCredential credential
    ) {

        if (profileSaving) {

            return;
        }


        profileSaving = true;


        // ========================================================
        // DISABLE BUTTONS
        // ========================================================

        if (btnVerifyOtp != null) {

            btnVerifyOtp.setEnabled(false);
        }

        if (btnSendOtp != null) {

            btnSendOtp.setEnabled(false);
        }


        // ========================================================
        // SIGN IN USING VERIFIED PHONE
        // ========================================================

        mAuth.signInWithCredential(
                        credential
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                FirebaseUser user =
                                        mAuth.getCurrentUser();


                                if (user != null) {

                                    saveUserToFirestore(
                                            user.getUid()
                                    );

                                } else {

                                    profileSaving = false;

                                    enableButtons();

                                    Toast.makeText(
                                            OtpAuthActivity.this,
                                            "Authentication failed",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }

                            } else {

                                profileSaving = false;

                                enableButtons();


                                String error =
                                        task.getException() != null
                                                ? task.getException()
                                                .getMessage()
                                                : "Invalid OTP";


                                Toast.makeText(
                                        OtpAuthActivity.this,
                                        "Invalid OTP:\n"
                                                + error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                );
    }


    // ============================================================
    // SAVE USER PROFILE TO FIRESTORE
    // ============================================================

    private void saveUserToFirestore(
            String uid
    ) {

        // ========================================================
        // PREPARE USER DATA
        // ========================================================

        Map<String, Object> userData =
                new HashMap<>();


        // ========================================================
        // AUTH DATA
        // ========================================================

        userData.put(
                "uid",
                uid
        );


        userData.put(
                "phone",
                userMobile != null
                        ? userMobile.trim()
                        : ""
        );


        // ========================================================
        // PERSONAL DETAILS
        // ========================================================

        userData.put(
                "fullName",
                fullName != null
                        ? fullName.trim()
                        : ""
        );


        userData.put(
                "dob",
                dob != null
                        ? dob.trim()
                        : ""
        );


        userData.put(
                "age",
                age != null
                        ? age.trim()
                        : ""
        );


        userData.put(
                "bloodGroup",
                bloodGroup != null
                        ? bloodGroup.trim()
                        : ""
        );


        userData.put(
                "email",
                email != null
                        ? email.trim()
                        : ""
        );


        userData.put(
                "userMobile",
                userMobile != null
                        ? userMobile.trim()
                        : ""
        );


        userData.put(
                "address",
                address != null
                        ? address.trim()
                        : ""
        );


        userData.put(
                "about",
                about != null
                        ? about.trim()
                        : ""
        );


        // ========================================================
        // CREATED TIME
        // ========================================================

        userData.put(
                "createdAt",
                System.currentTimeMillis()
        );


        // ========================================================
        // SAVE MAIN USER DOCUMENT
        // ========================================================

        db.collection("users")
                .document(uid)
                .set(userData)

                .addOnSuccessListener(
                        unused -> {

                            // =================================================
                            // USER PROFILE CREATED
                            // =================================================

                            if (guardianMobile != null
                                    && !guardianMobile.trim().isEmpty()) {

                                addGuardianToSubcollection(
                                        uid
                                );

                            } else {

                                // No guardian provided
                                navigateToMain();
                            }
                        }
                )

                .addOnFailureListener(
                        e -> {

                            profileSaving = false;

                            enableButtons();


                            Toast.makeText(
                                    OtpAuthActivity.this,
                                    "Error saving user profile:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // ============================================================
    // ADD GUARDIAN TO SUBCOLLECTION
    // ============================================================

    private void addGuardianToSubcollection(
            String uid
    ) {

        // ========================================================
        // GUARDIAN DATA
        // ========================================================

        Map<String, Object> guardianData =
                new HashMap<>();


        guardianData.put(
                "name",
                guardianName != null
                        ? guardianName.trim()
                        : ""
        );


        guardianData.put(
                "mobile",
                guardianMobile != null
                        ? guardianMobile.trim()
                        : ""
        );


        guardianData.put(
                "relation",
                guardianRelation != null
                        ? guardianRelation.trim()
                        : ""
        );


        guardianData.put(
                "bloodGroup",
                guardianBloodGroup != null
                        ? guardianBloodGroup.trim()
                        : ""
        );


        guardianData.put(
                "email",
                guardianEmail != null
                        ? guardianEmail.trim()
                        : ""
        );


        guardianData.put(
                "address",
                guardianAddress != null
                        ? guardianAddress.trim()
                        : ""
        );


        guardianData.put(
                "createdAt",
                System.currentTimeMillis()
        );


        // ========================================================
        // ADD GUARDIAN
        // ========================================================
        //
        // Firestore automatically creates a document ID.
        //
        // Path:
        //
        // users/{uid}/guardians/{guardianId}
        //
        // ========================================================

        db.collection("users")
                .document(uid)
                .collection("guardians")
                .add(guardianData)

                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    OtpAuthActivity.this,
                                    "Guardian added successfully",
                                    Toast.LENGTH_SHORT
                            ).show();


                            // =================================================
                            // OPEN MAIN ACTIVITY
                            // =================================================

                            navigateToMain();
                        }
                )

                .addOnFailureListener(
                        e -> {

                            profileSaving = false;

                            enableButtons();


                            Toast.makeText(
                                    OtpAuthActivity.this,
                                    "User created, but guardian could not be saved:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // ============================================================
    // NAVIGATE TO MAIN ACTIVITY
    // ============================================================

    private void navigateToMain() {

        // ========================================================
        // SAVE LOCAL SESSION
        // ========================================================

        SharedPreferences prefs =
                getSharedPreferences(
                        "UserSession",
                        MODE_PRIVATE
                );


        prefs.edit()

                .putString(
                        "USER_NAME",
                        fullName != null
                                ? fullName.trim()
                                : "User"
                )

                .putString(
                        "USER_MOBILE",
                        userMobile != null
                                ? userMobile.trim()
                                : ""
                )

                .putBoolean(
                        "IS_LOGGED_IN",
                        true
                )

                .apply();


        // ========================================================
        // SUCCESS MESSAGE
        // ========================================================

        Toast.makeText(
                OtpAuthActivity.this,
                "Sign Up Successful!",
                Toast.LENGTH_SHORT
        ).show();


        // ========================================================
        // OPEN MAIN ACTIVITY
        // ========================================================

        Intent intent =
                new Intent(
                        OtpAuthActivity.this,
                        MainActivity.class
                );


        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(intent);

        finish();
    }


    // ============================================================
    // ENABLE BUTTONS
    // ============================================================

    private void enableButtons() {

        if (btnVerifyOtp != null) {

            btnVerifyOtp.setEnabled(true);
        }


        if (btnSendOtp != null) {

            btnSendOtp.setEnabled(true);
        }
    }
}