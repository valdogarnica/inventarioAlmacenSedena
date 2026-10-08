package com.almacen;

import com.almacen.ui.MainWindow;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import javax.swing.*;
import java.awt.Cursor;
import java.awt.Image;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        configurarTema();
        
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

    /** Tema visual de la aplicación (FlatLaf con fuente Inter y paleta propia). */
    public static void configurarTema() {
        try {
            // Fuente Inter (incluida en el programa) para un aspecto moderno y uniforme en cualquier PC
            FlatInterFont.install();
            FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
            FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
            FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
            UIManager.setLookAndFeel(new FlatLightLaf());
            UIManager.put("defaultFont", UIManager.getFont("defaultFont").deriveFont(13f));
            UIManager.put("Component.arc", 10);
            UIManager.put("Button.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("CheckBox.arc", 6);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.focusColor", new java.awt.Color(0x93C5FD));
            UIManager.put("Component.borderColor", new java.awt.Color(0xCBD5E1));
            UIManager.put("Component.focusedBorderColor", new java.awt.Color(0x3B82F6));
            UIManager.put("TextComponent.selectionBackground", new java.awt.Color(0xBFDBFE));
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.width", 12);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.rowHeight", 32);
            UIManager.put("Table.selectionBackground", new java.awt.Color(0xDBEAFE));
            UIManager.put("Table.selectionForeground", new java.awt.Color(0x1E3A8A));
            UIManager.put("Table.selectionInactiveBackground", new java.awt.Color(0xE8EEF8));
            UIManager.put("Table.selectionInactiveForeground", new java.awt.Color(0x1E293B));
            UIManager.put("Table.alternateRowColor", new java.awt.Color(0xF8FAFC));
            UIManager.put("Table.gridColor", new java.awt.Color(0xEEF2F6));
            UIManager.put("List.selectionBackground", new java.awt.Color(0xDBEAFE));
            UIManager.put("List.selectionForeground", new java.awt.Color(0x1E3A8A));
            UIManager.put("ComboBox.selectionBackground", new java.awt.Color(0xDBEAFE));
            UIManager.put("ComboBox.selectionForeground", new java.awt.Color(0x1E3A8A));
            UIManager.put("Panel.background", new java.awt.Color(0xF3F5F9));
            UIManager.put("TabbedPane.selectedBackground", java.awt.Color.WHITE);
            UIManager.put("TabbedPane.underlineColor", new java.awt.Color(0x2563EB));
            UIManager.put("TabbedPane.tabHeight", 36);
            UIManager.put("SplitPane.dividerSize", 8);
            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("Button.cursor", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // Botones de los cuadros de diálogo en español aunque Windows esté en otro idioma
            UIManager.put("OptionPane.okButtonText", "Aceptar");
            UIManager.put("OptionPane.cancelButtonText", "Cancelar");
            UIManager.put("OptionPane.yesButtonText", "Sí");
            UIManager.put("OptionPane.noButtonText", "No");
        } catch (Exception e) {
            System.err.println("Error al configurar FlatLaf: " + e.getMessage());
        }
    }
}
