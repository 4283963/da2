package com.reefer.diagnosis.algorithm;

import com.reefer.diagnosis.config.DiagnosisProperties;
import com.reefer.diagnosis.model.DiagnosisResult;
import com.reefer.diagnosis.model.FaultType;
import com.reefer.diagnosis.model.Severity;
import com.reefer.diagnosis.model.TemperatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FaultDiagnosisAlgorithmTest {

    private FaultDiagnosisAlgorithm algorithm;
    private DiagnosisProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DiagnosisProperties();
        DiagnosisProperties.TemperatureThreshold tempProps = new DiagnosisProperties.TemperatureThreshold();
        tempProps.setRapidRiseRate(2.0);
        tempProps.setModerateRiseRate(0.8);
        tempProps.setMaxNormalTemp(-15.0);
        tempProps.setDangerTemp(-5.0);
        tempProps.setTrendWindowMinutes(30);
        properties.setTemperature(tempProps);

        DiagnosisProperties.CompressorThreshold compProps = new DiagnosisProperties.CompressorThreshold();
        compProps.setNormalMinCurrent(8.0);
        compProps.setNormalMaxCurrent(18.0);
        compProps.setMotorBurnLowCurrent(2.0);
        compProps.setMotorBurnHighCurrent(25.0);
        compProps.setCurrentFluctuationThreshold(5.0);
        properties.setCompressor(compProps);

        algorithm = new FaultDiagnosisAlgorithm(properties);
    }

    @Test
    @DisplayName("正常运行 - 温度稳定、电流正常")
    void testNormalOperation() {
        List<TemperatureRecord> records = generateNormalRecords("CONT001", 48);
        DiagnosisResult result = algorithm.diagnose("CONT001", records);

        assertNotNull(result);
        assertEquals(FaultType.NORMAL, result.getFaultType(), "正常运行应判定为NORMAL");
        assertEquals("CONT001", result.getContainerId());
        assertTrue(result.getConfidence() >= 0.7, "置信度应大于0.7");
        assertTrue(result.getIndicators().size() >= 5, "诊断指标数量应足够");
        assertNotNull(result.getDiagnosisSummary());
        assertNotNull(result.getSuggestion());
    }

    @Test
    @DisplayName("漏氟场景 - 温度缓慢升高，电流保持正常")
    void testRefrigerantLeak() {
        List<TemperatureRecord> records = generateLeakRecords("CONT002", 48);
        DiagnosisResult result = algorithm.diagnose("CONT002", records);

        assertNotNull(result);
        assertSame(result.getFaultType(), FaultType.REFRIGERANT_LEAK,
                "漏氟场景应判定为REFRIGERANT_LEAK，实际：" + result.getFaultType()
                        + "，摘要：" + result.getDiagnosisSummary());
        assertTrue(result.getConfidence() >= 0.6, "漏氟诊断置信度应>=0.6，实际：" + result.getConfidence());
        assertTrue(result.getSeverity() != Severity.LOW,
                "漏氟严重程度应至少为MEDIUM，实际：" + result.getSeverity());
    }

    @Test
    @DisplayName("电机烧毁 - 电流接近零、温度急剧升高")
    void testMotorBurnedZeroCurrent() {
        List<TemperatureRecord> records = generateMotorBurnedZeroCurrentRecords("CONT003", 36);
        DiagnosisResult result = algorithm.diagnose("CONT003", records);

        assertNotNull(result);
        assertSame(result.getFaultType(), FaultType.MOTOR_BURNED,
                "电机烧毁（零电流）应判定为MOTOR_BURNED，实际：" + result.getFaultType()
                        + "，摘要：" + result.getDiagnosisSummary());
        assertTrue(result.getConfidence() >= 0.7, "电机烧毁诊断置信度应>=0.7，实际：" + result.getConfidence());
    }

    @Test
    @DisplayName("电机烧毁 - 电流过高且剧烈波动")
    void testMotorBurnedHighCurrent() {
        List<TemperatureRecord> records = generateMotorBurnedHighCurrentRecords("CONT004", 36);
        DiagnosisResult result = algorithm.diagnose("CONT004", records);

        assertNotNull(result);
        assertSame(result.getFaultType(), FaultType.MOTOR_BURNED,
                "电机烧毁（过流）应判定为MOTOR_BURNED，实际：" + result.getFaultType()
                        + "，摘要：" + result.getDiagnosisSummary());
    }

    @Test
    @DisplayName("数据为空 - 应返回UNKNOWN并给出提示")
    void testEmptyData() {
        DiagnosisResult result = algorithm.diagnose("CONT005", new ArrayList<>());
        assertNotNull(result);
        assertEquals(FaultType.UNKNOWN, result.getFaultType());
        assertEquals(0.0, result.getConfidence());
        assertNotNull(result.getDiagnosisSummary());
    }

    @Test
    @DisplayName("仅有一条记录 - 应返回UNKNOWN")
    void testSingleRecord() {
        List<TemperatureRecord> records = new ArrayList<>();
        records.add(TemperatureRecord.builder()
                .containerId("CONT006")
                .timestamp(LocalDateTime.now())
                .temperature(-18.0)
                .compressorCurrent(12.0)
                .build());
        DiagnosisResult result = algorithm.diagnose("CONT006", records);
        assertEquals(FaultType.UNKNOWN, result.getFaultType());
    }

    private List<TemperatureRecord> generateNormalRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusHours(hours);
        double baseTemp = -18.0;
        double baseCurrent = 12.5;
        double ambient = 32.0;

        for (int i = 0; i < hours; i++) {
            double tempNoise = (Math.random() - 0.5) * 0.6;
            double currentNoise = (Math.random() - 0.5) * 1.5;
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(baseTemp + tempNoise)
                    .setTemperature(-18.0)
                    .ambientTemperature(ambient + (Math.random() - 0.5) * 2)
                    .compressorCurrent(baseCurrent + currentNoise)
                    .compressorVoltage(380.0)
                    .compressorStatus("RUN")
                    .build());
        }
        return records;
    }

    private List<TemperatureRecord> generateLeakRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusHours(hours);
        double startTemp = -18.0;
        double risePerHour = 1.2;
        double baseCurrent = 14.0;

        for (int i = 0; i < hours; i++) {
            double temp = startTemp + risePerHour * i + (Math.random() - 0.5) * 0.5;
            double currentNoise = (Math.random() - 0.5) * 1.2;
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(temp)
                    .setTemperature(-18.0)
                    .ambientTemperature(30.0 + Math.random() * 3)
                    .compressorCurrent(baseCurrent + currentNoise)
                    .compressorVoltage(380.0)
                    .compressorStatus("RUN")
                    .build());
        }
        return records;
    }

    private List<TemperatureRecord> generateMotorBurnedZeroCurrentRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusHours(hours);
        double startTemp = -18.0;
        double risePerHour = 3.5;

        for (int i = 0; i < hours; i++) {
            double temp = startTemp + risePerHour * i + (Math.random() - 0.5) * 0.8;
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(temp)
                    .setTemperature(-18.0)
                    .ambientTemperature(35.0)
                    .compressorCurrent(0.3 + Math.random() * 0.5)
                    .compressorVoltage(380.0)
                    .compressorStatus("STOP")
                    .build());
        }
        return records;
    }

    private List<TemperatureRecord> generateMotorBurnedHighCurrentRecords(String cid, int hours) {
        List<TemperatureRecord> records = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusHours(hours);
        double startTemp = -18.0;
        double risePerHour = 2.5;

        for (int i = 0; i < hours; i++) {
            double temp = startTemp + risePerHour * i + (Math.random() - 0.5) * 0.6;
            double current;
            if (i % 2 == 0) {
                current = 22.0 + Math.random() * 4;
            } else {
                current = 42.0 + Math.random() * 6;
            }
            records.add(TemperatureRecord.builder()
                    .containerId(cid)
                    .timestamp(base.plusMinutes((long) i * 60))
                    .temperature(temp)
                    .setTemperature(-18.0)
                    .ambientTemperature(33.0)
                    .compressorCurrent(current)
                    .compressorVoltage(380.0)
                    .compressorStatus("RUN")
                    .build());
        }
        return records;
    }
}
