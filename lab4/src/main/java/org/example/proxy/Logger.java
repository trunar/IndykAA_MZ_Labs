package org.example.proxy;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class Logger {
    private static final String logFile = "proxy.log";

    public static synchronized void log(String message) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(LocalDateTime.now() + " - " + message + "\n");
        } catch (IOException e) {
            System.err.println("Logging error: " + e.getMessage());
        }
    }
}
