package com.sheshield.app;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseHelper {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Callback interface for asynchronous operations
    public interface DataCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    // Register User with Email & Password
    public void registerUser(String email, String password, String name, String phone, DataCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        saveUserProfile(uid, name, email, phone, callback);
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Registration failed.");
                    }
                });
    }

    // Save User Profile in Firestore
    private void saveUserProfile(String uid, String name, String email, String phone, DataCallback callback) {
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> callback.onSuccess("User created successfully."))
                .addOnFailureListener(e -> callback.onFailure("Firestore Error: " + e.getMessage()));
    }

    // Login User
    public void loginUser(String email, String password, DataCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess("Login successful.");
                    } else {
                        callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Login failed.");
                    }
                });
    }
}