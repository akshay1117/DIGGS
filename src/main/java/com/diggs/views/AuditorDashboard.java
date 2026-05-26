// src/main/java/com/diggs/views/AuditorDashboard.java
package com.diggs.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.diggs.audit.AuditTrail;
import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.Auditor;
import com.diggs.model.Grievance;
import com.diggs.model.GrievanceHistory;
import com.diggs.model.User;
import com.diggs.service.AuthenticationService;

public class AuditorDashboard extends JFrame {
    private Auditor auditor;
    private JTable grievancesTable;
    private DefaultTableModel grievancesTableModel;
    private JTable auditTable;
    private DefaultTableModel auditTableModel;
    private JTextArea reportArea;
    private JComboBox<String> reportTypeCombo;
    private JComboBox<String> departmentFilterCombo;
    private JTextField startDateField;
    private JTextField endDateField;
    private JLabel statsLabel;
    private JPanel overviewPanel;
    private JPanel statsCardsPanel;
    private JPanel chartsPanel;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private JTable usersTable;
    private DefaultTableModel usersTableModel;

    public AuditorDashboard(Auditor auditor) {
        this.auditor = auditor;
        setTitle("DIGGS - Auditor Dashboard - " + auditor.getName());
        setSize(1400, 850);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());

        // Add Header
        mainContainer.add(createHeaderPanel(), BorderLayout.NORTH);

        // Main panel with tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Tab 1: Overview & Statistics
        tabbedPane.addTab("📊 Dashboard Overview", createOverviewPanel());

        // Tab 2: Grievances Analysis
        tabbedPane.addTab("📋 Grievances Analysis", createGrievancesPanel());

        // Tab 3: Audit Trail
        tabbedPane.addTab("🔍 Audit Trail", createAuditTrailPanel());

        // Tab 4: Compliance Reports
        tabbedPane.addTab("📄 Reports Generator", createReportsPanel());

        // Tab 5: System Health
        tabbedPane.addTab("⚙️ System Health", createSystemHealthPanel());

        // Tab 6: User Management
        tabbedPane.addTab("👥 User Management", createUserManagementPanel());

        mainContainer.add(tabbedPane, BorderLayout.CENTER);
        add(mainContainer);

        // Create menu bar
        createMenuBar();

        // Load initial data
        refreshAll();

