// src/main/java/com/diggs/views/OfficerDashboard.java
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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.escalation.EscalationService;
import com.diggs.fsm.GrievanceFSM;
import com.diggs.model.Grievance;
import com.diggs.model.GrievanceHistory;
import com.diggs.model.Officer;
import com.diggs.model.User;
import com.diggs.notification.NotificationService;
import com.diggs.service.AuthenticationService;
import com.diggs.sla.SLAManager;

public class OfficerDashboard extends JFrame {
    private Officer officer;
    private JTable grievancesTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> statusFilterCombo;
    private JLabel statsLabel;
    private JPanel contentPanel;
    private JPanel detailsPanel;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public OfficerDashboard(Officer officer) {
        this.officer = officer;
        setTitle("DIGGS - Officer Dashboard - " + officer.getName());
        setSize(1200, 750);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header panel with officer info
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Split pane: left side - grievances list, right side - details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(0.6);

        // Left panel: grievances list
        JPanel leftPanel = createGrievancesListPanel();
        splitPane.setLeftComponent(leftPanel);

        // Right panel: grievance details and actions
        detailsPanel = createDetailsPanel();
        splitPane.setRightComponent(detailsPanel);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Footer panel with stats
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Create menu bar
        createMenuBar();

        // Load initial data
        loadGrievances();
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

        // Officer info panel
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        infoPanel.setOpaque(false);

        JLabel welcomeLabel = new JLabel("Welcome, " + officer.getName() + " | Emp ID: " + officer.getEmployeeId());
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

    private JPanel createGrievancesListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Assigned Grievances"));

        // Top toolbar with search and filters
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        toolbarPanel.add(new JLabel("Search:"));
        searchField = new JTextField(15);
        searchField.addActionListener(e -> loadGrievances());
        toolbarPanel.add(searchField);

        JButton searchButton = new JButton("🔍");
        searchButton.addActionListener(e -> loadGrievances());
        toolbarPanel.add(searchButton);

        toolbarPanel.add(Box.createHorizontalStrut(10));

        toolbarPanel.add(new JLabel("Filter:"));
        String[] statuses = { "All", "SUBMITTED", "UNDER_REVIEW", "RESOLVED", "ESCALATED", "REJECTED", "CLOSED" };
        statusFilterCombo = new JComboBox<>(statuses);
        statusFilterCombo.addActionListener(e -> loadGrievances());
        toolbarPanel.add(statusFilterCombo);

        toolbarPanel.add(Box.createHorizontalStrut(10));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadGrievances());
        toolbarPanel.add(refreshButton);

        panel.add(toolbarPanel, BorderLayout.NORTH);

        // Table for grievances
        String[] columns = { "ID", "Title", "Status", "SLA Status", "Submitted By", "Deadline", "Submitted Date" };
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

