package com.dzavo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
public class DzavoApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DzavoApplication.class, args);
    }

    /**
     * Automatically loads .env file into System properties if present.
     */
    private static void loadDotEnv() {
        File[] possibleFiles = new File[]{
                new File(".env"),
                new File("DZAVO_BE/.env"),
                new File("../DZAVO_BE/.env")
        };

        for (File envFile : possibleFiles) {
            if (envFile.exists() && envFile.isFile()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int equalIdx = line.indexOf('=');
                        if (equalIdx > 0) {
                            String key = line.substring(0, equalIdx).trim();
                            String value = line.substring(equalIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (!value.isEmpty()) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    System.out.println("[DZAVO] Loaded environment configuration from: " + envFile.getAbsolutePath());
                    break;
                } catch (IOException e) {
                    System.err.println("[DZAVO] Failed to read .env file: " + e.getMessage());
                }
            }
        }
    }
}
