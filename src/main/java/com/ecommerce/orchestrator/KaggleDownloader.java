package com.ecommerce.orchestrator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public class KaggleDownloader {
    private static final Logger logger = LoggerFactory.getLogger(KaggleDownloader.class);
    private static final String DATASET_URL = "https://www.kaggle.com/api/v1/datasets/download/olistbr/brazilian-ecommerce";

    public static void download(String outputPath) throws IOException {
        String token = System.getenv("KAGGLE_API_TOKEN");
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("KAGGLE_API_TOKEN environment variable not set");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(DATASET_URL);
            request.setHeader("Authorization", "Bearer " + token);

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if (statusCode != 200) {
                String body = EntityUtils.toString(response.getEntity());
                throw new RuntimeException("Kaggle API returned " + statusCode + ": " + body);
            }

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream is = entity.getContent();
                     FileOutputStream fos = new FileOutputStream(outputPath)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                logger.info("✅ Dataset downloaded to: {}", outputPath);
            }
        }
    }
}