                if (!isSelected) {
                    String slaStatus = (String) tableModel.getValueAt(row, 3);
                    if (slaStatus != null && slaStatus.contains("VIOLATED")) {
                        c.setBackground(new Color(255, 200, 200));
                    } else if (slaStatus != null && slaStatus.contains("URGENT")) {
                        c.setBackground(new Color(255, 255, 200));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
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

        // Details text area
        JTextArea detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(detailsArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons panel
        JPanel actionPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton reviewButton = new JButton("📋 Start Review");
        reviewButton.setBackground(new Color(0, 102, 204));
        reviewButton.setOpaque(true);
        reviewButton.setBorderPainted(false);
        reviewButton.setForeground(Color.WHITE);
        reviewButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reviewButton.addActionListener(e -> startReview());
        actionPanel.add(reviewButton);

        JButton resolveButton = new JButton("✅ Resolve Grievance");
        resolveButton.setBackground(new Color(0, 153, 76));
        resolveButton.setOpaque(true);
        resolveButton.setBorderPainted(false);
        resolveButton.setForeground(Color.WHITE);
        resolveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resolveButton.addActionListener(e -> resolveGrievance());
        actionPanel.add(resolveButton);

        JButton escalateButton = new JButton("⚠️ Escalate to Authority");
        escalateButton.setBackground(new Color(204, 102, 0));
        escalateButton.setOpaque(true);
        escalateButton.setBorderPainted(false);
        escalateButton.setForeground(Color.WHITE);
        escalateButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        escalateButton.addActionListener(e -> escalateGrievance());
        actionPanel.add(escalateButton);

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
        refreshItem.addActionListener(e -> loadGrievances());
        fileMenu.add(refreshItem);
        fileMenu.addSeparator();
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        fileMenu.add(logoutItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void loadGrievances() {
        tableModel.setRowCount(0);

        List<Grievance> grievances = MySQLDatabaseService.getGrievancesByOfficer(officer.getUserId());

        String searchText = searchField.getText().toLowerCase();
        String statusFilter = (String) statusFilterCombo.getSelectedItem();

        int count = 0;
        int pendingCount = 0;
        int resolvedCount = 0;
        int violatedCount = 0;

        for (Grievance g : grievances) {
            // Apply search filter
            if (!searchText.isEmpty() &&
                    !g.getGrievanceId().toLowerCase().contains(searchText) &&
                    !g.getTitle().toLowerCase().contains(searchText)) {
                continue;
            }

            // Apply status filter
            if (!statusFilter.equals("All") && !g.getCurrentState().toString().equals(statusFilter)) {
                continue;
            }

            count++;

            if (g.getCurrentState() == Grievance.GrievanceState.UNDER_REVIEW ||
                    g.getCurrentState() == Grievance.GrievanceState.SUBMITTED) {
                pendingCount++;
            }
            if (g.getCurrentState() == Grievance.GrievanceState.RESOLVED) {
                resolvedCount++;
            }
            if (g.isSlaViolated()) {
                violatedCount++;
            }

            String slaStatus = getSLAStatus(g);
            String deadline = g.getSlaDeadline() != null ? g.getSlaDeadline().format(formatter) : "No SLA";

            tableModel.addRow(new Object[] {
                    g.getGrievanceId(),
                    g.getTitle(),
                    g.getCurrentState(),
                    slaStatus,
                    g.getSubmittedBy(),
                    deadline,
                    g.getCreatedAt().format(formatter)
            });
        }

        // Update stats
        statsLabel.setText(String.format(
                "Total: %d | Pending: %d | Resolved: %d | SLA Violations: %d%s",
                count, pendingCount, resolvedCount, violatedCount,
                violatedCount > 0 ? " ⚠️ Immediate attention required!" : ""));
    }

    private String getSLAStatus(Grievance g) {
        if (g.getCurrentState() == Grievance.GrievanceState.CLOSED) {
            return "Closed";
        }

        if (g.isSlaViolated()) {
            return "⚠️ VIOLATED";
        }

        if (g.getSlaDeadline() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = g.getSlaDeadline();

            long hoursLeft = java.time.Duration.between(now, deadline).toHours();

            if (hoursLeft <= 0) {
                return "⚠️ VIOLATED";
            } else if (hoursLeft <= 12) {
                return "⚠️ URGENT (" + hoursLeft + "h left)";
            } else if (hoursLeft <= 24) {
                return "⏰ Approaching (" + hoursLeft + "h)";
            } else {
                return "✓ On Track";
            }
        }

        return "No SLA";
    }

    private void loadGrievanceDetails() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            updateDetailsPanel("Select a grievance from the list to view details.");
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
        details.append("                    GRIEVANCE DETAILS\n");
        details.append("═══════════════════════════════════════════════════════════\n\n");

        details.append("ID: ").append(grievance.getGrievanceId()).append("\n");
        details.append("Title: ").append(grievance.getTitle()).append("\n");
        details.append("Description: ").append(grievance.getDescription()).append("\n");
        details.append("Category: ").append(grievance.getCategory()).append("\n");
        details.append("Department: ").append(grievance.getDepartment()).append("\n");
        details.append("Current Status: ").append(grievance.getCurrentState()).append("\n");
        details.append("Submitted By: ").append(grievance.getSubmittedBy()).append("\n");
        details.append("Submitted On: ").append(grievance.getCreatedAt().format(formatter)).append("\n");
        details.append("Last Updated: ").append(grievance.getUpdatedAt().format(formatter)).append("\n");

        if (grievance.getSlaDeadline() != null) {
            details.append("SLA Deadline: ").append(grievance.getSlaDeadline().format(formatter)).append("\n");
            details.append("SLA Status: ");
            if (grievance.isSlaViolated()) {
                details.append("VIOLATED - Immediate action required!\n");
            } else {
                long hoursLeft = java.time.Duration.between(LocalDateTime.now(), grievance.getSlaDeadline()).toHours();
                details.append("On Track - ").append(hoursLeft).append(" hours remaining\n");
            }
        }

        if (grievance.getEscalationLevel() > 0) {
            details.append("Escalation Level: ").append(grievance.getEscalationLevel()).append("\n");
            details.append("Escalation Status: ")
                    .append(EscalationService.getEscalationLevelDescription(grievance.getEscalationLevel()))
                    .append("\n");
        }

        // History
        if (!grievance.getHistory().isEmpty()) {
            details.append("\n───────────────────────────────────────────────────────────\n");
            details.append("                     HISTORY\n");
            details.append("───────────────────────────────────────────────────────────\n");
            for (GrievanceHistory h : grievance.getHistory()) {
                details.append(h.getChangedAt().format(formatter)).append(" | ");
                details.append(h.getAction()).append(" by ").append(h.getChangedBy());
                if (h.getComments() != null && !h.getComments().isEmpty()) {
                    details.append("\n   Comments: ").append(h.getComments());
                }
                details.append("\n");
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

        // Remove old components and add new
        detailsPanel.removeAll();
        detailsPanel.add(scrollPane, BorderLayout.CENTER);

        // Add buttons back
        JPanel actionPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton reviewButton = new JButton("📋 Start Review");
        reviewButton.setBackground(new Color(0, 102, 204));
        reviewButton.setOpaque(true);
        reviewButton.setBorderPainted(false);
        reviewButton.setForeground(Color.WHITE);
        reviewButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        reviewButton.addActionListener(e -> startReview());
        actionPanel.add(reviewButton);

        JButton resolveButton = new JButton("✅ Resolve Grievance");
        resolveButton.setBackground(new Color(0, 153, 76));
        resolveButton.setOpaque(true);
        resolveButton.setBorderPainted(false);
        resolveButton.setForeground(Color.WHITE);
        resolveButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resolveButton.addActionListener(e -> resolveGrievance());
        actionPanel.add(resolveButton);

        JButton escalateButton = new JButton("⚠️ Escalate to Authority");
        escalateButton.setBackground(new Color(204, 102, 0));
        escalateButton.setOpaque(true);
        escalateButton.setBorderPainted(false);
        escalateButton.setForeground(Color.WHITE);
        escalateButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        escalateButton.addActionListener(e -> escalateGrievance());
        actionPanel.add(escalateButton);

        detailsPanel.add(actionPanel, BorderLayout.SOUTH);

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private void startReview() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a grievance to review.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance == null) {
            JOptionPane.showMessageDialog(this, "Grievance not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (grievance.getCurrentState() != Grievance.GrievanceState.SUBMITTED) {
            JOptionPane.showMessageDialog(this,
                    "This grievance is not in SUBMITTED state. Current state: " + grievance.getCurrentState(),
                    "Invalid State", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Assign to this officer and transition to UNDER_REVIEW
            grievance.setAssignedTo(officer.getUserId());
            GrievanceFSM.transition(grievance, Grievance.GrievanceState.UNDER_REVIEW, officer);

            // Set SLA deadline for under review state
            SLAManager.assignSLADeadline(grievance);

            // Add to history
            GrievanceHistory history = new GrievanceHistory(
                    grievanceId, "SUBMITTED", "UNDER_REVIEW",
                    officer.getUserId(), officer.getRole().toString(), "REVIEW_STARTED");
            history.setComments("Officer " + officer.getName() + " started reviewing this grievance");
            grievance.addHistory(history);
            MySQLDatabaseService.saveGrievanceHistory(history);

            // Update database
            MySQLDatabaseService.updateGrievance(grievance);

            // Notify citizen
            User citizen = MySQLDatabaseService.findUserById(grievance.getSubmittedBy());
            if (citizen != null) {
                NotificationService.notifyGrievanceUpdate(citizen, grievance, "UNDER_REVIEW");
            }

            JOptionPane.showMessageDialog(this,
                    "✅ Grievance is now under review!\nSLA Deadline: " +
                            (grievance.getSlaDeadline() != null ? grievance.getSlaDeadline().format(formatter)
                                    : "Not set"),
                    "Review Started", JOptionPane.INFORMATION_MESSAGE);

            // Refresh data
            loadGrievances();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to start review: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resolveGrievance() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a grievance to resolve.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance == null) {
            JOptionPane.showMessageDialog(this, "Grievance not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (grievance.getCurrentState() != Grievance.GrievanceState.UNDER_REVIEW) {
            JOptionPane.showMessageDialog(this,
                    "Only grievances under review can be resolved. Current state: " + grievance.getCurrentState(),
                    "Invalid State", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Show resolution dialog
        JDialog resolutionDialog = new JDialog(this, "Resolve Grievance", true);
        resolutionDialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        resolutionDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Resolve Grievance"), BorderLayout.NORTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        JLabel commentsLabel = new JLabel("Resolution Comments:");
        commentsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        contentPanel.add(commentsLabel, BorderLayout.NORTH);

        JTextArea commentsArea = new JTextArea(8, 40);
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        commentsArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(commentsArea);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);

        JButton confirmButton = new JButton("Confirm Resolution");
        confirmButton.setBackground(new Color(0, 153, 76));
        confirmButton.setOpaque(true);
        confirmButton.setBorderPainted(false);
        confirmButton.setForeground(Color.WHITE);
        confirmButton.addActionListener(e -> {
            String comments = commentsArea.getText().trim();
            if (comments.isEmpty()) {
                JOptionPane.showMessageDialog(resolutionDialog,
                        "Please enter resolution comments.",
                        "Missing Comments", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                String oldState = grievance.getCurrentState().toString();
                GrievanceFSM.transition(grievance, Grievance.GrievanceState.RESOLVED, officer);

                // Add to history with comments
                GrievanceHistory history = new GrievanceHistory(
                        grievanceId, oldState, "RESOLVED",
                        officer.getUserId(), officer.getRole().toString(), "RESOLVED");
                history.setComments(comments);
                grievance.addHistory(history);
                MySQLDatabaseService.saveGrievanceHistory(history);

                // Update database
                MySQLDatabaseService.updateGrievance(grievance);

                // Notify citizen
                User citizen = MySQLDatabaseService.findUserById(grievance.getSubmittedBy());
                if (citizen != null) {
                    NotificationService.notifyGrievanceUpdate(citizen, grievance, "RESOLVED");
                    NotificationService.sendNotification(
                            citizen.getUserId(),
                            "Grievance Resolved",
                            "Your grievance " + grievanceId + " has been resolved.\nComments: " + comments,
                            NotificationService.Notification.NotificationType.RESOLUTION,
                            grievanceId);
                }

                JOptionPane.showMessageDialog(resolutionDialog,
                        "✅ Grievance resolved successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);

                resolutionDialog.dispose();

                // Refresh data
                loadGrievances();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(resolutionDialog,
                        "Failed to resolve grievance: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> resolutionDialog.dispose());

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        resolutionDialog.add(mainPanel);
        resolutionDialog.setVisible(true);
    }

    private void escalateGrievance() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a grievance to escalate.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
        Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);

        if (grievance == null) {
            JOptionPane.showMessageDialog(this, "Grievance not found.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Confirm escalation
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to escalate this grievance to higher authority?\n\n" +
                        "Grievance: " + grievance.getGrievanceId() + "\n" +
                        "Title: " + grievance.getTitle() + "\n\n" +
                        "This will transfer the grievance to an authority for further action.\n" +
                        "Current escalation level: " + (grievance.getEscalationLevel() + 1),
                "Confirm Escalation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Show reason dialog
        JDialog escalationDialog = new JDialog(this, "Escalate Grievance", true);
        escalationDialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        escalationDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Escalate Grievance"), BorderLayout.NORTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("Reason for Escalation:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea reasonArea = new JTextArea(5, 40);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(reasonArea);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);

        JButton escalateButton = new JButton("Escalate");
        escalateButton.setBackground(new Color(204, 102, 0));
        escalateButton.setOpaque(true);
        escalateButton.setBorderPainted(false);
        escalateButton.setForeground(Color.WHITE);
        escalateButton.addActionListener(e -> {
            String reason = reasonArea.getText().trim();
            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(escalationDialog,
                        "Please provide a reason for escalation.",
                        "Missing Reason", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                // Use EscalationService to escalate
                EscalationService.escalateGrievance(grievance);
                String escalationId = "ESC-" + grievanceId + "-" + System.currentTimeMillis();
                MySQLDatabaseService.saveEscalation(escalationId, grievanceId, grievance.getEscalationLevel(),
                        "PENDING", reason);

                // Add to history with reason
                GrievanceHistory history = new GrievanceHistory(
                        grievanceId,
                        grievance.getCurrentState().toString(),
                        Grievance.GrievanceState.ESCALATED.toString(),
                        officer.getUserId(),
                        officer.getRole().toString(),
                        "ESCALATED");
                history.setComments("Escalation reason: " + reason);
                grievance.addHistory(history);
                MySQLDatabaseService.saveGrievanceHistory(history);

                // Update assigned to (will be handled by authority)
                grievance.setAssignedTo(null); // Unassign from officer

                // Update database
                MySQLDatabaseService.updateGrievance(grievance);

                // Notify authorities (you'd need to get all authority users)
                List<User> authorities = MySQLDatabaseService.getUsersByRole(User.UserRole.AUTHORITY);
                for (User authority : authorities) {
                    NotificationService.sendNotification(
                            authority.getUserId(),
                            "Grievance Escalated",
                            "Grievance " + grievanceId + " has been escalated to Level " +
                                    grievance.getEscalationLevel() + "\nReason: " + reason,
                            NotificationService.Notification.NotificationType.ESCALATION,
                            grievanceId);
                }

                // Notify citizen
                User citizen = MySQLDatabaseService.findUserById(grievance.getSubmittedBy());
                if (citizen != null) {
                    NotificationService.sendNotification(
                            citizen.getUserId(),
                            "Grievance Escalated",
                            "Your grievance " + grievanceId + " has been escalated to higher authority.\nReason: "
                                    + reason,
                            NotificationService.Notification.NotificationType.ESCALATION,
                            grievanceId);
                }

                JOptionPane.showMessageDialog(escalationDialog,
                        "✅ Grievance escalated to Level " + grievance.getEscalationLevel() + " authority!\n" +
                                "The authority will review and assign it to an officer.",
                        "Escalated", JOptionPane.INFORMATION_MESSAGE);

                escalationDialog.dispose();

                // Refresh data
                loadGrievances();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(escalationDialog,
                        "Failed to escalate grievance: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> escalationDialog.dispose());

        buttonPanel.add(escalateButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        escalationDialog.add(mainPanel);
        escalationDialog.setVisible(true);
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
                        "Officer Dashboard\n" +
                        "Employee ID: " + officer.getEmployeeId() + "\n\n" +
                        "Features:\n" +
                        "• View assigned grievances\n" +
                        "• Review and resolve grievances\n" +
                        "• Escalate unresolved grievances\n" +
                        "• SLA monitoring and violation detection\n" +
                        "• Complete audit trail\n" +
                        "• Real-time notifications",
                "About DIGGS", JOptionPane.INFORMATION_MESSAGE);
    }
}