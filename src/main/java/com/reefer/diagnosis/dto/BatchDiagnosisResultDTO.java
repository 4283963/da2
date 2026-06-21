package com.reefer.diagnosis.dto;

import com.reefer.diagnosis.model.DiagnosisResult;

import java.util.List;

public class BatchDiagnosisResultDTO {

    private int totalContainers;
    private int normalCount;
    private int faultCount;
    private List<DiagnosisResult> results;

    public BatchDiagnosisResultDTO() {}

    private BatchDiagnosisResultDTO(Builder b) {
        this.totalContainers = b.totalContainers;
        this.normalCount = b.normalCount;
        this.faultCount = b.faultCount;
        this.results = b.results;
    }

    public static Builder builder() { return new Builder(); }

    public int getTotalContainers() { return totalContainers; }
    public void setTotalContainers(int totalContainers) { this.totalContainers = totalContainers; }
    public int getNormalCount() { return normalCount; }
    public void setNormalCount(int normalCount) { this.normalCount = normalCount; }
    public int getFaultCount() { return faultCount; }
    public void setFaultCount(int faultCount) { this.faultCount = faultCount; }
    public List<DiagnosisResult> getResults() { return results; }
    public void setResults(List<DiagnosisResult> results) { this.results = results; }

    public static class Builder {
        private int totalContainers;
        private int normalCount;
        private int faultCount;
        private List<DiagnosisResult> results;

        public Builder totalContainers(int v) { this.totalContainers = v; return this; }
        public Builder normalCount(int v) { this.normalCount = v; return this; }
        public Builder faultCount(int v) { this.faultCount = v; return this; }
        public Builder results(List<DiagnosisResult> v) { this.results = v; return this; }
        public BatchDiagnosisResultDTO build() { return new BatchDiagnosisResultDTO(this); }
    }
}
