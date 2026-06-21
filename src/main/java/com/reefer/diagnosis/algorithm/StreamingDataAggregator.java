package com.reefer.diagnosis.algorithm;

import com.reefer.diagnosis.model.TemperatureDataSummary;
import com.reefer.diagnosis.model.TemperatureRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;

@Component
public class StreamingDataAggregator {

    private static final int DEFAULT_RECENT_WINDOW_SIZE = 200;

    private final int recentWindowSize;

    public StreamingDataAggregator() {
        this(DEFAULT_RECENT_WINDOW_SIZE);
    }

    public StreamingDataAggregator(int recentWindowSize) {
        this.recentWindowSize = recentWindowSize;
    }

    public Aggregator createAggregator(String containerId) {
        return new Aggregator(containerId, recentWindowSize);
    }

    public static class Aggregator {
        private final String containerId;
        private final int windowSize;

        private int totalRecords = 0;
        private LocalDateTime firstTime = null;
        private LocalDateTime lastTime = null;

        private int tempCount = 0;
        private double firstTemp = Double.NaN;
        private double lastTemp = Double.NaN;
        private double maxTemp = Double.NaN;
        private double minTemp = Double.NaN;
        private LocalDateTime firstTempTime = null;
        private LocalDateTime lastTempTime = null;

        private int currentCount = 0;
        private double currentMean = 0.0;
        private double currentM2 = 0.0;
        private double maxCurrent = Double.NaN;
        private double minCurrent = Double.NaN;

        private int ambientCount = 0;
        private double ambientMean = 0.0;

        private boolean compressorRunning = false;

        private final Deque<WindowPoint> recentWindow;

        public Aggregator(String containerId, int windowSize) {
            this.containerId = containerId;
            this.windowSize = windowSize;
            this.recentWindow = new ArrayDeque<>(windowSize + 1);
        }

        public void accept(TemperatureRecord record) {
            totalRecords++;

            if (record.getTimestamp() != null) {
                if (firstTime == null) {
                    firstTime = record.getTimestamp();
                }
                lastTime = record.getTimestamp();
            }

            if (record.getTemperature() != null && !record.getTemperature().isNaN()) {
                double t = record.getTemperature();
                if (tempCount == 0) {
                    firstTemp = t;
                    firstTempTime = record.getTimestamp();
                    maxTemp = t;
                    minTemp = t;
                } else {
                    if (t > maxTemp) maxTemp = t;
                    if (t < minTemp) minTemp = t;
                }
                lastTemp = t;
                lastTempTime = record.getTimestamp();
                tempCount++;

                if (record.getTimestamp() != null) {
                    recentWindow.addLast(new WindowPoint(record.getTimestamp(), t));
                    if (recentWindow.size() > windowSize) {
                        recentWindow.pollFirst();
                    }
                }
            }

            if (record.getCompressorCurrent() != null
                    && !record.getCompressorCurrent().isNaN()
                    && record.getCompressorCurrent() >= 0) {
                double c = record.getCompressorCurrent();
                currentCount++;
                double delta = c - currentMean;
                currentMean += delta / currentCount;
                double delta2 = c - currentMean;
                currentM2 += delta * delta2;

                if (currentCount == 1) {
                    maxCurrent = c;
                    minCurrent = c;
                } else {
                    if (c > maxCurrent) maxCurrent = c;
                    if (c < minCurrent) minCurrent = c;
                }
            }

            if (record.getAmbientTemperature() != null
                    && !record.getAmbientTemperature().isNaN()) {
                double a = record.getAmbientTemperature();
                ambientCount++;
                double delta = a - ambientMean;
                ambientMean += delta / ambientCount;
            }

            if (!compressorRunning) {
                compressorRunning = isCompressorRunningRecord(record);
            }
        }

        private boolean isCompressorRunningRecord(TemperatureRecord record) {
            String status = record.getCompressorStatus();
            if (status != null) {
                String s = status.trim().toLowerCase();
                if (s.equals("run") || s.equals("running") || s.equals("on")
                        || s.equals("1") || s.equals("运行") || s.equals("开")) {
                    return true;
                }
            }
            Double current = record.getCompressorCurrent();
            if (current != null && !current.isNaN()) {
                return current > 2.0;
            }
            return false;
        }

        public TemperatureDataSummary buildSummary() {
            TemperatureDataSummary summary = new TemperatureDataSummary();
            summary.setContainerId(containerId);
            summary.setTotalRecords(totalRecords);
            summary.setStartTime(firstTime);
            summary.setEndTime(lastTime);

            summary.setValidTempCount(tempCount);
            summary.setStartTemperature(firstTemp);
            summary.setEndTemperature(lastTemp);
            summary.setMaxTemperature(maxTemp);
            summary.setMinTemperature(minTemp);

            summary.setValidCurrentCount(currentCount);
            summary.setAvgCurrent(currentCount > 0 ? currentMean : -1.0);
            double variance = currentCount > 1 ? currentM2 / currentCount : 0.0;
            summary.setCurrentVariance(variance);
            summary.setMaxCurrent(maxCurrent);
            summary.setMinCurrent(minCurrent);

            summary.setValidAmbientCount(ambientCount);
            summary.setAvgAmbient(ambientCount > 0 ? ambientMean : -1.0);

            summary.setCompressorEverRunning(compressorRunning);

            if (!recentWindow.isEmpty()) {
                WindowPoint first = recentWindow.peekFirst();
                WindowPoint last = recentWindow.peekLast();
                summary.setRecentWindowSize(recentWindow.size());
                summary.setRecentStartTime(first.time);
                summary.setRecentStartTemperature(first.temperature);
                summary.setRecentEndTime(last.time);
                summary.setRecentEndTemperature(last.temperature);
            } else {
                summary.setRecentWindowSize(0);
            }

            return summary;
        }

        private static class WindowPoint {
            final LocalDateTime time;
            final double temperature;

            WindowPoint(LocalDateTime time, double temperature) {
                this.time = time;
                this.temperature = temperature;
            }
        }
    }
}
