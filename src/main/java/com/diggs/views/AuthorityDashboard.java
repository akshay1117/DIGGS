// src/main/java/com/diggs/views/AuthorityDashboard.java
package com.diggs.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.escalation.EscalationService;
import com.diggs.fsm.GrievanceFSM;
import com.diggs.model.Authority;
import com.diggs.model.Grievance;
import com.diggs.model.GrievanceHistory;
import com.diggs.model.Officer;
import com.diggs.model.User;
import com.diggs.notification.NotificationService;
import com.diggs.service.AuthenticationService;
import com.diggs.sla.SLAManager;

public class AuthorityDashboard extends JFrame {
    private Authority authority;
    private JTable grievancesTable;
    private DefaultTableModel tableModel;
    private JTable statsTable;
    private DefaultTableModel statsTableModel;
    private JTextField searchField;
    private JComboBox<String> levelFilterCombo;
    private JLabel statsLabel;
    private JPanel detailsPanel;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AuthorityDashboard(Authority authority) {
        this.authority = authority;
        setTitle("DIGGS - Authority Dashboard - " + authority.getName());
        setSize(1400, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header panel with authority info
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Split pane: left side - escalated grievances, right side - details
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(800);
        mainSplitPane.setResizeWeight(0.6);

        // Left side: Top - Statistics, Bottom - Grievances list
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplitPane.setDividerLocation(250);
        leftSplitPane.setResizeWeight(0.3);

        // Statistics panel
        JPanel statsPanel = createStatisticsPanel();
        leftSplitPane.setTopComponent(statsPanel);

        // Grievances list panel
        JPanel grievancesPanel = createGrievancesListPanel();
        leftSplitPane.setBottomComponent(grievancesPanel);

        mainSplitPane.setLeftComponent(leftSplitPane);

        // Right panel: grievance details and actions
        detailsPanel = createDetailsPanel();
        mainSplitPane.setRightComponent(detailsPanel);

        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        // Footer panel
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Create menu bar
        createMenuBar();

        // Load initial data
        refreshData();

        // Auto-refresh every 30 seconds
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
            logoLabel = new JLabel("⚖️");
            logoLabel.setFont(new Font("Georgia", Font.PLAIN, 40));
            logoLabel.setForeground(new Color(200, 180, 130)); // Gold
        }

        JLabel titleLabel = new JLabel("DIGGS - Court Application Module");
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        brandPanel.add(logoLabel);
        brandPanel.add(titleLabel);

        headerPanel.add(brandPanel, BorderLayout.WEST);

        // Authority info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        infoPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel(
                "Welcome, " + authority.getName() + " | Level: " + authority.getAuthorityLevel());
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

