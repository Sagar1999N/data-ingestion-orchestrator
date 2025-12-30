package com.ecommerce.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class OrchestrationApp {
    private static final Logger logger = LoggerFactory.getLogger(OrchestrationApp.class);
    
    // Data directories
    private static final String BASE_DIR = "data";
    private static final String ZIP_PATH = BASE_DIR + "/brazilian-ecommerce.zip";
    private static final String EXTRACTED_DIR = BASE_DIR + "/extracted";
    private static final String PARTITIONED_DIR = BASE_DIR + "/partitioned";

    public static void main(String[] args) {
        logger.info("🚀 Starting Data Ingestion Orchestrator");
        
        try {
            // Step 1: Create base directory
            new File(BASE_DIR).mkdirs();
            
            // Step 2: Download Kaggle data if missing
            if (!Files.exists(Paths.get(ZIP_PATH))) {
                logger.info("📥 Downloading Kaggle dataset...");
                KaggleDownloader.download(ZIP_PATH);
            } else {
                logger.info("📁 Kaggle dataset already exists at {}", ZIP_PATH);
            }
            
            // Step 3: Extract if needed
            if (!Files.exists(Paths.get(EXTRACTED_DIR))) {
                logger.info("📦 Extracting dataset...");
                ZipUtils.extract(ZIP_PATH, EXTRACTED_DIR);
            } else {
                logger.info("📁 Dataset already extracted at {}", EXTRACTED_DIR);
            }
            
            // Step 4: Run Spark partitioning job if needed
            if (!isPartitionedDataAvailable()) {
                logger.info("⚡ Running Spark partitioning job...");
                runSparkPartitioningJob();
            } else {
                logger.info("📁 Data already partitioned at {}", PARTITIONED_DIR);
            }
            
            // Step 5: Start Data Provider API
            logger.info("🚀 Starting Data Provider API...");
            startDataProviderApi();
            
            logger.info("✅ Orchestration completed successfully!");
            
        } catch (Exception e) {
            logger.error("❌ Orchestration failed", e);
            System.exit(1);
        }
    }
    
    private static boolean isPartitionedDataAvailable() {
        Path partitionedPath = Paths.get(PARTITIONED_DIR);
        if (!Files.exists(partitionedPath)) {
            return false;
        }
        
        try {
            return Files.list(partitionedPath).count() > 0;
        } catch (IOException e) {
            logger.warn("Failed to check partitioned directory", e);
            return false;
        }
    }
    
    private static void runSparkPartitioningJob() throws Exception {
        // Build command to run Spark job JAR
        String sparkJar = "../data-partitioning-job/target/data-partitioning-job-1.0.0.jar";
        String[] command = {
            "java", "-cp", sparkJar, 
            "com.ecommerce.partitioning.SparkPartitioningJob"
        };
        
        logger.info("Executing Spark job: {}", String.join(" ", command));
        Process process = new ProcessBuilder(command)
            .inheritIO() // Show Spark logs in console
            .start();
            
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Spark partitioning job failed with exit code: " + exitCode);
        }
    }
    
    private static void startDataProviderApi() throws Exception {
        // Build command to run API JAR
        String apiJar = "../data-provider-api/target/data-provider-api-1.0.0.jar";
        String[] command = {
            "java", "-jar", apiJar
        };
        
        logger.info("Starting Data Provider API: {}", String.join(" ", command));
        Process process = new ProcessBuilder(command)
            .inheritIO() // Show API logs in console
            .start();
            
        // Keep main thread alive (API runs in background)
        Thread.sleep(5000); // Give API time to start
        logger.info("✅ Data Provider API is now running on http://localhost:8080");
    }
}
