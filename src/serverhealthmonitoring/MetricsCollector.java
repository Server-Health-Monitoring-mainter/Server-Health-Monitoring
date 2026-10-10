package serverhealthmonitoring;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class MetricsCollector {

    private final java.lang.management.OperatingSystemMXBean operatingSystem
            = ManagementFactory.getOperatingSystemMXBean();

    public Metrics collect(String name) {
        Metrics metrics = new Metrics();
        metrics.name = name;
        metrics.cpu = collectCpuPercent();
        collectMemory(metrics);
        return metrics;
    }

    private double collectCpuPercent() {
        if (!(operatingSystem instanceof OperatingSystemMXBean)) {
            return 0.0;
        }

        try {
            double load = ((OperatingSystemMXBean) operatingSystem).getCpuLoad();
            // Dùng 0 dự phòng khi API chưa sẵn sàng hoặc trả số không hợp lệ.
            if (!Double.isFinite(load) || load < 0.0 || load > 1.0) {
                return 0.0;
            }
            return load * 100.0;
        } catch (UnsupportedOperationException e) {
            return 0.0;
        }
    }

    private void collectMemory(Metrics metrics) {
        if (!(operatingSystem instanceof OperatingSystemMXBean)) {
            return;
        }

        try {
            OperatingSystemMXBean bean = (OperatingSystemMXBean) operatingSystem;
            long totalBytes = bean.getTotalMemorySize();
            long freeBytes = bean.getFreeMemorySize();
            if (totalBytes <= 0 || freeBytes < 0 || freeBytes > totalBytes) {
                return;
            }

            // Don vi MB cua Metrics: 1 MB = 1024 * 1024 byte.
            long bytesPerMB = 1024L * 1024L;
            long totalMB = totalBytes / bytesPerMB;
            if (totalMB <= 0) {
                return;
            }
            metrics.memTotalMB = totalMB;
            metrics.memUsedMB = (totalBytes - freeBytes) / bytesPerMB;
        } catch (UnsupportedOperationException e) {
            // Giu RAM mac dinh khi he dieu hanh khong ho tro API.
        }
    }
}
