package com.reefer.diagnosis.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Component
public class ZipExtractor {

    private static final Logger log = LoggerFactory.getLogger(ZipExtractor.class);

    public List<Path> extractToTemp(Path zipFile, Path targetDir) throws IOException {
        List<Path> extractedFiles = new ArrayList<>();
        Files.createDirectories(targetDir);

        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("非法的ZIP条目路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    continue;
                }

                Files.createDirectories(entryPath.getParent());

                try (InputStream is = zip.getInputStream(entry);
                     BufferedInputStream bis = new BufferedInputStream(is)) {
                    Files.copy(bis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    if (isCsvFile(entryPath)) {
                        extractedFiles.add(entryPath);
                    }
                }
                log.debug("解压文件: {}", entryPath);
            }
        }
        return extractedFiles;
    }

    public List<Path> extractToTemp(byte[] zipBytes, Path targetDir) throws IOException {
        Path tempZip = Files.createTempFile("reefer_upload_", ".zip");
        try {
            Files.write(tempZip, zipBytes);
            return extractToTemp(tempZip, targetDir);
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }

    public boolean isZipFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".zip");
    }

    public boolean isCsvFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".csv");
    }

    public Path createTempDir(String prefix) throws IOException {
        return Files.createTempDirectory(prefix);
    }

    public void deleteTempDir(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除临时文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", dir, e);
        }
    }
}
