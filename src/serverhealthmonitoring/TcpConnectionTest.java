/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package serverhealthmonitoring;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class TcpConnectionTest {

    public static void main(String[] args) throws Exception {
       
        Thread fakeAgent = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(7000);
                 Socket s = ss.accept();
                 PrintWriter out = new PrintWriter(
                         new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {
                for (int i = 1; i <= 5; i++) {
                    Metrics m = new Metrics();
                    m.name = "agent-gia";
                    m.cpu = 10 * i;
                    m.memUsedMB = 4000;
                    m.memTotalMB = 8000;
                    m.connections = 20 + i;
                    out.println(m.toLine());
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        fakeAgent.start();
        Thread.sleep(500);

        
        TcpConnection conn = new TcpConnection("localhost", 7000, new MetricsListener() {
            @Override
            public void onMetrics(String address, Metrics m) {
                System.out.println("Nhận từ " + address + ": " + m);
            }

            @Override
            public void onStatus(String address, String status) {
                System.out.println("[" + address + "] " + status);
            }
        });
        conn.start();

        Thread.sleep(8000);   
        conn.shutdown();
        System.out.println("KẾT THÚC CHẠY THỬ");
    }
}

