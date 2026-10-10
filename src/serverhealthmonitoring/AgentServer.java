package serverhealthmonitoring;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
/**
 *
 */
public class AgentServer {
private int port;
    private String name;
    // Đếm số kết nối động an toàn giữa các luồng
    private static final AtomicInteger activeConnections = new AtomicInteger(0);

    public AgentServer(int port, String name) {
        this.port = port;
        this.name = name;
    }

    public void start() {
        new Thread(this::startTcpServer).start();
        new Thread(this::startUdpServer).start();
    }

    private void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP Server [" + name + "] dang lang nghe port " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                activeConnections.incrementAndGet(); // Tăng khi có kết nối
                new Thread(new ClientHandler(clientSocket, name)).start();
            }
        } catch (IOException e) {
            System.err.println("Loi TCP Server: " + e.getMessage());
        }
    }

    private void startUdpServer() {
        try (DatagramSocket udpSocket = new DatagramSocket(port)) {
            System.out.println("UDP Server [" + name + "] dang lang nghe port " + port + "...");
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(request);
                
                String msg = new String(request.getData(), 0, request.getLength()).trim();
                if ("GET".equalsIgnoreCase(msg)) {
                    // Dùng class Metrics thay vì tự ghi chuỗi
                    Metrics m = new Metrics();
                    m.name = name;
                    m.cpu = 25.0;
                    m.memUsedMB = 1024;
                    m.memTotalMB = 4096;
                    m.connections = activeConnections.get();
                    
                    byte[] sendData = m.toLine().getBytes();
                    DatagramPacket response = new DatagramPacket(sendData, sendData.length, request.getAddress(), request.getPort());
                    udpSocket.send(response);
                }
            }
        } catch (IOException e) {
            System.err.println("Loi UDP Server: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String serverName;

        public ClientHandler(Socket socket, String name) {
            this.clientSocket = socket;
            this.serverName = name;
        }

        @Override
        public void run() {
            try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                while (!clientSocket.isClosed()) {
                    Metrics m = new Metrics();
                    m.name = serverName;
                    m.cpu = 25.0;
                    m.memUsedMB = 1024;
                    m.memTotalMB = 4096;
                    m.connections = activeConnections.get();
                    
                    out.println(m.toLine());
                    
                    // Ngắt luồng nếu Client ngắt đột ngột (kẹt luồng)
                    if (out.checkError()) {
                        break; 
                    }
                    Thread.sleep(Protocol.INTERVAL_MS); 
                }
            } catch (Exception e) {
                System.err.println("Client ngat ket noi.");
            } finally {
                activeConnections.decrementAndGet(); // Giảm khi Client thoát
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Bo qua
                }
            }
        }
    }

    public static void main(String[] args) {
        int currentPort = Protocol.DEFAULT_PORT;
        String currentName = "DefaultAgent";

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                currentPort = Integer.parseInt(args[i + 1]);
            } else if ("--name".equals(args[i]) && i + 1 < args.length) {
                currentName = args[i + 1];
            }
        }

        AgentServer server = new AgentServer(currentPort, currentName);
        server.start();
    }
}
    

