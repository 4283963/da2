package com.reefer.diagnosis.model;

import java.time.LocalDateTime;

public class TemperatureRecord {

    private String containerId;
    private LocalDateTime timestamp;
    private Double temperature;
    private Double setTemperature;
    private Double ambientTemperature;
    private Double evaporatorTemperature;
    private Double condenserTemperature;
    private Double compressorCurrent;
    private Double compressorVoltage;
    private Double suctionPressure;
    private Double dischargePressure;
    private String compressorStatus;
    private String defrostStatus;
    private String alarmCode;
    private String remark;

    public TemperatureRecord() {}

    private TemperatureRecord(Builder b) {
        this.containerId = b.containerId;
        this.timestamp = b.timestamp;
        this.temperature = b.temperature;
        this.setTemperature = b.setTemperature;
        this.ambientTemperature = b.ambientTemperature;
        this.evaporatorTemperature = b.evaporatorTemperature;
        this.condenserTemperature = b.condenserTemperature;
        this.compressorCurrent = b.compressorCurrent;
        this.compressorVoltage = b.compressorVoltage;
        this.suctionPressure = b.suctionPressure;
        this.dischargePressure = b.dischargePressure;
        this.compressorStatus = b.compressorStatus;
        this.defrostStatus = b.defrostStatus;
        this.alarmCode = b.alarmCode;
        this.remark = b.remark;
    }

    public static Builder builder() { return new Builder(); }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getSetTemperature() { return setTemperature; }
    public void setSetTemperature(Double setTemperature) { this.setTemperature = setTemperature; }
    public Double getAmbientTemperature() { return ambientTemperature; }
    public void setAmbientTemperature(Double ambientTemperature) { this.ambientTemperature = ambientTemperature; }
    public Double getEvaporatorTemperature() { return evaporatorTemperature; }
    public void setEvaporatorTemperature(Double evaporatorTemperature) { this.evaporatorTemperature = evaporatorTemperature; }
    public Double getCondenserTemperature() { return condenserTemperature; }
    public void setCondenserTemperature(Double condenserTemperature) { this.condenserTemperature = condenserTemperature; }
    public Double getCompressorCurrent() { return compressorCurrent; }
    public void setCompressorCurrent(Double compressorCurrent) { this.compressorCurrent = compressorCurrent; }
    public Double getCompressorVoltage() { return compressorVoltage; }
    public void setCompressorVoltage(Double compressorVoltage) { this.compressorVoltage = compressorVoltage; }
    public Double getSuctionPressure() { return suctionPressure; }
    public void setSuctionPressure(Double suctionPressure) { this.suctionPressure = suctionPressure; }
    public Double getDischargePressure() { return dischargePressure; }
    public void setDischargePressure(Double dischargePressure) { this.dischargePressure = dischargePressure; }
    public String getCompressorStatus() { return compressorStatus; }
    public void setCompressorStatus(String compressorStatus) { this.compressorStatus = compressorStatus; }
    public String getDefrostStatus() { return defrostStatus; }
    public void setDefrostStatus(String defrostStatus) { this.defrostStatus = defrostStatus; }
    public String getAlarmCode() { return alarmCode; }
    public void setAlarmCode(String alarmCode) { this.alarmCode = alarmCode; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static class Builder {
        private String containerId;
        private LocalDateTime timestamp;
        private Double temperature;
        private Double setTemperature;
        private Double ambientTemperature;
        private Double evaporatorTemperature;
        private Double condenserTemperature;
        private Double compressorCurrent;
        private Double compressorVoltage;
        private Double suctionPressure;
        private Double dischargePressure;
        private String compressorStatus;
        private String defrostStatus;
        private String alarmCode;
        private String remark;

        public Builder containerId(String v) { this.containerId = v; return this; }
        public Builder timestamp(LocalDateTime v) { this.timestamp = v; return this; }
        public Builder temperature(Double v) { this.temperature = v; return this; }
        public Builder setTemperature(Double v) { this.setTemperature = v; return this; }
        public Builder ambientTemperature(Double v) { this.ambientTemperature = v; return this; }
        public Builder evaporatorTemperature(Double v) { this.evaporatorTemperature = v; return this; }
        public Builder condenserTemperature(Double v) { this.condenserTemperature = v; return this; }
        public Builder compressorCurrent(Double v) { this.compressorCurrent = v; return this; }
        public Builder compressorVoltage(Double v) { this.compressorVoltage = v; return this; }
        public Builder suctionPressure(Double v) { this.suctionPressure = v; return this; }
        public Builder dischargePressure(Double v) { this.dischargePressure = v; return this; }
        public Builder compressorStatus(String v) { this.compressorStatus = v; return this; }
        public Builder defrostStatus(String v) { this.defrostStatus = v; return this; }
        public Builder alarmCode(String v) { this.alarmCode = v; return this; }
        public Builder remark(String v) { this.remark = v; return this; }
        public TemperatureRecord build() { return new TemperatureRecord(this); }
    }
}
