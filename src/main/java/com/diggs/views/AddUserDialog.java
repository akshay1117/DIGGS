package com.diggs.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.Authority;
import com.diggs.model.Officer;
import com.diggs.model.User;
import com.diggs.service.AuthenticationService;

public class AddUserDialog extends JDialog {
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JTextField addressField;
    private JTextField cityField;
    private JComboBox<User.UserRole> roleCombo;
    private boolean success = false;

    public AddUserDialog(JFrame parent) {
        super(parent, "Add User", true);
        setSize(400, 450);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("First Name:"), gbc);
        gbc.gridx = 1;
        firstNameField = new JTextField(15);
        panel.add(firstNameField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Last Name:"), gbc);
        gbc.gridx = 1;
        lastNameField = new JTextField(15);
        panel.add(lastNameField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(15);
        panel.add(emailField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        phoneField = new JTextField(15);
        panel.add(phoneField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Address:"), gbc);
        gbc.gridx = 1;
        addressField = new JTextField(15);
        panel.add(addressField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("City:"), gbc);
        gbc.gridx = 1;
        cityField = new JTextField(15);
        panel.add(cityField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        panel.add(passwordField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        roleCombo = new JComboBox<>(
                new User.UserRole[] { User.UserRole.CITIZEN, User.UserRole.OFFICER, User.UserRole.AUTHORITY });
        panel.add(roleCombo, gbc);
        row++;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveBtn = new JButton("Save");
        saveBtn.setBackground(new Color(0, 102, 204));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.addActionListener(e -> saveUser());

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        setLayout(new BorderLayout());
        add(com.diggs.utils.ThemeUtils.createStandardHeader("Create New User"), BorderLayout.NORTH);
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveUser() {
        String fname = firstNameField.getText().trim();
        String lname = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String pass = new String(passwordField.getPassword());
        User.UserRole selectedRole = (User.UserRole) roleCombo.getSelectedItem();

        if (fname.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || city.isEmpty()
                || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (selectedRole == User.UserRole.CITIZEN) {
                // Use AuthenticationService for full citizen registration semantics
                AuthenticationService.registerCitizen(email, phone, pass, fname, lname, address, city, "N/A", "000000");
                success = true;
                dispose();
            } else {
                // Officer or Authority
                User u = (selectedRole == User.UserRole.OFFICER) ? new Officer() : new Authority();
                String prefix = (selectedRole == User.UserRole.OFFICER) ? "OFF" : "AUT";
                u.setUserId(prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                u.setFirstName(fname);
                u.setLastName(lname);
                u.setEmail(email);
                u.setPhoneNumber(phone);
                u.setPasswordHash(pass);
                u.setRole(selectedRole);
                u.setCreatedAt(java.time.LocalDateTime.now());
                if (u instanceof Officer) {
                    ((Officer) u).setEmployeeId(u.getUserId());
                } else if (u instanceof Authority) {
                    ((Authority) u).setAuthorityLevel("Level 1");
                }

                if (MySQLDatabaseService.saveUser(u)) {
                    success = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to save user via DB.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
