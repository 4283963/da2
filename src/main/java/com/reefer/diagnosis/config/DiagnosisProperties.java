package com.reefer.diagnosis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "diagnosis")
public class DiagnosisProperties {

    private TemperatureThreshold temperature = new TemperatureThreshold();
    private CompressorThreshold compressor = new CompressorThreshold();

    public TemperatureThreshold getTemperature() { return temperature; }
    public void setTemperature(TemperatureThreshold temperature) { this.temperature = temperature; }
    public CompressorThreshold getCompressor() { return compressor; }
    public void setCompressor(CompressorThreshold compressor) { this.compressor = compressor; }

    public static class TemperatureThreshold {
        private double rapidRiseRate = 2.0;
        private double moderateRiseRate = 0.8;
        private double maxNormalTemp = -15.0;
        private double dangerTemp = -5.0;
        private int trendWindowMinutes = 30;

        public double getRapidRiseRate() { return rapidRiseRate; }
        public void setRapidRiseRate(double rapidRiseRate) { this.rapidRiseRate = rapidRiseRate; }
        public double getModerateRiseRate() { return moderateRiseRate; }
        public void setModerateRiseRate(double moderateRiseRate) { this.moderateRiseRate = moderateRiseRate; }
        public double getMaxNormalTemp() { return maxNormalTemp; }
        public void setMaxNormalTemp(double maxNormalTemp) { this.maxNormalTemp = maxNormalTemp; }
        public double getDangerTemp() { return dangerTemp; }
        public void setDangerTemp(double dangerTemp) { this.dangerTemp = dangerTemp; }
        public int getTrendWindowMinutes() { return trendWindowMinutes; }
        public void setTrendWindowMinutes(int trendWindowMinutes) { this.trendWindowMinutes = trendWindowMinutes; }
    }

    public static class CompressorThreshold {
        private double normalMinCurrent = 8.0;
        private double normalMaxCurrent = 18.0;
        private double motorBurnLowCurrent = 2.0;
        private double motorBurnHighCurrent = 25.0;
        private double currentFluctuationThreshold = 5.0;

        public double getNormalMinCurrent() { return normalMinCurrent; }
        public void setNormalMinCurrent(double normalMinCurrent) { this.normalMinCurrent = normalMinCurrent; }
        public double getNormalMaxCurrent() { return normalMaxCurrent; }
        public void setNormalMaxCurrent(double normalMaxCurrent) { this.normalMaxCurrent = normalMaxCurrent; }
        public double getMotorBurnLowCurrent() { return motorBurnLowCurrent; }
        public void setMotorBurnLowCurrent(double motorBurnLowCurrent) { this.motorBurnLowCurrent = motorBurnLowCurrent; }
        public double getMotorBurnHighCurrent() { return motorBurnHighCurrent; }
        public void setMotorBurnHighCurrent(double motorBurnHighCurrent) { this.motorBurnHighCurrent = motorBurnHighCurrent; }
        public double getCurrentFluctuationThreshold() { return currentFluctuationThreshold; }
        public void setCurrentFluctuationThreshold(double currentFluctuationThreshold) { this.currentFluctuationThreshold = currentFluctuationThreshold; }
    }
}
