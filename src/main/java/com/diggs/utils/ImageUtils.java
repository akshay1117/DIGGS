package com.diggs.utils;

import java.awt.Image;
import java.io.File;
import javax.swing.ImageIcon;

public class ImageUtils {
    /**
     * Attempts to safely load and scale the custom brand logo.
     * Fails silently by returning null if the user hasn't correctly saved the file yet.
     */
    public static ImageIcon loadScaledLogo(int width, int height) {
        try {
            File logoFile = new File("src/main/resources/images/diggs_logo.png");
            if (logoFile.exists()) {
                ImageIcon icon = new ImageIcon(logoFile.getAbsolutePath());
                Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            // Ignore failure if image is missing
        }
        return null; // Signals UI logic to use fallback Emoji styling
    }
}
