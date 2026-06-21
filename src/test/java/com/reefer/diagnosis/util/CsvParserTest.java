package com.reefer.diagnosis.util;

import com.reefer.diagnosis.model.TemperatureRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private CsvParser csvParser;

    @BeforeEach
    void setUp() {
        csvParser = new CsvParser();
    }

    @Test
    @DisplayName("解析标准英文表头CSV")
    void testParseStandardEnglishCsv(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test1.csv");
        String content = "container_id,timestamp,temperature,set_temperature,ambient_temperature,compressor_current,compressor_status\n" +
                "CONT001,2025-06-20 08:00:00,-18.5,-18.0,32.0,12.5,RUN\n" +
                "CONT001,2025-06-20 09:00:00,-18.3,-18.0,32.5,12.8,RUN\n" +
                "CONT001,2025-06-20 10:00:00,-18.1,-18.0,33.0,13.0,RUN\n";
        Files.write(csvFile, content.getBytes(StandardCharsets.UTF_8));

        List<TemperatureRecord> records = csvParser.parseCsvFile(csvFile);

        assertEquals(3, records.size());
        TemperatureRecord first = records.get(0);
        assertEquals("CONT001", first.getContainerId());
        assertEquals(-18.5, first.getTemperature());
        assertEquals(32.0, first.getAmbientTemperature());
        assertEquals(12.5, first.getCompressorCurrent());
        assertEquals("RUN", first.getCompressorStatus());
        assertNotNull(first.getTimestamp());
    }

    @Test
    @DisplayName("解析中文表头CSV")
    void testParseChineseHeaderCsv(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test2.csv");
        String content = "集装箱号,时间,温度,设定温度,环境温度,电流,状态\n" +
                "CONT002,2025-06-20 08:00:00,-20.0,-18.0,30.0,11.5,运行\n" +
                "CONT002,2025-06-20 09:00:00,-19.8,-18.0,30.5,11.8,运行\n";
        Files.write(csvFile, content.getBytes(StandardCharsets.UTF_8));

        List<TemperatureRecord> records = csvParser.parseCsvFile(csvFile);

        assertEquals(2, records.size());
        TemperatureRecord first = records.get(0);
        assertEquals("CONT002", first.getContainerId());
        assertEquals(-20.0, first.getTemperature());
        assertEquals(30.0, first.getAmbientTemperature());
        assertEquals(11.5, first.getCompressorCurrent());
    }

    @Test
    @DisplayName("解析混合字段、部分为空的CSV")
    void testParsePartialNulls(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test3.csv");
        String content = "container_id,timestamp,temperature,ambient_temperature,compressor_current\n" +
                "CONT003,2025/06/20 08:00:00,-17.5,,14.2\n" +
                "CONT003,2025-06-20T09:00:00,-17.3,35.0,\n";
        Files.write(csvFile, content.getBytes(StandardCharsets.UTF_8));

        List<TemperatureRecord> records = csvParser.parseCsvFile(csvFile);

        assertEquals(2, records.size());
        TemperatureRecord first = records.get(0);
        assertEquals(-17.5, first.getTemperature());
        assertNull(first.getAmbientTemperature());
        assertEquals(14.2, first.getCompressorCurrent());
        assertNotNull(first.getTimestamp());
    }

    @Test
    @DisplayName("温度值带单位符号应能解析")
    void testTemperatureWithUnit(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test4.csv");
        String content = "container_id,time,temp,current\n" +
                "CONT004,2025-06-20 08:00:00,\"-18.5°C\",12.5A\n" +
                "CONT004,2025-06-20 09:00:00,-18.3℃,12.8A\n";
        Files.write(csvFile, content.getBytes(StandardCharsets.UTF_8));

        List<TemperatureRecord> records = csvParser.parseCsvFile(csvFile);

        assertEquals(2, records.size());
        assertEquals(-18.5, records.get(0).getTemperature());
        assertEquals(12.5, records.get(0).getCompressorCurrent());
    }
}
