package serverhealthmonitoring;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Mô phỏng dữ liệu để kiểm tra giao diện mà không cần Agent thật.
 */
public class ClientFrameTest {

    private static final String TEST_ADDRESS = "demo-agent:6000";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientFrame frame = new ClientFrame();
            frame.setVisible(true);
            frame.addTestAgent(TEST_ADDRESS, "DemoAgent");

            Metrics[] samples = createSamples();
            int[] index = {0};

            Timer timer = new Timer(Protocol.INTERVAL_MS, e -> {
                Metrics metrics = samples[index[0]];
                metrics.timestamp = System.currentTimeMillis();

                frame.onMetrics(TEST_ADDRESS, metrics);

                index[0] = (index[0] + 1) % samples.length;
            });

            timer.setInitialDelay(500);
            timer.start();
        });
    }

    private static Metrics[] createSamples() {
        return new Metrics[]{
            // Bình thường
            createMetrics("DemoAgent", 35.0, 2048, 8192, 15),

            // CPU > 80% -> cảnh báo
            createMetrics("DemoAgent", 86.5, 3500, 8192, 22),

            // RAM > 90% -> cảnh báo (7600 / 8192 ≈ 92.8%)
            createMetrics("DemoAgent", 48.0, 7600, 8192, 28),

            // Số kết nối cao nhưng không phải ngưỡng cảnh báo của Tuần 2
            createMetrics("DemoAgent", 52.0, 3600, 8192, 125),

            // Trở lại bình thường
            createMetrics("DemoAgent", 42.0, 3000, 8192, 18)
        };
    }

    private static Metrics createMetrics(
            String name,
            double cpu,
            long memUsedMB,
            long memTotalMB,
            int connections) {

        Metrics metrics = new Metrics();
        metrics.name = name;
        metrics.cpu = cpu;
        metrics.memUsedMB = memUsedMB;
        metrics.memTotalMB = memTotalMB;
        metrics.connections = connections;
        return metrics;
    }
}
