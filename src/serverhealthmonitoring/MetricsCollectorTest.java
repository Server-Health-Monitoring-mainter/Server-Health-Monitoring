package serverhealthmonitoring;

public class MetricsCollectorTest {

    public static void main(String[] args) throws InterruptedException {
        MetricsCollector collector = new MetricsCollector();
        collector.collect("agent-test-2380601519");
        Thread.sleep(1000);
        Metrics metrics = collector.collect("agent-test-2380601519");

        System.out.println(String.valueOf(metrics).replace("Kết nối", "Ket noi"));

        if (metrics == null || !"agent-test-2380601519".equals(metrics.name)) {
            throw new IllegalStateException("collect() phai tra ve Metrics voi dung ten Agent.");
        }
        if (!Double.isFinite(metrics.cpu) || metrics.cpu < 0.0 || metrics.cpu > 100.0) {
            throw new IllegalStateException("CPU khong hop le: " + metrics.cpu);
        }
        System.out.printf("CPU hop le: %.1f%%%n", metrics.cpu);
        if (metrics.memTotalMB <= 0 || metrics.memUsedMB < 0
                || metrics.memUsedMB > metrics.memTotalMB) {
            throw new IllegalStateException("RAM khong hop le: used=" + metrics.memUsedMB
                    + " MB, total=" + metrics.memTotalMB + " MB");
        }
        double ramPercent = metrics.memPercent();
        if (!Double.isFinite(ramPercent) || ramPercent < 0.0 || ramPercent > 100.0) {
            throw new IllegalStateException("Phan tram RAM khong hop le: " + ramPercent);
        }
        System.out.printf("RAM hop le: %d/%d MB (%.1f%%)%n",
                metrics.memUsedMB, metrics.memTotalMB, ramPercent);
        System.out.println("KET QUA: DUNG - collect() tra ve Metrics voi dung ten Agent.");
    }
}