        // Auto-refresh every 60 seconds
        startAutoRefresh();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 45, 75)); // Court Navy Blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        brandPanel.setOpaque(false);

        javax.swing.ImageIcon logo = com.diggs.utils.ImageUtils.loadScaledLogo(60, 60);
        JLabel logoLabel;
        if (logo != null) {
            logoLabel = new JLabel(logo);
        } else {
            logoLabel = new JLabel("⚖️"); // Fallback emoji
            logoLabel.setFont(new Font("Georgia", Font.PLAIN, 40));
            logoLabel.setForeground(new Color(200, 180, 130)); // Gold
        }

        JLabel titleLabel = new JLabel("DIGGS - Court Application Module");
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        brandPanel.add(logoLabel);
        brandPanel.add(titleLabel);

        headerPanel.add(brandPanel, BorderLayout.WEST);

        // Auditor info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        infoPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome, " + auditor.getName() + " | Auditor Level: " + auditor.getRole());
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        welcomeLabel.setForeground(new Color(200, 180, 130)); // Gold
        infoPanel.add(welcomeLabel);

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(40, 50, 65));
        logoutButton.setOpaque(true);
        logoutButton.setBorderPainted(false);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFocusPainted(false);
        logoutButton.addActionListener(e -> logout());
        infoPanel.add(logoutButton);

        headerPanel.add(infoPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createOverviewPanel() {
        overviewPanel = new JPanel(new BorderLayout());
        overviewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Statistics cards panel
        statsCardsPanel = new JPanel(new GridLayout(2, 4, 15, 15));
        statsCardsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Charts section
        chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        chartsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Department-wise statistics
        JPanel deptPanel = new JPanel(new BorderLayout());
        deptPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 102, 204)),
                "Department-wise Statistics",
                TitledBorder.LEFT, TitledBorder.TOP));

        JTable deptTable = new JTable();
        deptPanel.add(new JScrollPane(deptTable), BorderLayout.CENTER);
        chartsPanel.add(deptPanel);

        // Status-wise statistics
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 102, 204)),
                "Status Distribution",
                TitledBorder.LEFT, TitledBorder.TOP));

        JTable statusTable = new JTable();
        statusPanel.add(new JScrollPane(statusTable), BorderLayout.CENTER);
        chartsPanel.add(statusPanel);

        // Performance metrics panel
        JPanel performancePanel = new JPanel(new BorderLayout());
        performancePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 102, 204)),
                "Performance Metrics",
                TitledBorder.LEFT, TitledBorder.TOP));

        JTextArea metricsArea = new JTextArea();
        metricsArea.setEditable(false);
        metricsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        performancePanel.add(new JScrollPane(metricsArea), BorderLayout.CENTER);

        // Update all components
        updateStatisticsCards();
        updateDepartmentTable(deptTable);
        updateStatusTable(statusTable);
        updatePerformanceMetrics(metricsArea);

        // Add refresh button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshStatsButton = new JButton("🔄 Refresh Statistics");
        refreshStatsButton.setBackground(new Color(0, 102, 204));
        refreshStatsButton.setOpaque(true);
        refreshStatsButton.setBorderPainted(false);
        refreshStatsButton.setForeground(Color.WHITE);
        refreshStatsButton.addActionListener(e -> {
            updateStatisticsCards();
            updateDepartmentTable(deptTable);
            updateStatusTable(statusTable);
            updatePerformanceMetrics(metricsArea);
        });
        buttonPanel.add(refreshStatsButton);

        overviewPanel.add(statsCardsPanel, BorderLayout.NORTH);
        overviewPanel.add(chartsPanel, BorderLayout.CENTER);
        overviewPanel.add(performancePanel, BorderLayout.SOUTH);

        return overviewPanel;
    }

    private void updateStatisticsCards() {
        statsCardsPanel.removeAll();

        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();
        List<User> allUsers = MySQLDatabaseService.getAllUsers();

        long total = allGrievances.size();
        long resolved = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED ||
                        g.getCurrentState() == Grievance.GrievanceState.CLOSED)
                .count();
        long escalated = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .count();
        long violated = allGrievances.stream()
                .filter(Grievance::isSlaViolated)
                .count();
        long pending = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.SUBMITTED ||
                        g.getCurrentState() == Grievance.GrievanceState.UNDER_REVIEW)
                .count();

        long citizens = allUsers.stream()
                .filter(u -> u.getRole() == User.UserRole.CITIZEN)
                .count();
        long officers = allUsers.stream()
                .filter(u -> u.getRole() == User.UserRole.OFFICER)
                .count();
        long authorities = allUsers.stream()
                .filter(u -> u.getRole() == User.UserRole.AUTHORITY)
                .count();

        double resolutionRate = total > 0 ? (resolved * 100.0 / total) : 0;

        statsCardsPanel.add(createStatCard("Total Grievances", String.valueOf(total), new Color(52, 152, 219), "📋",
                "Total grievances in system"));
        statsCardsPanel.add(createStatCard("Resolved", String.valueOf(resolved), new Color(46, 204, 113), "✅",
                "Successfully resolved"));
        statsCardsPanel.add(
                createStatCard("Pending", String.valueOf(pending), new Color(241, 196, 15), "⏳", "Awaiting action"));
        statsCardsPanel.add(createStatCard("Escalated", String.valueOf(escalated), new Color(231, 76, 60), "⚠️",
                "Requires authority attention"));
        statsCardsPanel.add(createStatCard("SLA Violations", String.valueOf(violated), new Color(192, 57, 43), "⏰",
                "Missed deadlines"));
        statsCardsPanel.add(createStatCard("Citizens", String.valueOf(citizens), new Color(155, 89, 182), "👥",
                "Registered citizens"));
        statsCardsPanel.add(createStatCard("Officers", String.valueOf(officers), new Color(52, 152, 219), "👮",
                "Department officers"));
        statsCardsPanel.add(createStatCard("Resolution Rate", String.format("%.1f%%", resolutionRate),
                new Color(46, 204, 113), "📈", "Success rate"));

        statsCardsPanel.revalidate();
        statsCardsPanel.repaint();
    }

    private JPanel createStatCard(String title, String value, Color color, String icon, String tooltip) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        card.setToolTipText(tooltip);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        topPanel.add(iconLabel, BorderLayout.WEST);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(Color.GRAY);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(topPanel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void updateDepartmentTable(JTable table) {
        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();

        Map<String, Long> deptStats = allGrievances.stream()
                .filter(g -> g.getDepartment() != null && !g.getDepartment().isEmpty())
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        Map<String, Long> deptResolved = allGrievances.stream()
                .filter(g -> g.getDepartment() != null &&
                        (g.getCurrentState() == Grievance.GrievanceState.RESOLVED ||
                                g.getCurrentState() == Grievance.GrievanceState.CLOSED))
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        Map<String, Long> deptViolations = allGrievances.stream()
                .filter(g -> g.getDepartment() != null && g.isSlaViolated())
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        Map<String, Long> deptEscalated = allGrievances.stream()
                .filter(g -> g.getDepartment() != null &&
                        g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        String[] columns = { "Department", "Total", "Resolved", "Violations", "Escalated", "Compliance Rate" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        for (Map.Entry<String, Long> entry : deptStats.entrySet()) {
            String dept = entry.getKey();
            long total = entry.getValue();
            long resolved = deptResolved.getOrDefault(dept, 0L);
            long violations = deptViolations.getOrDefault(dept, 0L);
            long escalated = deptEscalated.getOrDefault(dept, 0L);
            double complianceRate = total > 0 ? ((total - violations) * 100.0 / total) : 100.0;

            model.addRow(new Object[] {
                    dept,
                    total,
                    resolved,
                    violations,
                    escalated,
                    String.format("%.1f%%", complianceRate)
            });
        }

        table.setModel(model);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);

        // Color renderer for compliance rate
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected && column == 5 && value != null) {
                    String rateStr = value.toString().replace("%", "");
                    double rate = Double.parseDouble(rateStr);
                    if (rate >= 90) {
                        c.setBackground(new Color(200, 255, 200));
                    } else if (rate >= 70) {
                        c.setBackground(new Color(255, 255, 200));
                    } else {
                        c.setBackground(new Color(255, 200, 200));
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });
    }

    private void updateStatusTable(JTable table) {
        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();

        Map<Grievance.GrievanceState, Long> statusStats = allGrievances.stream()
                .collect(Collectors.groupingBy(Grievance::getCurrentState, Collectors.counting()));

        String[] columns = { "Status", "Count", "Percentage", "Icon" };
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        long total = allGrievances.size();
        for (Map.Entry<Grievance.GrievanceState, Long> entry : statusStats.entrySet()) {
            double percentage = total > 0 ? (entry.getValue() * 100.0 / total) : 0;
            String icon = getStatusIcon(entry.getKey());
            model.addRow(new Object[] {
                    entry.getKey(),
                    entry.getValue(),
                    String.format("%.1f%%", percentage),
                    icon
            });
        }

        table.setModel(model);
        table.setRowHeight(30);
    }

    private String getStatusIcon(Grievance.GrievanceState state) {
        switch (state) {
            case SUBMITTED:
                return "📝";
            case UNDER_REVIEW:
                return "🔍";
            case RESOLVED:
                return "✅";
            case ESCALATED:
                return "⚠️";
            case REJECTED:
                return "❌";
            case CLOSED:
                return "🔒";
            default:
                return "📌";
        }
    }

    private void updatePerformanceMetrics(JTextArea area) {
        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();

        long total = allGrievances.size();
        long resolved = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                .count();
        long violated = allGrievances.stream()
                .filter(Grievance::isSlaViolated)
                .count();
        long escalated = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .count();

        double avgResolutionTime = calculateAverageResolutionTime(allGrievances);
        double slaCompliance = total > 0 ? ((total - violated) * 100.0 / total) : 0;
        double escalationRate = total > 0 ? (escalated * 100.0 / total) : 0;
        double resolutionRate = total > 0 ? (resolved * 100.0 / total) : 0;
        double efficiencyScore = (slaCompliance * 0.5 + resolutionRate * 0.5);

        String metrics = String.format(
                "╔══════════════════════════════════════════════════════════════════╗\n" +
                        "║                    KEY PERFORMANCE INDICATORS                     ║\n" +
                        "╚══════════════════════════════════════════════════════════════════╝\n\n" +
                        "┌─────────────────────────────────────────────────────────────────┐\n" +
                        "│ %-45s │\n" +
                        "│ %-45s │\n" +
                        "│ %-45s │\n" +
                        "│ %-45s │\n" +
                        "│ %-45s │\n" +
                        "│ %-45s │\n" +
                        "└─────────────────────────────────────────────────────────────────┘\n\n" +
                        "╔══════════════════════════════════════════════════════════════════╗\n" +
                        "║                       RECOMMENDATIONS                             ║\n" +
                        "╚══════════════════════════════════════════════════════════════════╝\n\n",
                "Average Resolution Time: " + String.format("%.2f days", avgResolutionTime),
                "SLA Compliance Rate: " + String.format("%.1f%%", slaCompliance),
                "Escalation Rate: " + String.format("%.1f%%", escalationRate),
                "Resolution Rate: " + String.format("%.1f%%", resolutionRate),
                "Efficiency Score: " + String.format("%.1f/100", efficiencyScore),
                "Overall System Health: "
                        + (efficiencyScore >= 80 ? "🟢 GOOD" : efficiencyScore >= 60 ? "🟡 FAIR" : "🔴 POOR"));

        if (violated > total * 0.2) {
            metrics += "⚠️  CRITICAL: High SLA violation rate (" + violated + " cases)\n";
            metrics += "   • Immediate review required for underperforming departments\n";
            metrics += "   • Implement automated SLA monitoring and alerts\n";
            metrics += "   • Conduct officer training on time management\n\n";
        } else if (violated > 0) {
            metrics += "⚠️  SLA violations detected in " + violated + " cases\n";
            metrics += "   • Monitor affected departments closely\n";
            metrics += "   • Review root causes for delays\n\n";
        } else {
            metrics += "✅  Excellent SLA compliance across all departments\n\n";
        }

        if (escalationRate > 15) {
            metrics += "⚠️  High escalation rate (" + String.format("%.1f%%", escalationRate) + ")\n";
            metrics += "   • Review officer training and decision-making\n";
            metrics += "   • Analyze escalation patterns\n";
            metrics += "   • Provide additional support to high-escalation departments\n\n";
        }

        if (resolutionRate < 70) {
            metrics += "⚠️  Low resolution rate (" + String.format("%.1f%%", resolutionRate) + ")\n";
            metrics += "   • Reduce pending backlog\n";
            metrics += "   • Allocate additional resources\n";
            metrics += "   • Prioritize long-pending grievances\n";
        }

        area.setText(metrics);
    }

    private double calculateAverageResolutionTime(List<Grievance> grievances) {
        return grievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                .mapToDouble(g -> {
                    long hours = java.time.Duration.between(g.getCreatedAt(), g.getUpdatedAt()).toHours();
                    return hours / 24.0;
                })
                .average()
                .orElse(0);
    }

    private JPanel createGrievancesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        filterPanel.add(new JLabel("Department:"));
        departmentFilterCombo = new JComboBox<>();
        departmentFilterCombo.addItem("All");

        List<String> departments = MySQLDatabaseService.getAllGrievances().stream()
                .map(Grievance::getDepartment)
                .distinct()
                .filter(d -> d != null && !d.isEmpty())
                .sorted()
                .collect(Collectors.toList());
        departments.forEach(departmentFilterCombo::addItem);
        departmentFilterCombo.addActionListener(e -> loadGrievancesForAuditor());
        filterPanel.add(departmentFilterCombo);

        filterPanel.add(Box.createHorizontalStrut(10));

        filterPanel.add(new JLabel("Status:"));
        JComboBox<String> statusFilterCombo = new JComboBox<>();
        statusFilterCombo.addItem("All");
        for (Grievance.GrievanceState state : Grievance.GrievanceState.values()) {
            statusFilterCombo.addItem(state.toString());
        }
        statusFilterCombo.addActionListener(e -> loadGrievancesForAuditor());
        filterPanel.add(statusFilterCombo);

        filterPanel.add(Box.createHorizontalStrut(10));

        filterPanel.add(new JLabel("SLA Status:"));
        JComboBox<String> slaFilterCombo = new JComboBox<>();
        slaFilterCombo.addItem("All");
        slaFilterCombo.addItem("Violated");
        slaFilterCombo.addItem("On Track");
        slaFilterCombo.addItem("No SLA");
        slaFilterCombo.addActionListener(e -> loadGrievancesForAuditor());
        filterPanel.add(slaFilterCombo);

        filterPanel.add(Box.createHorizontalStrut(10));

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.setBackground(new Color(0, 102, 204));
        refreshButton.setOpaque(true);
        refreshButton.setBorderPainted(false);
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> loadGrievancesForAuditor());
        filterPanel.add(refreshButton);

        JButton exportButton = new JButton("📊 Export to CSV");
        exportButton.setBackground(new Color(46, 204, 113));
        exportButton.setOpaque(true);
        exportButton.setBorderPainted(false);
        exportButton.setForeground(Color.WHITE);
        exportButton.addActionListener(e -> exportGrievancesToCSV());
        filterPanel.add(exportButton);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Grievances table
        String[] columns = { "ID", "Title", "Status", "Department", "Submitted By", "Assigned To", "Submitted Date",
                "SLA Status", "Escalation" };
        grievancesTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        grievancesTable = new JTable(grievancesTableModel);
        grievancesTable.setRowHeight(30);
        grievancesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Color renderer for SLA status
        grievancesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected && column == 7) {
                    String status = (String) value;
                    if (status != null && status.contains("Violated")) {
                        c.setBackground(new Color(255, 200, 200));
                    } else if (status != null && status.contains("Urgent")) {
                        c.setBackground(new Color(255, 255, 200));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(grievancesTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add double-click listener to view details
        grievancesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    viewGrievanceDetails();
                }
            }
        });

        // Stats label
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsLabel = new JLabel("Total grievances: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsPanel.add(statsLabel);
        panel.add(statsPanel, BorderLayout.SOUTH);

        loadGrievancesForAuditor();

        return panel;
    }

    private void loadGrievancesForAuditor() {
        grievancesTableModel.setRowCount(0);

        List<Grievance> grievances = MySQLDatabaseService.getAllGrievances();
        String deptFilter = (String) departmentFilterCombo.getSelectedItem();

        // Get filters from the panel
        JPanel filterPanel = (JPanel) ((JPanel) grievancesTable.getParent().getParent().getParent()).getComponent(0);
        JComboBox<String> statusCombo = null;
        JComboBox<String> slaCombo = null;

        for (Component comp : filterPanel.getComponents()) {
            if (comp instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) comp;
                if (combo.getItemCount() > 1) {
                    String selected = (String) combo.getSelectedItem();
                    if (selected != null && (selected.equals("All") ||
                            selected.equals("Violated") || selected.equals("On Track") || selected.equals("No SLA"))) {
                        if (slaCombo == null)
                            slaCombo = (JComboBox<String>) combo;
                    } else if (selected != null) {
                        if (statusCombo == null)
                            statusCombo = (JComboBox<String>) combo;
                    }
                }
            }
        }

        String statusFilter = statusCombo != null ? (String) statusCombo.getSelectedItem() : "All";
        String slaFilter = slaCombo != null ? (String) slaCombo.getSelectedItem() : "All";

        int count = 0;

        for (Grievance g : grievances) {
            if (!deptFilter.equals("All") && !deptFilter.equals(g.getDepartment())) {
                continue;
            }
            if (!statusFilter.equals("All") && !statusFilter.equals(g.getCurrentState().toString())) {
                continue;
            }

            String slaStatus = getSLAStatusForDisplay(g);
            if (!slaFilter.equals("All") && !slaStatus.contains(slaFilter)) {
                continue;
            }

            count++;
            String escalation = g.getEscalationLevel() > 0 ? "Level " + g.getEscalationLevel() : "None";

            grievancesTableModel.addRow(new Object[] {
                    g.getGrievanceId(),
                    g.getTitle(),
                    g.getCurrentState(),
                    g.getDepartment() != null ? g.getDepartment() : "N/A",
                    g.getSubmittedBy(),
                    g.getAssignedTo() != null ? g.getAssignedTo() : "Unassigned",
                    g.getCreatedAt().format(formatter),
                    slaStatus,
                    escalation
            });
        }

        statsLabel.setText(String.format("Total grievances: %d", count));
    }

    private String getSLAStatusForDisplay(Grievance g) {
        if (g.isSlaViolated()) {
            return "⚠️ Violated";
        }

        if (g.getSlaDeadline() != null) {
            long hoursLeft = java.time.Duration.between(LocalDateTime.now(), g.getSlaDeadline()).toHours();
            if (hoursLeft <= 0) {
                return "⚠️ Violated";
            } else if (hoursLeft <= 12) {
                return "⚠️ Urgent (" + hoursLeft + "h)";
            } else if (hoursLeft <= 24) {
                return "⏰ " + hoursLeft + "h left";
            } else {
                return "✓ On Track";
            }
        }
        return "No SLA";
    }

    private void viewGrievanceDetails() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0)
            return;

        String grievanceId = (String) grievancesTableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance != null) {
            showGrievanceDetailsDialog(grievance);
        }
    }

    private void showGrievanceDetailsDialog(Grievance grievance) {
        JDialog dialog = new JDialog(this, "Grievance Details - " + grievance.getGrievanceId(), true);
        dialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Grievance Details"), BorderLayout.NORTH);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        StringBuilder details = new StringBuilder();
        details.append("╔═══════════════════════════════════════════════════════════════╗\n");
        details.append("║                    GRIEVANCE DETAILS                          ║\n");
        details.append("╚═══════════════════════════════════════════════════════════════╝\n\n");

        details.append("ID: ").append(grievance.getGrievanceId()).append("\n");
        details.append("Title: ").append(grievance.getTitle()).append("\n");
        details.append("Description: ").append(grievance.getDescription()).append("\n");
        details.append("Category: ").append(grievance.getCategory()).append("\n");
        details.append("Department: ").append(grievance.getDepartment()).append("\n");
        details.append("Status: ").append(grievance.getCurrentState()).append("\n");
        details.append("Submitted By: ").append(grievance.getSubmittedBy()).append("\n");
        details.append("Submitted On: ").append(grievance.getCreatedAt().format(formatter)).append("\n");
        details.append("Last Updated: ").append(grievance.getUpdatedAt().format(formatter)).append("\n");

        if (grievance.getAssignedTo() != null) {
            User officer = MySQLDatabaseService.findUserById(grievance.getAssignedTo());
            details.append("Assigned To: ").append(officer != null ? officer.getName() : grievance.getAssignedTo())
                    .append("\n");
        }

        if (grievance.getSlaDeadline() != null) {
            details.append("SLA Deadline: ").append(grievance.getSlaDeadline().format(formatter)).append("\n");
            details.append("SLA Status: ").append(grievance.isSlaViolated() ? "VIOLATED" : "Compliant").append("\n");
            if (!grievance.isSlaViolated() && grievance.getSlaDeadline() != null) {
                long hoursLeft = java.time.Duration.between(LocalDateTime.now(), grievance.getSlaDeadline()).toHours();
                details.append("Time Remaining: ").append(hoursLeft).append(" hours\n");
            }
        }

        if (grievance.getEscalationLevel() > 0) {
            details.append("Escalation Level: ").append(grievance.getEscalationLevel()).append("\n");
        }

        if (!grievance.getHistory().isEmpty()) {
            details.append("\n───────────────────────────────────────────────────────────\n");
            details.append("                     AUDIT HISTORY\n");
            details.append("───────────────────────────────────────────────────────────\n");
            for (GrievanceHistory h : grievance.getHistory()) {
                details.append(h.getChangedAt().format(formatter)).append(" | ");
                details.append(h.getAction()).append(" by ").append(h.getChangedBy()).append(" (")
                        .append(h.getChangedByRole()).append(")");
                if (h.getComments() != null && !h.getComments().isEmpty()) {
                    details.append("\n   └─ ").append(h.getComments());
                }
                details.append("\n");
            }
        }

        detailsArea.setText(details.toString());
        detailsArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private JPanel createAuditTrailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        filterPanel.add(new JLabel("From Date:"));
        startDateField = new JTextField(12);
        startDateField.setText(LocalDateTime.now().minusDays(30).format(dateFormatter));
        filterPanel.add(startDateField);

        filterPanel.add(new JLabel("To Date:"));
        endDateField = new JTextField(12);
        endDateField.setText(LocalDateTime.now().format(dateFormatter));
        filterPanel.add(endDateField);

        filterPanel.add(Box.createHorizontalStrut(10));

        filterPanel.add(new JLabel("Action:"));
        JComboBox<String> actionCombo = new JComboBox<>();
        actionCombo.addItem("All");
        actionCombo.addItem("LOGIN");
        actionCombo.addItem("LOGOUT");
        actionCombo.addItem("GRIEVANCE_CREATED");
        actionCombo.addItem("STATUS_UPDATE");
        actionCombo.addItem("ESCALATED");
        actionCombo.addItem("RESOLVED");
        actionCombo.addItem("CLAIMED");
        filterPanel.add(actionCombo);

        filterPanel.add(Box.createHorizontalStrut(10));

        filterPanel.add(new JLabel("Actor:"));
        JTextField actorField = new JTextField(15);
        filterPanel.add(actorField);

        filterPanel.add(Box.createHorizontalStrut(10));

        JButton searchButton = new JButton("🔍 Search");
        searchButton.setBackground(new Color(0, 102, 204));
        searchButton.setOpaque(true);
        searchButton.setBorderPainted(false);
        searchButton.setForeground(Color.WHITE);
        searchButton.addActionListener(e -> loadAuditTrail(
                startDateField.getText(),
                endDateField.getText(),
                (String) actionCombo.getSelectedItem(),
                actorField.getText().trim()));
        filterPanel.add(searchButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            startDateField.setText(LocalDateTime.now().minusDays(30).format(dateFormatter));
            endDateField.setText(LocalDateTime.now().format(dateFormatter));
            actionCombo.setSelectedIndex(0);
            actorField.setText("");
            loadAuditTrail(startDateField.getText(), endDateField.getText(), "All", "");
        });
        filterPanel.add(clearButton);

        JButton exportAuditButton = new JButton("📄 Export");
        exportAuditButton.addActionListener(e -> exportAuditTrail());
        filterPanel.add(exportAuditButton);

        panel.add(filterPanel, BorderLayout.NORTH);

        // Audit table
        String[] columns = { "Timestamp", "Action", "Actor", "Role", "Target ID", "Target Type", "Details" };
        auditTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        auditTable = new JTable(auditTableModel);
        auditTable.setRowHeight(30);
        auditTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        auditTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        auditTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        auditTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        auditTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        auditTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        auditTable.getColumnModel().getColumn(6).setPreferredWidth(300);

        JScrollPane scrollPane = new JScrollPane(auditTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Stats label
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel auditStatsLabel = new JLabel("Total entries: 0");
        auditStatsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsPanel.add(auditStatsLabel);
        panel.add(statsPanel, BorderLayout.SOUTH);

        loadAuditTrail(startDateField.getText(), endDateField.getText(), "All", "");

        return panel;
    }

    private void loadAuditTrail(String startDateStr, String endDateStr, String actionFilter, String actorFilter) {
        auditTableModel.setRowCount(0);

        try {
            LocalDateTime from = LocalDateTime.parse(startDateStr + "T00:00:00");
            LocalDateTime to = LocalDateTime.parse(endDateStr + "T23:59:59");

            List<AuditTrail.AuditEntry> entries = AuditTrail.viewAuditTrail(null, null, from, to);

            int count = 0;
            for (AuditTrail.AuditEntry entry : entries) {
                if (!actionFilter.equals("All") && !entry.getAction().contains(actionFilter)) {
                    continue;
                }
                if (!actorFilter.isEmpty() && !entry.getActor().toLowerCase().contains(actorFilter.toLowerCase())) {
                    continue;
                }
                count++;
                auditTableModel.addRow(new Object[] {
                        entry.getTimestamp().format(formatter),
                        entry.getAction(),
                        entry.getActor(),
                        entry.getActorRole(),
                        entry.getTargetId(),
                        entry.getTargetType(),
                        entry.getDetails().length() > 100 ? entry.getDetails().substring(0, 100) + "..."
                                : entry.getDetails()
                });
            }

            // Update stats label
            JPanel parentPanel = (JPanel) auditTable.getParent().getParent().getParent();
            JPanel statsPanel = (JPanel) parentPanel.getComponent(2);
            JLabel statsLabel = (JLabel) statsPanel.getComponent(0);
            statsLabel.setText(
                    String.format("Total entries: %d (Showing %d of %d)", entries.size(), count, entries.size()));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading audit trail: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Report controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        controlPanel.add(new JLabel("Report Type:"));
        String[] reportTypes = {
                "📊 Executive Summary",
                "📋 Compliance Report",
                "⏱️ SLA Performance",
                "🏢 Department Performance",
                "⚠️ Escalation Analysis",
                "👥 User Activity Report",
                "📈 Trend Analysis"
        };
        reportTypeCombo = new JComboBox<>(reportTypes);
        reportTypeCombo.setPreferredSize(new Dimension(200, 30));
        controlPanel.add(reportTypeCombo);

        controlPanel.add(Box.createHorizontalStrut(15));

        controlPanel.add(new JLabel("From:"));
        JTextField reportStartDate = new JTextField(12);
        reportStartDate.setText(LocalDateTime.now().minusDays(30).format(dateFormatter));
        controlPanel.add(reportStartDate);

        controlPanel.add(new JLabel("To:"));
        JTextField reportEndDate = new JTextField(12);
        reportEndDate.setText(LocalDateTime.now().format(dateFormatter));
        controlPanel.add(reportEndDate);

        controlPanel.add(Box.createHorizontalStrut(15));

        JButton generateButton = new JButton("📄 Generate Report");
        generateButton.setBackground(new Color(0, 102, 204));
        generateButton.setOpaque(true);
        generateButton.setBorderPainted(false);
        generateButton.setForeground(Color.WHITE);
        generateButton.addActionListener(e -> generateReport(
                (String) reportTypeCombo.getSelectedItem(),
                reportStartDate.getText(),
                reportEndDate.getText()));
        controlPanel.add(generateButton);

        JButton exportButton = new JButton("💾 Export to File");
        exportButton.setBackground(new Color(46, 204, 113));
        exportButton.setOpaque(true);
        exportButton.setBorderPainted(false);
        exportButton.setForeground(Color.WHITE);
        exportButton.addActionListener(e -> exportReportToFile(reportStartDate.getText(), reportEndDate.getText()));
        controlPanel.add(exportButton);

        JButton printButton = new JButton("🖨️ Print");
        printButton.addActionListener(e -> printReport());
        controlPanel.add(printButton);

        panel.add(controlPanel, BorderLayout.NORTH);

        // Report display area
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        reportArea.setLineWrap(true);
        reportArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(reportArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Generate initial report
        generateReport("📊 Executive Summary",
                LocalDateTime.now().minusDays(30).format(dateFormatter),
                LocalDateTime.now().format(dateFormatter));

        return panel;
    }

    private void generateReport(String reportType, String startDateStr, String endDateStr) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDateStr + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDateStr + "T23:59:59");

            List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();
            List<Grievance> periodGrievances = allGrievances.stream()
                    .filter(g -> !g.getCreatedAt().isBefore(start) && !g.getCreatedAt().isAfter(end))
                    .collect(Collectors.toList());

            StringBuilder report = new StringBuilder();
            report.append("╔════════════════════════════════════════════════════════════════════════════════╗\n");
            report.append(String.format("║ %-80s ║\n", reportType.toUpperCase()));
            report.append("╚════════════════════════════════════════════════════════════════════════════════╝\n");
            report.append(String.format("Period: %s to %s\n", startDateStr, endDateStr));
            report.append(String.format("Generated: %s\n", LocalDateTime.now().format(formatter)));
            report.append(
                    String.format("Generated By: %s (Audit ID: %s)\n\n", auditor.getName(), auditor.getAuditId()));

            switch (reportType) {
                case "📊 Executive Summary":
                    generateExecutiveSummary(report, periodGrievances, allGrievances);
                    break;
                case "📋 Compliance Report":
                    generateComplianceReport(report, periodGrievances);
                    break;
                case "⏱️ SLA Performance":
                    generateSLAPerformanceReport(report, periodGrievances);
                    break;
                case "🏢 Department Performance":
                    generateDepartmentPerformanceReport(report, periodGrievances);
                    break;
                case "⚠️ Escalation Analysis":
                    generateEscalationAnalysisReport(report, periodGrievances);
                    break;
                case "👥 User Activity Report":
                    generateUserActivityReport(report, periodGrievances);
                    break;
                case "📈 Trend Analysis":
                    generateTrendAnalysisReport(report, periodGrievances);
                    break;
            }

            reportArea.setText(report.toString());
            reportArea.setCaretPosition(0);

        } catch (Exception e) {
            reportArea.setText("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateExecutiveSummary(StringBuilder report, List<Grievance> periodGrievances,
            List<Grievance> allGrievances) {
        long total = periodGrievances.size();
        long resolved = periodGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                .count();
        long violated = periodGrievances.stream()
                .filter(Grievance::isSlaViolated)
                .count();
        long escalated = periodGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .count();

        double resolutionRate = total > 0 ? (resolved * 100.0 / total) : 0;
        double slaCompliance = total > 0 ? ((total - violated) * 100.0 / total) : 0;
        double avgResolutionTime = calculateAverageResolutionTime(periodGrievances);

        report.append("EXECUTIVE SUMMARY\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");
        report.append("KEY METRICS\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        report.append(String.format("Total Grievances: %d\n", total));
        report.append(String.format("Resolved: %d (%.1f%%)\n", resolved, resolutionRate));
        report.append(String.format("SLA Compliance: %.1f%%\n", slaCompliance));
        report.append(String.format("Average Resolution Time: %.1f days\n", avgResolutionTime));
        report.append(String.format("Escalation Rate: %.1f%%\n", total > 0 ? (escalated * 100.0 / total) : 0));
        report.append(String.format("SLA Violations: %d\n\n", violated));

        report.append("SYSTEM HEALTH ASSESSMENT\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        double healthScore = (slaCompliance * 0.4 + resolutionRate * 0.4 +
                (total > 0 ? ((total - escalated) * 100.0 / total) : 100) * 0.2);

        if (healthScore >= 80) {
            report.append("Overall System Health: 🟢 EXCELLENT\n");
            report.append("The system is performing well with good compliance and resolution rates.\n");
        } else if (healthScore >= 60) {
            report.append("Overall System Health: 🟡 SATISFACTORY\n");
            report.append("System performance is acceptable but requires monitoring in key areas.\n");
        } else {
            report.append("Overall System Health: 🔴 CRITICAL\n");
            report.append("Immediate intervention required to improve system performance.\n");
        }

        report.append(String.format("\nHealth Score: %.1f/100\n", healthScore));
    }

    private void generateComplianceReport(StringBuilder report, List<Grievance> grievances) {
        long total = grievances.size();
        long resolved = grievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                .count();
        long violated = grievances.stream()
                .filter(Grievance::isSlaViolated)
                .count();
        long escalated = grievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .count();

        double resolutionRate = total > 0 ? (resolved * 100.0 / total) : 0;
        double complianceRate = total > 0 ? ((total - violated) * 100.0 / total) : 0;

        report.append("COMPLIANCE REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");
        report.append("COMPLIANCE SUMMARY\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        report.append(String.format("Total Grievances: %d\n", total));
        report.append(String.format("Resolved: %d (%.1f%%)\n", resolved, resolutionRate));
        report.append(String.format("SLA Compliant: %d (%.1f%%)\n", total - violated, complianceRate));
        report.append(String.format("SLA Violations: %d\n", violated));
        report.append(String.format("Escalated: %d\n\n", escalated));

        report.append("COMPLIANCE SCORE\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        double complianceScore = (complianceRate * 0.6 + resolutionRate * 0.4);
        report.append(String.format("Overall Compliance Score: %.2f/100\n", complianceScore));

        if (complianceScore >= 90) {
            report.append("Rating: 🟢 EXCELLENT - All departments compliant\n");
        } else if (complianceScore >= 70) {
            report.append("Rating: 🟡 GOOD - Minor improvements needed\n");
        } else if (complianceScore >= 50) {
            report.append("Rating: 🟠 FAIR - Significant improvements required\n");
        } else {
            report.append("Rating: 🔴 POOR - Immediate action required\n");
        }
    }

    private void generateSLAPerformanceReport(StringBuilder report, List<Grievance> grievances) {
        long total = grievances.size();
        long violated = grievances.stream().filter(Grievance::isSlaViolated).count();

        report.append("SLA PERFORMANCE REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");
        report.append(String.format("Total Grievances: %d\n", total));
        report.append(String.format("SLA Compliant: %d (%.1f%%)\n", total - violated,
                total > 0 ? ((total - violated) * 100.0 / total) : 0));
        report.append(String.format("SLA Violations: %d (%.1f%%)\n\n", violated,
                total > 0 ? (violated * 100.0 / total) : 0));

        // Violations by department
        report.append("VIOLATIONS BY DEPARTMENT\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        Map<String, Long> violationsByDept = grievances.stream()
                .filter(Grievance::isSlaViolated)
                .filter(g -> g.getDepartment() != null)
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        if (violationsByDept.isEmpty()) {
            report.append("No SLA violations recorded in this period.\n");
        } else {
            for (Map.Entry<String, Long> entry : violationsByDept.entrySet()) {
                long deptTotal = grievances.stream()
                        .filter(g -> entry.getKey().equals(g.getDepartment()))
                        .count();
                double violationRate = deptTotal > 0 ? (entry.getValue() * 100.0 / deptTotal) : 0;
                report.append(String.format("%s: %d violations (%.1f%%)\n",
                        entry.getKey(), entry.getValue(), violationRate));
            }
        }
    }

    private void generateDepartmentPerformanceReport(StringBuilder report, List<Grievance> grievances) {
        report.append("DEPARTMENT PERFORMANCE REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        Map<String, List<Grievance>> byDepartment = grievances.stream()
                .filter(g -> g.getDepartment() != null)
                .collect(Collectors.groupingBy(Grievance::getDepartment));

        report.append(String.format("%-25s %10s %10s %10s %10s\n",
                "Department", "Total", "Resolved", "Violated", "Escalated"));
        report.append("───────────────────────────────────────────────────────────────\n");

        for (Map.Entry<String, List<Grievance>> entry : byDepartment.entrySet()) {
            String dept = entry.getKey();
            List<Grievance> deptGrievances = entry.getValue();
            long total = deptGrievances.size();
            long resolved = deptGrievances.stream()
                    .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                    .count();
            long violated = deptGrievances.stream()
                    .filter(Grievance::isSlaViolated)
                    .count();
            long escalated = deptGrievances.stream()
                    .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                    .count();

            report.append(String.format("%-25s %10d %10d %10d %10d\n",
                    dept, total, resolved, violated, escalated));
        }

        // Performance ratings
        report.append("\nPERFORMANCE RATINGS\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        for (Map.Entry<String, List<Grievance>> entry : byDepartment.entrySet()) {
            String dept = entry.getKey();
            List<Grievance> deptGrievances = entry.getValue();
            long total = deptGrievances.size();
            long resolved = deptGrievances.stream()
                    .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                    .count();
            long violated = deptGrievances.stream()
                    .filter(Grievance::isSlaViolated)
                    .count();

            double resolutionRate = total > 0 ? (resolved * 100.0 / total) : 100;
            double violationRate = total > 0 ? (violated * 100.0 / total) : 0;

            String rating;
            if (resolutionRate >= 80 && violationRate <= 10) {
                rating = "🟢 Excellent";
            } else if (resolutionRate >= 60 && violationRate <= 20) {
                rating = "🟡 Good";
            } else if (resolutionRate >= 40) {
                rating = "🟠 Fair";
            } else {
                rating = "🔴 Needs Improvement";
            }

            report.append(String.format("%s: %s (Resolution: %.1f%%, Violations: %.1f%%)\n",
                    dept, rating, resolutionRate, violationRate));
        }
    }

    private void generateEscalationAnalysisReport(StringBuilder report, List<Grievance> grievances) {
        long total = grievances.size();
        List<Grievance> escalated = grievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .collect(Collectors.toList());

        report.append("ESCALATION ANALYSIS REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");
        report.append(String.format("Total Grievances: %d\n", total));
        report.append(String.format("Escalated Grievances: %d (%.1f%%)\n\n", escalated.size(),
                total > 0 ? (escalated.size() * 100.0 / total) : 0));

        // Escalation by level
        report.append("ESCALATION LEVEL DISTRIBUTION\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        Map<Integer, Long> byLevel = escalated.stream()
                .collect(Collectors.groupingBy(Grievance::getEscalationLevel, Collectors.counting()));

        for (int i = 1; i <= 3; i++) {
            long count = byLevel.getOrDefault(i, 0L);
            report.append(String.format("Level %d: %d grievances (%.1f%%)\n", i, count,
                    escalated.size() > 0 ? (count * 100.0 / escalated.size()) : 0));
        }

        // Most escalated departments
        report.append("\nDEPARTMENTS WITH HIGH ESCALATION\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        Map<String, Long> byDepartment = escalated.stream()
                .filter(g -> g.getDepartment() != null)
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        byDepartment.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    long deptTotal = grievances.stream()
                            .filter(g -> entry.getKey().equals(g.getDepartment()))
                            .count();
                    double escalationRate = deptTotal > 0 ? (entry.getValue() * 100.0 / deptTotal) : 0;
                    report.append(String.format("%s: %d escalations (%.1f%% escalation rate)\n",
                            entry.getKey(), entry.getValue(), escalationRate));
                });

        // Recommendations
        report.append("\nRECOMMENDATIONS\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        if (escalated.size() > total * 0.15) {
            report.append("⚠️ High overall escalation rate detected.\n");
            report.append("   • Review officer training programs\n");
            report.append("   • Provide additional resources to high-escalation departments\n");
            report.append("   • Implement escalation review committee\n");
        }

        if (byLevel.getOrDefault(2, 0L) > 0) {
            report.append("⚠️ Level 2 escalations present - requires management attention\n");
        }

        if (byLevel.getOrDefault(3, 0L) > 0) {
            report.append("🔴 Level 3 escalations detected - immediate executive review required\n");
        }
    }

    private void generateUserActivityReport(StringBuilder report, List<Grievance> grievances) {
        List<User> allUsers = MySQLDatabaseService.getAllUsers();

        long citizens = allUsers.stream().filter(u -> u.getRole() == User.UserRole.CITIZEN).count();
        long officers = allUsers.stream().filter(u -> u.getRole() == User.UserRole.OFFICER).count();
        long authorities = allUsers.stream().filter(u -> u.getRole() == User.UserRole.AUTHORITY).count();

        report.append("USER ACTIVITY REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        report.append("USER REGISTRATION STATISTICS\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        report.append(String.format("Total Registered Users: %d\n", allUsers.size()));
        report.append(String.format("  • Citizens: %d\n", citizens));
        report.append(String.format("  • Officers: %d\n", officers));
        report.append(String.format("  • Authorities: %d\n\n", authorities));

        // Grievance submission by users
        report.append("GRIEVANCE SUBMISSION STATISTICS\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        Map<String, Long> submissionsByUser = grievances.stream()
                .collect(Collectors.groupingBy(Grievance::getSubmittedBy, Collectors.counting()));

        long activeCitizens = submissionsByUser.size();
        double avgPerCitizen = activeCitizens > 0 ? (grievances.size() * 1.0 / activeCitizens) : 0;

        report.append(String.format("Active Citizens: %d\n", activeCitizens));
        report.append(String.format("Average Grievances per Citizen: %.1f\n", avgPerCitizen));
        report.append(String.format("Most Active Citizen: %s\n\n",
                submissionsByUser.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> e.getKey() + " (" + e.getValue() + " grievances)")
                        .orElse("None")));

        // Officer performance
        report.append("OFFICER PERFORMANCE\n");
        report.append("───────────────────────────────────────────────────────────────\n");

        Map<String, Long> resolvedByOfficer = grievances.stream()
                .filter(g -> g.getAssignedTo() != null &&
                        g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                .collect(Collectors.groupingBy(Grievance::getAssignedTo, Collectors.counting()));

        for (User officer : allUsers) {
            if (officer.getRole() == User.UserRole.OFFICER) {
                long resolved = resolvedByOfficer.getOrDefault(officer.getUserId(), 0L);
                report.append(String.format("%s: %d grievances resolved\n", officer.getName(), resolved));
            }
        }
    }

    private void generateTrendAnalysisReport(StringBuilder report, List<Grievance> grievances) {
        report.append("TREND ANALYSIS REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n\n");

        // Group by month
        Map<String, Long> byMonth = grievances.stream()
                .collect(Collectors.groupingBy(g -> g.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()));

        report.append("MONTHLY TRENDS\n");
        report.append("───────────────────────────────────────────────────────────────\n");
        byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    report.append(String.format("%s: %d grievances\n", entry.getKey(), entry.getValue()));
                });

        // Calculate growth rate
        if (byMonth.size() >= 2) {
            List<Map.Entry<String, Long>> sorted = byMonth.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            long lastMonth = sorted.get(sorted.size() - 1).getValue();
            long previousMonth = sorted.get(sorted.size() - 2).getValue();
            double growthRate = previousMonth > 0 ? ((lastMonth - previousMonth) * 100.0 / previousMonth) : 0;

            report.append(String.format("\nGrowth Rate (Last Month): %.1f%%\n", growthRate));
            if (growthRate > 20) {
                report.append("⚠️ Significant increase in grievance submissions\n");
            } else if (growthRate < -20) {
                report.append("✅ Significant decrease in grievance submissions\n");
            }
        }
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Filter / Action panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("➕ Add User");
        addBtn.setBackground(new Color(46, 204, 113));
        addBtn.setForeground(Color.WHITE);
        addBtn.setOpaque(true);
        addBtn.setBorderPainted(false);
        addBtn.addActionListener(e -> {
            AddUserDialog dialog = new AddUserDialog(this);
            dialog.setVisible(true);
            if (dialog.isSuccess()) {
                loadUsersForAuditor();
            }
        });

        JButton suspendBtn = new JButton("⏸️ Suspend/Soft Delete");
        suspendBtn.setBackground(new Color(241, 196, 15));
        suspendBtn.setForeground(Color.BLACK);
        suspendBtn.setOpaque(true);
        suspendBtn.setBorderPainted(false);
        suspendBtn.addActionListener(e -> deleteSelectedUser(false));

        JButton hardDeleteBtn = new JButton("🗑️ Hard Delete");
        hardDeleteBtn.setBackground(new Color(231, 76, 60));
        hardDeleteBtn.setForeground(Color.WHITE);
        hardDeleteBtn.setOpaque(true);
        hardDeleteBtn.setBorderPainted(false);
        hardDeleteBtn.addActionListener(e -> deleteSelectedUser(true));

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> loadUsersForAuditor());

        actionPanel.add(addBtn);
        actionPanel.add(Box.createHorizontalStrut(10));
        actionPanel.add(suspendBtn);
        actionPanel.add(Box.createHorizontalStrut(10));
        actionPanel.add(hardDeleteBtn);
        actionPanel.add(Box.createHorizontalStrut(10));
        actionPanel.add(refreshBtn);

        panel.add(actionPanel, BorderLayout.NORTH);

        // Users table
        String[] columns = { "ID", "Name", "Role", "Email", "Phone", "Status" };
        usersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        usersTable = new JTable(usersTableModel);
        usersTable.setRowHeight(30);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        loadUsersForAuditor();

        return panel;
    }

    private void loadUsersForAuditor() {
        if (usersTableModel == null)
            return;
        usersTableModel.setRowCount(0);
        List<User> users = MySQLDatabaseService.getAllUsers();
        for (User u : users) {
            usersTableModel.addRow(new Object[] {
                    u.getUserId(),
                    u.getName(),
                    u.getRole().toString(),
                    u.getEmail(),
                    u.getPhoneNumber(),
                    u.getStatus()
            });
        }
    }

    private void deleteSelectedUser(boolean hardDelete) {
        int selectedRow = usersTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user from the table first.", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String userId = (String) usersTableModel.getValueAt(selectedRow, 0);
        String role = (String) usersTableModel.getValueAt(selectedRow, 2);

        if ("AUDITOR".equals(role)) {
            JOptionPane.showMessageDialog(this, "Cannot modify/delete an Auditor account.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + (hardDelete ? "HARD DELETE" : "SUSPEND") + " user " + userId + "?\n" +
                        (hardDelete ? "This action will permanently remove the user and cascade their records!"
                                : "This will set their status to INACTIVE."),
                "Confirm Action", JOptionPane.YES_NO_OPTION,
                hardDelete ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = hardDelete ? MySQLDatabaseService.hardDeleteUser(userId)
                    : MySQLDatabaseService.softDeleteUser(userId);
            if (success) {
                JOptionPane.showMessageDialog(this, "Action completed successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                loadUsersForAuditor();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to complete the action. Check database constraints.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createSystemHealthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane healthTabs = new JTabbedPane();

        // Database Health
        JPanel dbHealthPanel = createDatabaseHealthPanel();
        healthTabs.addTab("🗄️ Database Health", dbHealthPanel);

        // Performance Metrics
        JPanel performancePanel = createPerformanceHealthPanel();
        healthTabs.addTab("📊 Performance Metrics", performancePanel);

        // System Status
        JPanel systemStatusPanel = createSystemStatusPanel();
        healthTabs.addTab("⚙️ System Status", systemStatusPanel);

        panel.add(healthTabs, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDatabaseHealthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea dbHealthArea = new JTextArea();
        dbHealthArea.setEditable(false);
        dbHealthArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        StringBuilder health = new StringBuilder();
        health.append("DATABASE HEALTH STATUS\n");
        health.append("═══════════════════════════════════════════════════════════════\n\n");

        try {
            health.append("Connection Status: ")
                    .append(MySQLDatabaseService.isConnected() ? "✅ Connected" : "❌ Disconnected").append("\n");
            health.append("Database: diggs_db\n");
            health.append("Server: localhost:3306\n");

            // Table statistics
            health.append("\nTABLE STATISTICS\n");
            health.append("───────────────────────────────────────────────────────────────\n");
            health.append("users: ").append(MySQLDatabaseService.getAllUsers().size()).append(" records\n");
            health.append("grievances: ").append(MySQLDatabaseService.getAllGrievances().size()).append(" records\n");

        } catch (Exception e) {
            health.append("Error retrieving database health: " + e.getMessage());
        }

        dbHealthArea.setText(health.toString());
        panel.add(new JScrollPane(dbHealthArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPerformanceHealthPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea performanceArea = new JTextArea();
        performanceArea.setEditable(false);
        performanceArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();

        long total = allGrievances.size();
        long resolved = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.RESOLVED)
                .count();
        long pending = total - resolved;

        double avgResolutionTime = calculateAverageResolutionTime(allGrievances);

        StringBuilder perf = new StringBuilder();
        perf.append("PERFORMANCE METRICS\n");
        perf.append("═══════════════════════════════════════════════════════════════\n\n");
        perf.append(String.format("Current Load: %d active grievances\n", pending));
        perf.append(String.format("Resolution Capacity: %.1f grievances/day\n", resolved / 30.0));
        perf.append(String.format("Average Processing Time: %.1f days\n", avgResolutionTime));
        perf.append(String.format("System Throughput: %.1f grievances/day\n", total / 30.0));

        performanceArea.setText(perf.toString());
        panel.add(new JScrollPane(performanceArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSystemStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        StringBuilder status = new StringBuilder();
        status.append("SYSTEM STATUS\n");
        status.append("═══════════════════════════════════════════════════════════════\n\n");
        status.append("DIGGS Version: 2.0\n");
        status.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        status.append("OS: ").append(System.getProperty("os.name")).append("\n");
        status.append("Current Time: ").append(LocalDateTime.now().format(formatter)).append("\n");
        status.append("Auditor: ").append(auditor.getName()).append(" (").append(auditor.getAuditId()).append(")\n");

        statusArea.setText(status.toString());
        panel.add(new JScrollPane(statusArea), BorderLayout.CENTER);

        return panel;
    }

    private void exportGrievancesToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("grievances_export_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                // Write header
                for (int i = 0; i < grievancesTableModel.getColumnCount(); i++) {
                    writer.print(grievancesTableModel.getColumnName(i));
                    if (i < grievancesTableModel.getColumnCount() - 1)
                        writer.print(",");
                }
                writer.println();

                // Write data
                for (int i = 0; i < grievancesTableModel.getRowCount(); i++) {
                    for (int j = 0; j < grievancesTableModel.getColumnCount(); j++) {
                        Object value = grievancesTableModel.getValueAt(i, j);
                        writer.print(value != null ? value.toString().replace(",", ";") : "");
                        if (j < grievancesTableModel.getColumnCount() - 1)
                            writer.print(",");
                    }
                    writer.println();
                }

                JOptionPane.showMessageDialog(this, "Export successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportAuditTrail() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("audit_trail_export_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                // Write header
                for (int i = 0; i < auditTableModel.getColumnCount(); i++) {
                    writer.print(auditTableModel.getColumnName(i));
                    if (i < auditTableModel.getColumnCount() - 1)
                        writer.print(",");
                }
                writer.println();

                // Write data
                for (int i = 0; i < auditTableModel.getRowCount(); i++) {
                    for (int j = 0; j < auditTableModel.getColumnCount(); j++) {
                        Object value = auditTableModel.getValueAt(i, j);
                        writer.print(value != null ? value.toString().replace(",", ";") : "");
                        if (j < auditTableModel.getColumnCount() - 1)
                            writer.print(",");
                    }
                    writer.println();
                }

                JOptionPane.showMessageDialog(this, "Export successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportReportToFile(String startDate, String endDate) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(
                "diggs_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                writer.print(reportArea.getText());
                JOptionPane.showMessageDialog(this, "Report saved successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Save failed: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void printReport() {
        try {
            reportArea.print();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Print failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshAll() {
        updateStatisticsCards();
        loadGrievancesForAuditor();
        loadAuditTrail(startDateField.getText(), endDateField.getText(), "All", "");
        generateReport((String) reportTypeCombo.getSelectedItem(),
                startDateField.getText(), endDateField.getText());
        if (usersTableModel != null) {
            loadUsersForAuditor();
        }
    }

    private void startAutoRefresh() {
        Timer timer = new Timer(60000, e -> refreshAll());
        timer.start();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh All");
        refreshItem.addActionListener(e -> refreshAll());
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu reportMenu = new JMenu("Reports");
        JMenuItem exportReportItem = new JMenuItem("Export Current Report");
        exportReportItem.addActionListener(e -> exportReportToFile(startDateField.getText(), endDateField.getText()));
        reportMenu.add(exportReportItem);

        JMenuItem printReportItem = new JMenuItem("Print Report");
        printReportItem.addActionListener(e -> printReport());
        reportMenu.add(printReportItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(reportMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            AuthenticationService.logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "DIGGS - Digital Institutional Grievance Governance System\n" +
                        "Version 2.0\n\n" +
                        "Auditor Dashboard\n" +
                        "Audit ID: " + auditor.getAuditId() + "\n" +
                        "Department: " + auditor.getDepartment() + "\n\n" +
                        "Features:\n" +
                        "• Complete system overview with real-time statistics\n" +
                        "• Advanced grievance analysis with multiple filters\n" +
                        "• Full audit trail with search and export\n" +
                        "• Comprehensive report generation (7 report types)\n" +
                        "• SLA performance monitoring\n" +
                        "• Department performance tracking\n" +
                        "• Escalation analysis\n" +
                        "• System health monitoring\n" +
                        "• Auto-refresh every 60 seconds\n" +
                        "• Export to CSV and TXT formats\n\n" +
                        "© 2024 DIGGS - All Rights Reserved",
                "About DIGGS", JOptionPane.INFORMATION_MESSAGE);
    }
}