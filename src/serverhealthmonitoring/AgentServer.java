package serverhealthmonitoring;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentServer {

    private final int port;
    private final String name;

    // Dem so ket noi TCP dang hoat dong cua Agent
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    // Thu thap CPU va RAM thuc tu he dieu hanh
    private final MetricsCollector collector = new MetricsCollector();

    public AgentServer(int port, String name) {
        this.port = port;
        this.name = name;
    }

    // Khoi dong dong thoi TCP va UDP
    public void start() {
        new Thread(this::startTcpServer, "Agent-TCP-" + port).start();
        new Thread(this::startUdpServer, "Agent-UDP-" + port).start();
    }

    // ================= TCP SERVER =================
    private void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("TCP Server [" + name
                    + "] dang lang nghe port " + port + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                // Tang bo dem khi Client ket noi
                activeConnections.incrementAndGet();

                // Moi Client duoc xu ly trong mot luong rieng
                new Thread(
                        new ClientHandler(clientSocket),
                        "Agent-Client-" + clientSocket.getPort()
                ).start();
            }

        } catch (IOException e) {
            System.err.println("Loi TCP Server: " + e.getMessage());
        }
    }

    // ================= UDP SERVER =================
    private void startUdpServer() {
        try (DatagramSocket udpSocket = new DatagramSocket(port)) {

            System.out.println("UDP Server [" + name
                    + "] dang lang nghe port " + port + "...");

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket request =
                        new DatagramPacket(buffer, buffer.length);

                udpSocket.receive(request);

                String msg = new String(
                        request.getData(),
                        request.getOffset(),
                        request.getLength(),
                        StandardCharsets.UTF_8
                ).trim();

                // Chi tra du lieu khi Client gui lenh GET
                if (Protocol.CMD_GET.equalsIgnoreCase(msg)) {

                    // Lay CPU va RAM thuc
                    Metrics m = collector.collect(name);

                    // So ket noi do AgentServer quan ly
                    m.connections = activeConnections.get();

                    byte[] sendData = m.toLine().getBytes(
                            StandardCharsets.UTF_8
                    );

                    DatagramPacket response = new DatagramPacket(
                            sendData,
                            sendData.length,
                            request.getAddress(),
                            request.getPort()
                    );

                    udpSocket.send(response);
                }
            }

        } catch (IOException e) {
            System.err.println("Loi UDP Server: " + e.getMessage());
        }
    }

    // ================= TCP CLIENT HANDLER =================
    private class ClientHandler implements Runnable {

        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {

            try (Socket socket = clientSocket;
                 PrintWriter out = new PrintWriter(
                         socket.getOutputStream(), true,
                         StandardCharsets.UTF_8);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(
                                 socket.getInputStream(),
                                 StandardCharsets.UTF_8))) {

                // Cho toi da mot chu ky de nhan lenh
                socket.setSoTimeout(Protocol.INTERVAL_MS);

                while (!socket.isClosed()) {

                    // Gui Metrics dinh ky cho Client TCP
                    if (!sendMetrics(out)) {
                        break;
                    }

                    try {
                        // Doc lenh GET hoac QUIT tu Client
                        String command = in.readLine();

                        // Client da dong ket noi
                        if (command == null) {
                            break;
                        }

                        command = command.trim();

                        // Client yeu cau ngat ket noi
                        if (Protocol.CMD_QUIT.equalsIgnoreCase(command)) {
                            System.out.println(
                                    "Client gui QUIT: "
                                    + socket.getRemoteSocketAddress()
                            );
                            break;
                        }

                        // Client chu dong yeu cau Metrics
                        if (Protocol.CMD_GET.equalsIgnoreCase(command)) {
                            if (!sendMetrics(out)) {
                                break;
                            }
                        }

                    } catch (SocketTimeoutException e) {
                        // Khong co lenh trong mot chu ky:
                        // tiep tuc gui Metrics dinh ky.
                    }
                }

            } catch (IOException e) {
                System.err.println(
                        "Client ngat ket noi: " + e.getMessage()
                );

            } finally {
                // Giam bo dem dung mot lan khi Client thoat
                int remaining = activeConnections.decrementAndGet();

                System.out.println(
                        "Client da ngat. Ket noi con lai: " + remaining
                );
            }
        }

        // Thu thap va gui mot dong Metrics
        private boolean sendMetrics(PrintWriter out) {

            Metrics m = collector.collect(name);
            m.connections = activeConnections.get();

            out.println(m.toLine());

            // Phat hien loi ghi khi Client mat ket noi
            return !out.checkError();
        }
    }

    // ================= MAIN =================
    public static void main(String[] args) {

        int currentPort = Protocol.DEFAULT_PORT;
        String currentName = "DefaultAgent";

        for (int i = 0; i < args.length; i++) {

            if ("--port".equals(args[i]) && i + 1 < args.length) {
                currentPort = Integer.parseInt(args[++i]);

            } else if ("--name".equals(args[i]) && i + 1 < args.length) {
                currentName = args[++i];
            }
        }

        AgentServer server = new AgentServer(
                currentPort, currentName
        );

        server.start();
    }
}
    

