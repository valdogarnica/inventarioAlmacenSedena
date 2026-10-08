package com.almacen;

import com.almacen.ui.MainWindow;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.Cursor;
import java.awt.Image;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        // Configurar Look and Feel moderno
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 16);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("ScrollBar.thumbArc", 12);
            UIManager.put("ScrollBar.trackArc", 12);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.selectionBackground", new java.awt.Color(45, 108, 223));
            UIManager.put("Table.selectionForeground", java.awt.Color.WHITE);
            UIManager.put("Panel.background", new java.awt.Color(245, 247, 251));
            UIManager.put("Button.cursor", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Botones de los cuadros de diálogo en español aunque Windows esté en otro idioma
            UIManager.put("OptionPane.okButtonText", "Aceptar");
            UIManager.put("OptionPane.cancelButtonText", "Cancelar");
            UIManager.put("OptionPane.yesButtonText", "Sí");
            UIManager.put("OptionPane.noButtonText", "No");
        } catch (Exception e) {
            System.err.println("Error al configurar FlatLaf: " + e.getMessage());
        }
        
        // Configurar estilo de Swing
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            // Cargar icono desde resources y aplicarlo a la ventana principal
            try {
                java.net.URL iconUrl = Main.class.getResource("/icono.png");
                if (iconUrl != null) {
                    Image icon = ImageIO.read(iconUrl);
                    window.setIconImage(icon);
                }
            } catch (Exception ignored) {
                // Si falla, continuar sin icono
            }
            window.setVisible(true);
        });
    }
}

// bema280799