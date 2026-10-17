package serverhealthmonitoring;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class AgentServerTest {

    public static void main(String[] args) {

        boolean tcpOk = false;
        boolean udpOk = false;
        boolean quitOk = false;

        final String testName = "AgentTest";
        int testPort;

        try {
            // Tim mot cong TCP trong tren may
            try (ServerSocket temp = new ServerSocket(0)) {
                testPort = temp.getLocalPort();
            }

            // Tu khoi dong AgentServer o cong rieng
            AgentServer agent = new AgentServer(testPort, testName);
            agent.start();

            System.out.println("=== AGENT SERVER TEST ===");
            System.out.println("Test port: " + testPort);

            // ================= TEST TCP =================
            try (Socket socket = connectWithRetry(testPort);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(
                                 socket.getInputStream(),
                                 StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                         socket.getOutputStream(),
                         true,
                         StandardCharsets.UTF_8)) {

                socket.setSoTimeout(5000);

                String data = in.readLine();
                Metrics m = Metrics.parse(data);

                tcpOk = validMetrics(m, testName)
                        && m.connections == 1;

                System.out.println("TCP: " + data);
                System.out.println("TCP TEST: "
                        + (tcpOk ? "DUNG" : "SAI"));

                // Gui QUIT de Agent dong ket noi
                out.println(Protocol.CMD_QUIT);

                quitOk = in.readLine() == null;

                System.out.println("QUIT TEST: "
                        + (quitOk ? "DUNG" : "SAI"));
            }

            // ================= TEST UDP =================
            try (DatagramSocket udp = new DatagramSocket()) {

                udp.setSoTimeout(5000);

                byte[] requestData = Protocol.CMD_GET.getBytes(
                        StandardCharsets.UTF_8);

                DatagramPacket request = new DatagramPacket(
                        requestData,
                        requestData.length,
                        InetAddress.getLoopbackAddress(),
                        testPort
                );

                udp.send(request);

                byte[] buffer = new byte[2048];

                DatagramPacket response = new DatagramPacket(
                        buffer, buffer.length);

                udp.receive(response);

                String data = new String(
                        response.getData(),
                        response.getOffset(),
                        response.getLength(),
                        StandardCharsets.UTF_8
                );

                Metrics m = Metrics.parse(data);

                udpOk = validMetrics(m, testName)
                        && m.connections == 0;

                System.out.println("UDP: " + data);
                System.out.println("UDP TEST: "
                        + (udpOk ? "DUNG" : "SAI"));
            }

        } catch (Exception e) {
            System.out.println("TEST ERROR: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("==========================");

        if (tcpOk && udpOk && quitOk) {
            System.out.println("KET QUA: DUNG");
        } else {
            System.out.println("KET QUA: SAI");
        }

        System.exit(tcpOk && udpOk && quitOk ? 0 : 1);
    }

    // Ket noi lai neu TCP Server chua khoi dong xong
    private static Socket connectWithRetry(int port)
            throws Exception {

        Exception lastError = null;

        for (int i = 0; i < 20; i++) {
            try {
                return new Socket(
                        InetAddress.getLoopbackAddress(), port);
            } catch (Exception e) {
                lastError = e;
                Thread.sleep(100);
            }
        }

        throw new Exception(
                "Khong ket noi duoc TCP Agent", lastError);
    }

    // Kiem tra Metrics co du lieu hop le
    private static boolean validMetrics(Metrics m, String expectedName) {

        return m != null
                && expectedName.equals(m.name)
                && Double.isFinite(m.cpu)
                && m.cpu >= 0
                && m.cpu <= 100
                && m.memTotalMB > 0
                && m.memUsedMB >= 0
                && m.memUsedMB <= m.memTotalMB
                && m.connections >= 0;
    }
}
