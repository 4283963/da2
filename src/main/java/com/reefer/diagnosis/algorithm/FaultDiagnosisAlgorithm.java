package com.reefer.diagnosis.algorithm;

import com.reefer.diagnosis.config.DiagnosisProperties;
import com.reefer.diagnosis.model.DiagnosisIndicator;
import com.reefer.diagnosis.model.DiagnosisResult;
import com.reefer.diagnosis.model.FaultType;
import com.reefer.diagnosis.model.Severity;
import com.reefer.diagnosis.model.TemperatureDataSummary;
import com.reefer.diagnosis.model.TemperatureRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class FaultDiagnosisAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(FaultDiagnosisAlgorithm.class);

    private final DiagnosisProperties properties;

    public FaultDiagnosisAlgorithm(DiagnosisProperties properties) {
        this.properties = properties;
    }

    public DiagnosisResult diagnose(String containerId, List<TemperatureRecord> records) {
        if (records == null || records.isEmpty()) {
            return createEmptyResult(containerId, "数据为空，无法进行诊断");
        }

        List<TemperatureRecord> sortedRecords = new ArrayList<>();
        for (TemperatureRecord r : records) {
            if (r.getTimestamp() != null) {
                sortedRecords.add(r);
            }
        }
        sortedRecords.sort(Comparator.comparing(TemperatureRecord::getTimestamp));

        if (sortedRecords.size() < 2) {
            return createEmptyResult(containerId, "数据点不足（至少需要2条有效记录）");
        }

        List<DiagnosisIndicator> indicators = new ArrayList<>();

        double tempRiseRatePerHour = calculateTemperatureRiseRate(sortedRecords);
        indicators.add(DiagnosisIndicator.builder()
                .name("温度升高速率")
                .description("每小时平均温度变化")
                .value(String.format("%.3f ℃/h", tempRiseRatePerHour))
                .threshold(properties.getTemperature().getRapidRiseRate())
                .abnormal(tempRiseRatePerHour > properties.getTemperature().getModerateRiseRate())
                .weight(0.35)
                .build());

        double avgCurrent = calculateAverageCompressorCurrent(sortedRecords);
        double currentStdDev = calculateCurrentStdDev(sortedRecords, avgCurrent);
        indicators.add(DiagnosisIndicator.builder()
                .name("压缩机平均电流")
                .description("所有有效采样点电流平均值")
                .value(avgCurrent == -1 ? "无数据" : String.format("%.2f A", avgCurrent))
                .threshold(properties.getCompressor().getNormalMaxCurrent())
                .abnormal(avgCurrent != -1 && (avgCurrent < properties.getCompressor().getNormalMinCurrent()
                        || avgCurrent > properties.getCompressor().getNormalMaxCurrent()))
                .weight(0.30)
                .build());

        indicators.add(DiagnosisIndicator.builder()
                .name("电流波动标准差")
                .description("衡量电流稳定性")
                .value(String.format("%.2f", currentStdDev))
                .threshold(properties.getCompressor().getCurrentFluctuationThreshold())
                .abnormal(currentStdDev > properties.getCompressor().getCurrentFluctuationThreshold())
                .weight(0.10)
                .build());

        double avgAmbient = calculateAverageAmbient(sortedRecords);
        indicators.add(DiagnosisIndicator.builder()
                .name("平均环境温度")
                .description("外部环境温度均值")
                .value(avgAmbient == -1 ? "无数据" : String.format("%.2f ℃", avgAmbient))
                .threshold(45.0)
                .abnormal(avgAmbient > 45.0)
                .weight(0.10)
                .build());

        boolean compressorRunning = isCompressorRunning(sortedRecords);
        indicators.add(DiagnosisIndicator.builder()
                .name("压缩机运行状态")
                .description("根据电流和状态字段判断是否工作")
                .value(compressorRunning ? "运行中" : "未运行")
                .threshold(null)
                .abnormal(!compressorRunning)
                .weight(0.10)
                .build());

        double maxTemp = Double.NaN;
        for (TemperatureRecord r : sortedRecords) {
            if (r.getTemperature() != null && (Double.isNaN(maxTemp) || r.getTemperature() > maxTemp)) {
                maxTemp = r.getTemperature();
            }
        }
        indicators.add(DiagnosisIndicator.builder()
                .name("最高温度")
                .description("记录期内出现的最高箱内温度")
                .value(Double.isNaN(maxTemp) ? "无数据" : String.format("%.2f ℃", maxTemp))
                .threshold(properties.getTemperature().getDangerTemp())
                .abnormal(!Double.isNaN(maxTemp) && maxTemp > properties.getTemperature().getDangerTemp())
                .weight(0.05)
                .build());

        LocalDateTime dataStartTime = sortedRecords.get(0).getTimestamp();
        LocalDateTime dataEndTime = sortedRecords.get(sortedRecords.size() - 1).getTimestamp();
        int totalRecords = sortedRecords.size();

        return buildDiagnosisResult(containerId, dataStartTime, dataEndTime, totalRecords,
                indicators, tempRiseRatePerHour, avgCurrent, currentStdDev,
                compressorRunning, maxTemp);
    }

    public DiagnosisResult diagnoseFromSummary(String containerId, TemperatureDataSummary summary) {
        if (summary == null || summary.getValidTempCount() < 2) {
            return createEmptyResult(containerId, "数据为空或不足，无法进行诊断（流式聚合模式）");
        }

        List<DiagnosisIndicator> indicators = new ArrayList<>();

        double tempRiseRatePerHour = calculateTemperatureRiseRate(summary);
        indicators.add(DiagnosisIndicator.builder()
                .name("温度升高速率")
                .description("每小时平均温度变化（流式近似）")
                .value(String.format("%.3f ℃/h", tempRiseRatePerHour))
                .threshold(properties.getTemperature().getRapidRiseRate())
                .abnormal(tempRiseRatePerHour > properties.getTemperature().getModerateRiseRate())
                .weight(0.35)
                .build());

        double avgCurrent = summary.getValidCurrentCount() > 0 ? summary.getAvgCurrent() : -1;
        double currentStdDev = summary.getCurrentStdDev();
        indicators.add(DiagnosisIndicator.builder()
                .name("压缩机平均电流")
                .description("所有有效采样点电流平均值（流式）")
                .value(avgCurrent == -1 ? "无数据" : String.format("%.2f A", avgCurrent))
                .threshold(properties.getCompressor().getNormalMaxCurrent())
                .abnormal(avgCurrent != -1 && (avgCurrent < properties.getCompressor().getNormalMinCurrent()
                        || avgCurrent > properties.getCompressor().getNormalMaxCurrent()))
                .weight(0.30)
                .build());

        indicators.add(DiagnosisIndicator.builder()
                .name("电流波动标准差")
                .description("衡量电流稳定性（流式Welford算法）")
                .value(String.format("%.2f", currentStdDev))
                .threshold(properties.getCompressor().getCurrentFluctuationThreshold())
                .abnormal(currentStdDev > properties.getCompressor().getCurrentFluctuationThreshold())
                .weight(0.10)
                .build());

        double avgAmbient = summary.getValidAmbientCount() > 0 ? summary.getAvgAmbient() : -1;
        indicators.add(DiagnosisIndicator.builder()
                .name("平均环境温度")
                .description("外部环境温度均值（流式）")
                .value(avgAmbient == -1 ? "无数据" : String.format("%.2f ℃", avgAmbient))
                .threshold(45.0)
                .abnormal(avgAmbient > 45.0)
                .weight(0.10)
                .build());

        boolean compressorRunning = summary.isCompressorEverRunning();
        indicators.add(DiagnosisIndicator.builder()
                .name("压缩机运行状态")
                .description("根据电流和状态字段判断是否工作（流式）")
                .value(compressorRunning ? "运行中" : "未运行")
                .threshold(null)
                .abnormal(!compressorRunning)
                .weight(0.10)
                .build());

        double maxTemp = summary.getMaxTemperature();
        indicators.add(DiagnosisIndicator.builder()
                .name("最高温度")
                .description("记录期内出现的最高箱内温度（流式）")
                .value(Double.isNaN(maxTemp) ? "无数据" : String.format("%.2f ℃", maxTemp))
                .threshold(properties.getTemperature().getDangerTemp())
                .abnormal(!Double.isNaN(maxTemp) && maxTemp > properties.getTemperature().getDangerTemp())
                .weight(0.05)
                .build());

        return buildDiagnosisResult(containerId,
                summary.getStartTime(), summary.getEndTime(), summary.getTotalRecords(),
                indicators, tempRiseRatePerHour, avgCurrent, currentStdDev,
                compressorRunning, maxTemp);
    }

    private double calculateTemperatureRiseRate(TemperatureDataSummary summary) {
        if (summary.getValidTempCount() < 2 || Double.isNaN(summary.getStartTemperature())
                || Double.isNaN(summary.getEndTemperature())
                || summary.getStartTime() == null || summary.getEndTime() == null) {
            return 0.0;
        }

        long totalMinutes = Duration.between(summary.getStartTime(), summary.getEndTime()).toMinutes();
        if (totalMinutes <= 0) {
            return 0.0;
        }

        double overallRate = (summary.getEndTemperature() - summary.getStartTemperature())
                / (totalMinutes / 60.0);

        if (summary.getRecentWindowSize() >= 2
                && !Double.isNaN(summary.getRecentStartTemperature())
                && !Double.isNaN(summary.getRecentEndTemperature())
                && summary.getRecentStartTime() != null
                && summary.getRecentEndTime() != null) {
            long recentMinutes = Duration.between(
                    summary.getRecentStartTime(), summary.getRecentEndTime()).toMinutes();
            if (recentMinutes > 0) {
                double recentRate = (summary.getRecentEndTemperature() - summary.getRecentStartTemperature())
                        / (recentMinutes / 60.0);
                return recentRate * 0.6 + overallRate * 0.4;
            }
        }

        return overallRate;
    }

    private DiagnosisResult buildDiagnosisResult(String containerId,
                                                 LocalDateTime dataStartTime,
                                                 LocalDateTime dataEndTime,
                                                 int totalRecords,
                                                 List<DiagnosisIndicator> indicators,
                                                 double tempRiseRate,
                                                 double avgCurrent,
                                                 double currentStdDev,
                                                 boolean compressorRunning,
                                                 double maxTemp) {

        DiagnosisProperties.TemperatureThreshold tempProps = properties.getTemperature();
        DiagnosisProperties.CompressorThreshold compProps = properties.getCompressor();

        FaultType faultType = FaultType.NORMAL;
        Severity severity = Severity.LOW;
        double confidence = 0.0;
        StringBuilder summary = new StringBuilder();
        StringBuilder suggestion = new StringBuilder();

        boolean rapidRise = tempRiseRate > tempProps.getRapidRiseRate();
        boolean moderateRise = tempRiseRate > tempProps.getModerateRiseRate();
        boolean currentNearZero = avgCurrent != -1 && avgCurrent < compProps.getMotorBurnLowCurrent();
        boolean currentExcessive = avgCurrent != -1 && avgCurrent > compProps.getMotorBurnHighCurrent();
        boolean currentAbnormal = avgCurrent != -1 && (currentNearZero || currentExcessive);
        boolean currentNormalRange = avgCurrent != -1
                && avgCurrent >= compProps.getNormalMinCurrent()
                && avgCurrent <= compProps.getNormalMaxCurrent();
        boolean highFluctuation = currentStdDev > compProps.getCurrentFluctuationThreshold();

        if (!compressorRunning || currentNearZero) {
            faultType = FaultType.MOTOR_BURNED;
            confidence = currentNearZero ? 0.92 : 0.75;
            severity = currentNearZero ? Severity.CRITICAL : Severity.HIGH;
            summary.append("压缩机电机疑似烧毁：");
            if (currentNearZero) {
                summary.append(String.format("电流极低(%.2fA)，接近零值；", avgCurrent));
            }
            if (!compressorRunning) {
                summary.append("压缩机处于停机状态；");
            }
            if (rapidRise) {
                summary.append(String.format("温度急剧上升(%.2f℃/h)；", tempRiseRate));
            }
            suggestion.append("建议：立即检查压缩机电机绕组，测量三相电阻是否平衡，检查启动电容和接触器，必要时更换压缩机总成。");
        } else if (currentExcessive && highFluctuation) {
            faultType = FaultType.MOTOR_BURNED;
            confidence = 0.80;
            severity = Severity.HIGH;
            summary.append(String.format("压缩机电机疑似匝间短路：电流过高(%.2fA)且波动剧烈(标准差%.2f)，", avgCurrent, currentStdDev));
            if (rapidRise) {
                summary.append(String.format("同时温度快速上升(%.2f℃/h)；", tempRiseRate));
            }
            suggestion.append("建议：切断电源，用钳形表测量三相电流，检查电机绝缘电阻(MΩ表)，判断是否匝间短路或轴承抱死。");
        } else if (moderateRise && (currentNormalRange || avgCurrent == -1)) {
            faultType = FaultType.REFRIGERANT_LEAK;
            if (rapidRise) {
                confidence = 0.88;
                severity = Severity.HIGH;
            } else {
                confidence = 0.78;
                severity = Severity.MEDIUM;
            }
            summary.append(String.format("疑似制冷剂泄漏：温度以%.2f℃/h的速率升高，", tempRiseRate));
            if (currentNormalRange) {
                summary.append(String.format("但压缩机电流保持正常范围(%.2fA)，说明压缩机在工作但制冷效果下降；", avgCurrent));
            } else {
                summary.append("压缩机持续运行但箱温无法下降；");
            }
            suggestion.append("建议：用检漏仪（卤素/电子）检查所有管路接头、蒸发器、冷凝器是否有漏点，重点检查截止阀、膨胀阀、焊接处。确认后补漏、抽真空、按铭牌加注制冷剂。");
        } else if (moderateRise && currentAbnormal) {
            faultType = FaultType.REFRIGERANT_LEAK;
            confidence = 0.65;
            severity = Severity.MEDIUM;
            summary.append(String.format("温度上升(%.2f℃/h)且电流异常(%.2fA)，", tempRiseRate, avgCurrent));
            summary.append("初步判断为制冷剂泄漏，但不排除压缩机故障，需进一步检查；");
            suggestion.append("建议：先用压力表检测高低压侧静态压力判断是否缺氟，再结合电流判断压缩机状态。");
        } else if (rapidRise) {
            faultType = FaultType.UNKNOWN;
            confidence = 0.50;
            severity = Severity.HIGH;
            summary.append(String.format("温度快速上升(%.2f℃/h)，但电流数据不足以确诊具体故障；", tempRiseRate));
            suggestion.append("建议：人工全面检查制冷系统，包括视液镜、干燥过滤器、膨胀阀、风机等。");
        } else {
            faultType = FaultType.NORMAL;
            confidence = 0.85;
            severity = Severity.LOW;
            summary.append(String.format("冷藏箱运行状态正常：温度变化率%.2f℃/h，", tempRiseRate));
            if (avgCurrent != -1) {
                summary.append(String.format("平均电流%.2fA。", avgCurrent));
            }
            suggestion.append("建议：持续监控，定期记录数据，保证航行期间制冷系统稳定。");
        }

        if (!Double.isNaN(maxTemp) && maxTemp > tempProps.getDangerTemp()) {
            severity = Severity.CRITICAL;
            summary.insert(0, String.format("【危急】最高温度达%.2f℃已超过警戒线！", maxTemp));
        }

        return DiagnosisResult.builder()
                .containerId(containerId)
                .faultType(faultType)
                .severity(severity)
                .confidence(confidence)
                .analysisTime(LocalDateTime.now())
                .dataStartTime(dataStartTime)
                .dataEndTime(dataEndTime)
                .totalRecords(totalRecords)
                .indicators(indicators)
                .diagnosisSummary(summary.toString())
                .suggestion(suggestion.toString())
                .build();
    }

    private double calculateTemperatureRiseRate(List<TemperatureRecord> records) {
        List<TemperatureRecord> valid = new ArrayList<>();
        for (TemperatureRecord r : records) {
            if (r.getTemperature() != null) {
                valid.add(r);
            }
        }
        if (valid.size() < 2) {
            return 0.0;
        }

        int windowMinutes = properties.getTemperature().getTrendWindowMinutes();
        Duration totalSpan = Duration.between(valid.get(0).getTimestamp(), valid.get(valid.size() - 1).getTimestamp());
        long totalMinutes = totalSpan.toMinutes();

        if (totalMinutes <= 0) {
            return 0.0;
        }

        if (totalMinutes <= windowMinutes || valid.size() <= 20) {
            double tempDiff = valid.get(valid.size() - 1).getTemperature() - valid.get(0).getTemperature();
            return tempDiff / (totalMinutes / 60.0);
        }

        int startIdx = Math.max(0, valid.size() - valid.size() / 3);
        List<TemperatureRecord> recent = valid.subList(startIdx, valid.size());
        if (recent.size() >= 2) {
            Duration recentSpan = Duration.between(recent.get(0).getTimestamp(), recent.get(recent.size() - 1).getTimestamp());
            long recentMinutes = recentSpan.toMinutes();
            if (recentMinutes > 0) {
                double recentDiff = recent.get(recent.size() - 1).getTemperature() - recent.get(0).getTemperature();
                double recentRate = recentDiff / (recentMinutes / 60.0);
                double overallDiff = valid.get(valid.size() - 1).getTemperature() - valid.get(0).getTemperature();
                double overallRate = overallDiff / (totalMinutes / 60.0);
                return recentRate * 0.6 + overallRate * 0.4;
            }
        }

        double tempDiff = valid.get(valid.size() - 1).getTemperature() - valid.get(0).getTemperature();
        return tempDiff / (totalMinutes / 60.0);
    }

    private double calculateAverageCompressorCurrent(List<TemperatureRecord> records) {
        List<Double> currents = new ArrayList<>();
        for (TemperatureRecord r : records) {
            Double c = r.getCompressorCurrent();
            if (c != null && !c.isNaN() && c >= 0) {
                currents.add(c);
            }
        }
        if (currents.isEmpty()) {
            return -1;
        }
        double sum = 0;
        for (Double c : currents) {
            sum += c;
        }
        return sum / currents.size();
    }

    private double calculateCurrentStdDev(List<TemperatureRecord> records, double avgCurrent) {
        if (avgCurrent == -1) {
            return 0.0;
        }
        List<Double> currents = new ArrayList<>();
        for (TemperatureRecord r : records) {
            Double c = r.getCompressorCurrent();
            if (c != null && !c.isNaN() && c >= 0) {
                currents.add(c);
            }
        }
        if (currents.size() < 2) {
            return 0.0;
        }
        double sumSq = 0;
        for (Double c : currents) {
            sumSq += Math.pow(c - avgCurrent, 2);
        }
        return Math.sqrt(sumSq / currents.size());
    }

    private double calculateAverageAmbient(List<TemperatureRecord> records) {
        List<Double> ambients = new ArrayList<>();
        for (TemperatureRecord r : records) {
            Double a = r.getAmbientTemperature();
            if (a != null && !a.isNaN()) {
                ambients.add(a);
            }
        }
        if (ambients.isEmpty()) {
            return -1;
        }
        double sum = 0;
        for (Double a : ambients) {
            sum += a;
        }
        return sum / ambients.size();
    }

    private boolean isCompressorRunning(List<TemperatureRecord> records) {
        for (TemperatureRecord r : records) {
            String s = r.getCompressorStatus();
            if (s != null && (s.equalsIgnoreCase("RUN")
                    || s.equalsIgnoreCase("RUNNING")
                    || s.equalsIgnoreCase("ON")
                    || s.equalsIgnoreCase("1")
                    || s.equals("运行")
                    || s.equals("开"))) {
                return true;
            }
        }
        double threshold = properties.getCompressor().getNormalMinCurrent() * 0.5;
        for (TemperatureRecord r : records) {
            Double c = r.getCompressorCurrent();
            if (c != null && c > threshold) {
                return true;
            }
        }
        return false;
    }

    private DiagnosisResult createEmptyResult(String containerId, String reason) {
        return DiagnosisResult.builder()
                .containerId(containerId)
                .faultType(FaultType.UNKNOWN)
                .severity(Severity.MEDIUM)
                .confidence(0.0)
                .analysisTime(LocalDateTime.now())
                .totalRecords(0)
                .indicators(new ArrayList<>())
                .diagnosisSummary(reason)
                .suggestion("请提供有效的航行温度历史数据后重新诊断。")
                .build();
    }
}
