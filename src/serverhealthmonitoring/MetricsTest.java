package serverhealthmonitoring;

/**
 * Chạy thử lớp Metrics: chuột phải file này -> Run File (Shift+F6).
 * Kiểm tra: đóng gói thành chuỗi rồi đọc lại phải ra đúng số liệu ban đầu.
 */
public class MetricsTest {

    public static void main(String[] args) {
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

        boolean ok = m2.name.equals(m.name) && m2.cpu == m.cpu
                && m2.memUsedMB == m.memUsedMB && m2.memTotalMB == m.memTotalMB
                && m2.connections == m.connections;
        System.out.println(ok ? "KẾT QUẢ: ĐÚNG" : "KẾT QUẢ: SAI");

        try {
            Metrics.parse("du lieu rac");
            System.out.println("Lỗi: chuỗi sai định dạng mà không báo lỗi");
        } catch (IllegalArgumentException e) {
            System.out.println("Chuỗi sai định dạng bị từ chối đúng: " + e.getMessage());
        }
    }
}
