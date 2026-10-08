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
}
