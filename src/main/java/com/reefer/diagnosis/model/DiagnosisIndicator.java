package com.reefer.diagnosis.model;

public class DiagnosisIndicator {

    private String name;
    private String description;
    private Object value;
    private Double threshold;
    private boolean abnormal;
    private Double weight;

    public DiagnosisIndicator() {}

    private DiagnosisIndicator(Builder b) {
        this.name = b.name;
        this.description = b.description;
        this.value = b.value;
        this.threshold = b.threshold;
        this.abnormal = b.abnormal;
        this.weight = b.weight;
    }

    public static Builder builder() { return new Builder(); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
    public boolean isAbnormal() { return abnormal; }
    public void setAbnormal(boolean abnormal) { this.abnormal = abnormal; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public static class Builder {
        private String name;
        private String description;
        private Object value;
        private Double threshold;
        private boolean abnormal;
        private Double weight;

        public Builder name(String v) { this.name = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder value(Object v) { this.value = v; return this; }
        public Builder threshold(Double v) { this.threshold = v; return this; }
        public Builder abnormal(boolean v) { this.abnormal = v; return this; }
        public Builder weight(Double v) { this.weight = v; return this; }
        public DiagnosisIndicator build() { return new DiagnosisIndicator(this); }
    }
}
