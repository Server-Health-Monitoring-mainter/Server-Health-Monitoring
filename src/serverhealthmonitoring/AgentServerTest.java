package serverhealthmonitoring;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
/**
 *
 */
public class AgentServerTest {
public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", Protocol.DEFAULT_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            String data = in.readLine();
            // Đọc và kiểm tra dòng dữ liệu
            if (data != null && data.startsWith("METRICS|")) {
                System.out.println("Nhan duoc: " + data);
                System.out.println("KET QUA: DUNG");
            } else {
                System.out.println("KET QUA: SAI - Sai dinh dang du lieu");
            }
        } catch (Exception e) {
            System.out.println("KET QUA: SAI - Loi ket noi: " + e.getMessage());
        }
    }
}
