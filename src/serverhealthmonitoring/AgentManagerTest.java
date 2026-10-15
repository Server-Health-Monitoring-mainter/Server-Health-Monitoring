package serverhealthmonitoring;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentManagerTest {

    public static void main(String[] args) throws Exception {
        startFakeTcpAgent(7100, "agent-tcp");
        startFakeUdpAgent(7101, "agent-udp");
        Thread.sleep(300);

        Map<String, AtomicInteger> received = new ConcurrentHashMap<>();
        AgentManager manager = new AgentManager(new MetricsListener() {
            @Override
            public void onMetrics(String address, Metrics m) {
                received.computeIfAbsent(address, k -> new AtomicInteger()).incrementAndGet();
                System.out.println("Nhan tu " + address + ": " + m.toLine());
            }

            @Override
            public void onStatus(String address, String status) {
                System.out.println("[" + address + "] " + status);
            }
        });

        manager.addAgent("localhost", 7100, AgentManager.TCP);
        manager.addAgent("localhost", 7101, AgentManager.UDP);

        try {
            manager.addAgent("localhost", 7100, AgentManager.TCP);
            throw new IllegalStateException("Them trung Agent ma khong bao loi");
        } catch (IllegalArgumentException e) {
            System.out.println("Them trung bi tu choi dung: " + e.getMessage());
        }

        Thread.sleep(6000);

        int tcp = received.getOrDefault("localhost:7100", new AtomicInteger()).get();
        int udp = received.getOrDefault("localhost:7101", new AtomicInteger()).get();
        System.out.println("So lan nhan: TCP=" + tcp + ", UDP=" + udp
                + ", dang quan ly " + manager.size() + " Agent");

        manager.removeAgent("localhost:7100");
        if (manager.size() != 1) {
            throw new IllegalStateException("removeAgent khong xoa duoc");
        }
        manager.stopAll();

        if (tcp < 2 || udp < 2) {
            throw new IllegalStateException("Khong nhan du du lieu tu 2 Agent");
        }
        System.out.println("KET QUA: DUNG - quan ly nhieu Agent qua TCP va UDP.");
        System.exit(0);
    }

    private static Metrics fakeMetrics(String name, int i) {
        Metrics m = new Metrics();
        m.name = name;
        m.cpu = 10 + i;
        m.memUsedMB = 2000 + i;
        m.memTotalMB = 8000;
        m.connections = 1;
        return m;
    }

    private static void startFakeTcpAgent(int port, String name) {
        Thread t = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port);
                 Socket s = ss.accept();
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {
                for (int i = 1; !out.checkError(); i++) {
                    out.println(fakeMetrics(name, i).toLine());
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static void startFakeUdpAgent(int port, String name) {
        Thread t = new Thread(() -> {
            try (DatagramSocket udp = new DatagramSocket(port)) {
                byte[] buf = new byte[1024];
                for (int i = 1; ; i++) {
                    DatagramPacket req = new DatagramPacket(buf, buf.length);
                    udp.receive(req);
                    String msg = new String(req.getData(), 0, req.getLength(), StandardCharsets.UTF_8);
                    if (Protocol.CMD_GET.equals(msg.trim())) {
                        byte[] data = fakeMetrics(name, i).toLine().getBytes(StandardCharsets.UTF_8);
                        udp.send(new DatagramPacket(data, data.length, req.getAddress(), req.getPort()));
                    }
                }
            } catch (Exception e) {
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
