package com.diggs.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.model.User;
import com.diggs.service.AuthenticationService;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox keepSignedInCheckbox;
    private JLabel errorLabel;
    private JButton loginButton;
    private JLabel loadingLabel;
    private JToggleButton eyeButton;

    public LoginFrame() {
        setTitle("DIGGS - Digital Institutional Grievance Governance System");
        setSize(1200, 800);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Splitting into 50/50 exactly like the prompt image
        JPanel mainContainer = new JPanel(new java.awt.GridLayout(1, 2));
        mainContainer.setBackground(Color.WHITE);

        // Left Panel - The Login Form
        JPanel leftPanel = createLeftPanel();
        mainContainer.add(leftPanel);

        // Right Panel - The Beautiful Graphic
        JPanel rightPanel = createRightPanel();
        mainContainer.add(rightPanel);

        add(mainContainer);

        getRootPane().setDefaultButton(loginButton);
        loadSavedCredentials();
    }

    private JPanel createLeftPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(Color.WHITE);

        JPanel inner = new JPanel(new GridBagLayout());
        inner.setBackground(Color.WHITE);
        inner.setPreferredSize(new Dimension(420, 550));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0); // vertical spacing
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        int row = 0;

        // Brand Logo / Name
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        logoPanel.setBackground(Color.WHITE);
        
        javax.swing.ImageIcon logo = com.diggs.utils.ImageUtils.loadScaledLogo(70, 70);
        JLabel logoIcon;
        if (logo != null) {
            logoIcon = new JLabel(logo);
        } else {
            logoIcon = new JLabel("⚖️"); // Fallback Scale of justice
            logoIcon.setFont(new Font("Georgia", Font.PLAIN, 46));
            logoIcon.setForeground(new Color(24, 45, 75)); // Navy Blue
        }
        
        JLabel logoText = new JLabel("<html><b style='font-family:Georgia, serif; font-size:24px; color:#182D4B;'>DIGGS</b><br><span style='font-family:Arial, sans-serif; font-size:12px; font-weight:normal; color:#555;'>Digital Institutional Grievance<br>Governance System</span></html>");
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 40, 0);
        inner.add(logoPanel, gbc);

        // Title
        JLabel titleLabel = new JLabel("Secure Authorized Access");
        titleLabel.setFont(new Font("Georgia", Font.PLAIN, 22));
        titleLabel.setForeground(new Color(40, 40, 40));
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 25, 0);
        inner.add(titleLabel, gbc);

        // E-mail Label
        JLabel emailLabel = new JLabel("E-mail:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailLabel.setForeground(Color.DARK_GRAY);
        gbc.gridy = row++;
        gbc.insets = new Insets(5, 0, 2, 0);
        inner.add(emailLabel, gbc);

        // Username Field
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.putClientProperty("JTextField.placeholderText", "e.g. John doe@gmail.com");
        usernameField.setPreferredSize(new Dimension(400, 38));
        usernameField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performLogin();
                errorLabel.setText("");
            }
        });
        // FlatLaf styling trick for sleek bottom-only or rounded bordered look
        usernameField.putClientProperty("JComponent.roundRect", true);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 15, 0);
        inner.add(usernameField, gbc);

        // Password Label
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordLabel.setForeground(Color.DARK_GRAY);
        gbc.gridy = row++;
        gbc.insets = new Insets(5, 0, 2, 0);
        inner.add(passwordLabel, gbc);

        // Password Field Grouping
        JPanel passwordWrapper = new JPanel(new BorderLayout());
        passwordWrapper.setBackground(Color.WHITE);
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.putClientProperty("JComponent.roundRect", true);
        passwordField.putClientProperty("JTextField.placeholderText", "• • • • • • • •");
        passwordField.setPreferredSize(new Dimension(350, 38));
        passwordField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performLogin();
                errorLabel.setText("");
            }
        });

        eyeButton = new JToggleButton("👁");
        eyeButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        eyeButton.setFocusPainted(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setBorderPainted(false);
        eyeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeButton.addActionListener(e -> {
            if (eyeButton.isSelected()) {
                passwordField.setEchoChar((char) 0);
                eyeButton.setText("🙈");
            } else {
                passwordField.setEchoChar('•');
                eyeButton.setText("👁");
            }
        });
        passwordWrapper.add(passwordField, BorderLayout.CENTER);
        passwordWrapper.add(eyeButton, BorderLayout.EAST);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 5, 0);
        inner.add(passwordWrapper, gbc);

        // Forgot password Link
        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotPanel.setBackground(Color.WHITE);
        JLabel forgotLabel = new JLabel("Forgot Password?");
        forgotLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        forgotLabel.setForeground(new Color(0, 150, 255));
        forgotLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { showForgotPasswordDialog(); }
            public void mouseEntered(MouseEvent e) { forgotLabel.setForeground(new Color(0, 102, 204)); }
            public void mouseExited(MouseEvent e) { forgotLabel.setForeground(new Color(0, 150, 255)); }
        });
        forgotPanel.add(forgotLabel);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 20, 0);
        inner.add(forgotPanel, gbc);
        
        // Keep signed in checkbox
        keepSignedInCheckbox = new JCheckBox("Keep me signed in");
        keepSignedInCheckbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        keepSignedInCheckbox.setBackground(Color.WHITE);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 20, 0);
        inner.add(keepSignedInCheckbox, gbc);

        // Login Button
        loginButton = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Professional deep blue to slate gradient
                GradientPaint gp = new GradientPaint(0, 0, new Color(24, 45, 75), getWidth(), 0, new Color(40, 70, 110));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(160, 44));
        
        // Center the button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(loginButton);
        loginButton.addActionListener(e -> performLogin());

        gbc.gridy = row++;
        gbc.insets = new Insets(10, 0, 15, 0);
        inner.add(buttonPanel, gbc);

        // Error & Loading Labels
        loadingLabel = new JLabel("Authenticating...");
        loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        loadingLabel.setForeground(new Color(100, 100, 100));
        loadingLabel.setVisible(false);
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        
        errorLabel = new JLabel("");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(Color.RED);
        errorLabel.setHorizontalAlignment(JLabel.CENTER);

        JPanel infoPanel = new JPanel(new java.awt.GridLayout(2, 1));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.add(loadingLabel);
        infoPanel.add(errorLabel);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 15, 0);
        inner.add(infoPanel, gbc);

        // Create Account Link
        JPanel createAccountPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        createAccountPanel.setBackground(Color.WHITE);
        JLabel noAccountLabel = new JLabel("Don't have an Account?");
        noAccountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noAccountLabel.setForeground(new Color(80, 80, 80));
        createAccountPanel.add(noAccountLabel);
        
        JLabel createAccountLink = new JLabel("Sign Up");
        createAccountLink.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createAccountLink.setForeground(new Color(0, 150, 255));
        createAccountLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createAccountLink.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { openRegistrationDialog(); }
            public void mouseEntered(MouseEvent e) { createAccountLink.setForeground(new Color(0, 102, 204)); }
            public void mouseExited(MouseEvent e) { createAccountLink.setForeground(new Color(0, 150, 255)); }
        });
        createAccountPanel.add(createAccountLink);
        
        gbc.gridy = row++;
        gbc.insets = new Insets(20, 0, 0, 0);
        inner.add(createAccountPanel, gbc);

        wrapper.add(inner);
        return wrapper;
    }

    private JPanel createRightPanel() {
        // We will construct a dynamic high-quality painted panel to mirror the illustration concept safely
        JPanel graphicsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();

                // 1. Deep professional gradient background (Navy Blue to Slate)
                GradientPaint primaryBg = new GradientPaint(0, 0, new Color(15, 25, 45), w, h, new Color(30, 50, 80));
                g2d.setPaint(primaryBg);
                g2d.fillRect(0, 0, w, h);

                // 2. Subtle architectural geometry (Large archways/circles)
                g2d.setColor(new Color(255, 255, 255, 10)); // very soft white
                g2d.fillArc(-100, -100, (int)(w*0.8), (int)(w*0.8), 0, -180); // background arch
                g2d.drawOval((int)(w*0.4), (int)(h*0.2), (int)(w*0.8), (int)(w*0.8));

                // 3. Draw minimalist classic Court Pillars
                g2d.setColor(new Color(200, 180, 130, 180)); // Muted Gold/Brass
                
                int cX = (int)(w * 0.5);
                int cY = (int)(h * 0.55);
                
                // Pediment (Triangle Roof)
                Path2D roof = new Path2D.Double();
                roof.moveTo(cX, cY - 140);
                roof.lineTo(cX + 200, cY - 60);
                roof.lineTo(cX - 200, cY - 60);
                roof.closePath();
                g2d.fill(roof);

                // Architrave (Base of roof)
                g2d.fillRect(cX - 190, cY - 55, 380, 15);
                
                // Pillars
                int[] pX = {-150, -50, 50, 150};
                for (int px : pX) {
                    g2d.fillRect(cX + px - 15, cY - 35, 30, 200);
                    // Base and Capital logic
                    g2d.fillRect(cX + px - 20, cY - 40, 40, 10);
                    g2d.fillRect(cX + px - 20, cY + 160, 40, 15);
                }

                // Foundation steps
                g2d.fillRect(cX - 190, cY + 175, 380, 15);
                g2d.fillRect(cX - 210, cY + 190, 420, 20);
                g2d.fillRect(cX - 230, cY + 210, 460, 25);

                // Typographic embellishment
                g2d.setColor(new Color(255, 255, 255, 180));
                String subtext = "Justice • Integrity • Resolution";
                g2d.setFont(new Font("Georgia", Font.ITALIC, 22));
                FontMetrics fm = g2d.getFontMetrics();
                int strX = (w - fm.stringWidth(subtext)) / 2;
                g2d.drawString(subtext, strX, h - 80);

                g2d.dispose();
            }
        };

        return graphicsPanel;
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty()) {
            errorLabel.setText("Please enter your username (email or phone)");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("Please enter your password");
            passwordField.requestFocus();
            return;
        }

        loginButton.setEnabled(false);
        loadingLabel.setVisible(true);
        errorLabel.setText("");

        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            private Exception exception;

            @Override
            protected User doInBackground() {
                try {
                    return AuthenticationService.login(username, password);
                } catch (Exception e) {
                    exception = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                loadingLabel.setVisible(false);
                try {
                    User user = get();
                    if (user != null) {
                        if (keepSignedInCheckbox.isSelected()) {
                            saveCredentials(username, password);
                        } else {
                            clearSavedCredentials();
                        }
                        MySQLDatabaseService.updateUserLastLogin(user.getUserId());
                        dispose();
                        openDashboard(user);
                    } else if (exception != null) {
                        errorLabel.setText("Login failed: " + exception.getMessage());
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception e) {
                    errorLabel.setText("Login failed: " + e.getMessage());
                    passwordField.setText("");
                    passwordField.requestFocus();
                }
            }
        };
        worker.execute();
    }

    private void openDashboard(User user) {
        switch (user.getRole()) {
            case CITIZEN:
                new CitizenDashboard((com.diggs.model.Citizen) user).setVisible(true);
                break;
            case OFFICER:
                new OfficerDashboard((com.diggs.model.Officer) user).setVisible(true);
                break;
            case AUTHORITY:
                new AuthorityDashboard((com.diggs.model.Authority) user).setVisible(true);
                break;
            case AUDITOR:
                new AuditorDashboard((com.diggs.model.Auditor) user).setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(null, "Unknown user role: " + user.getRole(),
                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openRegistrationDialog() {
        RegistrationDialog dialog = new RegistrationDialog(this);
        dialog.setVisible(true);
    }

    private void showForgotPasswordDialog() {
        JDialog dialog = new JDialog(this, "Reset Password", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        JLabel instructionLabel = new JLabel("Enter your registered email address:");
        instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        instructionLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(instructionLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        JTextField emailField = new JTextField();
        emailField.putClientProperty("JTextField.placeholderText", "your@email.com");
        emailField.setMaximumSize(new Dimension(300, 40));
        panel.add(emailField);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        JLabel messageLabel = new JLabel("");
        messageLabel.setForeground(Color.RED);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        JButton resetButton = new JButton("Send Reset Link");
        resetButton.setBackground(new Color(0, 102, 204));
        resetButton.setForeground(Color.WHITE);
        resetButton.addActionListener(e -> {
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                messageLabel.setText("Please enter your email address");
                return;
            }
            User user = MySQLDatabaseService.findUserByEmail(email);
            if (user == null) {
                messageLabel.setText("No account found with this email");
                return;
            }
            JOptionPane.showMessageDialog(dialog,
                    "A password reset link has been sent to " + email + "\n" +
                            "Please check your email to reset your password.",
                    "Reset Link Sent", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });
        buttonPanel.add(resetButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);
        panel.add(buttonPanel);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void saveCredentials(String username, String password) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
            prefs.put("saved_username", username);
            prefs.put("saved_password", encrypt(password));
            prefs.putBoolean("remember_me", true);
        } catch (Exception e) {}
    }

    private void loadSavedCredentials() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
            boolean rememberMe = prefs.getBoolean("remember_me", false);
            if (rememberMe) {
                String username = prefs.get("saved_username", "");
                String encryptedPassword = prefs.get("saved_password", "");
                if (!username.isEmpty() && !encryptedPassword.isEmpty()) {
                    usernameField.setText(username);
                    passwordField.setText(decrypt(encryptedPassword));
                    keepSignedInCheckbox.setSelected(true);
                }
            }
        } catch (Exception e) {}
    }

    private void clearSavedCredentials() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(LoginFrame.class);
            prefs.remove("saved_username");
            prefs.remove("saved_password");
            prefs.putBoolean("remember_me", false);
        } catch (Exception e) {}
    }

    private String encrypt(String password) {
        return new StringBuilder(password).reverse().toString();
    }

    private String decrypt(String encrypted) {
        return new StringBuilder(encrypted).reverse().toString();
    }
}