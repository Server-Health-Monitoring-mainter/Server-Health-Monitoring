package serverhealthmonitoring;

/**
 * Chạy thử lớp Metrics: chuột phải file này -> Run File (Shift+F6).
 * Kiểm tra: đóng gói thành chuỗi rồi đọc lại phải ra đúng số liệu ban đầu,
 * và các dòng sai định dạng / số liệu vô lý phải bị từ chối.
 */
public class MetricsTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        // 1. Đóng gói rồi đọc lại
        Metrics m = new Metrics();
        m.name = "web-01";
        m.cpu = 23.5;
        m.memUsedMB = 4096;
        m.memTotalMB = 8192;
        m.connections = 57;

        String line = m.toLine();
        System.out.println("Gửi : " + line);
        Metrics m2 = Metrics.parse(line);
        System.out.println("Nhận: " + m2);

        check("Đọc lại đúng số liệu", m2.name.equals(m.name) && m2.cpu == m.cpu
                && m2.memUsedMB == m.memUsedMB && m2.memTotalMB == m.memTotalMB
                && m2.connections == m.connections);

        // 2. Dòng hợp lệ ở biên vẫn được nhận
        accept("CPU = 0 và RAM = 0/0", "METRICS|name=a|cpu=0|memUsed=0|memTotal=0|conn=0");
        accept("CPU = 100, RAM đầy", "METRICS|name=a|cpu=100|memUsed=8192|memTotal=8192|conn=1");
        accept("Có khoảng trắng đầu/cuối dòng", "  METRICS|name=a|cpu=5|memUsed=1|memTotal=2|conn=0  ");

        // 3. Dòng sai phải bị từ chối
        reject("Chuỗi rác", "du lieu rac");
        reject("null", null);
        reject("Thiếu name", "METRICS|cpu=5|memUsed=1|memTotal=2|conn=0");
        reject("name rỗng", "METRICS|name=|cpu=5|memUsed=1|memTotal=2|conn=0");
        reject("CPU âm", "METRICS|name=a|cpu=-5|memUsed=1|memTotal=2|conn=0");
        reject("CPU > 100", "METRICS|name=a|cpu=150|memUsed=1|memTotal=2|conn=0");
        reject("CPU không phải số", "METRICS|name=a|cpu=abc|memUsed=1|memTotal=2|conn=0");
        reject("RAM âm", "METRICS|name=a|cpu=5|memUsed=-1|memTotal=2|conn=0");
        reject("RAM dùng > tổng", "METRICS|name=a|cpu=5|memUsed=9|memTotal=1|conn=0");
        reject("Số kết nối âm", "METRICS|name=a|cpu=5|memUsed=1|memTotal=2|conn=-3");

        // 4. toString luôn dùng dấu chấm thập phân
        java.util.Locale old = java.util.Locale.getDefault();
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi-VN"));
        String text = m.toString();
        java.util.Locale.setDefault(old);
        check("toString dùng dấu chấm (" + text + ")", text.contains("23.5%"));

        System.out.println("Đạt " + passed + "/" + (passed + failed));
        System.out.println(failed == 0 ? "KET QUA: DUNG" : "KET QUA: SAI");
    }

    private static void accept(String name, String line) {
        try {
            Metrics.parse(line);
            check(name + " -> được nhận", true);
        } catch (IllegalArgumentException e) {
            check(name + " -> bị từ chối nhầm: " + e.getMessage(), false);
        }
    }

    private static void reject(String name, String line) {
        try {
            Metrics.parse(line);
            check(name + " -> KHÔNG bị từ chối", false);
        } catch (IllegalArgumentException e) {
            check(name + " -> bị từ chối đúng", true);
        }
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println((ok ? "[OK]   " : "[SAI]  ") + name);
    }
}
