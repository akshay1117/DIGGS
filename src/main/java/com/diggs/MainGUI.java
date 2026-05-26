package com.diggs;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.diggs.dao.MySQLDatabaseService;
import com.diggs.views.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;

public class MainGUI {
    public static void main(String[] args) {
        try {
            // Apply Modern FlatLaf design
            UIManager.setLookAndFeel(new FlatLightLaf());
            // Global overrides
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Component.arrowType", "chevron");
            UIManager.put("ScrollBar.showButtons", true);
            UIManager.put("ScrollBar.width", 12);
            
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    LoginFrame loginFrame = new LoginFrame();
                    loginFrame.setVisible(true);
                }
            });
            // Set global font size overrides for dialogue boxes and other un-styled
            // components
            java.awt.Font baseFont = new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 15);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(baseFont));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        MySQLDatabaseService.connect();
    }
}