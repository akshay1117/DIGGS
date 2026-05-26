// src/main/java/com/diggs/views/SubmitGrievanceDialog.java
package com.diggs.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.Citizen;
import com.diggs.model.Grievance;
import com.diggs.notification.NotificationService;
import com.diggs.sla.SLAManager;

public class SubmitGrievanceDialog extends JDialog {
    private JTextField titleField;
    private JTextField attachmentField;
    private JTextArea descriptionArea;
    private JComboBox<String> categoryCombo;
    private JComboBox<String> departmentCombo;
    private Citizen citizen;
    private final List<File> attachedFiles = new ArrayList<>();

    public SubmitGrievanceDialog(JFrame parent, Citizen citizen) {
        super(parent, "Submit New Grievance", true);
        this.citizen = citizen;
        setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Court Header
        JPanel headerPanel = com.diggs.utils.ThemeUtils.createStandardHeader("Submit New Grievance");
        headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(headerPanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Form fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 15, 15, 15);

        Font labelFont = new Font("Segoe UI", Font.BOLD, 24);
        Font inputFont = new Font("Segoe UI", Font.PLAIN, 22);

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel tLabel = new JLabel("Title:");
        tLabel.setFont(labelFont);
        formPanel.add(tLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        titleField = new JTextField(40);
        titleField.setFont(inputFont);
        formPanel.add(titleField, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel dLabel = new JLabel("Description:");
        dLabel.setFont(labelFont);
        formPanel.add(dLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        descriptionArea = new JTextArea(8, 40);
        descriptionArea.setFont(inputFont);
        descriptionArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(descriptionArea);
        formPanel.add(scrollPane, gbc);

        // Category
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel cLabel = new JLabel("Category:");
        cLabel.setFont(labelFont);
        formPanel.add(cLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        String[] categories = { "Infrastructure", "Health", "Education", "Sanitation",
                "Electricity", "Water Supply", "Others" };
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setFont(inputFont);
        formPanel.add(categoryCombo, gbc);

        // Department //
        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel depLabel = new JLabel("Department:");
        depLabel.setFont(labelFont);
        formPanel.add(depLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        String[] departments = { "DEPT001 (Public Works)", "DEPT002 (Internal Audit)", "DEPT003 (Administration)",
                "DEPT004 (Sanitation)", "DEPT005 (Electricity)", "DEPT006 (Water Supply)", "DEPT007 (Transport)" };
        departmentCombo = new JComboBox<>(departments);
        departmentCombo.setFont(inputFont);
        formPanel.add(departmentCombo, gbc);

        // Attachments
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel attachLabel = new JLabel("Attachments:");
        attachLabel.setFont(labelFont);
        formPanel.add(attachLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 4;
        JPanel attachPanel = new JPanel(new BorderLayout(10, 0));
        attachPanel.setBackground(Color.WHITE);
        attachmentField = new JTextField();
        attachmentField.setFont(inputFont);
        attachmentField.setEditable(false);
        JButton attachButton = new JButton("Add File");
        attachButton.setFont(inputFont);
        attachButton.addActionListener(e -> selectAttachment());
        attachPanel.add(attachmentField, BorderLayout.CENTER);
        attachPanel.add(attachButton, BorderLayout.EAST);
        formPanel.add(attachPanel, gbc);

        mainPanel.add(formPanel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));

        JButton submitButton = new JButton("Submit");
        submitButton.setFont(labelFont);
        submitButton.setBackground(new Color(0, 102, 204));
        submitButton.setOpaque(true);
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setPreferredSize(new Dimension(250, 60));
        submitButton.addActionListener(e -> submitGrievance());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(labelFont);
        cancelButton.setPreferredSize(new Dimension(250, 60));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(submitButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    private void selectAttachment() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            attachedFiles.add(selectedFile);
            attachmentField.setText(String.join(", ", attachedFiles.stream().map(File::getName).toList()));
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        return index > 0 ? name.substring(index + 1) : "unknown";
    }

    private void submitGrievance() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category = (String) categoryCombo.getSelectedItem();
        String department = ((String) departmentCombo.getSelectedItem()).split(" ")[0];

        if (title.isEmpty() || description.isEmpty() || department.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill all required fields.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Grievance grievance = new Grievance();
        grievance.setTitle(title);
        grievance.setDescription(description);
        grievance.setCategory(category);
        grievance.setDepartment(department);
        grievance.setSubmittedBy(citizen.getUserId());

        // Set SLA deadline
        SLAManager.assignSLADeadline(grievance);

        // Save to database
        if (MySQLDatabaseService.saveGrievance(grievance)) {
            for (File file : attachedFiles) {
                String evidenceId = "EVID-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                String fileType = getFileExtension(file);
                if (MySQLDatabaseService.saveEvidence(evidenceId, file.getName(), fileType, file.getAbsolutePath())) {
                    MySQLDatabaseService.saveAttachment(grievance.getGrievanceId(), evidenceId);
                }
            }

            NotificationService.notifyGrievanceUpdate(citizen, grievance, "SUBMITTED");

            JOptionPane.showMessageDialog(this,
                    "Grievance submitted successfully!\nYour Grievance ID: " + grievance.getGrievanceId(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Failed to submit grievance. Please try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}