package com.sheshield.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class IncidentAdapter
        extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    // ============================================================
    // DELETE LISTENER
    // ============================================================

    public interface OnIncidentDeleteListener {
        void onDeleteRequested(int position, IncidentReport report);
    }

    // ============================================================
    // PROOF / EVIDENCE LISTENER
    // ============================================================

    public interface OnProofClickListener {
        void onProofClicked(IncidentReport report);
    }

    // ============================================================
    // VARIABLES
    // ============================================================

    private final List<IncidentReport> incidentList;
    private final OnIncidentDeleteListener deleteListener;
    private final OnProofClickListener proofClickListener;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public IncidentAdapter(
            List<IncidentReport> incidentList,
            OnIncidentDeleteListener deleteListener,
            OnProofClickListener proofClickListener) {

        this.incidentList = incidentList;
        this.deleteListener = deleteListener;
        this.proofClickListener = proofClickListener;
    }

    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incident_card, parent, false);

        return new IncidentViewHolder(view);
    }

    // ============================================================
    // BIND DATA
    // ============================================================

    @Override
    public void onBindViewHolder(
            @NonNull IncidentViewHolder holder,
            int position) {

        IncidentReport report = incidentList.get(position);

        // ========================================================
        // 1. OFFENSE CATEGORY
        // ========================================================

        if (report.getOffenseCategory() != null
                && !report.getOffenseCategory().isEmpty()) {

            holder.tvCategory.setText(
                    "⚡ " + report.getOffenseCategory().toUpperCase()
            );

        } else {

            holder.tvCategory.setText("⚡ GENERAL INCIDENT");
        }

        // ========================================================
        // 2. OFFICIAL COMPLAINT BADGE
        // ========================================================

        if (report.isOfficialComplaint()) {

            holder.tvOfficialBadge.setVisibility(View.VISIBLE);

        } else {

            holder.tvOfficialBadge.setVisibility(View.GONE);
        }

        // ========================================================
        // 3. MY POST HIGHLIGHT
        // ========================================================

        if (report.isMyPost()) {

            holder.cardIncident.setCardBackgroundColor(
                    Color.parseColor("#FFF5F8")
            );

            holder.cardIncident.setStrokeColor(
                    Color.parseColor("#D81B60")
            );

            holder.cardIncident.setStrokeWidth(4);

            if (holder.tvMyPostBadge != null) {
                holder.tvMyPostBadge.setVisibility(View.VISIBLE);
            }

        } else {

            holder.cardIncident.setCardBackgroundColor(
                    Color.parseColor("#FFFFFF")
            );

            holder.cardIncident.setStrokeColor(
                    Color.parseColor("#F3C6D6")
            );

            holder.cardIncident.setStrokeWidth(2);

            if (holder.tvMyPostBadge != null) {
                holder.tvMyPostBadge.setVisibility(View.GONE);
            }
        }

        // ========================================================
        // 4. REPORTER INFORMATION
        // ========================================================

        String roleStr = "";

        if (report.getReporterStatus() != null
                && !report.getReporterStatus().isEmpty()) {

            roleStr = " (" + report.getReporterStatus() + ")";
        }

        holder.tvReporterInfo.setText(
                "Reported by: "
                        + report.getUserName()
                        + roleStr
        );

        // ========================================================
        // 5. LOCATION
        // ========================================================

        if (report.getLocation() != null
                && !report.getLocation().isEmpty()) {

            holder.tvLocation.setText(
                    "📍 Location: " + report.getLocation()
            );

        } else {

            holder.tvLocation.setText(
                    "📍 Location: Not specified"
            );
        }

        // ========================================================
        // 6. DATE & TIME
        // ========================================================

        String date = report.getDate() != null
                ? report.getDate()
                : "";

        String time = report.getTime() != null
                ? report.getTime()
                : "";

        holder.tvDateTime.setText(
                "📅 Date & Time: " + date + " at " + time
        );

        // ========================================================
        // 7. INCIDENT DETAILS
        // ========================================================

        if (report.getDetails() != null
                && !report.getDetails().isEmpty()) {

            holder.tvDetails.setText(
                    report.getDetails()
            );

        } else {

            holder.tvDetails.setText(
                    "No additional details provided."
            );
        }

        // ========================================================
        // 8. SUSPECT DETAILS
        // ========================================================

        if (report.getSuspectDetails() != null
                && !report.getSuspectDetails().trim().isEmpty()) {

            holder.layoutSuspect.setVisibility(View.VISIBLE);

            holder.tvSuspect.setText(
                    report.getSuspectDetails().trim()
            );

        } else {

            holder.layoutSuspect.setVisibility(View.GONE);
        }

        // ========================================================
        // 9. WITNESS DETAILS
        // ========================================================

        if (report.getWitnessDetails() != null
                && !report.getWitnessDetails().trim().isEmpty()) {

            holder.layoutWitness.setVisibility(View.VISIBLE);

            holder.tvWitness.setText(
                    report.getWitnessDetails().trim()
            );

        } else {

            holder.layoutWitness.setVisibility(View.GONE);
        }

        // ========================================================
        // 10. ACTION REQUESTED
        // ========================================================

        if (report.getActionRequested() != null
                && !report.getActionRequested().isEmpty()) {

            holder.tvActionRequested.setVisibility(View.VISIBLE);

            holder.tvActionRequested.setText(
                    "⚖️ Demanded Action: "
                            + report.getActionRequested()
            );

        } else {

            holder.tvActionRequested.setVisibility(View.GONE);
        }

        // ========================================================
        // 11. PROOF / EVIDENCE
        // ========================================================

        if (report.hasProof()
                && report.isProofPublic()) {

            holder.tvProofStatus.setVisibility(View.VISIBLE);

            holder.tvProofStatus.setText(
                    "📎 View Evidence ("
                            + report.getProofType()
                            + ")"
            );

            holder.tvProofStatus.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {

                            if (proofClickListener != null) {

                                proofClickListener
                                        .onProofClicked(report);
                            }
                        }
                    }
            );

        } else {

            holder.tvProofStatus.setVisibility(View.GONE);

            holder.tvProofStatus.setOnClickListener(null);
        }

        // ========================================================
        // 12. SECRET 5-TAP DELETE
        // ========================================================

        if (report.isMyPost()) {

            holder.itemView.setClickable(true);

            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {

                        private int tapCount = 0;
                        private long lastTapTime = 0;

                        @Override
                        public void onClick(View v) {

                            long currentTime =
                                    System.currentTimeMillis();

                            // Reset if taps are too far apart
                            if (currentTime - lastTapTime > 1500) {
                                tapCount = 0;
                            }

                            tapCount++;
                            lastTapTime = currentTime;

                            // Five taps
                            if (tapCount >= 5) {

                                tapCount = 0;

                                int adapterPosition =
                                        holder.getBindingAdapterPosition();

                                if (adapterPosition != RecyclerView.NO_POSITION) {

                                    if (deleteListener != null) {

                                        deleteListener.onDeleteRequested(
                                                adapterPosition,
                                                report
                                        );
                                    }
                                }
                            }
                        }
                    }
            );

        } else {

            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        }
    }

    // ============================================================
    // ITEM COUNT
    // ============================================================

    @Override
    public int getItemCount() {

        return incidentList != null
                ? incidentList.size()
                : 0;
    }

    // ============================================================
    // VIEW HOLDER
    // ============================================================

    public static class IncidentViewHolder
            extends RecyclerView.ViewHolder {

        MaterialCardView cardIncident;

        TextView tvCategory;
        TextView tvOfficialBadge;
        TextView tvMyPostBadge;
        TextView tvReporterInfo;

        TextView tvLocation;
        TextView tvDateTime;
        TextView tvDetails;

        LinearLayout layoutSuspect;
        LinearLayout layoutWitness;

        TextView tvSuspect;
        TextView tvWitness;

        TextView tvActionRequested;
        TextView tvProofStatus;

        // ========================================================
        // CONSTRUCTOR
        // ========================================================

        public IncidentViewHolder(
                @NonNull View itemView) {

            super(itemView);

            // ----------------------------------------------------
            // CARD
            // ----------------------------------------------------

            cardIncident =
                    itemView.findViewById(
                            R.id.cardIncident
                    );

            // ----------------------------------------------------
            // CATEGORY
            // ----------------------------------------------------

            tvCategory =
                    itemView.findViewById(
                            R.id.tvCategory
                    );

            // ----------------------------------------------------
            // OFFICIAL BADGE
            // ----------------------------------------------------

            tvOfficialBadge =
                    itemView.findViewById(
                            R.id.tvOfficialBadge
                    );

            // ----------------------------------------------------
            // MY POST BADGE
            // ----------------------------------------------------

            tvMyPostBadge =
                    itemView.findViewById(
                            R.id.tvMyPostBadge
                    );

            // ----------------------------------------------------
            // REPORTER
            // ----------------------------------------------------

            tvReporterInfo =
                    itemView.findViewById(
                            R.id.tvReporterInfo
                    );

            // ----------------------------------------------------
            // LOCATION
            // ----------------------------------------------------

            tvLocation =
                    itemView.findViewById(
                            R.id.tvLocation
                    );

            // ----------------------------------------------------
            // DATE / TIME
            // ----------------------------------------------------

            tvDateTime =
                    itemView.findViewById(
                            R.id.tvDateTime
                    );

            // ----------------------------------------------------
            // DETAILS
            // ----------------------------------------------------

            tvDetails =
                    itemView.findViewById(
                            R.id.tvDetails
                    );

            // ----------------------------------------------------
            // SUSPECT
            // ----------------------------------------------------

            layoutSuspect =
                    itemView.findViewById(
                            R.id.layoutSuspect
                    );

            tvSuspect =
                    itemView.findViewById(
                            R.id.tvSuspect
                    );

            // ----------------------------------------------------
            // WITNESS
            // ----------------------------------------------------

            layoutWitness =
                    itemView.findViewById(
                            R.id.layoutWitness
                    );

            tvWitness =
                    itemView.findViewById(
                            R.id.tvWitness
                    );

            // ----------------------------------------------------
            // ACTION
            // ----------------------------------------------------

            tvActionRequested =
                    itemView.findViewById(
                            R.id.tvActionRequested
                    );

            // ----------------------------------------------------
            // PROOF
            // ----------------------------------------------------

            tvProofStatus =
                    itemView.findViewById(
                            R.id.tvProofStatus
                    );
        }
    }
}