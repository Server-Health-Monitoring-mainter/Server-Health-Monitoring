package serverhealthmonitoring;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Giao diện Monitoring Client.
 *
 * Issue #6 - Tuần 2:
 * - Tích hợp AgentManager cho cả TCP và UDP.
 * - Nhận dữ liệu qua MetricsListener.
 * - Tách cột Giao thức khỏi cột Địa chỉ.
 * - Hỗ trợ thêm/xóa Agent và dừng toàn bộ kết nối khi đóng cửa sổ.
 * - Cảnh báo CPU > 80%, RAM > 90% và tô màu cả dòng.
 * - Lưu Metrics bằng MetricsLogger.
 */
public class ClientFrame extends JFrame implements MetricsListener {

    private static final Color COLOR_BACKGROUND = new Color(243, 246, 250);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_HEADER = new Color(23, 37, 61);
    private static final Color COLOR_PRIMARY = new Color(47, 128, 237);
    private static final Color COLOR_PRIMARY_HOVER = new Color(33, 105, 205);
    private static final Color COLOR_DANGER = new Color(220, 53, 69);
    private static final Color COLOR_DANGER_HOVER = new Color(190, 43, 57);
    private static final Color COLOR_TEXT = new Color(34, 45, 58);
    private static final Color COLOR_TEXT_SECONDARY = new Color(108, 117, 125);
    private static final Color COLOR_BORDER = new Color(218, 224, 232);
    private static final Color COLOR_TABLE_HEADER = new Color(236, 241, 247);
    private static final Color COLOR_TABLE_ALT = new Color(248, 250, 252);

    private static final Color STATUS_OK_BG = new Color(220, 252, 231);
    private static final Color STATUS_OK_FG = new Color(22, 101, 52);
    private static final Color STATUS_WARNING_BG = new Color(255, 237, 213);
    private static final Color STATUS_WARNING_FG = new Color(154, 52, 18);
    private static final Color STATUS_ERROR_BG = new Color(254, 226, 226);
    private static final Color STATUS_ERROR_FG = new Color(153, 27, 27);

    private static final double CPU_WARNING_PERCENT = 80.0;
    private static final double MEMORY_WARNING_PERCENT = 90.0;

    private static final DateTimeFormatter LOG_TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private JTextField txtHost;
    private JSpinner spnPort;
    private JComboBox<String> cboProtocol;
    private JButton btnAddAgent;
    private JButton btnRemoveAgent;

    private DefaultTableModel tableModel;
    private JTable tblAgents;
    private JLabel lblAgentCount;
    private JTextArea txtLog;

    private final AgentManager manager = new AgentManager(this);

    /**
     * Map address (host:port) -> model row.
     * AgentManager cũng dùng address làm khóa nên address là duy nhất.
     */
    private final Map<String, Integer> agentRows = new HashMap<>();

    /**
     * Lưu cảnh báo cuối cùng để tránh ghi log lặp lại mỗi 2 giây.
     */
    private final Map<String, String> lastWarningSignature = new HashMap<>();

    /**
     * Theo dõi trạng thái mất kết nối để tránh spam log.
     */
    private final Map<String, Boolean> disconnectedState = new HashMap<>();

    private int nextAgentNumber = 1;

    public ClientFrame() {
        initComponents();
        initFrame();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_HEADER);
        headerPanel.setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new javax.swing.BoxLayout(
                textPanel, javax.swing.BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Server Health Monitoring");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel lblSubtitle = new JLabel("Remote Agent Monitoring Client");
        lblSubtitle.setForeground(new Color(178, 193, 214));
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        textPanel.add(lblTitle);
        textPanel.add(javax.swing.Box.createVerticalStrut(4));
        textPanel.add(lblSubtitle);

        headerPanel.add(textPanel, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 18));
        mainPanel.setBackground(COLOR_BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(22, 25, 25, 25));

        mainPanel.add(createAgentFormCard(), BorderLayout.NORTH);

