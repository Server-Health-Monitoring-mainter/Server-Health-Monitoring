package serverhealthmonitoring;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class AgentManager {

    public static final String TCP = "TCP";
    public static final String UDP = "UDP";

    private final MetricsListener listener;
    private final Map<String, AgentConnection> connections = new LinkedHashMap<>();

    public AgentManager(MetricsListener listener) {
        this.listener = listener;
    }

    public synchronized String addAgent(String host, int port, String protocol) {
        String address = host + ":" + port;
        if (connections.containsKey(address)) {
            throw new IllegalArgumentException("Agent " + address + " đã có trong danh sách.");
        }
        AgentConnection conn;
        if (TCP.equalsIgnoreCase(protocol)) {
            conn = new TcpConnection(host, port, listener);
        } else if (UDP.equalsIgnoreCase(protocol)) {
            conn = new UdpConnection(host, port, listener);
        } else {
            throw new IllegalArgumentException("Giao thức không hợp lệ: " + protocol);
        }
        connections.put(address, conn);
        conn.start();
        return address;
    }
    
    
    public synchronized void removeAgent(String address) {
        AgentConnection conn = connections.remove(address);
        if (conn != null) {
            conn.shutdown();
        }
    }

    public synchronized void stopAll() {
        for (AgentConnection conn : connections.values()) {
            conn.shutdown();
        }
        connections.clear();
    }

    public synchronized List<String> getAddresses() {
        return new ArrayList<>(connections.keySet());
    }

    public synchronized int size() {
        return connections.size();
    }
}
