package com.reefer.diagnosis.controller;

import com.reefer.diagnosis.dto.ImportResultDTO;
import com.reefer.diagnosis.model.ContainerDataBatch;
import com.reefer.diagnosis.model.TemperatureDataSummary;
import com.reefer.diagnosis.service.LogImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/import")
public class LogImportController {

    private static final Logger log = LoggerFactory.getLogger(LogImportController.class);

    private final LogImportService logImportService;

    public LogImportController(LogImportService logImportService) {
        this.logImportService = logImportService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultDTO> uploadLogs(
            @RequestPart("files") MultipartFile[] files) {
        log.info("接收到日志上传请求，文件数量: {}", files.length);
        ImportResultDTO result = logImportService.importFromFiles(files);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload/streaming", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultDTO> uploadLogsStreaming(
            @RequestPart("files") MultipartFile[] files) {
        log.info("接收到流式日志上传请求（大文件模式），文件数量: {}", files.length);
        ImportResultDTO result = logImportService.importFromFilesStreaming(files);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/containers")
    public ResponseEntity<List<String>> listContainerIds() {
        return ResponseEntity.ok(logImportService.getAllContainerIds());
    }

    @GetMapping("/containers/{containerId}")
    public ResponseEntity<ContainerDataBatch> getContainerData(@PathVariable String containerId) {
        return ResponseEntity.ok(logImportService.getContainerData(containerId));
    }

    @GetMapping("/containers/{containerId}/summary")
    public ResponseEntity<TemperatureDataSummary> getContainerSummary(@PathVariable String containerId) {
        return ResponseEntity.ok(logImportService.getContainerSummary(containerId));
    }

    @GetMapping("/containers/all")
    public ResponseEntity<List<ContainerDataBatch>> getAllContainerData() {
        return ResponseEntity.ok(logImportService.getAllContainerData());
    }

    @GetMapping("/containers/summaries")
    public ResponseEntity<List<TemperatureDataSummary>> getAllSummaries() {
        return ResponseEntity.ok(logImportService.getAllSummaries());
    }

    @DeleteMapping("/containers/{containerId}")
    public ResponseEntity<Map<String, Object>> removeContainerData(@PathVariable String containerId) {
        boolean removed = logImportService.removeContainerData(containerId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", removed);
        response.put("containerId", containerId);
        response.put("message", removed ? "已删除" : "集装箱数据不存在");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllData() {
        logImportService.clearAllData();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "所有已导入数据已清除");
        return ResponseEntity.ok(response);
    }
}
