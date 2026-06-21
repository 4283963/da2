package com.reefer.diagnosis.service;

import com.reefer.diagnosis.algorithm.FaultDiagnosisAlgorithm;
import com.reefer.diagnosis.algorithm.StreamingDataAggregator;
import com.reefer.diagnosis.dto.BatchDiagnosisResultDTO;
import com.reefer.diagnosis.dto.ImportResultDTO;
import com.reefer.diagnosis.exception.DiagnosisException;
import com.reefer.diagnosis.model.ContainerDataBatch;
import com.reefer.diagnosis.model.DiagnosisResult;
import com.reefer.diagnosis.model.TemperatureDataSummary;
import com.reefer.diagnosis.model.TemperatureRecord;
import com.reefer.diagnosis.util.CsvParser;
import com.reefer.diagnosis.util.ZipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class LogImportService {

    private static final Logger log = LoggerFactory.getLogger(LogImportService.class);

    private final ZipExtractor zipExtractor;
    private final CsvParser csvParser;
    private final StreamingDataAggregator streamingAggregator;
    private final FaultDiagnosisAlgorithm diagnosisAlgorithm;

    private final Map<String, ContainerDataBatch> containerStore = new ConcurrentHashMap<>();
    private final Map<String, TemperatureDataSummary> summaryStore = new ConcurrentHashMap<>();

    public LogImportService(ZipExtractor zipExtractor, CsvParser csvParser,
                            StreamingDataAggregator streamingAggregator,
                            FaultDiagnosisAlgorithm diagnosisAlgorithm) {
        this.zipExtractor = zipExtractor;
        this.csvParser = csvParser;
        this.streamingAggregator = streamingAggregator;
        this.diagnosisAlgorithm = diagnosisAlgorithm;
    }

    public ImportResultDTO importFromFiles(MultipartFile[] files) {
        return importFromFilesInternal(files, false);
    }

    public ImportResultDTO importFromFilesStreaming(MultipartFile[] files) {
        return importFromFilesInternal(files, true);
    }

    private ImportResultDTO importFromFilesInternal(MultipartFile[] files, boolean streamingMode) {
        int totalFiles = files.length;
        int successCount = 0;
        int failCount = 0;
        int totalRecords = 0;
        List<ImportResultDTO.FileImportResult> fileResults = new ArrayList<>();

        for (MultipartFile file : files) {
            ImportResultDTO.FileImportResult fileResult = handleSingleFile(file, streamingMode);
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
                .streamingMode(streamingMode)
                .build();
    }

    public DiagnosisResult diagnoseFromUpload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "unknown_file";
        }

        try {
            String lowerName = originalName.toLowerCase();
            if (lowerName.endsWith(".zip")) {
                return diagnoseZipStreaming(file, originalName);
            } else if (lowerName.endsWith(".csv")) {
                return diagnoseCsvStreaming(file, originalName);
            } else {
                throw new DiagnosisException("不支持的文件格式，请上传 .zip 或 .csv 文件");
            }
        } catch (DiagnosisException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传即诊断失败: {}", originalName, e);
            throw new DiagnosisException("处理异常: " + e.getMessage());
        }
    }

    public BatchDiagnosisResultDTO diagnoseAllFromUpload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "unknown_file";
        }

        Map<String, StreamingDataAggregator.Aggregator> aggregatorMap = new HashMap<>();

        try {
            String lowerName = originalName.toLowerCase();
            if (lowerName.endsWith(".zip")) {
                aggregateZip(file, originalName, aggregatorMap);
            } else if (lowerName.endsWith(".csv")) {
                aggregateCsvFile(file, originalName, aggregatorMap);
            } else {
                throw new DiagnosisException("不支持的文件格式，请上传 .zip 或 .csv 文件");
            }
        } catch (DiagnosisException e) {
            throw e;
        } catch (Exception e) {
            log.error("批量上传诊断失败: {}", originalName, e);
            throw new DiagnosisException("处理异常: " + e.getMessage());
        }

        List<DiagnosisResult> results = new ArrayList<>();
        for (Map.Entry<String, StreamingDataAggregator.Aggregator> entry : aggregatorMap.entrySet()) {
            TemperatureDataSummary summary = entry.getValue().buildSummary();
            DiagnosisResult result = diagnosisAlgorithm.diagnoseFromSummary(entry.getKey(), summary);
            results.add(result);
        }

        int normalCount = 0;
        int faultCount = 0;
        for (DiagnosisResult r : results) {
            if (r.getFaultType() == null || r.getFaultType() == com.reefer.diagnosis.model.FaultType.NORMAL) {
                normalCount++;
            } else {
                faultCount++;
            }
        }

        return BatchDiagnosisResultDTO.builder()
                .totalContainers(results.size())
                .normalCount(normalCount)
                .faultCount(faultCount)
                .results(results)
                .build();
    }

    private DiagnosisResult diagnoseZipStreaming(MultipartFile file, String originalName) throws IOException {
        AtomicReference<DiagnosisResult> resultRef = new AtomicReference<>();
        try (InputStream is = file.getInputStream()) {
            zipExtractor.streamCsvFromZip(is, (entryName, entryStream) -> {
                String cid = deriveContainerIdFromFilename(entryName);
                StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
                try {
                    csvParser.parseCsvStream(entryStream, record -> {
                        if (record.getContainerId() != null && !record.getContainerId().trim().isEmpty()) {
                        }
                        agg.accept(record);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("解析CSV失败: " + entryName, e);
                }
                TemperatureDataSummary summary = agg.buildSummary();
                DiagnosisResult result = diagnosisAlgorithm.diagnoseFromSummary(cid, summary);
                resultRef.set(result);
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
        if (resultRef.get() == null) {
            throw new DiagnosisException("压缩包内未找到有效的CSV文件");
        }
        return resultRef.get();
    }

    private DiagnosisResult diagnoseCsvStreaming(MultipartFile file, String originalName) throws IOException {
        String cid = deriveContainerIdFromFilename(originalName);
        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(cid);
        try (InputStream is = file.getInputStream()) {
            csvParser.parseCsvStream(is, record -> {
                agg.accept(record);
            });
        }
        TemperatureDataSummary summary = agg.buildSummary();
        return diagnosisAlgorithm.diagnoseFromSummary(cid, summary);
    }

    private void aggregateZip(MultipartFile file, String originalName,
                              Map<String, StreamingDataAggregator.Aggregator> aggregatorMap) throws IOException {
        try (InputStream is = file.getInputStream()) {
            zipExtractor.streamCsvFromZip(is, (entryName, entryStream) -> {
                String defaultCid = deriveContainerIdFromFilename(entryName);
                StreamingDataAggregator.Aggregator[] currentAgg = new StreamingDataAggregator.Aggregator[1];
                currentAgg[0] = aggregatorMap.computeIfAbsent(
                        defaultCid, c -> streamingAggregator.createAggregator(c));
                try {
                    csvParser.parseCsvStream(entryStream, record -> {
                        if (record.getContainerId() != null && !record.getContainerId().trim().isEmpty()) {
                            currentAgg[0] = aggregatorMap.computeIfAbsent(
                                    record.getContainerId(),
                                    c -> streamingAggregator.createAggregator(c));
                        }
                        currentAgg[0].accept(record);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("解析CSV失败: " + entryName, e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    private void aggregateCsvFile(MultipartFile file, String originalName,
                                  Map<String, StreamingDataAggregator.Aggregator> aggregatorMap) throws IOException {
        String defaultCid = deriveContainerIdFromFilename(originalName);
        StreamingDataAggregator.Aggregator[] currentAgg = new StreamingDataAggregator.Aggregator[1];
        currentAgg[0] = aggregatorMap.computeIfAbsent(defaultCid,
                c -> streamingAggregator.createAggregator(c));
        try (InputStream is = file.getInputStream()) {
            csvParser.parseCsvStream(is, record -> {
                if (record.getContainerId() != null && !record.getContainerId().trim().isEmpty()
                        && !record.getContainerId().equals(currentAgg[0].buildSummary().getContainerId())) {
                    currentAgg[0] = aggregatorMap.computeIfAbsent(
                            record.getContainerId(),
                            c -> streamingAggregator.createAggregator(c));
                }
                currentAgg[0].accept(record);
            });
        }
    }

    private ImportResultDTO.FileImportResult handleSingleFile(MultipartFile file, boolean streamingMode) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            originalName = "unknown_file";
        }

        try {
            String lowerName = originalName.toLowerCase();
            if (lowerName.endsWith(".zip")) {
                return handleZipFile(file, originalName, streamingMode);
            } else if (lowerName.endsWith(".csv")) {
                return handleCsvFile(file, originalName, streamingMode);
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

    private ImportResultDTO.FileImportResult handleZipFile(MultipartFile file, String originalName,
                                                           boolean streamingMode) throws IOException {
        if (streamingMode) {
            return handleZipFileStreaming(file, originalName);
        } else {
            return handleZipFileLegacy(file, originalName);
        }
    }

    private ImportResultDTO.FileImportResult handleZipFileStreaming(MultipartFile file, String originalName) throws IOException {
        final int[] totalRecords = {0};
        final String[] firstContainerId = {null};

        try (InputStream is = file.getInputStream()) {
            zipExtractor.streamCsvFromZip(is, (entryName, entryStream) -> {
                String defaultCid = deriveContainerIdFromFilename(entryName);
                StreamingDataAggregator.Aggregator agg =
                        streamingAggregator.createAggregator(defaultCid);

                try {
                    csvParser.parseCsvStream(entryStream, record -> {
                        agg.accept(record);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("解析CSV失败: " + entryName, e);
                }

                TemperatureDataSummary summary = agg.buildSummary();
                String cid = summary.getContainerId();
                if (cid == null || cid.isEmpty()) {
                    cid = defaultCid;
                    summary.setContainerId(cid);
                }
                if (firstContainerId[0] == null) {
                    firstContainerId[0] = cid;
                }
                totalRecords[0] += summary.getTotalRecords();

                summaryStore.merge(cid, summary, (old, newSum) -> mergeSummary(old, newSum));
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }

        if (totalRecords[0] == 0) {
            return ImportResultDTO.FileImportResult.builder()
                    .fileName(originalName)
                    .success(false)
                    .recordCount(0)
                    .errorMessage("压缩包内未找到有效数据")
                    .build();
        }

        return ImportResultDTO.FileImportResult.builder()
                .fileName(originalName)
                .success(true)
                .recordCount(totalRecords[0])
                .containerId(firstContainerId[0])
                .build();
    }

    private TemperatureDataSummary mergeSummary(TemperatureDataSummary a, TemperatureDataSummary b) {
        return b;
    }

    private ImportResultDTO.FileImportResult handleZipFileLegacy(MultipartFile file, String originalName) throws IOException {
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
                List<TemperatureRecord> records = new ArrayList<>();
                csvParser.parseCsvFile(csv, records::add);
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

    private ImportResultDTO.FileImportResult handleCsvFile(MultipartFile file, String originalName,
                                                           boolean streamingMode) throws IOException {
        if (streamingMode) {
            return handleCsvFileStreaming(file, originalName);
        } else {
            return handleCsvFileLegacy(file, originalName);
        }
    }

    private ImportResultDTO.FileImportResult handleCsvFileStreaming(MultipartFile file, String originalName) throws IOException {
        String defaultCid = deriveContainerIdFromFilename(originalName);
        StreamingDataAggregator.Aggregator agg = streamingAggregator.createAggregator(defaultCid);
        final int[] count = {0};

        try (InputStream is = file.getInputStream()) {
            csvParser.parseCsvStream(is, record -> {
                agg.accept(record);
                count[0]++;
            });
        }

        TemperatureDataSummary summary = agg.buildSummary();
        String cid = summary.getContainerId();
        if (cid == null || cid.isEmpty()) {
            cid = defaultCid;
            summary.setContainerId(cid);
        }

        summaryStore.merge(cid, summary, (old, newSum) -> newSum);

        if (count[0] == 0) {
            return ImportResultDTO.FileImportResult.builder()
                    .fileName(originalName)
                    .success(false)
                    .recordCount(0)
                    .errorMessage("CSV 文件中未解析到有效记录")
                    .build();
        }

        return ImportResultDTO.FileImportResult.builder()
                .fileName(originalName)
                .success(true)
                .recordCount(count[0])
                .containerId(cid)
                .build();
    }

    private ImportResultDTO.FileImportResult handleCsvFileLegacy(MultipartFile file, String originalName) throws IOException {
        List<TemperatureRecord> records = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            csvParser.parseCsvStream(is, records::add);
        }
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

    private String deriveContainerIdFromFilename(String fileName) {
        String name = fileName;
        int slashIdx = name.lastIndexOf('/');
        if (slashIdx >= 0) {
            name = name.substring(slashIdx + 1);
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();
    }

    private String resolveContainerId(List<TemperatureRecord> records, String fileName) {
        for (TemperatureRecord r : records) {
            String id = r.getContainerId();
            if (id != null && !id.trim().isEmpty()) {
                return id;
            }
        }
        return deriveContainerIdFromFilename(fileName);
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

    public TemperatureDataSummary getContainerSummary(String containerId) {
        TemperatureDataSummary summary = summaryStore.get(containerId);
        if (summary == null) {
            throw new DiagnosisException(404, "未找到集装箱 [" + containerId + "] 的流式摘要数据");
        }
        return summary;
    }

    public List<ContainerDataBatch> getAllContainerData() {
        return new ArrayList<>(containerStore.values());
    }

    public List<TemperatureDataSummary> getAllSummaries() {
        return new ArrayList<>(summaryStore.values());
    }

    public List<String> getAllContainerIds() {
        List<String> ids = new ArrayList<>();
        ids.addAll(containerStore.keySet());
        for (String id : summaryStore.keySet()) {
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    public void clearAllData() {
        containerStore.clear();
        summaryStore.clear();
    }

    public boolean removeContainerData(String containerId) {
        boolean removed = containerStore.remove(containerId) != null;
        removed = summaryStore.remove(containerId) != null || removed;
        return removed;
    }
}
