package com.reefer.diagnosis.dto;

import java.util.List;

public class ImportResultDTO {

    private int totalFiles;
    private int successCount;
    private int failCount;
    private int totalRecords;
    private boolean streamingMode;
    private List<FileImportResult> fileResults;

    public ImportResultDTO() {}

    private ImportResultDTO(Builder b) {
        this.totalFiles = b.totalFiles;
        this.successCount = b.successCount;
        this.failCount = b.failCount;
        this.totalRecords = b.totalRecords;
        this.streamingMode = b.streamingMode;
        this.fileResults = b.fileResults;
    }

    public static Builder builder() { return new Builder(); }

    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public boolean isStreamingMode() { return streamingMode; }
    public void setStreamingMode(boolean streamingMode) { this.streamingMode = streamingMode; }
    public List<FileImportResult> getFileResults() { return fileResults; }
    public void setFileResults(List<FileImportResult> fileResults) { this.fileResults = fileResults; }

    public static class Builder {
        private int totalFiles;
        private int successCount;
        private int failCount;
        private int totalRecords;
        private boolean streamingMode;
        private List<FileImportResult> fileResults;

        public Builder totalFiles(int v) { this.totalFiles = v; return this; }
        public Builder successCount(int v) { this.successCount = v; return this; }
        public Builder failCount(int v) { this.failCount = v; return this; }
        public Builder totalRecords(int v) { this.totalRecords = v; return this; }
        public Builder streamingMode(boolean v) { this.streamingMode = v; return this; }
        public Builder fileResults(List<FileImportResult> v) { this.fileResults = v; return this; }
        public ImportResultDTO build() { return new ImportResultDTO(this); }
    }

    public static class FileImportResult {
        private String fileName;
        private boolean success;
        private int recordCount;
        private String containerId;
        private String errorMessage;

        public FileImportResult() {}

        private FileImportResult(Builder b) {
            this.fileName = b.fileName;
            this.success = b.success;
            this.recordCount = b.recordCount;
            this.containerId = b.containerId;
            this.errorMessage = b.errorMessage;
        }

        public static Builder builder() { return new Builder(); }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public int getRecordCount() { return recordCount; }
        public void setRecordCount(int recordCount) { this.recordCount = recordCount; }
        public String getContainerId() { return containerId; }
        public void setContainerId(String containerId) { this.containerId = containerId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public static class Builder {
            private String fileName;
            private boolean success;
            private int recordCount;
            private String containerId;
            private String errorMessage;

            public Builder fileName(String v) { this.fileName = v; return this; }
            public Builder success(boolean v) { this.success = v; return this; }
            public Builder recordCount(int v) { this.recordCount = v; return this; }
            public Builder containerId(String v) { this.containerId = v; return this; }
            public Builder errorMessage(String v) { this.errorMessage = v; return this; }
            public FileImportResult build() { return new FileImportResult(this); }
        }
    }
}
