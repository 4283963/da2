package com.reefer.diagnosis.service;

import com.reefer.diagnosis.model.FaultType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaintenanceGuideService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceGuideService.class);

    private static final String GUIDE_FILE = "maintenance_guide.txt";

    private final Map<FaultType, List<String>> guideMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadGuideFromFile();
    }

    private void loadGuideFromFile() {
        try {
            ClassPathResource resource = new ClassPathResource(GUIDE_FILE);
            if (!resource.exists()) {
                log.warn("维修指南文件不存在: {}", GUIDE_FILE);
                loadDefaultGuides();
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                FaultType currentType = null;
                List<String> currentSteps = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    if (line.startsWith("[") && line.endsWith("]")) {
                        if (currentType != null && !currentSteps.isEmpty()) {
                            guideMap.put(currentType, currentSteps);
                        }

                        String typeName = line.substring(1, line.length() - 1).trim();
                        try {
                            currentType = FaultType.valueOf(typeName);
                            currentSteps = new ArrayList<>();
                        } catch (IllegalArgumentException e) {
                            log.warn("维修指南中存在未知故障类型: {}", typeName);
                            currentType = null;
                            currentSteps = new ArrayList<>();
                        }
                        continue;
                    }

                    if (currentType != null) {
                        String step = cleanStepLine(line);
                        if (step != null && !step.isEmpty()) {
                            currentSteps.add(step);
                        }
                    }
                }

                if (currentType != null && !currentSteps.isEmpty()) {
                    guideMap.put(currentType, currentSteps);
                }
            }

            log.info("维修指南加载完成，共 {} 种故障类型", guideMap.size());
            for (FaultType type : guideMap.keySet()) {
                log.debug("  - {}: {} 步", type, guideMap.get(type).size());
            }

        } catch (IOException e) {
            log.error("加载维修指南文件失败", e);
            loadDefaultGuides();
        }
    }

    private String cleanStepLine(String line) {
        String cleaned = line;
        int firstDot = cleaned.indexOf('.');
        if (firstDot > 0 && firstDot <= 4) {
            String numPart = cleaned.substring(0, firstDot);
            if (numPart.matches("\\d+")) {
                cleaned = cleaned.substring(firstDot + 1).trim();
            }
        }
        return cleaned;
    }

    private void loadDefaultGuides() {
        log.info("加载默认维修指南");

        List<String> leakSteps = new ArrayList<>();
        leakSteps.add("立即关闭制冷机组电源，悬挂禁止合闸警示牌");
        leakSteps.add("佩戴防护手套和护目镜，确保工作区域通风良好");
        leakSteps.add("用电子检漏仪检查所有管路接头、蒸发器、冷凝器");
        leakSteps.add("确认漏点后，回收系统内制冷剂");
        leakSteps.add("补漏或更换损坏部件");
        leakSteps.add("氮气保压试验，保压24小时压降合格");
        leakSteps.add("抽真空处理，真空度达到500微米以下");
        leakSteps.add("按铭牌规定充注制冷剂");
        leakSteps.add("试运行2小时，确认温度、压力正常");
        leakSteps.add("填写维修记录");
        guideMap.put(FaultType.REFRIGERANT_LEAK, leakSteps);

        List<String> motorSteps = new ArrayList<>();
        motorSteps.add("立即切断制冷机组总电源，悬挂禁止合闸警示牌");
        motorSteps.add("测量压缩机三相绕组电阻和绝缘电阻");
        motorSteps.add("检查供电回路、接触器、热继电器、启动电容");
        motorSteps.add("确认电机烧毁后，准备同型号压缩机");
        motorSteps.add("回收系统内制冷剂，关闭吸排气截止阀");
        motorSteps.add("拆卸并更换压缩机");
        motorSteps.add("更换干燥过滤器");
        motorSteps.add("抽真空并按规定充注制冷剂");
        motorSteps.add("启动后检查三相电流、振动、噪音");
        motorSteps.add("连续运行观察4小时，确认各项参数正常");
        guideMap.put(FaultType.MOTOR_BURNED, motorSteps);

        List<String> normalSteps = new ArrayList<>();
        normalSteps.add("冷藏箱运行状态正常，无需维修");
        normalSteps.add("持续监控温度数据，每4小时记录一次箱温");
        normalSteps.add("每日检查压缩机运行声音、电压、电流是否正常");
        guideMap.put(FaultType.NORMAL, normalSteps);

        List<String> unknownSteps = new ArrayList<>();
        unknownSteps.add("故障原因未知，建议人工全面检查");
        unknownSteps.add("检查电源供电是否正常");
        unknownSteps.add("检查控制器显示及报警信息");
        unknownSteps.add("检查压缩机启停状态、吸排气压力");
        unknownSteps.add("如无法现场排除，联系岸基技术支持");
        guideMap.put(FaultType.UNKNOWN, unknownSteps);
    }

    public List<String> getMaintenanceSteps(FaultType faultType) {
        if (faultType == null) {
            return new ArrayList<>();
        }
        List<String> steps = guideMap.get(faultType);
        if (steps == null || steps.isEmpty()) {
            log.warn("未找到故障类型 [{}] 的维修步骤", faultType);
            return new ArrayList<>();
        }
        return new ArrayList<>(steps);
    }

    public String getMaintenanceStepsAsText(FaultType faultType) {
        List<String> steps = getMaintenanceSteps(faultType);
        if (steps.isEmpty()) {
            return "暂无维修步骤";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            sb.append(i + 1).append(". ").append(steps.get(i));
            if (i < steps.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public boolean hasGuide(FaultType faultType) {
        return guideMap.containsKey(faultType) && !guideMap.get(faultType).isEmpty();
    }

    public int getStepCount(FaultType faultType) {
        List<String> steps = guideMap.get(faultType);
        return steps != null ? steps.size() : 0;
    }
}
