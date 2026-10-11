package com.sheshield.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class IncidentReportActivity extends BaseActivity
        implements IncidentAdapter.OnIncidentDeleteListener {

    private List<IncidentReport> incidentList;
    private IncidentAdapter adapter;

    private boolean isProofAttached = false;
    private String currentProofType = "NONE";
    private String currentProofUri = "";

    private TextView activeTvProofStatus;


    // ============================================================
    // MEDIA PICKER
    // ============================================================

    private final ActivityResultLauncher<Intent> mediaPickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            Uri selectedUri =
                                    result.getData().getData();

                            if (selectedUri != null) {

                                isProofAttached = true;
                                currentProofUri =
                                        selectedUri.toString();

                                String type =
                                        getContentResolver()
                                                .getType(selectedUri);

                                if (type != null
                                        && type.contains("video")) {

                                    currentProofType = "VIDEO";

                                } else if (type != null
                                        && type.contains("audio")) {

                                    currentProofType = "AUDIO";

                                } else {

                                    currentProofType = "IMAGE";
                                }

                                if (activeTvProofStatus != null) {

                                    activeTvProofStatus.setText(
                                            "Attached "
                                                    + currentProofType
                                                    + " Evidence"
                                    );
                                }
                            }
                        }
                    }
            );


    // ============================================================
    // AUDIO RECORDING
    // ============================================================

    private final ActivityResultLauncher<Intent> audioRecordLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {

                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            Uri audioUri =
                                    result.getData().getData();

                            isProofAttached = true;
                            currentProofType = "AUDIO";

                            currentProofUri =
                                    audioUri != null
                                            ? audioUri.toString()
                                            : "";

                            if (activeTvProofStatus != null) {

                                activeTvProofStatus.setText(
                                        "🎙️ Live Audio Recorded & Attached"
                                );
                            }
                        }
                    }
            );


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_incident_report);


        // Back button
        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());


        // Open report form
        findViewById(R.id.btnOpenReportForm)
                .setOnClickListener(
                        v -> showReportBottomSheet()
                );


        // RecyclerView
        RecyclerView recyclerView =
                findViewById(R.id.rvIncidents);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );


        // Incident list
        incidentList = new ArrayList<>();


        // ========================================================
        // MOCK SAMPLE FEED
        // ========================================================

        incidentList.add(
                new IncidentReport(
                        "1",
                        "Priya Sharma",
                        "+91 9876543210",
                        "Victim",
                        "Harassment",
                        "Followed near Central Park gate by unidentified person.",
                        "Central Park Gate 2, Bengaluru",
                        "22-Sep-2026",
                        "07:45 PM",
                        "Tall individual in black jacket",
                        "Shopkeeper nearby",
                        "Police Investigation",
                        false,
                        true,
                        true,
                        "IMAGE",
                        "",
                        true,
                        false
                )
        );


        // ========================================================
        // INCIDENT ADAPTER
        // ========================================================

        adapter = new IncidentAdapter(
                incidentList,
                this,
                new IncidentAdapter.OnProofClickListener() {

                    @Override
                    public void onProofClicked(
                            IncidentReport report) {

                        String proofUri =
                                report.getProofUri();

                        if (proofUri != null
                                && !proofUri.isEmpty()) {

                            try {

                                Intent intent =
                                        new Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(proofUri)
                                        );

                                intent.addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                );

                                startActivity(intent);

                            } catch (Exception e) {

                                Toast.makeText(
                                        IncidentReportActivity.this,
                                        "Unable to open this evidence.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                        } else {

                            Toast.makeText(
                                    IncidentReportActivity.this,
                                    "No evidence file is attached.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        );


        recyclerView.setAdapter(adapter);
    }


    // ============================================================
    // REPORT BOTTOM SHEET
    // ============================================================

    private void showReportBottomSheet() {

        BottomSheetDialog dialog =
                new BottomSheetDialog(this);

        View dialogView =
                LayoutInflater.from(this)
                        .inflate(
                                R.layout.dialog_report_incident,
                                null
                        );

        dialog.setContentView(dialogView);


        // ========================================================
        // FORM FIELDS
        // ========================================================

        TextInputEditText etReporterName =
                dialogView.findViewById(
                        R.id.etReporterName
                );

        TextInputEditText etReporterPhone =
                dialogView.findViewById(
                        R.id.etReporterPhone
                );

        AutoCompleteTextView actvReporterStatus =
                dialogView.findViewById(
                        R.id.actvReporterStatus
                );


        AutoCompleteTextView actvOffenseCategory =
                dialogView.findViewById(
                        R.id.actvOffenseCategory
                );

        TextInputEditText etDetails =
                dialogView.findViewById(
                        R.id.etDetails
                );

        TextInputEditText etLocation =
                dialogView.findViewById(
                        R.id.etLocation
                );

        TextInputEditText etDate =
                dialogView.findViewById(
                        R.id.etDate
                );

        TextInputEditText etTime =
                dialogView.findViewById(
                        R.id.etTime
                );


        TextInputEditText etSuspectDetails =
                dialogView.findViewById(
                        R.id.etSuspectDetails
                );

        TextInputEditText etWitnessDetails =
                dialogView.findViewById(
                        R.id.etWitnessDetails
                );

        AutoCompleteTextView actvActionRequested =
                dialogView.findViewById(
                        R.id.actvActionRequested
                );


        // Proof
        activeTvProofStatus =
                dialogView.findViewById(
                        R.id.tvProofStatus
                );

        Button btnAttachProof =
                dialogView.findViewById(
                        R.id.btnAttachProof
                );

        Button btnRecordAudio =
                dialogView.findViewById(
                        R.id.btnRecordAudio
                );


        // Switches
        SwitchMaterial switchFileComplaint =
                dialogView.findViewById(
                        R.id.switchFileComplaint
                );

        SwitchMaterial switchAnonymous =
                dialogView.findViewById(
                        R.id.switchAnonymous
                );

        SwitchMaterial switchPublicProof =
                dialogView.findViewById(
                        R.id.switchPublicProof
                );


        Button btnSubmit =
                dialogView.findViewById(
                        R.id.btnSubmitReport
                );


        // ========================================================
        // REPORTER STATUS DROPDOWN
        // ========================================================

        String[] roles = {
                "Victim",
                "Eyewitness",
                "Relative / Representative"
        };

        actvReporterStatus.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        roles
                )
        );


        // ========================================================
        // OFFENSE CATEGORY DROPDOWN
        // ========================================================

        String[] categories = {
                "Harassment",
                "Stalking",
                "Physical Assault",
                "Cyber Crime",
                "Domestic Violence",
                "Theft / Robbery",
                "Other"
        };

        actvOffenseCategory.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        categories
                )
        );


        // ========================================================
        // ACTION REQUESTED DROPDOWN
        // ========================================================

        String[] actions = {
                "File Official FIR",
                "Request Police Patrol / Protection",
                "Initiate Investigation",
                "Cyber / Media Content Removal"
        };

        actvActionRequested.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        actions
                )
        );


        // Reset proof state
        isProofAttached = false;
        currentProofType = "NONE";
        currentProofUri = "";


        // ========================================================
        // DATE PICKER
        // ========================================================

        etDate.setOnClickListener(v -> {

            Calendar calendar =
                    Calendar.getInstance();

            DatePickerDialog datePicker =
                    new DatePickerDialog(
                            this,
                            (view, year, month, dayOfMonth) -> {

                                String dateStr =
                                        String.format(
                                                Locale.getDefault(),
                                                "%02d-%02d-%d",
                                                dayOfMonth,
                                                month + 1,
                                                year
                                        );

                                etDate.setText(dateStr);
                            },

                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                    );

            datePicker.show();
        });


        // ========================================================
        // TIME PICKER
        // ========================================================

        etTime.setOnClickListener(v -> {

            Calendar calendar =
                    Calendar.getInstance();

            TimePickerDialog timePicker =
                    new TimePickerDialog(
                            this,
                            (view, hourOfDay, minute) -> {

                                String amPm =
                                        hourOfDay >= 12
                                                ? "PM"
                                                : "AM";

                                int formattedHour =
                                        hourOfDay % 12 == 0
                                                ? 12
                                                : hourOfDay % 12;

                                String timeStr =
                                        String.format(
                                                Locale.getDefault(),
                                                "%02d:%02d %s",
                                                formattedHour,
                                                minute,
                                                amPm
                                        );

                                etTime.setText(timeStr);
                            },

                            calendar.get(
                                    Calendar.HOUR_OF_DAY
                            ),

                            calendar.get(
                                    Calendar.MINUTE
                            ),

                            false
                    );

            timePicker.show();
        });


        // ========================================================
        // ATTACH PROOF
        // ========================================================

        btnAttachProof.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            Intent.ACTION_GET_CONTENT
                    );

            intent.setType("*/*");

            String[] mimeTypes = {
                    "image/*",
                    "video/*",
                    "audio/*"
            };

            intent.putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    mimeTypes
            );

            mediaPickerLauncher.launch(intent);
        });


        // ========================================================
        // RECORD AUDIO
        // ========================================================

        btnRecordAudio.setOnClickListener(v -> {

            try {

                Intent intent =
                        new Intent(
                                MediaStore.Audio.Media.RECORD_SOUND_ACTION
                        );

                audioRecordLauncher.launch(intent);

            } catch (Exception e) {

                isProofAttached = true;
                currentProofType = "AUDIO";
                currentProofUri = "";

                if (activeTvProofStatus != null) {

                    activeTvProofStatus.setText(
                            "🎙️ Live Audio Recorded & Attached"
                    );
                }
            }
        });


        // ========================================================
        // SUBMIT REPORT
        // ========================================================

        btnSubmit.setOnClickListener(v -> {

            String name =
                    etReporterName.getText() != null
                            ? etReporterName.getText()
                            .toString()
                            .trim()
                            : "";

            String phone =
                    etReporterPhone.getText() != null
                            ? etReporterPhone.getText()
                            .toString()
                            .trim()
                            : "";

            String role =
                    actvReporterStatus
                            .getText()
                            .toString()
                            .trim();


            String category =
                    actvOffenseCategory
                            .getText()
                            .toString()
                            .trim();

            String details =
                    etDetails.getText() != null
                            ? etDetails.getText()
                            .toString()
                            .trim()
                            : "";

            String location =
                    etLocation.getText() != null
                            ? etLocation.getText()
                            .toString()
                            .trim()
                            : "";

            String date =
                    etDate.getText() != null
                            ? etDate.getText()
                            .toString()
                            .trim()
                            : "";

            String time =
                    etTime.getText() != null
                            ? etTime.getText()
                            .toString()
                            .trim()
                            : "";


            String suspect =
                    etSuspectDetails.getText() != null
                            ? etSuspectDetails.getText()
                            .toString()
                            .trim()
                            : "";

            String witness =
                    etWitnessDetails.getText() != null
                            ? etWitnessDetails.getText()
                            .toString()
                            .trim()
                            : "";

            String action =
                    actvActionRequested
                            .getText()
                            .toString()
                            .trim();


            // ====================================================
            // VALIDATION
            // ====================================================

            if (name.isEmpty()
                    || phone.isEmpty()
                    || role.isEmpty()
                    || category.isEmpty()
                    || details.isEmpty()
                    || location.isEmpty()
                    || date.isEmpty()
                    || time.isEmpty()
                    || action.isEmpty()) {

                Toast.makeText(
                        this,
                        "Please complete all mandatory fields (*)",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // ====================================================
            // SWITCH VALUES
            // ====================================================

            boolean isComplaint =
                    switchFileComplaint.isChecked();

            boolean isAnon =
                    switchAnonymous.isChecked();

            boolean isPublicProof =
                    switchPublicProof.isChecked();


            // ====================================================
            // CREATE REPORT
            // ====================================================

            IncidentReport newReport =
                    new IncidentReport(

                            String.valueOf(
                                    System.currentTimeMillis()
                            ),

                            name,
                            phone,
                            role,
                            category,
                            details,
                            location,
                            date,
                            time,
                            suspect,
                            witness,
                            action,

                            isAnon,
                            isComplaint,
                            isProofAttached,

                            currentProofType,
                            currentProofUri,

                            isPublicProof,

                            true
                    );


            // ====================================================
            // OFFICIAL COMPLAINT WITHOUT PROOF
            // ====================================================

            if (isComplaint && !isProofAttached) {

                new AlertDialog.Builder(this)
                        .setTitle("Missing Evidence")
                        .setMessage(
                                "Attaching proof makes official complaints actionable faster. "
                                        + "Do you wish to submit without proof?"
                        )
                        .setPositiveButton(
                                "Submit Anyway",
                                (d, w) ->
                                        submitReport(
                                                dialog,
                                                newReport
                                        )
                        )
                        .setNegativeButton(
                                "Add Evidence",
                                (d, w) -> d.dismiss()
                        )
                        .show();

            } else {

                submitReport(
                        dialog,
                        newReport
                );
            }
        });


        dialog.show();
    }


    // ============================================================
    // SUBMIT REPORT
    // ============================================================

    private void submitReport(
            BottomSheetDialog dialog,
            IncidentReport report) {

        incidentList.add(
                0,
                report
        );

        adapter.notifyItemInserted(0);

        dialog.dismiss();

        Toast.makeText(
                this,
                "Incident Reported Successfully",
                Toast.LENGTH_SHORT
        ).show();
    }


    // ============================================================
    // DELETE REPORT
    // ============================================================

    @Override
    public void onDeleteRequested(
            int position,
            IncidentReport report) {

        new AlertDialog.Builder(this)

                .setTitle("Delete Report")

                .setMessage(
                        "Are you sure you want to remove this incident report?"
                )

                .setPositiveButton(
                        "Delete",
                        (dialog, which) -> {

                            if (position >= 0
                                    && position < incidentList.size()) {

                                incidentList.remove(position);

                                adapter.notifyItemRemoved(
                                        position
                                );

                                Toast.makeText(
                                        this,
                                        "Report deleted",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )

                .setNegativeButton(
                        "Cancel",
                        null
                )

                .show();
    }
}