        JPanel monitoringPanel = new JPanel(new BorderLayout(0, 18));
        monitoringPanel.setOpaque(false);
        monitoringPanel.add(createAgentTableCard(), BorderLayout.CENTER);
        monitoringPanel.add(createLogCard(), BorderLayout.SOUTH);

        mainPanel.add(monitoringPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createAgentFormCard() {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(18, 22, 20, 22)));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Kết nối Agent Server");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(COLOR_TEXT);

        JLabel lblDescription = new JLabel(
                "Nhập địa chỉ Agent cần giám sát");
        lblDescription.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDescription.setForeground(COLOR_TEXT_SECONDARY);

        JPanel titles = new JPanel();
        titles.setLayout(new javax.swing.BoxLayout(
                titles, javax.swing.BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(lblTitle);
        titles.add(javax.swing.Box.createVerticalStrut(3));
        titles.add(lblDescription);
        titlePanel.add(titles, BorderLayout.WEST);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        formPanel.add(createFieldLabel("Host"), gbc);

        txtHost = new JTextField("127.0.0.1");
        styleTextField(txtHost);
        txtHost.setPreferredSize(new Dimension(230, 38));

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(txtHost, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(createFieldLabel("Cổng"), gbc);

        spnPort = new JSpinner(new SpinnerNumberModel(
                Protocol.DEFAULT_PORT, 1, 65535, 1));
        styleSpinner(spnPort);
        spnPort.setPreferredSize(new Dimension(100, 38));

        gbc.gridx = 3;
        formPanel.add(spnPort, gbc);

        gbc.gridx = 4;
        formPanel.add(createFieldLabel("Giao thức"), gbc);

        cboProtocol = new JComboBox<>(new String[]{
            AgentManager.TCP, AgentManager.UDP
        });
        styleComboBox(cboProtocol);
        cboProtocol.setPreferredSize(new Dimension(95, 38));

        gbc.gridx = 5;
        formPanel.add(cboProtocol, gbc);

        btnAddAgent = new JButton("Thêm agent");
        stylePrimaryButton(btnAddAgent);

        gbc.gridx = 6;
        gbc.insets = new Insets(0, 4, 0, 8);
        formPanel.add(btnAddAgent, gbc);

        btnRemoveAgent = new JButton("Xóa agent");
        styleDangerButton(btnRemoveAgent);

        gbc.gridx = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(btnRemoveAgent, gbc);

        btnAddAgent.addActionListener(e -> addAgentFromForm());
        btnRemoveAgent.addActionListener(e -> removeSelectedAgent());
        txtHost.addActionListener(e -> addAgentFromForm());

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(formPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAgentTableCard() {
        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(COLOR_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(18, 22, 18, 22)));

        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Danh sách Agent");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(COLOR_TEXT);

        lblAgentCount = new JLabel("0 agent");
        lblAgentCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblAgentCount.setForeground(COLOR_TEXT_SECONDARY);

        tableHeaderPanel.add(lblTitle, BorderLayout.WEST);
        tableHeaderPanel.add(lblAgentCount, BorderLayout.EAST);

        String[] columns = {
            "Agent",
            "Địa chỉ",
            "Giao thức",
            "CPU %",
            "Memory %",
            "Số kết nối",
            "Trạng thái"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        /*
         * Tô màu CẢ DÒNG dựa trên trạng thái.
         * row là view row nên phải convert sang model row vì JTable có sorter.
         */
        tblAgents = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(
                    javax.swing.table.TableCellRenderer renderer,
                    int row,
                    int column) {

                Component component = super.prepareRenderer(renderer, row, column);

                if (isRowSelected(row)) {
                    component.setBackground(getSelectionBackground());
                    component.setForeground(getSelectionForeground());
                    return component;
                }

                int modelRow = convertRowIndexToModel(row);
                String status = String.valueOf(
                        tableModel.getValueAt(modelRow, 6)).toUpperCase();

                if (isDisconnectedStatus(status)) {
                    component.setBackground(STATUS_ERROR_BG);
                    component.setForeground(STATUS_ERROR_FG);
                } else if (status.contains("CẢNH BÁO")
                        || status.contains("DỮ LIỆU SAI")) {
                    component.setBackground(STATUS_WARNING_BG);
                    component.setForeground(STATUS_WARNING_FG);
                } else if (status.contains("ỔN ĐỊNH")) {
                    component.setBackground(STATUS_OK_BG);
                    component.setForeground(STATUS_OK_FG);
                } else {
                    component.setBackground(row % 2 == 0
                            ? Color.WHITE : COLOR_TABLE_ALT);
                    component.setForeground(COLOR_TEXT);
                }

                return component;
            }
        };

        styleTable();

        JScrollPane scrollPane = new JScrollPane(tblAgents);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(tableHeaderPanel, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLogCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(COLOR_CARD);
        card.setPreferredSize(new Dimension(0, 170));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(14, 18, 14, 18)));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Nhật ký giám sát");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_TEXT);

        JLabel lblThreshold = new JLabel(String.format(
                "Ngưỡng cảnh báo: CPU > %.0f%%  |  RAM > %.0f%%",
                CPU_WARNING_PERCENT,
                MEMORY_WARNING_PERCENT));
        lblThreshold.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblThreshold.setForeground(COLOR_TEXT_SECONDARY);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblThreshold, BorderLayout.EAST);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setForeground(COLOR_TEXT);
        txtLog.setBackground(new Color(248, 250, 252));
        txtLog.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane logScroll = new JScrollPane(txtLog);
        logScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        card.add(header, BorderLayout.NORTH);
        card.add(logScroll, BorderLayout.CENTER);
        return card;
    }

    private void styleTable() {
        tblAgents.setRowHeight(38);
        tblAgents.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblAgents.setForeground(COLOR_TEXT);
        tblAgents.setBackground(Color.WHITE);
        tblAgents.setGridColor(new Color(235, 239, 244));
        tblAgents.setShowVerticalLines(false);
        tblAgents.setShowHorizontalLines(true);
        tblAgents.setSelectionBackground(new Color(220, 235, 252));
        tblAgents.setSelectionForeground(COLOR_TEXT);
        tblAgents.setFillsViewportHeight(true);
        tblAgents.setAutoCreateRowSorter(true);

        tblAgents.getTableHeader().setBackground(COLOR_TABLE_HEADER);
        tblAgents.getTableHeader().setForeground(COLOR_TEXT);
        tblAgents.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblAgents.getTableHeader().setPreferredSize(new Dimension(0, 40));
        tblAgents.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        for (int column = 2; column <= 6; column++) {
            tblAgents.getColumnModel()
                    .getColumn(column)
                    .setCellRenderer(centerRenderer);
        }

        tblAgents.getColumnModel().getColumn(0).setPreferredWidth(125);
        tblAgents.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblAgents.getColumnModel().getColumn(2).setPreferredWidth(85);
        tblAgents.getColumnModel().getColumn(3).setPreferredWidth(85);
        tblAgents.getColumnModel().getColumn(4).setPreferredWidth(95);
        tblAgents.getColumnModel().getColumn(5).setPreferredWidth(105);
        tblAgents.getColumnModel().getColumn(6).setPreferredWidth(135);
    }

    /**
     * Thêm Agent qua AgentManager cho cả TCP và UDP.
     */
    private void addAgentFromForm() {
        String host = txtHost.getText().trim();
        int port = (Integer) spnPort.getValue();
        String protocol = (String) cboProtocol.getSelectedItem();

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng nhập địa chỉ Host.",
                    "Thiếu thông tin",
                    JOptionPane.WARNING_MESSAGE);
            txtHost.requestFocusInWindow();
            return;
        }

