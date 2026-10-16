package serverhealthmonitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class TcpConnection extends Thread implements AgentConnection {

    private static final int CONNECT_TIMEOUT_MS = 3000;   
    private static final int READ_TIMEOUT_MS = 15000;     
    private static final int RETRY_MS = 5000;             

    private final String host;
    private final int port;
    private final MetricsListener listener;
    private volatile boolean running = true;
    private Socket socket;

    public TcpConnection(String host, int port, MetricsListener listener) {
        this.host = host;
        this.port = port;
        this.listener = listener;
        setDaemon(true);                                   
        setName("TCP-" + host + ":" + port);
    }

    @Override
    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public void run() {
        while (running) {
            try {
                listener.onStatus(getAddress(), "ĐANG KẾT NỐI");
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
                socket.setSoTimeout(READ_TIMEOUT_MS);
                listener.onStatus(getAddress(), "ĐÃ KẾT NỐI");

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while (running && (line = in.readLine()) != null) {
                    try {
                        Metrics m = Metrics.parse(line);
                        m.timestamp = System.currentTimeMillis();
                        listener.onMetrics(getAddress(), m);
                    } catch (IllegalArgumentException e) {
                        listener.onStatus(getAddress(), "DỮ LIỆU SAI: " + line);
                    }
                }
                if (running) listener.onStatus(getAddress(), "MẤT KẾT NỐI");
            } catch (IOException e) {
                if (running) listener.onStatus(getAddress(), "MẤT KẾT NỐI: " + e.getMessage());
            } finally {
                closeSocket();
            }
            if (running) {
                try {
                    Thread.sleep(RETRY_MS);           
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    @Override
    public void shutdown() {
        running = false;
        closeSocket();
        interrupt();
    }

    private void closeSocket() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}