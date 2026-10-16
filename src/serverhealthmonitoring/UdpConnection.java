package serverhealthmonitoring;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;


public class UdpConnection extends Thread implements AgentConnection {

    private static final int RESPONSE_TIMEOUT_MS = 3000;

    private final String host;
    private final int port;
    private final MetricsListener listener;
    private volatile boolean running = true;
    private DatagramSocket socket;

    public UdpConnection(String host, int port, MetricsListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        setDaemon(true);
        setName("UDP-" + host + ":" + port);
    }

    @Override
    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public void run() {
        byte[] get = Protocol.CMD_GET.getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[1024];
        String lastStatus = "";

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(RESPONSE_TIMEOUT_MS);
            InetAddress address = InetAddress.getByName(host);

            while (running) {
                String status;
                try {
                    socket.send(new DatagramPacket(get, get.length, address, port));
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    socket.receive(response);
                    String line = new String(response.getData(), 0, response.getLength(),
                            StandardCharsets.UTF_8).trim();
                    try {
                        Metrics m = Metrics.parse(line);
                        m.timestamp = System.currentTimeMillis();
                        listener.onMetrics(getAddress(), m);
                        status = "ĐANG NHẬN (UDP)";
                    } catch (IllegalArgumentException e) {
                        status = "DỮ LIỆU SAI";
                    }
                } catch (SocketTimeoutException e) {
                    status = "KHÔNG PHẢN HỒI";
                }

                if (!status.equals(lastStatus)) {
                    listener.onStatus(getAddress(), status);
                    lastStatus = status;
                }
                Thread.sleep(Protocol.INTERVAL_MS);
            }
        } catch (IOException e) {
            if (running) {
                listener.onStatus(getAddress(), "LỖI UDP: " + e.getMessage());
            }
        } catch (InterruptedException e) {
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    @Override
    public void shutdown() {
        running = false;
        if (socket != null) {
            socket.close();
        }
        interrupt();
    }
}
