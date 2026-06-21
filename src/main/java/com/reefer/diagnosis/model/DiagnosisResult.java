package com.reefer.diagnosis.model;

import java.time.LocalDateTime;
import java.util.List;

public class DiagnosisResult {

    private String containerId;
    private FaultType faultType;
    private Severity severity;
    private double confidence;
    private LocalDateTime analysisTime;
    private LocalDateTime dataStartTime;
    private LocalDateTime dataEndTime;
    private int totalRecords;
    private List<DiagnosisIndicator> indicators;
    private String diagnosisSummary;
    private String suggestion;

    public DiagnosisResult() {}

    private DiagnosisResult(Builder b) {
        this.containerId = b.containerId;
        this.faultType = b.faultType;
        this.severity = b.severity;
        this.confidence = b.confidence;
        this.analysisTime = b.analysisTime;
        this.dataStartTime = b.dataStartTime;
        this.dataEndTime = b.dataEndTime;
        this.totalRecords = b.totalRecords;
        this.indicators = b.indicators;
        this.diagnosisSummary = b.diagnosisSummary;
        this.suggestion = b.suggestion;
    }

    public static Builder builder() { return new Builder(); }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    public FaultType getFaultType() { return faultType; }
    public void setFaultType(FaultType faultType) { this.faultType = faultType; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }
    public LocalDateTime getDataStartTime() { return dataStartTime; }
    public void setDataStartTime(LocalDateTime dataStartTime) { this.dataStartTime = dataStartTime; }
    public LocalDateTime getDataEndTime() { return dataEndTime; }
    public void setDataEndTime(LocalDateTime dataEndTime) { this.dataEndTime = dataEndTime; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public List<DiagnosisIndicator> getIndicators() { return indicators; }
    public void setIndicators(List<DiagnosisIndicator> indicators) { this.indicators = indicators; }
    public String getDiagnosisSummary() { return diagnosisSummary; }
    public void setDiagnosisSummary(String diagnosisSummary) { this.diagnosisSummary = diagnosisSummary; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public static class Builder {
        private String containerId;
        private FaultType faultType;
        private Severity severity;
        private double confidence;
        private LocalDateTime analysisTime;
        private LocalDateTime dataStartTime;
        private LocalDateTime dataEndTime;
        private int totalRecords;
        private List<DiagnosisIndicator> indicators;
        private String diagnosisSummary;
        private String suggestion;

        public Builder containerId(String v) { this.containerId = v; return this; }
        public Builder faultType(FaultType v) { this.faultType = v; return this; }
        public Builder severity(Severity v) { this.severity = v; return this; }
        public Builder confidence(double v) { this.confidence = v; return this; }
        public Builder analysisTime(LocalDateTime v) { this.analysisTime = v; return this; }
        public Builder dataStartTime(LocalDateTime v) { this.dataStartTime = v; return this; }
        public Builder dataEndTime(LocalDateTime v) { this.dataEndTime = v; return this; }
        public Builder totalRecords(int v) { this.totalRecords = v; return this; }
        public Builder indicators(List<DiagnosisIndicator> v) { this.indicators = v; return this; }
        public Builder diagnosisSummary(String v) { this.diagnosisSummary = v; return this; }
        public Builder suggestion(String v) { this.suggestion = v; return this; }
        public DiagnosisResult build() { return new DiagnosisResult(this); }
    }
}
