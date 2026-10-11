package com.sheshield.app;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TrustedContactsActivity extends BaseActivity {

    // ============================================================
    // FIREBASE
    // ============================================================

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String userUid;

    // ============================================================
    // VIEWS
    // ============================================================

    private LinearLayout contactsContainer;

    // ============================================================
    // COLORS
    // ============================================================

    private static final int PINK =
            Color.parseColor("#E91E63");

    private static final int DARK_PINK =
            Color.parseColor("#C2185B");

    private static final int PURPLE =
            Color.parseColor("#8E24AA");

    private static final int DARK_TEXT =
            Color.parseColor("#292642");

    private static final int GREY_TEXT =
            Color.parseColor("#777487");

    private static final int LIGHT_PINK =
            Color.parseColor("#FCE4EC");

    private static final int VERY_LIGHT_PINK =
            Color.parseColor("#FFF7FA");

    private static final int BORDER =
            Color.parseColor("#F3D7E3");

    private static final int WHITE =
            Color.WHITE;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_trusted_contacts
        );

        // ========================================================
        // FIREBASE
        // ========================================================

        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        // ========================================================
        // CURRENT USER
        // ========================================================

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {

            showTranslatedToast(
                    "Please login first",
                    Toast.LENGTH_LONG
            );

            finish();
            return;
        }

        userUid =
                currentUser.getUid();

        // ========================================================
        // CONTAINER
        // ========================================================

        contactsContainer =
                findViewById(
                        R.id.contactsContainer
                );

        // ========================================================
        // BACK BUTTON
        // ========================================================

        View btnBack =
                findViewById(
                        R.id.btnBack
                );

        if (btnBack != null) {

            btnBack.setOnClickListener(
                    v -> finish()
            );
        }

        // ========================================================
        // ADD CONTACT
        // ========================================================

        MaterialButton btnAddContact =
                findViewById(
                        R.id.btnAddContact
                );

        if (btnAddContact != null) {

            btnAddContact.setOnClickListener(
                    v -> showAddContactDialog(null)
            );
        }

        // ========================================================
        // LOAD CONTACTS
        // ========================================================

        loadContactsFromFirestore();
    }


    // ============================================================
    // LOAD CONTACTS
    // ============================================================

    private void loadContactsFromFirestore() {

        if (contactsContainer == null) {
            return;
        }

        contactsContainer.removeAllViews();

        // ========================================================
        // LOADING VIEW
        // ========================================================

        TextView loading =
                new TextView(this);

        loading.setText(
                "Loading your trusted people..."
        );

        loading.setTextSize(14);

        loading.setTextColor(
                GREY_TEXT
        );

        loading.setGravity(
                Gravity.CENTER
        );

        loading.setPadding(
                dpToPx(20),
                dpToPx(50),
                dpToPx(20),
                dpToPx(50)
        );

        contactsContainer.addView(
                loading
        );

        // IMPORTANT:
        // This view was created dynamically, so BaseActivity
        // cannot translate it automatically.
        translateNewView(loading);

        // ========================================================
        // FIRESTORE
        // ========================================================

        db.collection("users")
                .document(userUid)
                .collection("guardians")
                .get()

                .addOnSuccessListener(
                        queryDocumentSnapshots -> {

                            contactsContainer.removeAllViews();

                            if (queryDocumentSnapshots.isEmpty()) {

                                showEmptyMessage();

                                return;
                            }

                            for (DocumentSnapshot document :
                                    queryDocumentSnapshots.getDocuments()) {

                                addContactCard(
                                        document
                                );
                            }
                        }
                )

                .addOnFailureListener(
                        e -> {

                            contactsContainer.removeAllViews();

                            showEmptyMessage();

                            showTranslatedToast(
                                    "Could not load trusted contacts:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            );
                        }
                );
    }


    // ============================================================
    // ADD / EDIT DIALOG
    // ============================================================

    private void showAddContactDialog(
            DocumentSnapshot existingDocument
    ) {

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_add_trusted_contact,
                        null
                );

        // ========================================================
        // FORM FIELDS
        // ========================================================

        EditText etGuardianName =
                dialogView.findViewById(
                        R.id.etGuardianName
                );

        AutoCompleteTextView etGuardianRelation =
                dialogView.findViewById(
                        R.id.etGuardianRelation
                );

        EditText etGuardianMobile =
                dialogView.findViewById(
                        R.id.etGuardianMobile
                );

        AutoCompleteTextView etGuardianBloodGroup =
                dialogView.findViewById(
                        R.id.etGuardianBloodGroup
                );

        EditText etGuardianEmail =
                dialogView.findViewById(
                        R.id.etGuardianEmail
                );

        EditText etGuardianAddress =
                dialogView.findViewById(
                        R.id.etGuardianAddress
                );

        MaterialButton btnCancel =
                dialogView.findViewById(
                        R.id.btnCancel
                );

        MaterialButton btnSave =
                dialogView.findViewById(
                        R.id.btnSave
                );

        // ========================================================
        // SAFETY CHECK
        // ========================================================

        if (etGuardianName == null ||
                etGuardianRelation == null ||
                etGuardianMobile == null ||
                etGuardianBloodGroup == null ||
                etGuardianEmail == null ||
                etGuardianAddress == null ||
                btnCancel == null ||
                btnSave == null) {

            showTranslatedToast(
                    "Trusted contact form IDs are incomplete",
                    Toast.LENGTH_LONG
            );

            return;
        }

        // ========================================================
        // RELATION OPTIONS
        // ========================================================

        String[] relations = {

                "Mother",
                "Father",
                "Sister",
                "Brother",
                "Daughter",
                "Son",
                "Spouse",
                "Partner",
                "Friend",
                "Relative",
                "Guardian",
                "Colleague",
                "Other"
        };

        ArrayAdapter<String> relationAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        relations
                );

        etGuardianRelation.setAdapter(
                relationAdapter
        );

        etGuardianRelation.setInputType(
                InputType.TYPE_NULL
        );

        etGuardianRelation.setOnClickListener(
                v -> etGuardianRelation.showDropDown()
        );

        // ========================================================
        // BLOOD GROUP OPTIONS
        // ========================================================

        String[] bloodGroups = {

                "A+",
                "A-",
                "B+",
                "B-",
                "AB+",
                "AB-",
                "O+",
                "O-"
        };

        ArrayAdapter<String> bloodAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        bloodGroups
                );

        etGuardianBloodGroup.setAdapter(
                bloodAdapter
        );

        etGuardianBloodGroup.setInputType(
                InputType.TYPE_NULL
        );

        etGuardianBloodGroup.setOnClickListener(
                v -> etGuardianBloodGroup.showDropDown()
        );

        // ========================================================
        // EDIT MODE
        // ========================================================

        boolean isEdit =
                existingDocument != null;

        if (isEdit) {

            etGuardianName.setText(
                    existingDocument.getString("name")
            );

            etGuardianRelation.setText(
                    existingDocument.getString("relation"),
                    false
            );

            etGuardianMobile.setText(
                    existingDocument.getString("mobile")
            );

            etGuardianBloodGroup.setText(
                    existingDocument.getString("bloodGroup"),
                    false
            );

            etGuardianEmail.setText(
                    existingDocument.getString("email")
            );

            etGuardianAddress.setText(
                    existingDocument.getString("address")
            );

            btnSave.setText(
                    "Update Contact"
            );
        }

        // ========================================================
        // CREATE DIALOG
        // ========================================================

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create();

        dialog.setCanceledOnTouchOutside(false);

        // ========================================================
        // SHOW DIALOG
        // ========================================================

        dialog.show();

        // ========================================================
        // TRANSLATE DIALOG
        // ========================================================

        translateNewView(dialogView);

        // ========================================================
        // CANCEL
        // ========================================================

        btnCancel.setOnClickListener(
                v -> dialog.dismiss()
        );

        // ========================================================
        // SAVE / UPDATE
        // ========================================================

        btnSave.setOnClickListener(
                v -> {

                    String name =
                            etGuardianName
                                    .getText()
                                    .toString()
                                    .trim();

                    String relation =
                            etGuardianRelation
                                    .getText()
                                    .toString()
                                    .trim();

                    String mobile =
                            etGuardianMobile
                                    .getText()
                                    .toString()
                                    .trim();

                    String bloodGroup =
                            etGuardianBloodGroup
                                    .getText()
                                    .toString()
                                    .trim();

                    String email =
                            etGuardianEmail
                                    .getText()
                                    .toString()
                                    .trim();

                    String address =
                            etGuardianAddress
                                    .getText()
                                    .toString()
                                    .trim();

                    // =================================================
                    // NAME
                    // =================================================

                    if (name.isEmpty()) {

                        etGuardianName.setError(
                                getTranslatedString(
                                        "Please enter full name"
                                )
                        );

                        etGuardianName.requestFocus();

                        return;
                    }

                    if (!name.matches(
                            "^[a-zA-Z][a-zA-Z .'-]{1,49}$"
                    )) {

                        etGuardianName.setError(
                                getTranslatedString(
                                        "Please enter a valid name"
                                )
                        );

                        etGuardianName.requestFocus();

                        return;
                    }

                    // =================================================
                    // RELATION
                    // =================================================

                    if (relation.isEmpty()) {

                        etGuardianRelation.setError(
                                getTranslatedString(
                                        "Please select relationship"
                                )
                        );

                        etGuardianRelation.requestFocus();

                        return;
                    }

                    // =================================================
                    // MOBILE
                    // =================================================

                    if (mobile.isEmpty()) {

                        etGuardianMobile.setError(
                                getTranslatedString(
                                        "Please enter mobile number"
                                )
                        );

                        etGuardianMobile.requestFocus();

                        return;
                    }

                    if (!mobile.matches(
                            "^[6-9][0-9]{9}$"
                    )) {

                        etGuardianMobile.setError(
                                getTranslatedString(
                                        "Enter a valid 10-digit mobile number"
                                )
                        );

                        etGuardianMobile.requestFocus();

                        return;
                    }

                    // =================================================
                    // BLOOD GROUP
                    // =================================================

                    if (bloodGroup.isEmpty()) {

                        etGuardianBloodGroup.setError(
                                getTranslatedString(
                                        "Please select blood group"
                                )
                        );

                        etGuardianBloodGroup.requestFocus();

                        return;
                    }

                    // =================================================
                    // EMAIL
                    // =================================================

                    if (!email.isEmpty()
                            && !android.util.Patterns
                            .EMAIL_ADDRESS
                            .matcher(email)
                            .matches()) {

                        etGuardianEmail.setError(
                                getTranslatedString(
                                        "Enter a valid email address"
                                )
                        );

                        etGuardianEmail.requestFocus();

                        return;
                    }

                    // =================================================
                    // SAVE OR UPDATE
                    // =================================================

                    if (isEdit) {

                        updateContact(
                                existingDocument.getId(),
                                name,
                                relation,
                                mobile,
                                bloodGroup,
                                email,
                                address,
                                dialog
                        );

                    } else {

                        saveContact(
                                name,
                                relation,
                                mobile,
                                bloodGroup,
                                email,
                                address,
                                dialog
                        );
                    }
                }
        );

        // ========================================================
        // DIALOG SIZE
        // ========================================================

        if (dialog.getWindow() != null) {

            dialog.getWindow()
                    .setBackgroundDrawableResource(
                            android.R.color.transparent
                    );

            dialog.getWindow().setLayout(
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels * 0.94
                    ),
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .heightPixels * 0.88
                    )
            );
        }
    }


    // ============================================================
    // SAVE CONTACT
    // ============================================================

    private void saveContact(
            String name,
            String relation,
            String mobile,
            String bloodGroup,
            String email,
            String address,
            AlertDialog dialog
    ) {

        Map<String, Object> guardianData =
                new HashMap<>();

        guardianData.put(
                "name",
                name
        );

        guardianData.put(
                "relation",
                relation
        );

        guardianData.put(
                "mobile",
                mobile
        );

        guardianData.put(
                "bloodGroup",
                bloodGroup
        );

        guardianData.put(
                "email",
                email
        );

        guardianData.put(
                "address",
                address
        );

        guardianData.put(
                "createdAt",
                System.currentTimeMillis()
        );

        db.collection("users")
                .document(userUid)
                .collection("guardians")
                .add(guardianData)

                .addOnSuccessListener(
                        documentReference -> {

                            dialog.dismiss();

                            showTranslatedToast(
                                    "Trusted contact added ❤️",
                                    Toast.LENGTH_SHORT
                            );

                            loadContactsFromFirestore();
                        }
                )

                .addOnFailureListener(
                        e -> {

                            showTranslatedToast(
                                    "Could not save contact:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            );
                        }
                );
    }


    // ============================================================
    // UPDATE CONTACT
    // ============================================================

    private void updateContact(
            String guardianId,
            String name,
            String relation,
            String mobile,
            String bloodGroup,
            String email,
            String address,
            AlertDialog dialog
    ) {

        Map<String, Object> guardianData =
                new HashMap<>();

        guardianData.put(
                "name",
                name
        );

        guardianData.put(
                "relation",
                relation
        );

        guardianData.put(
                "mobile",
                mobile
        );

        guardianData.put(
                "bloodGroup",
                bloodGroup
        );

        guardianData.put(
                "email",
                email
        );

        guardianData.put(
                "address",
                address
        );

        guardianData.put(
                "updatedAt",
                System.currentTimeMillis()
        );

        db.collection("users")
                .document(userUid)
                .collection("guardians")
                .document(guardianId)
                .update(guardianData)

                .addOnSuccessListener(
                        unused -> {

                            dialog.dismiss();

                            showTranslatedToast(
                                    "Trusted contact updated ❤️",
                                    Toast.LENGTH_SHORT
                            );

                            loadContactsFromFirestore();
                        }
                )

                .addOnFailureListener(
                        e -> {

                            showTranslatedToast(
                                    "Could not update contact:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            );
                        }
                );
    }


    // ============================================================
    // CREATE CONTACT CARD
    // ============================================================

    private void addContactCard(
            DocumentSnapshot contact
    ) {

        // ========================================================
        // OUTER CARD
        // ========================================================

        CardView card =
                new CardView(this);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                dpToPx(16)
        );

        card.setLayoutParams(
                cardParams
        );

        card.setRadius(
                dpToPx(24)
        );

        card.setCardElevation(
                dpToPx(4)
        );

        card.setCardBackgroundColor(
                WHITE
        );

        card.setUseCompatPadding(
                true
        );

        // ========================================================
        // MAIN LAYOUT
        // ========================================================

        LinearLayout mainLayout =
                new LinearLayout(this);

        mainLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.setPadding(
                dpToPx(18),
                dpToPx(18),
                dpToPx(18),
                dpToPx(16)
        );

        // ========================================================
        // CONTACT HEADER
        // ========================================================

        LinearLayout headerRow =
                new LinearLayout(this);

        headerRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        headerRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        // ========================================================
        // AVATAR CIRCLE
        // ========================================================

        CardView avatarCard =
                new CardView(this);

        avatarCard.setRadius(
                dpToPx(30)
        );

        avatarCard.setCardElevation(
                0
        );

        avatarCard.setCardBackgroundColor(
                LIGHT_PINK
        );

        TextView avatar =
                new TextView(this);

        String contactName =
                getSafeString(
                        contact,
                        "name",
                        "?"
                );

        String initial =
                "?";

        if (!contactName.isEmpty()) {

            initial =
                    contactName
                            .substring(0, 1)
                            .toUpperCase();
        }

        avatar.setText(
                initial
        );

        avatar.setTextColor(
                PINK
        );

        avatar.setTextSize(
                23
        );

        avatar.setTypeface(
                null,
                Typeface.BOLD
        );

        avatar.setGravity(
                Gravity.CENTER
        );

        avatarCard.addView(
                avatar
        );

        LinearLayout.LayoutParams avatarParams =
                new LinearLayout.LayoutParams(
                        dpToPx(60),
                        dpToPx(60)
                );

        avatarParams.rightMargin =
                dpToPx(14);

        avatarCard.setLayoutParams(
                avatarParams
        );

        headerRow.addView(
                avatarCard
        );

        // ========================================================
        // NAME / RELATION
        // ========================================================

        LinearLayout nameLayout =
                new LinearLayout(this);

        nameLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        nameLayout.setLayoutParams(
                nameParams
        );

        TextView name =
                new TextView(this);

        name.setText(
                contactName
        );

        name.setTextColor(
                DARK_TEXT
        );

        name.setTextSize(
                19
        );

        name.setTypeface(
                null,
                Typeface.BOLD
        );

        nameLayout.addView(
                name
        );

        TextView relation =
                new TextView(this);

        relation.setText(
                getSafeString(
                        contact,
                        "relation",
                        "Trusted Contact"
                )
        );

        relation.setTextColor(
                PINK
        );

        relation.setTextSize(
                12
        );

        relation.setTypeface(
                null,
                Typeface.BOLD
        );

        relation.setPadding(
                0,
                dpToPx(4),
                0,
                0
        );

        nameLayout.addView(
                relation
        );

        headerRow.addView(
                nameLayout
        );

        mainLayout.addView(
                headerRow
        );

        // ========================================================
        // SMALL TRUST BADGE
        // ========================================================

        CardView trustedBadge =
                new CardView(this);

        trustedBadge.setRadius(
                dpToPx(12)
        );

        trustedBadge.setCardElevation(
                0
        );

        trustedBadge.setCardBackgroundColor(
                VERY_LIGHT_PINK
        );

        LinearLayout badgeLayout =
                new LinearLayout(this);

        badgeLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        badgeLayout.setGravity(
                Gravity.CENTER_VERTICAL
        );

        badgeLayout.setPadding(
                dpToPx(10),
                dpToPx(6),
                dpToPx(10),
                dpToPx(6)
        );

        TextView badgeText =
                new TextView(this);

        badgeText.setText(
                "♥  TRUSTED CONTACT"
        );

        badgeText.setTextColor(
                DARK_PINK
        );

        badgeText.setTextSize(
                9
        );

        badgeText.setTypeface(
                null,
                Typeface.BOLD
        );

        badgeLayout.addView(
                badgeText
        );

        trustedBadge.addView(
                badgeLayout
        );

        LinearLayout.LayoutParams badgeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        badgeParams.topMargin =
                dpToPx(12);

        trustedBadge.setLayoutParams(
                badgeParams
        );

        mainLayout.addView(
                trustedBadge
        );

        // ========================================================
        // DIVIDER
        // ========================================================

        View divider =
                new View(this);

        LinearLayout.LayoutParams dividerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                );

        dividerParams.topMargin =
                dpToPx(14);

        dividerParams.bottomMargin =
                dpToPx(8);

        divider.setLayoutParams(
                dividerParams
        );

        divider.setBackgroundColor(
                BORDER
        );

        mainLayout.addView(
                divider
        );

        // ========================================================
        // CONTACT DETAILS TITLE
        // ========================================================

        TextView detailsTitle =
                new TextView(this);

        detailsTitle.setText(
                "CONTACT DETAILS"
        );

        detailsTitle.setTextColor(
                DARK_PINK
        );

        detailsTitle.setTextSize(
                10
        );

        detailsTitle.setTypeface(
                null,
                Typeface.BOLD
        );

        detailsTitle.setPadding(
                0,
                dpToPx(3),
                0,
                dpToPx(7)
        );

        mainLayout.addView(
                detailsTitle
        );

        // ========================================================
        // MOBILE
        // ========================================================

        mainLayout.addView(
                createDetailBox(
                        "📞",
                        "Mobile Number",
                        getSafeString(
                                contact,
                                "mobile",
                                "-"
                        )
                )
        );

        // ========================================================
        // BLOOD GROUP
        // ========================================================

        mainLayout.addView(
                createDetailBox(
                        "🩸",
                        "Blood Group",
                        getSafeString(
                                contact,
                                "bloodGroup",
                                "-"
                        )
                )
        );

        // ========================================================
        // EMAIL
        // ========================================================

        String email =
                getSafeString(
                        contact,
                        "email",
                        ""
                );

        if (!email.isEmpty()) {

            mainLayout.addView(
                    createDetailBox(
                            "✉",
                            "Email",
                            email
                    )
            );
        }

        // ========================================================
        // ADDRESS
        // ========================================================

        String address =
                getSafeString(
                        contact,
                        "address",
                        ""
                );

        if (!address.isEmpty()) {

            mainLayout.addView(
                    createDetailBox(
                            "📍",
                            "Address",
                            address
                    )
            );
        }

        // ========================================================
        // ACTION LABEL
        // ========================================================

        TextView actionTitle =
                new TextView(this);

        actionTitle.setText(
                "MANAGE CONTACT"
        );

        actionTitle.setTextColor(
                GREY_TEXT
        );

        actionTitle.setTextSize(
                9
        );

        actionTitle.setTypeface(
                null,
                Typeface.BOLD
        );

        actionTitle.setPadding(
                0,
                dpToPx(14),
                0,
                dpToPx(7)
        );

        mainLayout.addView(
                actionTitle
        );

        // ========================================================
        // BUTTON ROW
        // ========================================================

        LinearLayout buttonRow =
                new LinearLayout(this);

        buttonRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        buttonRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        // ========================================================
        // EDIT BUTTON
        // ========================================================

        MaterialButton editButton =
                new MaterialButton(this);

        editButton.setText(
                "✏  Edit Contact"
        );

        editButton.setTextSize(
                12
        );

        editButton.setAllCaps(
                false
        );

        editButton.setTextColor(
                PURPLE
        );

        editButton.setCornerRadius(
                dpToPx(14)
        );

        editButton.setStrokeWidth(
                dpToPx(1)
        );

        editButton.setStrokeColor(
                ColorStateList.valueOf(
                        PURPLE
                )
        );

        editButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        WHITE
                )
        );

        LinearLayout.LayoutParams editParams =
                new LinearLayout.LayoutParams(
                        0,
                        dpToPx(46),
                        1f
                );

        editParams.rightMargin =
                dpToPx(5);

        editButton.setLayoutParams(
                editParams
        );

        editButton.setOnClickListener(
                v -> showAddContactDialog(
                        contact
                )
        );

        buttonRow.addView(
                editButton
        );

        // ========================================================
        // REMOVE BUTTON
        // ========================================================

        MaterialButton removeButton =
                new MaterialButton(this);

        removeButton.setText(
                "Remove"
        );

        removeButton.setTextSize(
                12
        );

        removeButton.setAllCaps(
                false
        );

        removeButton.setTextColor(
                PINK
        );

        removeButton.setCornerRadius(
                dpToPx(14)
        );

        removeButton.setStrokeWidth(
                dpToPx(1)
        );

        removeButton.setStrokeColor(
                ColorStateList.valueOf(
                        PINK
                )
        );

        removeButton.setBackgroundTintList(
                ColorStateList.valueOf(
                        WHITE
                )
        );

        LinearLayout.LayoutParams removeParams =
                new LinearLayout.LayoutParams(
                        0,
                        dpToPx(46),
                        0.65f
                );

        removeParams.leftMargin =
                dpToPx(5);

        removeButton.setLayoutParams(
                removeParams
        );

        removeButton.setOnClickListener(
                v -> confirmDelete(
                        contact.getId()
                )
        );

        buttonRow.addView(
                removeButton
        );

        mainLayout.addView(
                buttonRow
        );

        // ========================================================
        // ADD MAIN CONTENT TO CARD
        // ========================================================

        card.addView(
                mainLayout
        );

        contactsContainer.addView(
                card
        );

        // ========================================================
        // IMPORTANT
        // ========================================================
        // This card is created AFTER BaseActivity's normal
        // translation scan.
        //
        // Therefore we manually translate the complete card.
        // ========================================================

        translateNewView(card);
    }


    // ============================================================
    // DETAIL BOX
    // ============================================================

    private View createDetailBox(
            String icon,
            String title,
            String value
    ) {

        CardView box =
                new CardView(this);

        box.setRadius(
                dpToPx(14)
        );

        box.setCardElevation(
                0
        );

        box.setCardBackgroundColor(
                VERY_LIGHT_PINK
        );

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        boxParams.setMargins(
                0,
                dpToPx(4),
                0,
                dpToPx(4)
        );

        box.setLayoutParams(
                boxParams
        );

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dpToPx(12),
                dpToPx(9),
                dpToPx(12),
                dpToPx(9)
        );

        // ========================================================
        // ICON
        // ========================================================

        TextView iconView =
                new TextView(this);

        iconView.setText(
                icon
        );

        iconView.setTextSize(
                17
        );

        iconView.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        dpToPx(32),
                        dpToPx(32)
                );

        iconParams.rightMargin =
                dpToPx(10);

        iconView.setLayoutParams(
                iconParams
        );

        row.addView(
                iconView
        );

        // ========================================================
        // TEXT
        // ========================================================

        LinearLayout textLayout =
                new LinearLayout(this);

        textLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                );

        textLayout.setLayoutParams(
                textParams
        );

        TextView titleView =
                new TextView(this);

        titleView.setText(
                title
        );

        titleView.setTextColor(
                GREY_TEXT
        );

        titleView.setTextSize(
                9
        );

        titleView.setTypeface(
                null,
                Typeface.BOLD
        );

        textLayout.addView(
                titleView
        );

        TextView valueView =
                new TextView(this);

        valueView.setText(
                value
        );

        valueView.setTextColor(
                DARK_TEXT
        );

        valueView.setTextSize(
                13
        );

        valueView.setTypeface(
                null,
                Typeface.BOLD
        );

        valueView.setPadding(
                0,
                dpToPx(2),
                0,
                0
        );

        textLayout.addView(
                valueView
        );

        row.addView(
                textLayout
        );

        box.addView(
                row
        );

        return box;
    }


    // ============================================================
    // SAFE STRING
    // ============================================================

    private String getSafeString(
            DocumentSnapshot document,
            String field,
            String defaultValue
    ) {

        String value =
                document.getString(field);

        if (value == null) {
            return defaultValue;
        }

        return value;
    }


    // ============================================================
    // EMPTY STATE
    // ============================================================

    private void showEmptyMessage() {

        CardView card =
                new CardView(this);

        card.setRadius(
                dpToPx(24)
        );

        card.setCardElevation(
                dpToPx(3)
        );

        card.setCardBackgroundColor(
                WHITE
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        card.setLayoutParams(
                cardParams
        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER
        );

        layout.setPadding(
                dpToPx(25),
                dpToPx(35),
                dpToPx(25),
                dpToPx(35)
        );

        // ========================================================
        // ICON
        // ========================================================

        CardView iconCard =
                new CardView(this);

        iconCard.setRadius(
                dpToPx(40)
        );

        iconCard.setCardElevation(
                0
        );

        iconCard.setCardBackgroundColor(
                LIGHT_PINK
        );

        TextView icon =
                new TextView(this);

        icon.setText(
                "♥"
        );

        icon.setTextColor(
                PINK
        );

        icon.setTextSize(
                31
        );

        icon.setGravity(
                Gravity.CENTER
        );

        iconCard.addView(
                icon
        );

        layout.addView(
                iconCard,
                new LinearLayout.LayoutParams(
                        dpToPx(80),
                        dpToPx(80)
                )
        );

        // ========================================================
        // TITLE
        // ========================================================

        TextView title =
                new TextView(this);

        title.setText(
                "No trusted contacts yet"
        );

        title.setTextColor(
                DARK_TEXT
        );

        title.setTextSize(
                18
        );

        title.setTypeface(
                null,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        titleParams.topMargin =
                dpToPx(17);

        title.setLayoutParams(
                titleParams
        );

        layout.addView(
                title
        );

        // ========================================================
        // DESCRIPTION
        // ========================================================

        TextView description =
                new TextView(this);

        description.setText(
                "Add family members or friends you trust.\n"
                        + "They can be contacted quickly during an emergency."
        );

        description.setTextColor(
                GREY_TEXT
        );

        description.setTextSize(
                12
        );

        description.setGravity(
                Gravity.CENTER
        );

        description.setLineSpacing(
                dpToPx(2),
                1f
        );

        LinearLayout.LayoutParams descParams =
                new LinearLayout.LayoutParams(
                        dpToPx(290),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        descParams.topMargin =
                dpToPx(8);

        description.setLayoutParams(
                descParams
        );

        layout.addView(
                description
        );

        // ========================================================
        // FOOTER
        // ========================================================

        TextView footer =
                new TextView(this);

        footer.setText(
                "♥  Your safety circle can include as many contacts as needed."
        );

        footer.setTextColor(
                DARK_PINK
        );

        footer.setTextSize(
                10
        );

        footer.setTypeface(
                null,
                Typeface.BOLD
        );

        footer.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        footerParams.topMargin =
                dpToPx(18);

        footer.setLayoutParams(
                footerParams
        );

        layout.addView(
                footer
        );

        card.addView(
                layout
        );

        contactsContainer.addView(
                card
        );

        // Translate dynamically created empty state
        translateNewView(card);
    }


    // ============================================================
    // DELETE CONFIRMATION
    // ============================================================

    private void confirmDelete(
            String guardianId
    ) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                getTranslatedString(
                                        "Remove Trusted Contact?"
                                )
                        )
                        .setMessage(
                                getTranslatedString(
                                        "This person will no longer receive "
                                                + "your emergency alerts."
                                )
                        )
                        .setNegativeButton(
                                getTranslatedString(
                                        "Cancel"
                                ),
                                null
                        )
                        .setPositiveButton(
                                getTranslatedString(
                                        "Remove"
                                ),
                                (dialogInterface, which) ->
                                        deleteContact(
                                                guardianId
                                        )
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    if (dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ) != null) {

                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setTextColor(
                                PINK
                        );
                    }

                    if (dialog.getButton(
                            AlertDialog.BUTTON_NEGATIVE
                    ) != null) {

                        dialog.getButton(
                                AlertDialog.BUTTON_NEGATIVE
                        ).setTextColor(
                                PURPLE
                        );
                    }
                }
        );

        dialog.show();
    }


    // ============================================================
    // DELETE CONTACT
    // ============================================================

    private void deleteContact(
            String guardianId
    ) {

        db.collection("users")
                .document(userUid)
                .collection("guardians")
                .document(guardianId)
                .delete()

                .addOnSuccessListener(
                        unused -> {

                            showTranslatedToast(
                                    "Trusted contact removed",
                                    Toast.LENGTH_SHORT
                            );

                            loadContactsFromFirestore();
                        }
                )

                .addOnFailureListener(
                        e -> {

                            showTranslatedToast(
                                    "Could not remove contact:\n"
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            );
                        }
                );
    }


    // ============================================================
    // TRANSLATE DYNAMICALLY CREATED VIEW
    // ============================================================

    private void translateNewView(View view) {

        if (view == null) {
            return;
        }

        String langCode =
                LocaleHelper.getSavedLanguage(this);

        if (langCode == null ||
                langCode.equals("en")) {
            return;
        }

        // ========================================================
        // TEXT VIEW
        // ========================================================

        if (view instanceof TextView) {

            try {

                LanguageManager.translateTextView(
                        this,
                        (TextView) view,
                        langCode
                );

            } catch (Exception ignored) {
                // Translation must never crash the page.
            }
        }

        // ========================================================
        // CHILD VIEWS
        // ========================================================

        if (view instanceof android.view.ViewGroup) {

            android.view.ViewGroup group =
                    (android.view.ViewGroup) view;

            for (
                    int i = 0;
                    i < group.getChildCount();
                    i++
            ) {

                translateNewView(
                        group.getChildAt(i)
                );
            }
        }
    }


    // ============================================================
    // TRANSLATED STRING
    // ============================================================

    private String getTranslatedString(
            String englishText
    ) {

        if (englishText == null) {
            return "";
        }

        String langCode =
                LocaleHelper.getSavedLanguage(this);

        if (langCode == null ||
                langCode.equals("en")) {

            return englishText;
        }

        try {

            TextView temp =
                    new TextView(this);

            temp.setText(
                    englishText
            );

            LanguageManager.translateTextView(
                    this,
                    temp,
                    langCode
            );

            return temp.getText()
                    .toString();

        } catch (Exception ignored) {

            return englishText;
        }
    }


    // ============================================================
    // TRANSLATED TOAST
    // ============================================================

    private void showTranslatedToast(
            String englishText,
            int duration
    ) {

        Toast.makeText(
                this,
                getTranslatedString(englishText),
                duration
        ).show();
    }


    // ============================================================
    // DP TO PX
    // ============================================================

    private int dpToPx(
            int dp
    ) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                dp * density
        );
    }
}