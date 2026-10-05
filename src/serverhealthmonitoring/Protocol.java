package serverhealthmonitoring;

/**
 * Quy ước giao tiếp chung giữa Monitoring Client và Agent Server.
 * Cả nhóm dùng chung lớp này - muốn sửa phải tạo Issue và báo nhóm trưởng.
 *
 * TCP (giữ kết nối): Client kết nối tới Agent, Agent tự gửi 1 dòng METRICS mỗi chu kỳ.
 * UDP (hỏi - đáp):   Client gửi "GET", Agent trả về 1 gói tin chứa 1 dòng METRICS.
 */
public final class Protocol {

    /** Cổng mặc định của Agent, dùng chung số cổng cho TCP và UDP. */
    public static final int DEFAULT_PORT = 6000;

    /** Client gửi lệnh này để xin số liệu (bắt buộc với UDP, tùy chọn với TCP). */
    public static final String CMD_GET = "GET";

    /** Client gửi lệnh này để báo ngắt kết nối TCP. */
    public static final String CMD_QUIT = "QUIT";

    /** Chu kỳ gửi / hỏi số liệu (mili giây). */
    public static final int INTERVAL_MS = 2000;

    private Protocol() {
        // Lớp chỉ chứa hằng số, không cho tạo đối tượng
    }
}
