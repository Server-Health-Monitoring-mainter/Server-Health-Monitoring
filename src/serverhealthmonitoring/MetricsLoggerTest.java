package serverhealthmonitoring;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MetricsLoggerTest {

    private static final List<String> HEADER = Arrays.asList("thoi_gian", "agent",
            "dia_chi", "cpu", "mem_used_mb", "mem_total_mb", "ket_noi");

    public static void main(String[] args) throws Exception {
        LocalDateTime start = LocalDateTime.now();
        Path file = Path.of("data", "metrics-" + start.toLocalDate() + ".csv");
        String beforeText = Files.exists(file)
                ? Files.readString(file, StandardCharsets.UTF_8) : "";
        List<List<String>> before = parseCsv(beforeText);
        if (!before.isEmpty()) {
            check(before.get(0).equals(HEADER), "Header file cu khong dung");
        }
        int beforeCount = Math.max(0, before.size() - 1);
        String marker = "logger-test-" + UUID.randomUUID();
        Map<String, Metrics> expected = new HashMap<>();
        Map<String, String> addresses = new HashMap<>();
        List<Metrics> samples = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            Metrics m = new Metrics();
            m.name = marker + "-" + i;
            String address = "127.0.0.1:" + (6000 + i);
            if (i == 1) {
                m.name += ",\"Agent tiếng Việt\"";
                address = "host,\"quoted\"";
            } else if (i == 2) {
                m.name += "\r\nagent-next";
                address = "host\nnext";
            }
            m.cpu = 12.5 + i;
            m.memUsedMB = 4096 + i;
            m.memTotalMB = 8192;
            m.connections = i;
            samples.add(m);
            expected.put(m.name, m);
            addresses.put(m.name, address);
        }
        for (int i = 0; i < 3; i++) {
            Metrics m = samples.get(i);
            MetricsLogger.log(addresses.get(m.name), m);
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Void>> writes = new ArrayList<>();
            for (int i = 3; i < samples.size(); i++) {
                Metrics m = samples.get(i);
                writes.add(pool.submit(() -> {
                    MetricsLogger.log(addresses.get(m.name), m);
                    return null;
                }));
            }
            for (Future<Void> write : writes) {
                write.get();
            }
        } finally {
            pool.shutdown();
        }

        LocalDateTime end = LocalDateTime.now();
        check(LocalDate.now().equals(start.toLocalDate()),
                "Ngay da thay doi khi test, hay chay lai");
        check(Files.exists(file), "File CSV khong ton tai");
        String afterText = Files.readString(file, StandardCharsets.UTF_8);
        check(afterText.startsWith(beforeText), "Du lieu cu bi thay doi");
        List<List<String>> after = parseCsv(afterText);
        check(after.get(0).equals(HEADER), "Header khong dung");
        check(after.size() == beforeCount + samples.size() + 1,
                "So ban ghi CSV khong dung");
        Set<String> found = new HashSet<>();
        String sampleRow = null;
        for (int i = 1; i < after.size(); i++) {
            List<String> row = after.get(i);
            check(!row.equals(HEADER), "Header bi ghi lap");
            check(row.size() == 7, "Ban ghi CSV khong du 7 cot");
            Metrics m = expected.get(row.get(1));
            if (m == null) {
                continue;
            }
            check(found.add(m.name), "Du lieu test bi ghi lap");
            LocalDateTime time = LocalDateTime.parse(row.get(0));
            check(!time.isBefore(start.minusSeconds(1)) && !time.isAfter(end),
                    "Thoi gian ghi khong dung");
            check(row.get(2).equals(addresses.get(m.name)), "Dia chi khong dung");
            check(Double.parseDouble(row.get(3)) == m.cpu, "CPU khong dung");
            check(Long.parseLong(row.get(4)) == m.memUsedMB, "RAM used khong dung");
            check(Long.parseLong(row.get(5)) == m.memTotalMB, "RAM total khong dung");
            check(Integer.parseInt(row.get(6)) == m.connections, "Ket noi khong dung");
            if (m == samples.get(0)) {
                sampleRow = String.join(",", row);
            }
        }
        check(found.size() == samples.size(), "Thieu du lieu vua ghi");
        System.out.println("File CSV: " + file.toAbsolutePath());
        System.out.println("Header: DUNG");
        System.out.println("So ban ghi truoc/sau: " + beforeCount + "/" + (after.size() - 1));
        System.out.println("Dong mau: " + sampleRow);
        System.out.println("Escape CSV va ghi nhieu luong: DUNG");
        System.out.println("KET QUA: DUNG");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    // Doc theo ban ghi CSV vi text trong dau nhay co the chua xuong dong.
    private static List<List<String>> parseCsv(String text) {
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && ch == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (!quoted && (ch == '\r' || ch == '\n')) {
                row.add(field.toString());
                rows.add(row);
                row = new ArrayList<>();
                field.setLength(0);
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                field.append(ch);
            }
        }
        check(!quoted, "CSV thieu dau nhay dong");
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        return rows;
    }
}
