package com.reefer.diagnosis.dto;

import com.reefer.diagnosis.model.TemperatureRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class DirectDiagnosisRequest {

    @NotBlank(message = "集装箱编号不能为空")
    private String containerId;

    private String voyageNumber;

    @NotEmpty(message = "温度记录不能为空")
    @Valid
    private List<TemperatureRecord> records;

    public DirectDiagnosisRequest() {}

    private DirectDiagnosisRequest(Builder b) {
        this.containerId = b.containerId;
        this.voyageNumber = b.voyageNumber;
        this.records = b.records;
    }

    public static Builder builder() { return new Builder(); }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    public String getVoyageNumber() { return voyageNumber; }
    public void setVoyageNumber(String voyageNumber) { this.voyageNumber = voyageNumber; }
    public List<TemperatureRecord> getRecords() { return records; }
    public void setRecords(List<TemperatureRecord> records) { this.records = records; }

    public static class Builder {
        private String containerId;
        private String voyageNumber;
        private List<TemperatureRecord> records;

        public Builder containerId(String v) { this.containerId = v; return this; }
        public Builder voyageNumber(String v) { this.voyageNumber = v; return this; }
        public Builder records(List<TemperatureRecord> v) { this.records = v; return this; }
        public DirectDiagnosisRequest build() { return new DirectDiagnosisRequest(this); }
    }
}
