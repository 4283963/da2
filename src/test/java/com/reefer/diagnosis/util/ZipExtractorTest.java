package com.reefer.diagnosis.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipExtractorTest {

    private ZipExtractor zipExtractor;

    @BeforeEach
    void setUp() {
        zipExtractor = new ZipExtractor();
    }

    @Test
    @DisplayName("从字节数组解压ZIP并提取CSV文件")
    void testExtractZipBytes(@TempDir Path tempDir) throws IOException {
        byte[] zipBytes = createTestZip(new String[]{
                "reefer1.csv",
                "readme.txt",
                "subdir/reefer2.csv"
        });

        Path target = zipExtractor.createTempDir("zip_test_");
        try {
            List<Path> csvFiles = zipExtractor.extractToTemp(zipBytes, target);

            assertEquals(2, csvFiles.size(), "应提取出两个CSV文件");
            assertTrue(csvFiles.stream().allMatch(p -> p.toString().endsWith(".csv")));

            for (Path csv : csvFiles) {
                assertTrue(Files.exists(csv));
                String content = Files.readString(csv, StandardCharsets.UTF_8);
                assertTrue(content.contains("container_id"));
            }
        } finally {
            zipExtractor.deleteTempDir(target);
        }
    }

    @Test
    @DisplayName("正确识别ZIP和CSV文件类型")
    void testFileTypeDetection(@TempDir Path tempDir) {
        Path zipFile = tempDir.resolve("archive.ZIP");
        Path csvFile = tempDir.resolve("data.CSV");
        Path txtFile = tempDir.resolve("notes.txt");

        assertTrue(zipExtractor.isZipFile(zipFile));
        assertTrue(zipExtractor.isCsvFile(csvFile));
        assertFalse(zipExtractor.isZipFile(csvFile));
        assertFalse(zipExtractor.isCsvFile(txtFile));
    }

    @Test
    @DisplayName("创建临时目录并能清理")
    void testCreateAndDeleteTempDir() throws IOException {
        Path temp = zipExtractor.createTempDir("unittest_");
        assertTrue(Files.exists(temp));
        assertTrue(Files.isDirectory(temp));

        Path subFile = temp.resolve("test.txt");
        Files.writeString(subFile, "hello");
        Path subDir = temp.resolve("sub");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("nested.txt"), "nested");

        zipExtractor.deleteTempDir(temp);
        assertFalse(Files.exists(temp));
    }

    private byte[] createTestZip(String[] fileNames) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String name : fileNames) {
                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                String content = "container_id,timestamp,temperature,current\n" +
                        "T001,2025-06-20 08:00:00,-18.0,12.5\n";
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
