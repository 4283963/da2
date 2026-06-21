package com.reefer.diagnosis.util;

import com.reefer.diagnosis.exception.DiagnosisException;
import com.reefer.diagnosis.model.TemperatureRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);

    private static final int BOM_SIZE = 4096;

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    public int parseCsvFile(Path csvFile, Consumer<TemperatureRecord> recordConsumer) throws IOException {
        try (InputStream is = Files.newInputStream(csvFile);
             BufferedInputStream bis = new BufferedInputStream(is)) {
            Charset charset = detectCharset(bis);
            return parseFromInputStreamWithCharset(bis, charset, recordConsumer);
        }
    }

    public int parseCsvStream(InputStream inputStream, Consumer<TemperatureRecord> recordConsumer) throws IOException {
        if (!(inputStream instanceof BufferedInputStream)) {
            inputStream = new BufferedInputStream(inputStream);
        }
        Charset charset = detectCharset((BufferedInputStream) inputStream);
        return parseFromInputStreamWithCharset(inputStream, charset, recordConsumer);
    }

    public List<TemperatureRecord> parseCsvFile(Path csvFile) throws IOException {
        List<TemperatureRecord> records = new ArrayList<>();
        parseCsvFile(csvFile, records::add);
        return records;
    }

    public List<TemperatureRecord> parseCsvStream(InputStream inputStream) throws IOException {
        List<TemperatureRecord> records = new ArrayList<>();
        parseCsvStream(inputStream, records::add);
        return records;
    }

    private int parseFromInputStreamWithCharset(InputStream inputStream, Charset charset,
                                                Consumer<TemperatureRecord> recordConsumer) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setNullString("")
                .build();

        int count = 0;
        int lineNum = 1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));
             CSVParser parser = new CSVParser(reader, format)) {
            for (CSVRecord csvRecord : parser) {
                lineNum++;
                try {
                    TemperatureRecord record = mapToRecord(csvRecord);
                    if (record != null) {
                        recordConsumer.accept(record);
                        count++;
                        if (count % 10000 == 0) {
                            log.debug("已解析 {} 行...", count);
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析第 {} 行失败: {}", lineNum, e.getMessage());
                }
            }
        }
        return count;
    }

    private TemperatureRecord mapToRecord(CSVRecord csv) {
        TemperatureRecord.Builder builder = TemperatureRecord.builder();

        builder.containerId(getValueOrNull(csv, "container_id", "containerid", "container", "集装箱号", "箱号"));
        builder.timestamp(parseDateTime(getValueOrNull(csv, "timestamp", "time", "datetime", "date_time", "时间", "记录时间")));
        builder.temperature(parseDouble(getValueOrNull(csv, "temperature", "temp", "cargo_temp", "cargo", "温度", "货物温度", "箱内温度")));
        builder.setTemperature(parseDouble(getValueOrNull(csv, "set_temperature", "set_temp", "setpoint", "设定温度", "目标温度")));
        builder.ambientTemperature(parseDouble(getValueOrNull(csv, "ambient_temperature", "ambient", "ambient_temp", "环境温度", "外界温度")));
        builder.evaporatorTemperature(parseDouble(getValueOrNull(csv, "evaporator_temperature", "evaporator", "evap_temp", "蒸发器温度", "蒸发温度")));
        builder.condenserTemperature(parseDouble(getValueOrNull(csv, "condenser_temperature", "condenser", "cond_temp", "冷凝器温度")));
        builder.compressorCurrent(parseDouble(getValueOrNull(csv, "compressor_current", "current", "amps", "压缩机电流", "电流")));
        builder.compressorVoltage(parseDouble(getValueOrNull(csv, "compressor_voltage", "voltage", "volts", "电压", "压缩机电压")));
        builder.suctionPressure(parseDouble(getValueOrNull(csv, "suction_pressure", "suction", "low_pressure", "吸气压力", "低压")));
        builder.dischargePressure(parseDouble(getValueOrNull(csv, "discharge_pressure", "discharge", "high_pressure", "排气压力", "高压")));
        builder.compressorStatus(getValueOrNull(csv, "compressor_status", "comp_status", "压缩机状态"));
        builder.defrostStatus(getValueOrNull(csv, "defrost_status", "defrost", "除霜状态"));
        builder.alarmCode(getValueOrNull(csv, "alarm_code", "alarm", "报警代码", "告警代码"));
        builder.remark(getValueOrNull(csv, "remark", "note", "备注", "说明"));

        TemperatureRecord record = builder.build();
        if (record.getTimestamp() == null && record.getTemperature() == null) {
            return null;
        }
        return record;
    }

    private String getValueOrNull(CSVRecord csv, String... headers) {
        for (String header : headers) {
            if (csv.isMapped(header) && csv.isSet(header)) {
                String value = csv.get(header);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null) {
            return null;
        }
        try {
            String cleaned = value.replaceAll("[^0-9.\\-]", "");
            if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
                return null;
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new DiagnosisException("无法解析时间格式: " + value);
    }

    private Charset detectCharset(BufferedInputStream bis) throws IOException {
        bis.mark(BOM_SIZE);
        byte[] bytes = new byte[BOM_SIZE];
        int read = bis.read(bytes);
        bis.reset();

        if (read <= 0) {
            return StandardCharsets.UTF_8;
        }
        if (read >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (read >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (read >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }
}
