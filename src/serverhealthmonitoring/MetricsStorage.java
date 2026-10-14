package serverhealthmonitoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MetricsStorage {

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final DateTimeFormatter CSV_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String HEADER =
            "timestamp,address,agent,cpu_percent,memory_used_mb,"
            + "memory_total_mb,memory_percent,connections";

    private final Path dataDirectory;

    public MetricsStorage() {
        this(Paths.get("data"));
    }

    MetricsStorage(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

  
    public synchronized Path save(String address, Metrics metrics) throws IOException {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics khong duoc null");
        }

        Files.createDirectories(dataDirectory);

        long timestamp = metrics.timestamp > 0
                ? metrics.timestamp
                : System.currentTimeMillis();

        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault());

        LocalDate date = dateTime.toLocalDate();
        Path file = dataDirectory.resolve(
                "metrics_" + FILE_DATE_FORMAT.format(date) + ".csv");

        boolean writeHeader = Files.notExists(file) || Files.size(file) == 0;

        try (BufferedWriter writer = Files.newBufferedWriter(
                file,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {

            if (writeHeader) {
                writer.write(HEADER);
                writer.newLine();
            }

            writer.write(toCsvRow(address, metrics, dateTime));
            writer.newLine();
        }

        return file;
    }

    private String toCsvRow(String address, Metrics metrics, LocalDateTime dateTime) {
        return String.join(",",
                csv(CSV_TIME_FORMAT.format(dateTime)),
                csv(address),
                csv(metrics.name),
                String.format(java.util.Locale.US, "%.1f", metrics.cpu),
                Long.toString(metrics.memUsedMB),
                Long.toString(metrics.memTotalMB),
                String.format(java.util.Locale.US, "%.1f", metrics.memPercent()),
                Integer.toString(metrics.connections));
    }

 
    private String csv(String value) {
        String safe = value == null ? "" : value;

        if (safe.contains(",") || safe.contains("\"")
                || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }

        return safe;
    }
}
