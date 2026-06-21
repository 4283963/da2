package com.reefer.diagnosis.model;

import java.time.LocalDateTime;

public class TemperatureDataSummary {

    private String containerId;
    private int totalRecords;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private int validTempCount;
    private double startTemperature;
    private double endTemperature;
    private double maxTemperature;
    private double minTemperature;

    private int validCurrentCount;
    private double avgCurrent;
    private double currentVariance;
    private double maxCurrent;
    private double minCurrent;

    private int validAmbientCount;
    private double avgAmbient;

    private boolean compressorEverRunning;
    private int recentWindowSize;
    private double recentStartTemperature;
    private LocalDateTime recentStartTime;
    private double recentEndTemperature;
    private LocalDateTime recentEndTime;

    public TemperatureDataSummary() {
        this.totalRecords = 0;
        this.validTempCount = 0;
        this.startTemperature = Double.NaN;
        this.endTemperature = Double.NaN;
        this.maxTemperature = Double.NaN;
        this.minTemperature = Double.NaN;
        this.validCurrentCount = 0;
        this.avgCurrent = 0.0;
        this.currentVariance = 0.0;
        this.maxCurrent = Double.NaN;
        this.minCurrent = Double.NaN;
        this.validAmbientCount = 0;
        this.avgAmbient = 0.0;
        this.compressorEverRunning = false;
        this.recentWindowSize = 0;
        this.recentStartTemperature = Double.NaN;
        this.recentEndTemperature = Double.NaN;
    }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getValidTempCount() { return validTempCount; }
    public void setValidTempCount(int validTempCount) { this.validTempCount = validTempCount; }

    public double getStartTemperature() { return startTemperature; }
    public void setStartTemperature(double startTemperature) { this.startTemperature = startTemperature; }

    public double getEndTemperature() { return endTemperature; }
    public void setEndTemperature(double endTemperature) { this.endTemperature = endTemperature; }

    public double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(double maxTemperature) { this.maxTemperature = maxTemperature; }

    public double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(double minTemperature) { this.minTemperature = minTemperature; }

    public int getValidCurrentCount() { return validCurrentCount; }
    public void setValidCurrentCount(int validCurrentCount) { this.validCurrentCount = validCurrentCount; }

    public double getAvgCurrent() { return avgCurrent; }
    public void setAvgCurrent(double avgCurrent) { this.avgCurrent = avgCurrent; }

    public double getCurrentStdDev() { return Math.sqrt(currentVariance); }

    public double getCurrentVariance() { return currentVariance; }
    public void setCurrentVariance(double currentVariance) { this.currentVariance = currentVariance; }

    public double getMaxCurrent() { return maxCurrent; }
    public void setMaxCurrent(double maxCurrent) { this.maxCurrent = maxCurrent; }

    public double getMinCurrent() { return minCurrent; }
    public void setMinCurrent(double minCurrent) { this.minCurrent = minCurrent; }

    public int getValidAmbientCount() { return validAmbientCount; }
    public void setValidAmbientCount(int validAmbientCount) { this.validAmbientCount = validAmbientCount; }

    public double getAvgAmbient() { return avgAmbient; }
    public void setAvgAmbient(double avgAmbient) { this.avgAmbient = avgAmbient; }

    public boolean isCompressorEverRunning() { return compressorEverRunning; }
    public void setCompressorEverRunning(boolean compressorEverRunning) { this.compressorEverRunning = compressorEverRunning; }

    public int getRecentWindowSize() { return recentWindowSize; }
    public void setRecentWindowSize(int recentWindowSize) { this.recentWindowSize = recentWindowSize; }

    public double getRecentStartTemperature() { return recentStartTemperature; }
    public void setRecentStartTemperature(double recentStartTemperature) { this.recentStartTemperature = recentStartTemperature; }

    public LocalDateTime getRecentStartTime() { return recentStartTime; }
    public void setRecentStartTime(LocalDateTime recentStartTime) { this.recentStartTime = recentStartTime; }

    public double getRecentEndTemperature() { return recentEndTemperature; }
    public void setRecentEndTemperature(double recentEndTemperature) { this.recentEndTemperature = recentEndTemperature; }

    public LocalDateTime getRecentEndTime() { return recentEndTime; }
    public void setRecentEndTime(LocalDateTime recentEndTime) { this.recentEndTime = recentEndTime; }
}
