// src/main/java/com/diggs/views/CitizenDashboard.java
package com.diggs.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.Citizen;
import com.diggs.model.Grievance;
import com.diggs.notification.NotificationService;

public class CitizenDashboard extends JFrame {
    private Citizen citizen;
    private JPanel contentPanel;
    private JTable grievancesTable;
    private DefaultTableModel tableModel;

    public CitizenDashboard(Citizen citizen) {
        this.citizen = citizen;
        setTitle("DIGGS - Citizen Dashboard");
        setSize(1000, 700);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> logout());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 45, 75)); // Court Navy Blue
        headerPanel.setPreferredSize(new Dimension(1000, 80));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        brandPanel.setOpaque(false);

        // Attempt to load the new external logo
        javax.swing.ImageIcon logo = com.diggs.utils.ImageUtils.loadScaledLogo(60, 60);
        JLabel logoLabel;
        if (logo != null) {
            logoLabel = new JLabel(logo);
        } else {
            logoLabel = new JLabel("⚖️"); // Fallback emoji
            logoLabel.setFont(new Font("Georgia", Font.PLAIN, 40));
            logoLabel.setForeground(new Color(200, 180, 130)); // Muted gold
        }

        JLabel titleLabel = new JLabel("DIGGS - Court Application Module");
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        brandPanel.add(logoLabel);
        brandPanel.add(titleLabel);

        JLabel welcomeLabel = new JLabel("Welcome, " + citizen.getName());
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        welcomeLabel.setForeground(new Color(200, 180, 130)); // Gold accent
        welcomeLabel.setVerticalAlignment(SwingConstants.CENTER);

        headerPanel.add(brandPanel, BorderLayout.WEST);
        headerPanel.add(welcomeLabel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Sidebar buttons
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(new Color(240, 245, 250)); // Soft slate light
        sidebarPanel.setPreferredSize(new Dimension(200, 600));
        sidebarPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton submitButton = createSidebarButton("Submit Grievance", new Color(24, 45, 75)); // Navy Action
        submitButton.addActionListener(e -> submitGrievance());

        JButton trackButton = createSidebarButton("Track Grievances", new Color(40, 50, 65));
        trackButton.addActionListener(e -> trackGrievance());

        JButton notificationsButton = createSidebarButton("Notifications", new Color(40, 50, 65));
        notificationsButton.addActionListener(e -> showNotifications());

        JButton profileButton = createSidebarButton("My Profile", new Color(40, 50, 65));
        profileButton.addActionListener(e -> showProfile());

        sidebarPanel.add(submitButton);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebarPanel.add(trackButton);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebarPanel.add(notificationsButton);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebarPanel.add(profileButton);
        sidebarPanel.add(Box.createVerticalGlue());

        mainPanel.add(sidebarPanel, BorderLayout.WEST);

        // Content panel
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create table for grievances
        String[] columns = { "ID", "Title", "Status", "SLA Status", "Date" };
        tableModel = new DefaultTableModel(columns, 0);
        grievancesTable = new JTable(tableModel);
        grievancesTable.setRowHeight(30);
        grievancesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                viewSelectedGrievance();
            }
        });

        JScrollPane scrollPane = new JScrollPane(grievancesTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Add refresh button
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadGrievances());
        topPanel.add(refreshButton);
        contentPanel.add(topPanel, BorderLayout.NORTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel);

        // Load initial grievances
        loadGrievances();
    }

    private JButton createSidebarButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(180, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBackground(bgColor);
        button.setOpaque(true);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void submitGrievance() {
        SubmitGrievanceDialog dialog = new SubmitGrievanceDialog(this, citizen);
        dialog.setVisible(true);
        loadGrievances(); // Refresh after submission
    }

    private void trackGrievance() {
        String id = JOptionPane.showInputDialog(this, "Enter Grievance ID:");
        if (id != null && !id.trim().isEmpty()) {
            Grievance g = MySQLDatabaseService.findGrievanceById(id.trim());
            if (g != null && g.getSubmittedBy().equals(citizen.getUserId())) {
                showGrievanceDetails(g);
            } else {
                JOptionPane.showMessageDialog(this, "Grievance not found or access denied.");
            }
        }
    }

    private void loadGrievances() {
        tableModel.setRowCount(0);
        List<Grievance> grievances = MySQLDatabaseService.getGrievancesByCitizen(citizen.getUserId());

        for (Grievance g : grievances) {
            String slaStatus = g.isSlaViolated() ? "⚠️ Violated" : "✓ On Track";
            if (g.getSlaDeadline() == null)
                slaStatus = "No SLA";

            tableModel.addRow(new Object[] {
                    g.getGrievanceId(),
                    g.getTitle(),
                    g.getCurrentState(),
                    slaStatus,
                    g.getCreatedAt().toLocalDate().toString()
            });
        }
    }

    private void viewSelectedGrievance() {
        int selectedRow = grievancesTable.getSelectedRow();
        if (selectedRow >= 0) {
            String grievanceId = (String) tableModel.getValueAt(selectedRow, 0);
            Grievance grievance = MySQLDatabaseService.findGrievanceById(grievanceId);
            if (grievance != null) {
                showGrievanceDetails(grievance);
            }
        }
    }

    private void showGrievanceDetails(Grievance grievance) {
        JDialog dialog = new JDialog(this, "Grievance Details", true);
        dialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Grievance Details"), BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        addDetail(contentPanel, "ID:", grievance.getGrievanceId());
        addDetail(contentPanel, "Title:", grievance.getTitle());
        addDetail(contentPanel, "Description:", grievance.getDescription());
        addDetail(contentPanel, "Category:", grievance.getCategory());
        addDetail(contentPanel, "Department:", grievance.getDepartment());
        addDetail(contentPanel, "Status:", grievance.getCurrentState().toString());
        addDetail(contentPanel, "Submitted:", grievance.getCreatedAt().toString());

        if (grievance.getAssignedTo() != null) {
            addDetail(contentPanel, "Assigned To:", grievance.getAssignedTo());
        }

        if (grievance.getSlaDeadline() != null) {
            addDetail(contentPanel, "SLA Deadline:", grievance.getSlaDeadline().toString());
            addDetail(contentPanel, "SLA Status:", grievance.isSlaViolated() ? "VIOLATED" : "On Track");
        }

        if (grievance.getCurrentState().toString().equals("ESCALATED")) {
            addDetail(contentPanel, "Escalation Level:", String.valueOf(grievance.getEscalationLevel()));
        }

        if (!grievance.getHistory().isEmpty()) {
            contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            JLabel historyLabel = new JLabel("History:");
            historyLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            contentPanel.add(historyLabel);

            for (com.diggs.model.GrievanceHistory h : grievance.getHistory()) {
                JLabel historyEntry = new JLabel("  • " + h.getChangedAt().toLocalDate() + ": " + h.getAction());
                historyEntry.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                contentPanel.add(historyEntry);
            }
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);

        if (grievance.getCurrentState() == Grievance.GrievanceState.SUBMITTED) {
            JButton deleteButton = new JButton("Delete Grievance");
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setOpaque(true);
            deleteButton.setBorderPainted(false);
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to permanently delete this grievance?", "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (MySQLDatabaseService.deleteGrievance(grievance.getGrievanceId())) {
                        JOptionPane.showMessageDialog(dialog, "Grievance deleted successfully.");
                        dialog.dispose();
                        loadGrievances();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to delete grievance.");
                    }
                }
            });
            buttonPanel.add(deleteButton);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(buttonPanel);

        mainPanel.add(new JScrollPane(contentPanel), BorderLayout.CENTER);
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void addDetail(JPanel panel, String label, String value) {
        JLabel detailLabel = new JLabel(label);
        detailLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        panel.add(detailLabel);

        JLabel detailValue = new JLabel(value);
        detailValue.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(detailValue);

        panel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    private void showNotifications() {
        JDialog dialog = new JDialog(this, "Notifications", true);
        dialog.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("Notifications"), BorderLayout.NORTH);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        List<NotificationService.Notification> notifications = NotificationService
                .getAllNotifications(citizen.getUserId());

        StringBuilder sb = new StringBuilder();
        for (NotificationService.Notification n : notifications) {
            sb.append("[").append(n.getTimestamp().toLocalDate()).append("] ");
            sb.append(n.getTitle()).append("\n");
            sb.append("   ").append(n.getMessage()).append("\n\n");
        }

        textArea.setText(sb.toString());
        JScrollPane scrollPane = new JScrollPane(textArea);
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

    private void showProfile() {
        JDialog dialog = new JDialog(this, "My Profile", true);
        dialog.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.add(com.diggs.utils.ThemeUtils.createStandardHeader("My Profile"), BorderLayout.NORTH);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        addProfileDetail(panel, "User ID:", citizen.getUserId());
        addProfileDetail(panel, "Name:", citizen.getName());
        addProfileDetail(panel, "Email:", citizen.getEmail());
        addProfileDetail(panel, "Phone:", citizen.getPhoneNumber());
        addProfileDetail(panel, "Address:", citizen.getAddress());
        addProfileDetail(panel, "City:", citizen.getCity());
        addProfileDetail(panel, "State:", citizen.getState());
        addProfileDetail(panel, "Pincode:", citizen.getPincode());
        addProfileDetail(panel, "Member Since:", citizen.getCreatedAt().toLocalDate().toString());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(closeButton);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void addProfileDetail(JPanel panel, String label, String value) {
        JPanel detailPanel = new JPanel(new BorderLayout());
        JLabel labelLabel = new JLabel(label);
        labelLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel valueLabel = new JLabel(value != null ? value : "Not specified");
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detailPanel.add(labelLabel, BorderLayout.NORTH);
        detailPanel.add(valueLabel, BorderLayout.CENTER);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        panel.add(detailPanel);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Logout",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "DIGGS - Digital Institutional Grievance Governance System\n" +
                        "Version 2.0\n\n" +
                        "A comprehensive grievance management system with FSM workflow,\n" +
                        "SLA monitoring, and complete audit trails.",
                "About DIGGS", JOptionPane.INFORMATION_MESSAGE);
    }
}