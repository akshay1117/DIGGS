package com.diggs.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class ThemeUtils {
    /**
     * Creates a standardized application header panel for Dialogs and Popups,
     * maintaining the custom Court Theme branding.
     * 
     * @param titleText The specific context string (e.g. "Register Citizen", "Resolve Grievance")
     * @return JPanel to be placed at BorderLayout.NORTH
     */
    public static JPanel createStandardHeader(String titleText) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 45, 75)); // Court Navy Blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        brandPanel.setOpaque(false);
        
        // Smaller logo for generic dialogs
        javax.swing.ImageIcon logo = ImageUtils.loadScaledLogo(40, 40);
        JLabel logoLabel;
        if (logo != null) {
            logoLabel = new JLabel(logo);
        } else {
            logoLabel = new JLabel("⚖️"); // Fallback emoji
            logoLabel.setFont(new Font("Georgia", Font.PLAIN, 28));
            logoLabel.setForeground(new Color(200, 180, 130)); // Classic Gold
        }

        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        brandPanel.add(logoLabel);
        brandPanel.add(titleLabel);

        headerPanel.add(brandPanel, BorderLayout.WEST);
        return headerPanel;
    }
}
