package com.reefer.diagnosis.service;

import com.reefer.diagnosis.algorithm.FaultDiagnosisAlgorithm;
import com.reefer.diagnosis.dto.BatchDiagnosisResultDTO;
import com.reefer.diagnosis.dto.DirectDiagnosisRequest;
import com.reefer.diagnosis.exception.DiagnosisException;
import com.reefer.diagnosis.model.ContainerDataBatch;
import com.reefer.diagnosis.model.DiagnosisResult;
import com.reefer.diagnosis.model.FaultType;
import com.reefer.diagnosis.model.TemperatureRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FaultAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(FaultAnalysisService.class);

    private final LogImportService logImportService;
    private final FaultDiagnosisAlgorithm algorithm;

    public FaultAnalysisService(LogImportService logImportService, FaultDiagnosisAlgorithm algorithm) {
        this.logImportService = logImportService;
        this.algorithm = algorithm;
    }

    public DiagnosisResult diagnoseByContainerId(String containerId) {
        ContainerDataBatch batch = logImportService.getContainerData(containerId);
        log.info("对集装箱 {} 进行故障诊断，共 {} 条记录", containerId, batch.getRecords().size());
        return algorithm.diagnose(containerId, batch.getRecords());
    }

    public BatchDiagnosisResultDTO diagnoseAllImported() {
        List<ContainerDataBatch> allBatches = logImportService.getAllContainerData();
        if (allBatches.isEmpty()) {
            throw new DiagnosisException(400, "暂无已导入的数据，请先上传日志文件");
        }

        List<DiagnosisResult> results = new ArrayList<>();
        int normalCount = 0;
        int faultCount = 0;

        for (ContainerDataBatch batch : allBatches) {
            try {
                DiagnosisResult result = algorithm.diagnose(batch.getContainerId(), batch.getRecords());
                results.add(result);
                if (result.getFaultType() == FaultType.NORMAL) {
                    normalCount++;
                } else {
                    faultCount++;
                }
            } catch (Exception e) {
                log.error("诊断集装箱 {} 失败: {}", batch.getContainerId(), e.getMessage());
            }
        }

        return BatchDiagnosisResultDTO.builder()
                .totalContainers(results.size())
                .normalCount(normalCount)
                .faultCount(faultCount)
                .results(results)
                .build();
    }

    public DiagnosisResult diagnoseWithDirectData(DirectDiagnosisRequest request) {
        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new DiagnosisException(400, "温度记录不能为空");
        }
        List<TemperatureRecord> records = request.getRecords();
        String containerId = request.getContainerId();
        for (TemperatureRecord r : records) {
            if (r.getContainerId() == null) {
                r.setContainerId(containerId);
            }
        }
        log.info("直接数据诊断 - 集装箱: {}, 记录数: {}", containerId, records.size());
        return algorithm.diagnose(containerId, records);
    }

    public BatchDiagnosisResultDTO diagnoseWithDirectBatch(List<DirectDiagnosisRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new DiagnosisException(400, "诊断请求不能为空");
        }
        List<DiagnosisResult> results = new ArrayList<>();
        int normalCount = 0;
        int faultCount = 0;

        for (DirectDiagnosisRequest req : requests) {
            try {
                DiagnosisResult result = diagnoseWithDirectData(req);
                results.add(result);
                if (result.getFaultType() == FaultType.NORMAL) {
                    normalCount++;
                } else {
                    faultCount++;
                }
            } catch (Exception e) {
                log.error("诊断集装箱 {} 失败: {}", req.getContainerId(), e.getMessage());
            }
        }

        return BatchDiagnosisResultDTO.builder()
                .totalContainers(results.size())
                .normalCount(normalCount)
                .faultCount(faultCount)
                .results(results)
                .build();
    }
}