    private JPanel createStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("System Statistics"));

        // Statistics table
        String[] columns = { "Metric", "Value" };
        statsTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        statsTable = new JTable(statsTableModel);
        statsTable.setRowHeight(25);
        statsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(statsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Refresh stats button
        JButton refreshStatsBtn = new JButton("Refresh Stats");
        refreshStatsBtn.addActionListener(e -> updateStatistics());
        panel.add(refreshStatsBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createGrievancesListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Escalated Grievances"));

        // Toolbar
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        toolbarPanel.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        searchField.addActionListener(e -> loadGrievances());
        toolbarPanel.add(searchField);

        JButton searchButton = new JButton("🔍");
        searchButton.addActionListener(e -> loadGrievances());
        toolbarPanel.add(searchButton);

        toolbarPanel.add(Box.createHorizontalStrut(10));

        toolbarPanel.add(new JLabel("Escalation Level:"));
        String[] levels = { "All", "Level 1", "Level 2", "Level 3" };
        levelFilterCombo = new JComboBox<>(levels);
        levelFilterCombo.addActionListener(e -> loadGrievances());
        toolbarPanel.add(levelFilterCombo);

        toolbarPanel.add(Box.createHorizontalStrut(10));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());
        toolbarPanel.add(refreshButton);

        panel.add(toolbarPanel, BorderLayout.NORTH);

        // Grievances table
        String[] columns = { "ID", "Title", "Escalation Level", "SLA Status", "Submitted By", "Original Officer",
                "Escalated On" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        grievancesTable = new JTable(tableModel);
        grievancesTable.setRowHeight(35);
        grievancesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grievancesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadGrievanceDetails();
            }
        });

        // Color renderer for SLA status
        grievancesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected && column == 3) {
                    String status = (String) value;
                    if (status != null && status.contains("VIOLATED")) {
                        c.setBackground(new Color(255, 200, 200));
                    } else if (status != null && status.contains("URGENT")) {
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

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Grievance Details & Actions"));

        // Details area
        JTextArea detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons panel
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton handleButton = new JButton("👥 Handle Escalated Grievance");
        handleButton.setBackground(new Color(0, 102, 204));
        handleButton.setOpaque(true);
        handleButton.setBorderPainted(false);
        handleButton.setForeground(Color.WHITE);
        handleButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        handleButton.addActionListener(e -> handleEscalatedGrievance());
        actionPanel.add(handleButton);

        JButton assignButton = new JButton("👤 Assign to Officer");
        assignButton.setBackground(new Color(0, 153, 76));
        assignButton.setOpaque(true);
        assignButton.setBorderPainted(false);
        assignButton.setForeground(Color.WHITE);
        assignButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        assignButton.addActionListener(e -> assignToOfficer());
        actionPanel.add(assignButton);

        // Escalate button strictly removed. Authority is the final layer.

        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        footerPanel.setBackground(new Color(240, 240, 240));

        statsLabel = new JLabel("Loading statistics...");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerPanel.add(statsLabel, BorderLayout.WEST);

        return footerPanel;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> refreshData());
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem statsItem = new JMenuItem("Refresh Statistics");
        statsItem.addActionListener(e -> updateStatistics());
        viewMenu.add(statsItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void refreshData() {
        updateStatistics();
        loadGrievances();
    }

    private void updateStatistics() {
        statsTableModel.setRowCount(0);

        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();
        List<Grievance> escalated = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .collect(Collectors.toList());

        // Department statistics
        Map<String, Long> deptStats = escalated.stream()
                .filter(g -> g.getDepartment() != null)
                .collect(Collectors.groupingBy(Grievance::getDepartment, Collectors.counting()));

        // Level statistics
        Map<Integer, Long> levelStats = escalated.stream()
                .collect(Collectors.groupingBy(Grievance::getEscalationLevel, Collectors.counting()));

        // Add statistics
        statsTableModel.addRow(new Object[] { "Total Grievances", allGrievances.size() });
        statsTableModel.addRow(new Object[] { "Escalated Grievances", escalated.size() });
        statsTableModel.addRow(new Object[] { "SLA Violations",
                allGrievances.stream().filter(Grievance::isSlaViolated).count() });

        for (Map.Entry<Integer, Long> entry : levelStats.entrySet()) {
            statsTableModel.addRow(new Object[] { "Level " + entry.getKey() + " Escalations", entry.getValue() });
        }

        statsTableModel.addRow(new Object[] { "--- Departments ---", "" });
        for (Map.Entry<String, Long> entry : deptStats.entrySet()) {
            statsTableModel.addRow(new Object[] { "  " + entry.getKey(), entry.getValue() });
        }

        // Update footer
        long urgentCount = escalated.stream()
                .filter(g -> g.getSlaDeadline() != null &&
                        java.time.Duration.between(LocalDateTime.now(), g.getSlaDeadline()).toHours() <= 12)
                .count();

        statsLabel.setText(String.format(
                "Total Escalated: %d | Urgent (12h): %d | Pending Assignment: %d",
                escalated.size(),
                urgentCount,
                escalated.stream().filter(g -> g.getAssignedTo() == null).count()));
    }

    private void loadGrievances() {
        tableModel.setRowCount(0);

        List<Grievance> allGrievances = MySQLDatabaseService.getAllGrievances();
        List<Grievance> escalated = allGrievances.stream()
                .filter(g -> g.getCurrentState() == Grievance.GrievanceState.ESCALATED)
                .collect(Collectors.toList());

        String searchText = searchField.getText().toLowerCase();
        String levelFilter = (String) levelFilterCombo.getSelectedItem();

        for (Grievance g : escalated) {
            // Apply search filter
            if (!searchText.isEmpty() &&
                    !g.getGrievanceId().toLowerCase().contains(searchText) &&
                    !g.getTitle().toLowerCase().contains(searchText)) {
                continue;
            }

            // Apply level filter
            if (!levelFilter.equals("All")) {
                int level = Integer.parseInt(levelFilter.split(" ")[1]);
                if (g.getEscalationLevel() != level) {
                    continue;
                }
            }

            String slaStatus = getSLAStatus(g);

            tableModel.addRow(new Object[] {
                    g.getGrievanceId(),
                    g.getTitle(),
                    "Level " + g.getEscalationLevel(),
                    slaStatus,
                    g.getSubmittedBy(),
                    g.getAssignedTo() != null ? g.getAssignedTo() : "Unassigned",
                    g.getUpdatedAt().format(formatter)
            });
        }
    }

    private String getSLAStatus(Grievance g) {
        if (g.isSlaViolated()) {
            return "⚠️ VIOLATED";
        }

        if (g.getSlaDeadline() != null) {
            long hoursLeft = java.time.Duration.between(LocalDateTime.now(), g.getSlaDeadline()).toHours();
            if (hoursLeft <= 0) {
                return "⚠️ VIOLATED";
            } else if (hoursLeft <= 12) {
                return "⚠️ URGENT (" + hoursLeft + "h)";
            } else if (hoursLeft <= 24) {
                return "⏰ " + hoursLeft + "h remaining";
            }
        }
        return "✓ On Track";
    }

    private void loadGrievanceDetails() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            updateDetailsPanel("Select an escalated grievance to view details.");
            return;
        }

        String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance != null) {
            showGrievanceDetails(grievance);
        }
    }

    private void showGrievanceDetails(Grievance grievance) {
        StringBuilder details = new StringBuilder();
        details.append("═══════════════════════════════════════════════════════════\n");
        details.append("              ESCALATED GRIEVANCE DETAILS\n");
        details.append("═══════════════════════════════════════════════════════════\n\n");

        details.append("ID: ").append(grievance.getGrievanceId()).append("\n");
        details.append("Title: ").append(grievance.getTitle()).append("\n");
        details.append("Description: ").append(grievance.getDescription()).append("\n");
        details.append("Category: ").append(grievance.getCategory()).append("\n");
        details.append("Department: ").append(grievance.getDepartment()).append("\n");
        details.append("Current Status: ").append(grievance.getCurrentState()).append("\n");
        details.append("Submitted By: ").append(grievance.getSubmittedBy()).append("\n");
        details.append("Submitted On: ").append(grievance.getCreatedAt().format(formatter)).append("\n");
        details.append("Escalated On: ").append(grievance.getUpdatedAt().format(formatter)).append("\n");
        details.append("Escalation Level: ").append(grievance.getEscalationLevel()).append("\n");
        details.append("Escalation Status: ")
                .append(EscalationService.getEscalationLevelDescription(grievance.getEscalationLevel())).append("\n");

        if (grievance.getAssignedTo() != null) {
            User officer = MySQLDatabaseService.findUserById(grievance.getAssignedTo());
            details.append("Assigned To: ").append(officer != null ? officer.getName() : grievance.getAssignedTo())
                    .append("\n");
        } else {
            details.append("Assigned To: Not yet assigned\n");
        }

        if (grievance.getSlaDeadline() != null) {
            details.append("\nSLA Deadline: ").append(grievance.getSlaDeadline().format(formatter)).append("\n");
            long hoursLeft = java.time.Duration.between(LocalDateTime.now(), grievance.getSlaDeadline()).toHours();
            if (hoursLeft <= 0) {
                details.append("SLA Status: VIOLATED - Immediate action required!\n");
            } else {
                details.append("SLA Status: ").append(hoursLeft).append(" hours remaining\n");
            }
        }

        // History
        if (!grievance.getHistory().isEmpty()) {
            details.append("\n───────────────────────────────────────────────────────────\n");
            details.append("                     ESCALATION HISTORY\n");
            details.append("───────────────────────────────────────────────────────────\n");
            for (GrievanceHistory h : grievance.getHistory()) {
                if (h.getAction().contains("ESCALAT")) {
                    details.append("🔴 ").append(h.getChangedAt().format(formatter)).append(" | ");
                    details.append(h.getAction()).append(" by ").append(h.getChangedBy());
                    if (h.getComments() != null && !h.getComments().isEmpty()) {
                        details.append("\n   Reason: ").append(h.getComments());
                    }
                    details.append("\n");
                }
            }
        }

        updateDetailsPanel(details.toString());
    }

    private void updateDetailsPanel(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);

        detailsPanel.removeAll();
        detailsPanel.add(scrollPane, BorderLayout.CENTER);

        // Add action buttons
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton handleButton = new JButton("👥 Handle Escalated Grievance");
        handleButton.setBackground(new Color(0, 102, 204));
        handleButton.setOpaque(true);
        handleButton.setBorderPainted(false);
        handleButton.setForeground(Color.WHITE);
        handleButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        handleButton.addActionListener(e -> handleEscalatedGrievance());
        actionPanel.add(handleButton);

        JButton assignButton = new JButton("👤 Assign to Officer");
        assignButton.setBackground(new Color(0, 153, 76));
        assignButton.setOpaque(true);
        assignButton.setBorderPainted(false);
        assignButton.setForeground(Color.WHITE);
        assignButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        assignButton.addActionListener(e -> assignToOfficer());
        actionPanel.add(assignButton);

        // Escalate button strictly removed. Authority is the final layer.

        detailsPanel.add(actionPanel, BorderLayout.SOUTH);

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private void handleEscalatedGrievance() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a grievance to handle.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance == null) {
            JOptionPane.showMessageDialog(this, "Grievance not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Show handling dialog
        JDialog handleDialog = new JDialog(this, "Handle Escalated Grievance", true);
        handleDialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        handleDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Handle Escalated Grievance"),
                BorderLayout.NORTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Handle Grievance: " + grievance.getGrievanceId());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);

        JLabel commentsLabel = new JLabel("Handling Instructions / Comments:");
        commentsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        centerPanel.add(commentsLabel, BorderLayout.NORTH);

        JTextArea commentsArea = new JTextArea(5, 40);
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        commentsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(commentsArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanelInner = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionPanelInner.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        actionPanelInner.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        actionPanelInner.add(new JLabel("Action:"));
        JComboBox<String> actionCombo = new JComboBox<>(new String[] { "Take Over for Review", "Resolve Now" });
        actionCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        actionPanelInner.add(actionCombo);
        centerPanel.add(actionPanelInner, BorderLayout.SOUTH);

        contentPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton confirmButton = new JButton("Confirm");
        confirmButton.setBackground(new Color(0, 102, 204));
        confirmButton.setOpaque(true);
        confirmButton.setBorderPainted(false);
        confirmButton.setForeground(Color.WHITE);
        confirmButton.addActionListener(e -> {
            String comments = commentsArea.getText().trim();
            String selectedAction = (String) actionCombo.getSelectedItem();
            boolean resolveNow = "Resolve Now".equals(selectedAction);
            try {
                String targetState = resolveNow ? "RESOLVED" : "UNDER_REVIEW";

                if (resolveNow) {
                    GrievanceFSM.transition(grievance, Grievance.GrievanceState.RESOLVED, authority);
                    grievance.setAssignedTo(authority.getUserId());
                    grievance.setSlaViolated(false);
                } else {
                    GrievanceFSM.transition(grievance, Grievance.GrievanceState.UNDER_REVIEW, authority);
                    grievance.setAssignedTo(authority.getUserId());
                    EscalationService.handleEscalatedGrievance(grievance, authority.getUserId());
                }

                GrievanceHistory history = new GrievanceHistory(
                        grievanceId, "ESCALATED", targetState,
                        authority.getUserId(), authority.getRole().toString(),
                        resolveNow ? "RESOLVED" : "ESCALATION_HANDLED");
                history.setComments(
                        "Handled by authority: " + (comments.isEmpty() ? "No additional comments" : comments));
                grievance.addHistory(history);
                MySQLDatabaseService.saveGrievanceHistory(history);

                if (!resolveNow) {
                    SLAManager.assignSLADeadline(grievance);
                }
                MySQLDatabaseService.updateGrievance(grievance);

                // Notify original officer and citizen
                if (grievance.getAssignedTo() != null && !grievance.getAssignedTo().equals(authority.getUserId())) {
                    User officer = MySQLDatabaseService.findUserById(grievance.getAssignedTo());
                    if (officer != null) {
                        NotificationService.sendNotification(
                                officer.getUserId(),
                                "Escalation Handled",
                                "Escalated grievance " + grievanceId + " has been handled by authority",
                                NotificationService.Notification.NotificationType.ESCALATION,
                                grievanceId);
                    }
                }

                User citizen = MySQLDatabaseService.findUserById(grievance.getSubmittedBy());
                if (citizen != null) {
                    NotificationService.sendNotification(
                            citizen.getUserId(),
                            "Grievance Escalation Update",
                            resolveNow
                                    ? "Your grievance " + grievanceId + " has been resolved by authority"
                                    : "Your grievance " + grievanceId + " is now being reviewed by authority",
                            NotificationService.Notification.NotificationType.ESCALATION,
                            grievanceId);
                }

                JOptionPane.showMessageDialog(handleDialog,
                        resolveNow
                                ? "✅ Grievance resolved successfully!"
                                : "✅ Grievance taken over successfully!\nIt is now under your review.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                handleDialog.dispose();
                refreshData();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(handleDialog,
                        "Failed to handle grievance: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> handleDialog.dispose());

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        handleDialog.add(mainPanel);
        handleDialog.setVisible(true);
    }

    private void assignToOfficer() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a grievance to assign.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance == null) {
            JOptionPane.showMessageDialog(this, "Grievance not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get available officers
        List<User> officers = MySQLDatabaseService.getUsersByRole(User.UserRole.OFFICER);
        if (officers.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No officers available for assignment.",
                    "No Officers", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show officer selection dialog
        JDialog assignDialog = new JDialog(this, "Assign to Officer", true);
        assignDialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        assignDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Assign to Officer"), BorderLayout.NORTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Assign Grievance: " + grievance.getGrievanceId());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Officer selection list
        DefaultListModel<String> officerListModel = new DefaultListModel<>();
        for (User officer : officers) {
            Officer off = (Officer) officer;
            officerListModel.addElement(off.getUserId() + " - " + off.getName() + " (" + off.getDepartment() + ")");
        }

        JList<String> officerList = new JList<>(officerListModel);
        officerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        officerList.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(officerList);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton assignButton = new JButton("Assign");
        assignButton.setBackground(new Color(0, 153, 76));
        assignButton.setOpaque(true);
        assignButton.setBorderPainted(false);
        assignButton.setForeground(Color.WHITE);
        assignButton.addActionListener(e -> {
            int selected = officerList.getSelectedIndex();
            if (selected < 0) {
                JOptionPane.showMessageDialog(assignDialog,
                        "Please select an officer.",
                        "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String selectedText = officerList.getSelectedValue();
            String officerId = selectedText.split(" - ")[0];
            User officer = MySQLDatabaseService.findUserById(officerId);

            if (officer != null) {
                String oldOfficer = grievance.getAssignedTo();
                grievance.setAssignedTo(officerId);

                // Add to history
                GrievanceHistory history = new GrievanceHistory(
                        grievanceId,
                        grievance.getCurrentState().toString(),
                        grievance.getCurrentState().toString(),
                        authority.getUserId(),
                        authority.getRole().toString(),
                        "OFFICER_ASSIGNED");
                history.setComments("Assigned to " + officer.getName() + " from " +
                        (oldOfficer != null ? oldOfficer : "unassigned"));
                grievance.addHistory(history);
                MySQLDatabaseService.saveGrievanceHistory(history);

                MySQLDatabaseService.updateGrievance(grievance);

                // Notify officer
                NotificationService.sendNotification(officerId,
                        "Grievance Assigned",
                        "Grievance " + grievanceId + " has been assigned to you by authority",
                        NotificationService.Notification.NotificationType.ASSIGNMENT,
                        grievanceId);

                JOptionPane.showMessageDialog(assignDialog,
                        "✅ Grievance assigned to " + officer.getName() + " successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                assignDialog.dispose();
                refreshData();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> assignDialog.dispose());

        buttonPanel.add(assignButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        assignDialog.add(mainPanel);
        assignDialog.setVisible(true);
    }

    // escalateFurther() method logic removed. Authority is the final layer.

    private void startAutoRefresh() {
        Timer timer = new Timer(30000, e -> refreshData());
        timer.start();
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
                        "Authority Dashboard\n" +
                        "Level: " + authority.getAuthorityLevel() + "\n" +
                        "Jurisdiction: " + authority.getJurisdiction() + "\n" +
                        "Department: " + authority.getDepartment() + "\n\n" +
                        "Features:\n" +
                        "• View all escalated grievances\n" +
                        "• Handle escalated cases\n" +
                        "• Assign grievances to officers\n" +
                        "• Department-wise statistics\n" +
                        "• SLA monitoring and violation detection",
                "About DIGGS", JOptionPane.INFORMATION_MESSAGE);
    }
}