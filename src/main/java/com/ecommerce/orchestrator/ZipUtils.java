package com.ecommerce.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
    private static final Logger logger = LoggerFactory.getLogger(ZipUtils.class);
    private static final int BUFFER_SIZE = 8192;

    public static void extract(String zipFilePath, String destDirPath) throws IOException {
        logger.info("📦 Extracting {} to {}", zipFilePath, destDirPath);
        
        Path destDir = Paths.get(destDirPath);
        Files.createDirectories(destDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zis.getNextEntry();
            
            while (entry != null) {
                Path targetPath = sanitizePath(destDir, entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    extractFile(zis, targetPath);
                }
                
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        }
        
        logger.info("✅ Successfully extracted files to {}", destDirPath);
    }

    private static Path sanitizePath(Path destDir, String entryName) throws IOException {
        Path targetPath = destDir.resolve(entryName).normalize();
        if (!targetPath.startsWith(destDir)) {
            throw new IOException("Zip Slip detected! Entry: " + entryName);
        }
        return targetPath;
    }

    private static void extractFile(ZipInputStream zis, Path targetPath) throws IOException {
        try (OutputStream fos = Files.newOutputStream(targetPath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }
}