        try {
            /*
             * AgentManager trả về đúng address theo AgentConnection.getAddress():
             * host:port.
             */
            String address = manager.addAgent(host, port, protocol);

            int modelRow = tableModel.getRowCount();
            String agentName = "Agent " + nextAgentNumber;

            tableModel.addRow(new Object[]{
                agentName,
                address,
                protocol,
                "--",
                "--",
                "--",
                "ĐANG KẾT NỐI"
            });

            agentRows.put(address, modelRow);
            disconnectedState.put(address, false);
            nextAgentNumber++;
            updateAgentCount();

            appendLog(
                    "INFO",
                    agentName + " - Đã thêm "
                    + address + " qua " + protocol);

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Không thể thêm Agent",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Xóa Agent đang chọn trong JTable.
     */
    private void removeSelectedAgent() {
        int viewRow = tblAgents.getSelectedRow();

        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn Agent cần xóa.",
                    "Chưa chọn Agent",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int modelRow = tblAgents.convertRowIndexToModel(viewRow);
        String address = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String agentName = getAgentName(modelRow);

        manager.removeAgent(address);

        tableModel.removeRow(modelRow);
        agentRows.remove(address);
        lastWarningSignature.remove(address);
        disconnectedState.remove(address);

        rebuildAgentRows();
        updateAgentCount();

        appendLog("INFO", agentName + " - Đã xóa Agent " + address);
    }

    /**
     * Sau khi xóa một dòng, index các dòng phía sau thay đổi.
     */
    private void rebuildAgentRows() {
        agentRows.clear();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object address = tableModel.getValueAt(row, 1);
            if (address != null) {
                agentRows.put(address.toString(), row);
            }
        }
    }

