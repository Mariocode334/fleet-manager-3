package com.fleet.util;

import com.fleet.config.Config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AppLogger {
    private static AppLogger instance;
    private final String logFilePath;
    private final SimpleDateFormat dateFormat;
    private final ConcurrentLinkedQueue<String> logQueue;
    private final ExecutorService executor;
    private volatile boolean running = true;

    private AppLogger() {
        Config config = Config.getInstance();
        this.logFilePath = config.getLogPath() + "/fleet-manager.log";
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Logger-Worker"));
        
        ensureLogDirectory();
        startLogWriter();
    }

    public static synchronized AppLogger getInstance() {
        if (instance == null) {
            instance = new AppLogger();
        }
        return instance;
    }

    private void ensureLogDirectory() {
        File logDir = new File(logFilePath).getParentFile();
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs();
        }
    }

    private void startLogWriter() {
        executor.submit(() -> {
            while (running || !logQueue.isEmpty()) {
                try {
                    String logEntry = logQueue.poll();
                    if (logEntry != null) {
                        writeToFile(logEntry);
                    } else {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    System.err.println("Error writing to log file: " + e.getMessage());
                }
            }
        });
    }

    private void writeToFile(String message) throws IOException {
        try (FileWriter fw = new FileWriter(logFilePath, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(message);
        }
    }

    public void info(String droneId, String message) {
        log("INFO", droneId, message);
    }

    public void warning(String droneId, String message) {
        log("WARNING", droneId, message);
    }

    public void error(String droneId, String message) {
        log("ERROR", droneId, message);
    }

    private void log(String level, String droneId, String message) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] [%s] %s", timestamp, level, droneId, message);
        logQueue.offer(logEntry);
    }

    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
