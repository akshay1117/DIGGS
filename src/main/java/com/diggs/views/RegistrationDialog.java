// src/main/java/com/diggs/views/RegistrationDialog.java
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.diggs.model.Citizen;
import com.diggs.service.AuthenticationService;

public class RegistrationDialog extends JDialog {
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JTextField addressField;
    private JTextField cityField;
    private JTextField stateField;
    private JTextField pincodeField;
    private JLabel errorLabel;
    private JButton registerButton;

    public RegistrationDialog(JFrame parent) {
        super(parent, "Create New Account", true);
        setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Court Header
        JPanel headerPanel = com.diggs.utils.ThemeUtils.createStandardHeader("Register Citizen Account");
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        int row = 0;

        // First Name
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("First Name:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        firstNameField = new JTextField(25);
        formPanel.add(firstNameField, gbc);
        row++;

        // Last Name
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Last Name:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        lastNameField = new JTextField(25);
        formPanel.add(lastNameField, gbc);
        row++;

        // Email
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Email:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        emailField = new JTextField(25);
        formPanel.add(emailField, gbc);
        row++;

        // Phone
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Phone Number:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        phoneField = new JTextField(25);
        formPanel.add(phoneField, gbc);
        row++;

        // Password
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Password:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        passwordField = new JPasswordField(25);
        formPanel.add(passwordField, gbc);
        row++;

        // Confirm Password
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Confirm Password:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        confirmField = new JPasswordField(25);
        formPanel.add(confirmField, gbc);
        row++;

        // Separator
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        JSeparator separator = new JSeparator();
        formPanel.add(separator, gbc);
        row++;
        gbc.gridwidth = 1;

        // Address
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Address:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        addressField = new JTextField(25);
        formPanel.add(addressField, gbc);
        row++;

        // City
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("City:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        cityField = new JTextField(25);
        formPanel.add(cityField, gbc);
        row++;

        // State
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("State:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        stateField = new JTextField(25);
        formPanel.add(stateField, gbc);
        row++;

        // Pincode
        gbc.gridx = 0;
        gbc.gridy = row;
        formPanel.add(new JLabel("Pincode:*"), gbc);
        gbc.gridx = 1;
        gbc.gridy = row;
        pincodeField = new JTextField(25);
        formPanel.add(pincodeField, gbc);
        row++;

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel combining error and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // Error label
        JPanel errorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        errorPanel.add(errorLabel);
        bottomPanel.add(errorPanel, BorderLayout.NORTH);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        registerButton = new JButton("Create Account");
        registerButton.setBackground(new Color(0, 102, 204));
        registerButton.setOpaque(true);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorderPainted(false);
        registerButton.setPreferredSize(new Dimension(150, 40));
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performRegistration();
            }
        });
        buttonPanel.add(registerButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void performRegistration() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String state = stateField.getText().trim();
        String pincode = pincodeField.getText().trim();

        // Validation
        if (firstName.isEmpty()) {
            errorLabel.setText("Please enter your first name");
            firstNameField.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            errorLabel.setText("Please enter your last name");
            lastNameField.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            errorLabel.setText("Please enter your email address");
            emailField.requestFocus();
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errorLabel.setText("Please enter a valid email address");
            emailField.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            errorLabel.setText("Please enter your phone number");
            phoneField.requestFocus();
            return;
        }

        if (!phone.matches("^\\d{10}$")) {
            errorLabel.setText("Phone number must be 10 digits");
            phoneField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            errorLabel.setText("Please enter a password");
            passwordField.requestFocus();
            return;
        }

        if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters");
            passwordField.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match");
            confirmField.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            errorLabel.setText("Please enter your address");
            addressField.requestFocus();
            return;
        }

        if (city.isEmpty()) {
            errorLabel.setText("Please enter your city");
            cityField.requestFocus();
            return;
        }

        if (state.isEmpty()) {
            errorLabel.setText("Please enter your state");
            stateField.requestFocus();
            return;
        }

        if (pincode.isEmpty()) {
            errorLabel.setText("Please enter your pincode");
            pincodeField.requestFocus();
            return;
        }

        if (!pincode.matches("^\\d{6}$")) {
            errorLabel.setText("Pincode must be 6 digits");
            pincodeField.requestFocus();
            return;
        }

        // Disable register button during processing
        registerButton.setEnabled(false);
        registerButton.setText("Creating Account...");

        // Perform registration in background
        SwingWorker<Citizen, Void> worker = new SwingWorker<Citizen, Void>() {
            private Exception exception;

            @Override
            protected Citizen doInBackground() {
                try {
                    return AuthenticationService.registerCitizen(
                            email, phone, password, firstName, lastName, address, city, state, pincode);
                } catch (Exception e) {
                    exception = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                registerButton.setEnabled(true);
                registerButton.setText("Create Account");

                try {
                    Citizen citizen = get();
                    if (citizen != null) {
                        JOptionPane.showMessageDialog(RegistrationDialog.this,
                                "✅ Registration successful!\n\n" +
                                        "Your User ID: " + citizen.getUserId() + "\n" +
                                        "You can now login with your email/phone and password.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else if (exception != null) {
                        errorLabel.setText("Registration failed: " + exception.getMessage());
                    }
                } catch (Exception e) {
                    errorLabel.setText("Registration failed: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }
}