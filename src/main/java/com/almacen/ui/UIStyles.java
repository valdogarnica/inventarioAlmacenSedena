package com.almacen.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public final class UIStyles {
    public static final Color BG = new Color(245, 247, 251);
    public static final Color CARD = new Color(255, 255, 255);
    public static final Color BORDER = new Color(224, 228, 238);
    public static final Color TEXT = new Color(31, 42, 68);

    private UIStyles() {
    }

    public static JPanel createCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD);
        card.setOpaque(true);
        Border line = new LineBorder(BORDER, 1, true);
        TitledBorder titled = BorderFactory.createTitledBorder(line, title);
        titled.setTitleColor(TEXT);
        titled.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        Border padding = BorderFactory.createEmptyBorder(12, 12, 12, 12);
        card.setBorder(BorderFactory.createCompoundBorder(titled, padding));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    public static void stylePrimaryButton(JButton button) {
        button.putClientProperty("FlatLaf.style", "background: #2D6CDF; foreground: #FFFFFF;");
    }

    public static void styleSecondaryButton(JButton button) {
        button.putClientProperty("FlatLaf.style", "background: #EEF2F9; foreground: #1F2A44;");
    }

    public static void styleToggleButton(JToggleButton button) {
        button.putClientProperty("FlatLaf.style", "background: #EEF2F9; foreground: #1F2A44;");
    }

    public static void styleDangerButton(JButton button) {
        button.putClientProperty("FlatLaf.style", "background: #E34B4B; foreground: #FFFFFF;");
    }

    public static void styleSuccessButton(JButton button) {
        button.putClientProperty("FlatLaf.style", "background: #2E9E50; foreground: #FFFFFF;");
    }

    public static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(66, 139, 202)); // Azul más oscuro
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 120, 180), 1),
            BorderFactory.createEmptyBorder(8, 5, 8, 5)
        ));
        
        // Renderer para centrar el texto del encabezado
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(new Color(66, 139, 202));
                setForeground(Color.WHITE);
                setFont(new Font("SansSerif", Font.BOLD, 13));
                setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
                return this;
            }
        };
        
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        centrarCeldas(table);
    }

    /** Centra el contenido de las celdas de texto y números, alineado con su encabezado. */
    public static void centrarCeldas(JTable table) {
        DefaultTableCellRenderer centro = createCenteredNumberRenderer();
        table.setDefaultRenderer(Object.class, centro);
        table.setDefaultRenderer(String.class, centro);
        table.setDefaultRenderer(Number.class, centro);
        table.setDefaultRenderer(Integer.class, centro);
        table.setDefaultRenderer(Long.class, centro);
        table.setDefaultRenderer(Double.class, centro);
        table.setDefaultRenderer(Float.class, centro);
    }

    public static DefaultTableCellRenderer createCenteredNumberRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        return renderer;
    }

    /**
     * Tamaño mínimo del botón; si el texto necesita más espacio el botón crece
     * (a diferencia de setPreferredSize, que recorta el texto).
     */
    public static void tamanoMinimo(javax.swing.AbstractButton button, int ancho, int alto) {
        button.putClientProperty("JComponent.minimumWidth", ancho);
        button.putClientProperty("JComponent.minimumHeight", alto);
    }

    public static void applySvgIcon(JButton button, String resourcePath, int size) {
        if (button == null || resourcePath == null || resourcePath.trim().isEmpty()) {
            return;
        }
        try {
            String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            java.net.URL url = UIStyles.class.getClassLoader().getResource(normalized);
            if (url == null) {
                url = UIStyles.class.getResource(resourcePath);
            }
            if (url == null) {
                return;
            }
            FlatSVGIcon icon = new FlatSVGIcon(normalized, size, size);
            if (button.getForeground() != null) {
                icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> button.getForeground()));
            }
            button.setIcon(icon);
            button.setIconTextGap(6);
            button.setHorizontalTextPosition(SwingConstants.RIGHT);
            // Sin tamaño fijo: el botón toma el ancho de su texto e icono
            button.putClientProperty("JComponent.minimumHeight", size + 16);
        } catch (Exception ignored) {
            // Si falla, continuar sin icono
        }
    }
}
