package serverhealthmonitoring;

import java.util.Locale;

/**
 * Các chỉ số sức khỏe của 1 Agent Server tại 1 thời điểm.
 * Được truyền qua mạng dưới dạng 1 dòng văn bản:
 *   METRICS|name=web01|cpu=23.5|memUsed=4096|memTotal=8192|conn=57
 */
public class Metrics {

    public static final String PREFIX = "METRICS";

    public String name = "";   // tên Agent Server
    public double cpu;         // CPU Load (%)
    public long memUsedMB;     // bộ nhớ đã dùng (MB)
    public long memTotalMB;    // tổng bộ nhớ (MB)
    public int connections;    // số lượng kết nối TCP đang mở
    public long timestamp;     // thời điểm Client nhận được (Client tự điền)

    /** Phần trăm bộ nhớ đã dùng. */
    public double memPercent() {
        return memTotalMB == 0 ? 0 : memUsedMB * 100.0 / memTotalMB;
    }

    /** Đóng gói thành 1 dòng văn bản để gửi qua TCP/UDP. */
    public String toLine() {
        return PREFIX
                + "|name=" + name.replace("|", " ").replace("=", " ")
                + "|cpu=" + Math.round(cpu * 10) / 10.0
                + "|memUsed=" + memUsedMB
                + "|memTotal=" + memTotalMB
                + "|conn=" + connections;
    }

    /**
     * Đọc lại 1 dòng nhận được thành đối tượng Metrics.
     * Từ chối dòng sai định dạng hoặc số liệu vô lý để Client không hiển thị dữ liệu rác.
     * @throws IllegalArgumentException nếu dòng sai định dạng hoặc giá trị không hợp lệ
     */
    public static Metrics parse(String line) {
        if (line == null) {
            throw new IllegalArgumentException("Sai định dạng: null");
        }
        line = line.trim();
        if (!line.startsWith(PREFIX + "|")) {
            throw new IllegalArgumentException("Sai định dạng: " + line);
        }
        Metrics m = new Metrics();
        m.name = null;
        try {
            for (String part : line.split("\\|")) {
                int eq = part.indexOf('=');
                if (eq < 0) {
                    continue; // bỏ qua phần "METRICS"
                }
                String key = part.substring(0, eq);
                String val = part.substring(eq + 1).trim();
                switch (key) {
                    case "name":     m.name = val; break;
                    case "cpu":      m.cpu = Double.parseDouble(val); break;
                    case "memUsed":  m.memUsedMB = Long.parseLong(val); break;
                    case "memTotal": m.memTotalMB = Long.parseLong(val); break;
                    case "conn":     m.connections = Integer.parseInt(val); break;
                    default: break; // trường lạ: bỏ qua để dễ mở rộng
                }
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số không hợp lệ: " + line);
        }
        validate(m, line);
        return m;
    }

    /** Kiểm tra số liệu có hợp lý không. */
    private static void validate(Metrics m, String line) {
        if (m.name == null || m.name.isEmpty()) {
            throw new IllegalArgumentException("Thiếu tên Agent: " + line);
        }
        if (!Double.isFinite(m.cpu) || m.cpu < 0 || m.cpu > 100) {
            throw new IllegalArgumentException("CPU phải trong khoảng 0-100: " + line);
        }
        if (m.memUsedMB < 0 || m.memTotalMB < 0 || m.connections < 0) {
            throw new IllegalArgumentException("Số liệu không được âm: " + line);
        }
        if (m.memUsedMB > m.memTotalMB) {
            throw new IllegalArgumentException("RAM đã dùng lớn hơn tổng RAM: " + line);
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s | CPU %.1f%% | Mem %.1f%% (%d/%d MB) | Kết nối %d",
                name, cpu, memPercent(), memUsedMB, memTotalMB, connections);
    }
}
