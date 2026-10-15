package serverhealthmonitoring;

public interface AgentConnection {

    String getAddress();

    void start();

    void shutdown();
}
