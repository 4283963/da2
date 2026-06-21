package com.reefer.diagnosis.model;

import java.util.ArrayList;
import java.util.List;

public class ContainerDataBatch {

    private String containerId;
    private String voyageNumber;
    private String sourceFileName;
    private List<TemperatureRecord> records = new ArrayList<>();
    private int recordCount;

    public ContainerDataBatch() {}

    private ContainerDataBatch(Builder b) {
        this.containerId = b.containerId;
        this.voyageNumber = b.voyageNumber;
        this.sourceFileName = b.sourceFileName;
        this.records = b.records != null ? b.records : new ArrayList<>();
        this.recordCount = b.recordCount;
    }

    public static Builder builder() { return new Builder(); }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    public String getVoyageNumber() { return voyageNumber; }
    public void setVoyageNumber(String voyageNumber) { this.voyageNumber = voyageNumber; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public List<TemperatureRecord> getRecords() { return records; }
    public void setRecords(List<TemperatureRecord> records) { this.records = records; }
    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    public static class Builder {
        private String containerId;
        private String voyageNumber;
        private String sourceFileName;
        private List<TemperatureRecord> records;
        private int recordCount;

        public Builder containerId(String v) { this.containerId = v; return this; }
        public Builder voyageNumber(String v) { this.voyageNumber = v; return this; }
        public Builder sourceFileName(String v) { this.sourceFileName = v; return this; }
        public Builder records(List<TemperatureRecord> v) { this.records = v; return this; }
        public Builder recordCount(int v) { this.recordCount = v; return this; }
        public ContainerDataBatch build() { return new ContainerDataBatch(this); }
    }
}