    /**
     * Callback chạy từ thread kết nối.
     * Lưu CSV rồi chuyển phần cập nhật Swing về EDT.
     */
    @Override
    public void onMetrics(String address, Metrics metrics) {
        saveMetrics(address, metrics);

        SwingUtilities.invokeLater(
                () -> updateMetricsOnUi(address, metrics));
    }

    /**
     * Callback trạng thái chạy từ thread kết nối.
     * Mọi cập nhật JTable/log được chuyển về EDT.
     */
    @Override
    public void onStatus(String address, String status) {
        SwingUtilities.invokeLater(
                () -> updateStatusOnUi(address, status));
    }

    /**
     * Tuần 2 dùng MetricsLogger của Issue #15 / PR #19.
     */
    private void saveMetrics(String address, Metrics metrics) {
        try {
            MetricsLogger.log(address, metrics);
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> appendLog(
                    "LỖI LƯU TRỮ",
                    "Không thể lưu Metrics của "
                    + address + ": " + e.getMessage()));
        }
    }

    /**
     * Chỉ phục vụ ClientFrameTest, không tạo kết nối thật.
     */
    void addTestAgent(String address, String agentName) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(
                    () -> addTestAgent(address, agentName));
            return;
        }

        if (address == null
                || address.trim().isEmpty()
                || agentRows.containsKey(address)) {
            return;
        }

        String displayName =
                (agentName == null || agentName.trim().isEmpty())
                ? "TestAgent"
                : agentName.trim();

        int modelRow = tableModel.getRowCount();

        tableModel.addRow(new Object[]{
            displayName,
            address,
            "TEST",
            "--",
            "--",
            "--",
            "CHƯA CÓ DỮ LIỆU"
        });

        agentRows.put(address, modelRow);
        disconnectedState.put(address, false);
        updateAgentCount();

        appendLog(
                "TEST",
                displayName + " - Đã thêm Agent mô phỏng " + address);
    }

    private void updateMetricsOnUi(String address, Metrics metrics) {
        Integer row = agentRows.get(address);

        if (row == null) {
            return;
        }

        if (metrics.name != null && !metrics.name.trim().isEmpty()) {
            tableModel.setValueAt(metrics.name, row, 0);
        }

        tableModel.setValueAt(
                String.format("%.1f%%", metrics.cpu), row, 3);

        tableModel.setValueAt(
                String.format("%.1f%%", metrics.memPercent()), row, 4);

        tableModel.setValueAt(metrics.connections, row, 5);

        List<String> warnings = collectWarnings(metrics);

        if (warnings.isEmpty()) {
            tableModel.setValueAt("ỔN ĐỊNH", row, 6);

            String oldWarning = lastWarningSignature.remove(address);
            if (oldWarning != null && !oldWarning.isEmpty()) {
                appendLog(
                        "PHỤC HỒI",
                        getAgentName(row)
                        + " - CPU/RAM đã trở lại dưới ngưỡng");
            }
        } else {
            tableModel.setValueAt("CẢNH BÁO", row, 6);

            String signature = String.join(" | ", warnings);
            String previous = lastWarningSignature.get(address);

            if (!signature.equals(previous)) {
                appendLog(
                        "CẢNH BÁO",
                        getAgentName(row) + " - " + signature);

                lastWarningSignature.put(address, signature);
            }
        }

        /*
         * Không reset disconnectedState ở đây.
         * Trạng thái kết nối thật được quản lý ở onStatus().
         */
        tblAgents.repaint();
    }

    private void updateStatusOnUi(String address, String rawStatus) {
        Integer row = agentRows.get(address);

        if (row == null || rawStatus == null) {
            return;
        }

        String status = rawStatus.trim();

        /*
         * TCP đã kết nối hoặc UDP đang nhận dữ liệu.
         * Nếu Metrics đang vượt ngưỡng thì vẫn giữ CẢNH BÁO.
         */
        if (status.startsWith("ĐÃ KẾT NỐI")
                || status.startsWith("ĐANG NHẬN (UDP)")) {

            boolean wasDisconnected =
                    disconnectedState.getOrDefault(address, false);

            disconnectedState.put(address, false);

            if (!lastWarningSignature.containsKey(address)) {
                tableModel.setValueAt("ỔN ĐỊNH", row, 6);
            }

            if (wasDisconnected) {
                appendLog(
                        "KẾT NỐI",
                        getAgentName(row)
                        + " - Đã phục hồi kết nối tới " + address);
            } else if (status.startsWith("ĐÃ KẾT NỐI")) {
                appendLog(
                        "KẾT NỐI",
                        getAgentName(row)
                        + " - Đã kết nối tới " + address);
            }

            tblAgents.repaint();
            return;
        }

        /*
         * TCP mất kết nối hoặc UDP không phản hồi/lỗi mạng -> đỏ.
         */
        if (isDisconnectedStatus(status)) {
            tableModel.setValueAt("MẤT KẾT NỐI", row, 6);

            boolean alreadyDisconnected =
                    disconnectedState.getOrDefault(address, false);

            if (!alreadyDisconnected) {
                appendLog(
                        "MẤT KẾT NỐI",
                        getAgentName(row) + " - " + status);
            }

            disconnectedState.put(address, true);
            lastWarningSignature.remove(address);

            tblAgents.repaint();
            return;
        }

        /*
         * Dữ liệu sai: vẫn còn kết nối nhưng dữ liệu không hợp lệ.
         */
        if (status.startsWith("DỮ LIỆU SAI")) {
            tableModel.setValueAt("CẢNH BÁO", row, 6);
            appendLog(
                    "CẢNH BÁO",
                    getAgentName(row) + " - " + status);
            tblAgents.repaint();
            return;
        }

        if (status.startsWith("ĐANG KẾT NỐI")) {
            tableModel.setValueAt("ĐANG KẾT NỐI", row, 6);
            tblAgents.repaint();
        }
    }

    private boolean isDisconnectedStatus(String status) {
        if (status == null) {
            return false;
        }

        String upper = status.toUpperCase();

        return upper.contains("MẤT KẾT NỐI")
                || upper.contains("KHÔNG PHẢN HỒI")
                || upper.contains("LỖI UDP");
    }

    /**
     * Đúng yêu cầu Tuần 2:
     * - CPU > 80%
     * - RAM > 90%
     *
     * Không dùng số kết nối làm ngưỡng cảnh báo ở Tuần 2.
     */
    private List<String> collectWarnings(Metrics metrics) {
        List<String> warnings = new ArrayList<>();

        if (metrics.cpu > CPU_WARNING_PERCENT) {
            warnings.add(String.format(
                    "CPU %.1f%% vượt ngưỡng %.0f%%",
                    metrics.cpu,
                    CPU_WARNING_PERCENT));
        }

        double memoryPercent = metrics.memPercent();

        if (memoryPercent > MEMORY_WARNING_PERCENT) {
            warnings.add(String.format(
                    "RAM %.1f%% vượt ngưỡng %.0f%%",
                    memoryPercent,
                    MEMORY_WARNING_PERCENT));
        }

        return warnings;
    }

    private String getAgentName(int modelRow) {
        Object value = tableModel.getValueAt(modelRow, 0);
        return value == null ? "Agent" : value.toString();
    }

    private void appendLog(String level, String message) {
        if (txtLog == null) {
            return;
        }

        String time = LocalTime.now().format(LOG_TIME_FORMAT);

        txtLog.append(
                String.format("[%s] [%s] %s%n",
                        time, level, message));

        txtLog.setCaretPosition(
                txtLog.getDocument().getLength());
    }

    private void updateAgentCount() {
        lblAgentCount.setText(
                tableModel.getRowCount() + " agent");
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(COLOR_TEXT_SECONDARY);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(COLOR_TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(6, 10, 6, 10)));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinner.setBorder(
                BorderFactory.createLineBorder(COLOR_BORDER));

        JSpinner.DefaultEditor editor =
                (JSpinner.DefaultEditor) spinner.getEditor();

        editor.getTextField().setFont(
                new Font("Segoe UI", Font.PLAIN, 13));

        editor.getTextField().setBorder(
                new EmptyBorder(5, 7, 5, 7));
    }

    private void styleComboBox(
            JComboBox<String> comboBox) {

        comboBox.setFont(
                new Font("Segoe UI", Font.PLAIN, 13));

        comboBox.setBackground(Color.WHITE);
        comboBox.setForeground(COLOR_TEXT);
    }

    private void stylePrimaryButton(JButton button) {
        button.setFont(
                new Font("Segoe UI", Font.BOLD, 13));

        button.setForeground(Color.WHITE);
        button.setBackground(COLOR_PRIMARY);
        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(10, 20, 10, 20));

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(
                    java.awt.event.MouseEvent evt) {
                button.setBackground(
                        COLOR_PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(
                    java.awt.event.MouseEvent evt) {
                button.setBackground(
                        COLOR_PRIMARY);
            }
        });
    }

    private void styleDangerButton(JButton button) {
        button.setFont(
                new Font("Segoe UI", Font.BOLD, 13));

        button.setForeground(Color.WHITE);
        button.setBackground(COLOR_DANGER);
        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(10, 20, 10, 20));

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(
                new java.awt.event.MouseAdapter() {

            @Override
            public void mouseEntered(
                    java.awt.event.MouseEvent evt) {
                button.setBackground(
                        COLOR_DANGER_HOVER);
            }

            @Override
            public void mouseExited(
                    java.awt.event.MouseEvent evt) {
                button.setBackground(
                        COLOR_DANGER);
            }
        });
    }

    private void initFrame() {
        setTitle("Server Health Monitoring");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                manager.stopAll();
                dispose();
            }
        });
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info
                    : UIManager.getInstalledLookAndFeels()) {

                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(
                            info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println(
                    "Không thể bật Nimbus Look & Feel.");
        }

        SwingUtilities.invokeLater(() -> {
            ClientFrame frame = new ClientFrame();
            frame.setVisible(true);
        });
    }
}
