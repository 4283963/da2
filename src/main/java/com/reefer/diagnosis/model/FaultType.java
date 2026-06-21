package com.reefer.diagnosis.model;

public enum FaultType {

    NORMAL("正常", "冷藏箱运行状态正常"),
    REFRIGERANT_LEAK("漏氟", "制冷剂泄漏，温度缓慢升高，电流正常或偏低"),
    MOTOR_BURNED("压缩机电机烧毁", "电机烧毁或严重故障，电流异常或为零"),
    TEMPERATURE_SENSOR_FAULT("温度传感器故障", "温度读数异常波动"),
    UNKNOWN("未知故障", "无法诊断，请人工检查");

    private final String displayName;
    private final String description;

    FaultType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
