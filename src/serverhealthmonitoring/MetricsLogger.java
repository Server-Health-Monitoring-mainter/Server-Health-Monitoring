package serverhealthmonitoring;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MetricsLogger {

    private static final String HEADER
            = "thoi_gian,agent,dia_chi,cpu,mem_used_mb,mem_total_mb,ket_noi";
    private static final DateTimeFormatter TIME_FORMAT
            = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    public static synchronized void log(String address, Metrics m) throws IOException {
        Objects.requireNonNull(m, "Metrics khong duoc null");
        LocalDateTime now = LocalDateTime.now();
        Path directory = Path.of("data");
        Files.createDirectories(directory);
        Path file = directory.resolve("metrics-" + now.toLocalDate() + ".csv");
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0;
        String row = now.format(TIME_FORMAT) + "," + escape(m.name) + ","
                + escape(address) + "," + Double.toString(m.cpu) + ","
                + m.memUsedMB + "," + m.memTotalMB + "," + m.connections;

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (needsHeader) {
                // BOM giup Excel nhan dien UTF-8 khi mo truc tiep.
                writer.write('\uFEFF');
                writer.write(HEADER);
                writer.write("\r\n");
            }
            writer.write(row);
            writer.write("\r\n");
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")
                || value.contains("\r") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
