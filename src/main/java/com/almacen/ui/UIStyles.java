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
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import com.formdev.flatlaf.ui.FlatLineBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public final class UIStyles {
    // Paleta (tonos "slate" para fondos y texto, azul de acento)
    public static final Color BG = new Color(0xF3F5F9);
    public static final Color CARD = Color.WHITE;
    public static final Color BORDER = new Color(0xE2E8F0);
    public static final Color TEXT = new Color(0x1E293B);
    public static final Color MUTED = new Color(0x64748B);
    public static final Color PRIMARY = new Color(0x2563EB);
    public static final Color HEADER_BG = new Color(0xF1F5F9);
    public static final Color HEADER_FG = new Color(0x334155);

    private static final String PRIMARIO = estiloBoton("#2563EB", "#FFFFFF", "#1D4ED8", "#1E40AF", "#1E40AF");
    private static final String SECUNDARIO = estiloBoton("#FFFFFF", "#1E293B", "#F1F5F9", "#E2E8F0", "#CBD5E1");
    private static final String PELIGRO = estiloBoton("#DC2626", "#FFFFFF", "#B91C1C", "#991B1B", "#991B1B");
    private static final String EXITO = estiloBoton("#16A34A", "#FFFFFF", "#15803D", "#166534", "#166534");

    private UIStyles() {
    }

    /**
     * Estilo de botón con todos sus estados definidos. Sin focusedBackground, FlatLaf pinta
     * el botón con foco (por ejemplo, después de presionarlo) en un azul muy claro y el
     * texto blanco deja de verse; aquí el fondo se mantiene y solo cambia el borde.
     */
    private static String estiloBoton(String fondo, String texto, String hover, String presionado, String borde) {
        return "background: " + fondo + "; foreground: " + texto + "; "
            + "hoverBackground: " + hover + "; pressedBackground: " + presionado + "; "
            + "focusedBackground: " + fondo + "; selectedBackground: " + presionado + "; "
            + "selectedForeground: " + texto + "; "
            + "borderColor: " + borde + "; hoverBorderColor: " + borde + "; "
            + "focusedBorderColor: #60A5FA; pressedBorderColor: " + borde + "; "
            + "borderWidth: 1; focusWidth: 0; innerFocusWidth: 0; arc: 10; margin: 6,14,6,14; iconTextGap: 6";
    }

    /** Tarjeta blanca con esquinas redondeadas y título. */
    public static JPanel createCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD);
        card.setOpaque(true);
        card.putClientProperty("FlatLaf.style", "arc: 14");
        card.setBorder(new FlatLineBorder(new Insets(14, 16, 16, 16), BORDER, 1, 14));
        if (title != null && !title.isEmpty()) {
            JLabel lblTitulo = new JLabel(title);
            lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, lblTitulo.getFont().getSize2D() + 1.5f));
            lblTitulo.setForeground(TEXT);
            card.add(lblTitulo, BorderLayout.NORTH);
        }
        if (content instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) content;
            scroll.setBorder(new FlatLineBorder(new Insets(1, 1, 1, 1), BORDER, 1, 10));
            scroll.getViewport().setBackground(CARD);
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * Barra blanca para acciones y filtros de una página. Cada fila debe usar
     * {@link WrapLayout} para que sus controles bajen de línea en pantallas angostas.
     */
    public static JPanel createBarra(JComponent... filas) {
        JPanel barra = new JPanel();
        barra.setLayout(new javax.swing.BoxLayout(barra, javax.swing.BoxLayout.Y_AXIS));
        barra.setBackground(CARD);
        barra.putClientProperty("FlatLaf.style", "arc: 14");
        barra.setBorder(new FlatLineBorder(new Insets(6, 8, 6, 8), BORDER, 1, 14));
        for (JComponent fila : filas) {
            fila.setOpaque(false);
            fila.setAlignmentX(Component.LEFT_ALIGNMENT);
            barra.add(fila);
        }
        return barra;
    }

    /** Texto de varias líneas que se acomoda al ancho disponible (sin anchos fijos). */
    public static JComponent textoAjustable(String texto) {
        javax.swing.JTextArea area = new javax.swing.JTextArea(texto);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(javax.swing.UIManager.getFont("Label.font"));
        area.setForeground(MUTED);
        area.setColumns(20);
        return area;
    }

    public static void stylePrimaryButton(JButton button) {
        button.putClientProperty("FlatLaf.style", PRIMARIO);
    }

    public static void styleSecondaryButton(JButton button) {
        button.putClientProperty("FlatLaf.style", SECUNDARIO);
    }

    public static void styleToggleButton(JToggleButton button) {
        button.putClientProperty("FlatLaf.style", estiloBoton("#FFFFFF", "#334155", "#F1F5F9", "#DBEAFE", "#CBD5E1")
            + "; selectedBackground: #DBEAFE; selectedForeground: #1D4ED8");
    }

    public static void styleDangerButton(JButton button) {
        button.putClientProperty("FlatLaf.style", PELIGRO);
    }

    public static void styleSuccessButton(JButton button) {
        button.putClientProperty("FlatLaf.style", EXITO);
    }

    /** Encabezado de tabla claro, en negritas y centrado. */
    public static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        Font fuente = header.getFont().deriveFont(Font.BOLD);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(HEADER_BG);
                setForeground(HEADER_FG);
                setFont(fuente);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                    BorderFactory.createEmptyBorder(9, 6, 9, 6)));
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        table.setRowHeight(Math.max(table.getRowHeight(), 32));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEEF2F6));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
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
