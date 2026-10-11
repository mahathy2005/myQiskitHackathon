package com.sheshield.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etMobile;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private TextView tvGoToSignUp;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etMobile = findViewById(R.id.etLoginMobile);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);

        // Auto-login check if session already exists
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
            return;
        }

        if (tvGoToSignUp != null) {
            tvGoToSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            });
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                if (validateInputs()) {
                    performLogin();
                }
            });
        }
    }

    private boolean validateInputs() {
        String mobile = etMobile != null && etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(mobile)) {
            etMobile.setError("Mobile number required");
            etMobile.requestFocus();
            return false;
        }

        if (!mobile.matches("^[6-9]\\d{9}$")) {
            etMobile.setError("Enter a valid 10-digit mobile number");
            etMobile.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void performLogin() {
        String mobile = etMobile.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        String firebaseEmail = mobile + "@sheshield.app";

        if (btnLogin != null) {
            btnLogin.setEnabled(false);
        }

        mAuth.signInWithEmailAndPassword(firebaseEmail, password)
                .addOnCompleteListener(this, task -> {
                    if (btnLogin != null) {
                        btnLogin.setEnabled(true);
                    }

                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Invalid credentials";
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}