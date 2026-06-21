package com.reefer.diagnosis.model;

public enum Severity {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    CRITICAL("危急");

    private final String displayName;

    Severity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
