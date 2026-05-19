# Data Ingestion Orchestrator

A Java application that orchestrates the initial data ingestion pipeline for the Brazilian E-commerce Analytics system. It automates downloading the Olist dataset from Kaggle, extracting it, and invoking the Spark partitioning job to prepare data for downstream services.

---

## Overview

This orchestrator is the **entry point** of the overall data pipeline. It coordinates three sequential steps:

1. **Download** — Fetches the Brazilian E-commerce dataset ZIP from the Kaggle API
2. **Extract** — Unzips the downloaded archive into a local directory
3. **Partition** — Invokes `data-partitioning-job` via `spark-submit` to partition the extracted CSVs by date

Each step is idempotent: if the output already exists, the step is skipped.

---

## System Context

```
┌─────────────────────────────────────────────────────┐
│              Data Ingestion Orchestrator             │
│                                                     │
│  1. Kaggle API Download ──► data/brazilian-ecommerce.zip
│  2. ZIP Extraction      ──► data/extracted/          │
│  3. spark-submit        ──► data/partitioned-zip/    │
└─────────────────────────────────────────────────────┘
         │
         ▼
  data-partitioning-job (Spark)
         │
         ▼
  data-provider-api (serves partitioned ZIPs)
```

---

## Tech Stack

| Component        | Technology              | Version   |
|------------------|-------------------------|-----------|
| Language         | Java                    | 17        |
| Build Tool       | Maven                   | 3.x       |
| HTTP Client      | Apache HttpComponents   | 4.5.14    |
| Logging          | SLF4J + Log4j2          | 2.0.7 / 2.20.0 |
| Spark (external) | Apache Spark            | 3.4.0     |

---

## Project Structure

```
data-ingestion-orchestrator/
├── src/main/java/com/ecommerce/orchestrator/
│   ├── OrchestrationApp.java     # Main entry point — drives the 3-step workflow
│   ├── KaggleDownloader.java     # Downloads dataset from Kaggle API
│   └── ZipUtils.java             # Safe ZIP extraction (Zip Slip protection)
├── src/main/resources/
│   └── log4j2.xml                # Logging configuration
├── pom.xml                       # Maven build config
└── target/
    └── data-ingestion-orchestrator-1.0.0.jar
```

---

## Prerequisites

- Java 17+
- Maven 3.x
- Apache Spark installed with `spark-submit.cmd` on PATH
- `data-partitioning-job-1.0.0.jar` built and present at:
  `C:\Code_Base_P\BDPETE\data-partitioning-job\target\data-partitioning-job-1.0.0.jar`
- Network access to `https://www.kaggle.com/api/v1/`
- `KAGGLE_API_TOKEN` environment variable set

---

## Environment Variables

| Variable           | Required | Description                              |
|--------------------|----------|------------------------------------------|
| `KAGGLE_API_TOKEN` | Yes      | Bearer token for Kaggle API authentication |

---

## Build

```bash
mvn clean package
```

Produces: `target/data-ingestion-orchestrator-1.0.0.jar`

---

## Run

```powershell
# Set Kaggle API token
$env:KAGGLE_API_TOKEN = "your_kaggle_token_here"

# Run the orchestrator
java -jar target/data-ingestion-orchestrator-1.0.0.jar
```

Or via Maven:

```bash
mvn clean compile exec:java@orchestrator
```

---

## Workflow Detail

### Step 1: Download
- Checks for `data/brazilian-ecommerce.zip`
- If missing, calls Kaggle API: `GET https://www.kaggle.com/api/v1/datasets/download/olistbr/brazilian-ecommerce`
- Streams response in 8 KB chunks to disk

### Step 2: Extract
- Checks for `data/extracted/` directory
- Extracts the ZIP with Zip Slip vulnerability protection
- All CSV files land in `data/extracted/`

### Step 3: Partition
- Checks for `data/partitioned-zip/` directory
- Invokes Spark job: `spark-submit.cmd --master local[*] data-partitioning-job-1.0.0.jar`
- Job partitions CSVs by order date and writes ZIPs to `data/partitioned-zip/`

---

## Data Paths

| Path                        | Contents                                     |
|-----------------------------|----------------------------------------------|
| `data/brazilian-ecommerce.zip` | Downloaded raw Kaggle archive             |
| `data/extracted/`           | Extracted CSV files from the archive         |
| `data/partitioned-zip/`     | Date-partitioned ZIPs produced by Spark job  |
| `logs/`                     | Rolling log files (daily, 100 MB max, 10 kept) |

---

## Logging

Configured via `log4j2.xml`:
- Console output (STDOUT)
- Rolling file appender in `logs/` directory
- JSON structured log appender for machine parsing
- `com.ecommerce` package logs at DEBUG; Spark/Hadoop suppressed to WARN/ERROR

---

## License

Apache 2.0