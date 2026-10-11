package com.sheshield.app;

public class IncidentReport {
    private String id;
    private String reporterName;
    private String reporterPhone;
    private String reporterStatus; // "Victim", "Eyewitness", "Relative/Representative"
    private String offenseCategory; // "Harassment", "Stalking", "Physical Assault", "Cyber Crime", "Domestic Violence", "Theft", "Other"
    private String details;
    private String location;
    private String date;
    private String time;
    private String suspectDetails;
    private String witnessDetails;
    private String actionRequested; // "File Official FIR", "Police Protection", "Investigation", "Content Removal"
    private boolean isAnonymous;
    private boolean isOfficialComplaint;
    private boolean hasProof;
    private String proofType; // "IMAGE", "AUDIO", "VIDEO", "NONE"
    private String proofUri;
    private boolean isProofPublic;
    private boolean isMyPost;

    public IncidentReport(String id, String reporterName, String reporterPhone, String reporterStatus,
                          String offenseCategory, String details, String location, String date, String time,
                          String suspectDetails, String witnessDetails, String actionRequested,
                          boolean isAnonymous, boolean isOfficialComplaint, boolean hasProof,
                          String proofType, String proofUri, boolean isProofPublic, boolean isMyPost) {
        this.id = id;
        this.reporterName = reporterName;
        this.reporterPhone = reporterPhone;
        this.reporterStatus = reporterStatus;
        this.offenseCategory = offenseCategory;
        this.details = details;
        this.location = location;
        this.date = date;
        this.time = time;
        this.suspectDetails = suspectDetails;
        this.witnessDetails = witnessDetails;
        this.actionRequested = actionRequested;
        this.isAnonymous = isAnonymous;
        this.isOfficialComplaint = isOfficialComplaint;
        this.hasProof = hasProof;
        this.proofType = proofType;
        this.proofUri = proofUri;
        this.isProofPublic = isProofPublic;
        this.isMyPost = isMyPost;
    }

    public String getId() { return id; }
    public String getUserName() { return isAnonymous ? "Anonymous User" : reporterName; }
    public String getReporterPhone() { return reporterPhone; }
    public String getReporterStatus() { return reporterStatus; }
    public String getOffenseCategory() { return offenseCategory; }
    public String getDetails() { return details; }
    public String getLocation() { return location; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getSuspectDetails() { return suspectDetails; }
    public String getWitnessDetails() { return witnessDetails; }
    public String getActionRequested() { return actionRequested; }
    public boolean isOfficialComplaint() { return isOfficialComplaint; }
    public boolean hasProof() { return hasProof; }
    public String getProofType() { return proofType != null ? proofType : "NONE"; }
    public String getProofUri() { return proofUri; }
    public boolean isProofPublic() { return isProofPublic; }
    public boolean isMyPost() { return isMyPost; }
}