package com.reefer.diagnosis.algorithm;

import com.reefer.diagnosis.config.DiagnosisProperties;
import com.reefer.diagnosis.model.DiagnosisResult;
import com.reefer.diagnosis.model.FaultType;
import com.reefer.diagnosis.model.TemperatureDataSummary;
import com.reefer.diagnosis.model.TemperatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreamingDataAggregatorTest {

    private StreamingDataAggregator streamingAggregator;
    private FaultDiagnosisAlgorithm algorithm;
    private DiagnosisProperties properties;

    @BeforeEach
    void setUp() {
        streamingAggregator = new StreamingDataAggregator(200);

        DiagnosisProperties.TemperatureThreshold tempThreshold =
                new DiagnosisProperties.TemperatureThreshold();
        tempThreshold.setRapidRiseRate(2.0);
        tempThreshold.setModerateRiseRate(0.8);
        tempThreshold.setDangerTemp(-5.0);
        tempThreshold.setTrendWindowMinutes(60);

        DiagnosisProperties.CompressorThreshold compThreshold =
                new DiagnosisProperties.CompressorThreshold();
        compThreshold.setNormalMinCurrent(8.0);
        compThreshold.setNormalMaxCurrent(18.0);
        compThreshold.setMotorBurnLowCurrent(2.0);
        compThreshold.setMotorBurnHighCurrent(25.0);
        compThreshold.setCurrentFluctuationThreshold(5.0);

        properties = new DiagnosisProperties();
        properties.setTemperature(tempThreshold);
        properties.setCompressor(compThreshold);

        algorithm = new FaultDiagnosisAlgorithm(properties);
    }

    @Test
    void testStreamingAggregationMatchesFullCalculation() {
        String cid = "TEST001";
        List<TemperatureRecord> records = generateNormalRecords(cid, 100);

        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
        for (TemperatureRecord r : records) {
            agg.accept(r);
        }
        TemperatureDataSummary summary = agg.buildSummary();

        assertEquals(100, summary.getTotalRecords());
        assertEquals(100, summary.getValidTempCount());
        assertEquals(100, summary.getValidCurrentCount());

        double expectedAvgCurrent = records.stream()
                .mapToDouble(TemperatureRecord::getCompressorCurrent)
                .average()
                .orElse(0);
        assertEquals(expectedAvgCurrent, summary.getAvgCurrent(), 0.001,
                "流式平均电流应与全量计算一致");

        double expectedMaxTemp = records.stream()
                .mapToDouble(TemperatureRecord::getTemperature)
                .max()
                .orElse(0);
        assertEquals(expectedMaxTemp, summary.getMaxTemperature(), 0.001,
                "流式最高温度应与全量计算一致");

        double expectedStdDev = calculateStdDev(records, expectedAvgCurrent);
        assertEquals(expectedStdDev, summary.getCurrentStdDev(), 0.001,
                "流式电流标准差应与全量计算一致");
    }

    @Test
    void testStreamingDiagnosisNormal() {
        String cid = "TEST002";
        List<TemperatureRecord> records = generateNormalRecords(cid, 50);

        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
        for (TemperatureRecord r : records) {
            agg.accept(r);
        }
        TemperatureDataSummary summary = agg.buildSummary();
        DiagnosisResult streamingResult = algorithm.diagnoseFromSummary(cid, summary);

        DiagnosisResult fullResult = algorithm.diagnose(cid, records);

        assertEquals(fullResult.getFaultType(), streamingResult.getFaultType(),
                "流式诊断故障类型应与全量诊断一致");
        assertEquals(fullResult.getSeverity(), streamingResult.getSeverity(),
                "流式诊断严重程度应与全量诊断一致");
        assertEquals(50, streamingResult.getTotalRecords());
    }

    @Test
    void testStreamingDiagnosisRefrigerantLeak() {
        String cid = "TEST003";
        List<TemperatureRecord> records = generateRefrigerantLeakRecords(cid, 48);

        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
        for (TemperatureRecord r : records) {
            agg.accept(r);
        }
        TemperatureDataSummary summary = agg.buildSummary();
        DiagnosisResult result = algorithm.diagnoseFromSummary(cid, summary);

        assertEquals(FaultType.REFRIGERANT_LEAK, result.getFaultType(),
                "温度缓慢升高+电流正常应诊断为漏氟");
        assertNotNull(result.getDiagnosisSummary());
        assertTrue(result.getConfidence() > 0.6);
    }

    @Test
    void testStreamingDiagnosisMotorBurnedZeroCurrent() {
        String cid = "TEST004";
        List<TemperatureRecord> records = generateMotorBurnedZeroCurrentRecords(cid, 24);

        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
        for (TemperatureRecord r : records) {
            agg.accept(r);
        }
        TemperatureDataSummary summary = agg.buildSummary();
        DiagnosisResult result = algorithm.diagnoseFromSummary(cid, summary);

        assertEquals(FaultType.MOTOR_BURNED, result.getFaultType(),
                "零电流+温度快速升高应诊断为电机烧毁");
        assertTrue(result.getConfidence() > 0.8);
    }

    @Test
    void testStreamingEmptyData() {
        String cid = "TEST005";
        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
        TemperatureDataSummary summary = agg.buildSummary();

        DiagnosisResult result = algorithm.diagnoseFromSummary(cid, summary);
        assertEquals(FaultType.UNKNOWN, result.getFaultType());
        assertEquals(0, result.getTotalRecords());
    }

    @Test
    void testStreamingLargeDatasetMemoryEfficiency() {
        String cid = "TEST_LARGE";
        int largeCount = 10000;
        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);

        LocalDateTime base = LocalDateTime.of(2025, 1, 1, 0, 0);
        for (int i = 0; i < largeCount; i++) {
            TemperatureRecord record = TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 10))
                    .temperature(-18.0 + Math.random() * 0.5)
                    .ambientTemperature(30.0)
                    .compressorCurrent(12.0 + Math.random() * 2)
                    .compressorStatus("RUN")
                    .build();
            agg.accept(record);
        }

        TemperatureDataSummary summary = agg.buildSummary();
        assertEquals(largeCount, summary.getTotalRecords());
        assertTrue(summary.getAvgCurrent() > 10 && summary.getAvgCurrent() < 16);
        assertTrue(summary.getMaxTemperature() > -18);
    }

    private double calculateStdDev(List<TemperatureRecord> records, double mean) {
        double sumSq = 0;
        int count = 0;
        for (TemperatureRecord r : records) {
            if (r.getCompressorCurrent() != null) {
                sumSq += Math.pow(r.getCompressorCurrent() - mean, 2);
                count++;
            }
        }
        return count > 1 ? Math.sqrt(sumSq / count) : 0.0;
    }

    private List<TemperatureRecord> generateNormalRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 0, 0);
        double startTemp = -18.0;
        for (int i = 0; i < hours; i++) {
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(startTemp + (Math.random() - 0.5) * 0.4)
                    .setTemperature(-18.0)
                    .ambientTemperature(30.0)
                    .compressorCurrent(12.0 + Math.random() * 2)
                    .compressorVoltage(380.0)
                    .compressorStatus("RUN")
                    .build());
        }
        records.sort(Comparator.comparing(TemperatureRecord::getTimestamp));
        return records;
    }

    private List<TemperatureRecord> generateRefrigerantLeakRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 0, 0);
        double startTemp = -18.0;
        double risePerHour = 1.2;
        for (int i = 0; i < hours; i++) {
            double temp = startTemp + risePerHour * i + (Math.random() - 0.5) * 0.3;
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(temp)
                    .setTemperature(-18.0)
                    .ambientTemperature(32.0)
                    .compressorCurrent(13.0 + Math.random() * 1.5)
                    .compressorVoltage(380.0)
                    .compressorStatus("RUN")
                    .build());
        }
        records.sort(Comparator.comparing(TemperatureRecord::getTimestamp));
        return records;
    }

    private List<TemperatureRecord> generateMotorBurnedZeroCurrentRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2025, 6, 1, 0, 0);
        double startTemp = -18.0;
        double risePerHour = 3.5;
        for (int i = 0; i < hours; i++) {
            double temp = startTemp + risePerHour * i;
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(temp)
                    .setTemperature(-18.0)
                    .ambientTemperature(35.0)
                    .compressorCurrent(0.5)
                    .compressorVoltage(0.0)
                    .compressorStatus("STOP")
                    .build());
        }
        records.sort(Comparator.comparing(TemperatureRecord::getTimestamp));
        return records;
    }
}
