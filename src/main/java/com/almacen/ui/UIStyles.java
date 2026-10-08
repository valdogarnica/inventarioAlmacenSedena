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
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Component;
import com.formdev.flatlaf.extras.FlatSVGIcon;

public final class UIStyles {
    private static final String PRIMARIO = estiloBoton("$App.accent", "#FFFFFF", "$App.accentHover", "$App.accentPressed", "$App.accentPressed");
    private static final String SECUNDARIO = estiloBoton("$App.secondary", "$App.text", "$App.secondaryHover", "$App.secondaryPressed", "$App.border");
    private static final String PELIGRO = estiloBoton("$App.danger", "#FFFFFF", "$App.dangerHover", "$App.dangerPressed", "$App.dangerPressed");
    private static final String EXITO = estiloBoton("$App.success", "#FFFFFF", "$App.successHover", "$App.successPressed", "$App.successPressed");

    private UIStyles() {
    }

    /**
     * Estilo de botón con todos sus estados definidos. Sin focusedBackground, FlatLaf pinta
     * el botón con foco (por ejemplo, después de presionarlo) en un azul muy claro y el
     * texto blanco deja de verse; aquí el fondo se mantiene y solo cambia el borde.
     * Los colores "$App.*" vienen del tema activo (claro u oscuro).
     */
    private static String estiloBoton(String fondo, String texto, String hover, String presionado, String borde) {
        return "background: " + fondo + "; foreground: " + texto + "; "
            + "hoverBackground: " + hover + "; pressedBackground: " + presionado + "; "
            + "focusedBackground: " + fondo + "; selectedBackground: " + presionado + "; "
            + "selectedForeground: " + texto + "; "
            + "borderColor: " + borde + "; hoverBorderColor: " + borde + "; "
            + "focusedBorderColor: $App.accent; pressedBorderColor: " + borde + "; "
            + "borderWidth: 1; arc: 8; margin: 5,14,5,14; iconTextGap: 6";
    }

    /** Agrega propiedades de estilo FlatLaf a las que ya tenga el componente. */
    public static void estilo(JComponent c, String estilo) {
        Object actual = c.getClientProperty("FlatLaf.style");
        c.putClientProperty("FlatLaf.style", actual == null || actual.toString().isEmpty()
            ? estilo : actual + "; " + estilo);
    }

    /** Texto gris de apoyo (ayudas, subtítulos). */
    public static void textoSecundario(JComponent c) {
        estilo(c, "foreground: $App.muted");
    }

    /** Fondo de tarjeta (blanco en modo claro, gris oscuro en modo oscuro). */
    public static void fondoTarjeta(JComponent c) {
        c.setOpaque(true);
        estilo(c, "background: $App.card");
    }

    /** Título en negritas y un poco más grande. */
    public static JLabel titulo(String texto, float aumento) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, lbl.getFont().getSize2D() + aumento));
        return lbl;
    }

    /** Tarjeta con esquinas redondeadas y título. */
    public static JPanel createCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setOpaque(true);
        card.putClientProperty("FlatLaf.style", "background: $App.card; arc: 12; border: 14,16,16,16,$App.border,1,12");
        if (title != null && !title.isEmpty()) {
            card.add(titulo(title, 1.5f), BorderLayout.NORTH);
        }
        if (content instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) content;
            scroll.putClientProperty("FlatLaf.style", "border: 1,1,1,1,$App.border,1,8; background: $App.card");
            scroll.getViewport().putClientProperty("FlatLaf.style", "background: $App.card");
        }
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * Barra para acciones y filtros de una página. Cada fila debe usar
     * {@link WrapLayout} para que sus controles bajen de línea en pantallas angostas.
     */
    public static JPanel createBarra(JComponent... filas) {
        JPanel barra = new JPanel();
        barra.setLayout(new javax.swing.BoxLayout(barra, javax.swing.BoxLayout.Y_AXIS));
        barra.setOpaque(true);
        barra.putClientProperty("FlatLaf.style", "background: $App.card; arc: 12; border: 6,8,6,8,$App.border,1,12");
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
        area.setColumns(20);
        area.putClientProperty("FlatLaf.style", "foreground: $App.muted; border: 0,0,0,0; font: $Label.font");
        return area;
    }

    public static void stylePrimaryButton(JButton button) {
        button.putClientProperty("FlatLaf.style", PRIMARIO);
    }

    public static void styleSecondaryButton(JButton button) {
        button.putClientProperty("FlatLaf.style", SECUNDARIO);
    }

    public static void styleToggleButton(JToggleButton button) {
        button.putClientProperty("FlatLaf.style", estiloBoton("$App.secondary", "$App.text", "$App.secondaryHover", "$App.secondaryPressed", "$App.border")
            + "; selectedBackground: $App.accentSoft; selectedForeground: $App.accentSoftText");
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
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(Tema.color("App.header"));
                setForeground(Tema.color("App.headerText"));
                setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.color("App.border")),
                    BorderFactory.createEmptyBorder(9, 6, 9, 6)));
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        table.setRowHeight(Math.max(table.getRowHeight(), 34));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
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
