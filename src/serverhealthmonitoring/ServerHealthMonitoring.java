package serverhealthmonitoring;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Điểm khởi động chung của chương trình (main class của project).
 *
 * Cách chạy:
 *   (không tham số)                          -> mở Monitoring Client (giao diện)
 *   client                                   -> mở Monitoring Client (giao diện)
 *   agent [--port 6000] [--name web-01]      -> chạy Agent Server trên máy này
 *   --help                                   -> in hướng dẫn
 *
 * Nếu không truyền --name, Agent lấy tên máy tính làm tên.
 */
public class ServerHealthMonitoring {

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "client";
        String[] rest = args.length > 0 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        switch (mode) {
            case "agent":
                startAgent(rest);
                break;
            case "client":
                ClientFrame.main(rest);
                break;
            case "--help":
            case "-h":
            case "help":
                printUsage();
                break;
            default:
                System.err.println("Chế độ không hợp lệ: " + args[0]);
                printUsage();
                System.exit(1);
        }
    }

    /** Chạy Agent Server, tự thêm tên máy nếu người dùng không truyền --name. */
    private static void startAgent(String[] options) {
        List<String> agentArgs = new ArrayList<>(Arrays.asList(options));
        if (!agentArgs.contains("--name")) {
            agentArgs.add("--name");
            agentArgs.add(hostName());
        }
        System.out.println("Khởi động Agent Server: " + String.join(" ", agentArgs));
        AgentServer.main(agentArgs.toArray(new String[0]));
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "agent";
        }
    }

    private static void printUsage() {
        System.out.println("Cách dùng:");
        System.out.println("  java -jar ServerHealthMonitoring.jar                 Mở Monitoring Client");
        System.out.println("  java -jar ServerHealthMonitoring.jar client          Mở Monitoring Client");
        System.out.println("  java -jar ServerHealthMonitoring.jar agent [--port "
                + Protocol.DEFAULT_PORT + "] [--name web-01]");
        System.out.println("                                                       Chạy Agent Server (TCP + UDP)");
    }
}
