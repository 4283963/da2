package com.reefer.diagnosis.service;

import com.reefer.diagnosis.dto.ImportResultDTO;
import com.reefer.diagnosis.exception.DiagnosisException;
import com.reefer.diagnosis.model.ContainerDataBatch;
import com.reefer.diagnosis.model.TemperatureRecord;
import com.reefer.diagnosis.util.CsvParser;
import com.reefer.diagnosis.util.ZipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogImportService {

    private static final Logger log = LoggerFactory.getLogger(LogImportService.class);

    private final ZipExtractor zipExtractor;
    private final CsvParser csvParser;
    private final Map<String, ContainerDataBatch> containerStore = new HashMap<>();

    public LogImportService(ZipExtractor zipExtractor, CsvParser csvParser) {
        this.zipExtractor = zipExtractor;
        this.csvParser = csvParser;
    }

    public ImportResultDTO importFromFiles(MultipartFile[] files) {
        int totalFiles = files.length;
        int successCount = 0;
        int failCount = 0;
        int totalRecords = 0;
        List<ImportResultDTO.FileImportResult> fileResults = new ArrayList<>();

        for (MultipartFile file : files) {
            ImportResultDTO.FileImportResult fileResult = handleSingleFile(file);
            fileResults.add(fileResult);
            if (fileResult.isSuccess()) {
                successCount++;
                totalRecords += fileResult.getRecordCount();
            } else {
                failCount++;
            }
        }

        return ImportResultDTO.builder()
                .totalFiles(totalFiles)
                .successCount(successCount)
                .failCount(failCount)
                .totalRecords(totalRecords)
                .fileResults(fileResults)
                .build();
    }

    private ImportResultDTO.FileImportResult handleSingleFile(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "unknown_file";
        }

        try {
            String lowerName = originalName.toLowerCase();
            if (lowerName.endsWith(".zip")) {
                return handleZipFile(file, originalName);
            } else if (lowerName.endsWith(".csv")) {
                return handleCsvFile(file, originalName);
            } else {
                return ImportResultDTO.FileImportResult.builder()
                        .fileName(originalName)
                        .success(false)
                        .recordCount(0)
                        .errorMessage("不支持的文件格式，请上传 .zip 或 .csv 文件")
                        .build();
            }
        } catch (Exception e) {
            log.error("处理文件失败: {}", originalName, e);
            return ImportResultDTO.FileImportResult.builder()
                    .fileName(originalName)
                    .success(false)
                    .recordCount(0)
                    .errorMessage("处理异常: " + e.getMessage())
                    .build();
        }
    }

    private ImportResultDTO.FileImportResult handleZipFile(MultipartFile file, String originalName) throws IOException {
        Path tempDir = zipExtractor.createTempDir("reefer_import_");
        try {
            List<Path> csvFiles = zipExtractor.extractToTemp(file.getBytes(), tempDir);
            if (csvFiles.isEmpty()) {
                return ImportResultDTO.FileImportResult.builder()
                        .fileName(originalName)
                        .success(false)
                        .recordCount(0)
                        .errorMessage("压缩包内未找到 CSV 文件")
                        .build();
            }

            int totalRecordsInZip = 0;
            String firstContainerId = null;
            for (Path csv : csvFiles) {
                List<TemperatureRecord> records = csvParser.parseCsvFile(csv);
                if (records.isEmpty()) {
                    continue;
                }
                String cid = resolveContainerId(records, csv.getFileName().toString());
                if (firstContainerId == null) {
                    firstContainerId = cid;
                }
                storeBatch(cid, originalName, records);
                totalRecordsInZip += records.size();
            }

            return ImportResultDTO.FileImportResult.builder()
                    .fileName(originalName)
                    .success(totalRecordsInZip > 0)
                    .recordCount(totalRecordsInZip)
                    .containerId(firstContainerId)
                    .errorMessage(totalRecordsInZip > 0 ? null : "压缩包内无有效数据")
                    .build();
        } finally {
            zipExtractor.deleteTempDir(tempDir);
        }
    }

    private ImportResultDTO.FileImportResult handleCsvFile(MultipartFile file, String originalName) throws IOException {
        try (InputStream is = file.getInputStream()) {
            List<TemperatureRecord> records = csvParser.parseCsvStream(is);
            if (records.isEmpty()) {
                return ImportResultDTO.FileImportResult.builder()
                        .fileName(originalName)
                        .success(false)
                        .recordCount(0)
                        .errorMessage("CSV 文件中未解析到有效记录")
                        .build();
            }
            String containerId = resolveContainerId(records, originalName);
            storeBatch(containerId, originalName, records);
            return ImportResultDTO.FileImportResult.builder()
                    .fileName(originalName)
                    .success(true)
                    .recordCount(records.size())
                    .containerId(containerId)
                    .build();
        }
    }

    private String resolveContainerId(List<TemperatureRecord> records, String fileName) {
        for (TemperatureRecord r : records) {
            String id = r.getContainerId();
            if (id != null && !id.trim().isEmpty()) {
                return id;
            }
        }
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        return base.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
    }

    private void storeBatch(String containerId, String sourceFile, List<TemperatureRecord> records) {
        ContainerDataBatch existing = containerStore.get(containerId);
        if (existing == null) {
            existing = ContainerDataBatch.builder()
                    .containerId(containerId)
                    .sourceFileName(sourceFile)
                    .records(new ArrayList<>())
                    .build();
            containerStore.put(containerId, existing);
        }
        existing.getRecords().addAll(records);
        existing.setRecordCount(existing.getRecords().size());
    }

    public ContainerDataBatch getContainerData(String containerId) {
        ContainerDataBatch batch = containerStore.get(containerId);
        if (batch == null) {
            throw new DiagnosisException(404, "未找到集装箱 [" + containerId + "] 的数据，请先导入日志文件");
        }
        return batch;
    }

    public List<ContainerDataBatch> getAllContainerData() {
        return new ArrayList<>(containerStore.values());
    }

    public List<String> getAllContainerIds() {
        return new ArrayList<>(containerStore.keySet());
    }

    public void clearAllData() {
        containerStore.clear();
    }

    public boolean removeContainerData(String containerId) {
        return containerStore.remove(containerId) != null;
    }
}
