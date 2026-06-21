package com.reefer.diagnosis.controller;

import com.reefer.diagnosis.dto.BatchDiagnosisResultDTO;
import com.reefer.diagnosis.dto.DirectDiagnosisRequest;
import com.reefer.diagnosis.model.DiagnosisResult;
import com.reefer.diagnosis.service.FaultAnalysisService;
import com.reefer.diagnosis.service.LogImportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/diagnosis")
public class FaultAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(FaultAnalysisController.class);

    private final FaultAnalysisService faultAnalysisService;
    private final LogImportService logImportService;

    public FaultAnalysisController(FaultAnalysisService faultAnalysisService,
                                   LogImportService logImportService) {
        this.faultAnalysisService = faultAnalysisService;
        this.logImportService = logImportService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DiagnosisResult> diagnoseFromUpload(
            @RequestPart("file") MultipartFile file) {
        log.info("上传即诊断请求 - 文件: {}, 大小: {}bytes",
                file.getOriginalFilename(), file.getSize());
        DiagnosisResult result = logImportService.diagnoseFromUpload(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BatchDiagnosisResultDTO> diagnoseBatchFromUpload(
            @RequestPart("file") MultipartFile file) {
        log.info("批量上传诊断请求 - 文件: {}, 大小: {}bytes",
                file.getOriginalFilename(), file.getSize());
        BatchDiagnosisResultDTO result = logImportService.diagnoseAllFromUpload(file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/container/{containerId}")
    public ResponseEntity<DiagnosisResult> diagnoseByContainerId(@PathVariable String containerId) {
        log.info("诊断请求 - 集装箱ID: {}", containerId);
        DiagnosisResult result = faultAnalysisService.diagnoseByContainerId(containerId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<BatchDiagnosisResultDTO> diagnoseAllImported() {
        log.info("批量诊断请求 - 所有已导入数据");
        BatchDiagnosisResultDTO result = faultAnalysisService.diagnoseAllImported();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/direct")
    public ResponseEntity<DiagnosisResult> diagnoseDirectData(
            @Valid @RequestBody DirectDiagnosisRequest request) {
        log.info("直接数据诊断请求 - 集装箱: {}, 记录数: {}",
                request.getContainerId(),
                request.getRecords() != null ? request.getRecords().size() : 0);
        DiagnosisResult result = faultAnalysisService.diagnoseWithDirectData(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/direct/batch")
    public ResponseEntity<BatchDiagnosisResultDTO> diagnoseDirectBatch(
            @Valid @RequestBody List<DirectDiagnosisRequest> requests) {
        log.info("直接数据批量诊断请求 - 数量: {}", requests.size());
        BatchDiagnosisResultDTO result = faultAnalysisService.diagnoseWithDirectBatch(requests);
        return ResponseEntity.ok(result);
    }
}
