package com.sheshield.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class PasswordSetupActivity extends AppCompatActivity {

    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnFinalSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_password_setup);

        etPassword =
                findViewById(R.id.etPassword);

        etConfirmPassword =
                findViewById(R.id.etConfirmPassword);

        btnFinalSignUp =
                findViewById(R.id.btnFinalSignUp);

        if (btnFinalSignUp != null) {

            btnFinalSignUp.setOnClickListener(v -> {

                if (validatePasswords()) {

                    openOtpScreen();
                }
            });
        }
    }

    // ============================================================
    // PASSWORD VALIDATION
    // ============================================================

    private boolean validatePasswords() {

        String password =
                etPassword != null
                        && etPassword.getText() != null
                        ? etPassword.getText()
                        .toString()
                        .trim()
                        : "";

        String confirmPassword =
                etConfirmPassword != null
                        && etConfirmPassword.getText() != null
                        ? etConfirmPassword.getText()
                        .toString()
                        .trim()
                        : "";

        if (TextUtils.isEmpty(password)) {

            etPassword.setError(
                    "Password is required"
            );

            etPassword.requestFocus();

            return false;
        }

        if (password.length() < 6) {

            etPassword.setError(
                    "Password must be at least 6 characters"
            );

            etPassword.requestFocus();

            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {

            etConfirmPassword.setError(
                    "Please confirm your password"
            );

            etConfirmPassword.requestFocus();

            return false;
        }

        if (!password.equals(confirmPassword)) {

            etConfirmPassword.setError(
                    "Passwords do not match"
            );

            etConfirmPassword.requestFocus();

            return false;
        }

        return true;
    }

    // ============================================================
    // OPEN OTP SCREEN
    // ============================================================

    private void openOtpScreen() {

        Intent oldIntent =
                getIntent();

        String password =
                etPassword.getText()
                        .toString()
                        .trim();

        Intent intent =
                new Intent(
                        PasswordSetupActivity.this,
                        OtpAuthActivity.class
                );

        // ========================================================
        // PERSONAL DETAILS
        // ========================================================

        intent.putExtra(
                "fullName",
                oldIntent.getStringExtra("fullName")
        );

        intent.putExtra(
                "dob",
                oldIntent.getStringExtra("dob")
        );

        intent.putExtra(
                "age",
                oldIntent.getStringExtra("age")
        );

        intent.putExtra(
                "bloodGroup",
                oldIntent.getStringExtra("bloodGroup")
        );

        intent.putExtra(
                "email",
                oldIntent.getStringExtra("email")
        );

        intent.putExtra(
                "userMobile",
                oldIntent.getStringExtra("userMobile")
        );

        intent.putExtra(
                "address",
                oldIntent.getStringExtra("address")
        );

        intent.putExtra(
                "about",
                oldIntent.getStringExtra("about")
        );

        // ========================================================
        // GUARDIAN DETAILS
        // ========================================================

        intent.putExtra(
                "guardianName",
                oldIntent.getStringExtra("guardianName")
        );

        intent.putExtra(
                "guardianRelation",
                oldIntent.getStringExtra("guardianRelation")
        );

        intent.putExtra(
                "guardianMobile",
                oldIntent.getStringExtra("guardianMobile")
        );

        intent.putExtra(
                "guardianBloodGroup",
                oldIntent.getStringExtra("guardianBloodGroup")
        );

        intent.putExtra(
                "guardianEmail",
                oldIntent.getStringExtra("guardianEmail")
        );

        intent.putExtra(
                "guardianAddress",
                oldIntent.getStringExtra("guardianAddress")
        );

        // ========================================================
        // PASSWORD
        // ========================================================

        intent.putExtra(
                "password",
                password
        );

        startActivity(intent);

        finish();
    }
}