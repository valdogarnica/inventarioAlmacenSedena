package com.almacen.ui;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Celda que muestra su texto como una insignia redondeada de color suave
 * (por ejemplo, el estado de un préstamo). Los colores salen del tema activo.
 */
public abstract class InsigniaRenderer extends DefaultTableCellRenderer {
    public enum Tono { EXITO, AVISO, PELIGRO, INFO }

    private Tono tono;

    protected InsigniaRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    /** Tono de la insignia para esa celda, o null para mostrar texto normal. */
    protected abstract Tono tono(JTable table, Object value, int row);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        // DefaultTableCellRenderer guarda el color que se le pone y lo usaría en las celdas
        // siguientes: se borra para que las celdas sin insignia tomen el color de la tabla
        setForeground(null);
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setHorizontalAlignment(SwingConstants.CENTER);
        try {
            tono = tono(table, value, table.convertRowIndexToModel(row));
        } catch (RuntimeException e) {
            tono = null;
        }
        if (tono != null) {
            setForeground(colorTexto(tono));
            setFont(getFont().deriveFont(java.awt.Font.BOLD, getFont().getSize2D() - 1f));
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (tono != null) {
            if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            FontMetrics fm = g2.getFontMetrics(getFont());
            int ancho = Math.min(getWidth() - 8, fm.stringWidth(getText()) + 22);
            int alto = Math.min(getHeight() - 8, fm.getHeight() + 6);
            int x = (getWidth() - ancho) / 2;
            int y = (getHeight() - alto) / 2;
            g2.setColor(colorFondo(tono));
            g2.fillRoundRect(x, y, ancho, alto, alto, alto);
            g2.dispose();
            boolean opaco = isOpaque();
            setOpaque(false);
            super.paintComponent(g);
            setOpaque(opaco);
            return;
        }
        super.paintComponent(g);
    }

    private static Color colorFondo(Tono tono) {
        switch (tono) {
            case EXITO: return Tema.color("App.successSoft");
            case AVISO: return Tema.color("App.warningSoft");
            case PELIGRO: return Tema.color("App.dangerSoft");
            default: return Tema.color("App.accentSoft");
        }
    }

    private static Color colorTexto(Tono tono) {
        switch (tono) {
            case EXITO: return Tema.color("App.successText");
            case AVISO: return Tema.color("App.warning");
            case PELIGRO: return Tema.color("App.dangerText");
            default: return Tema.color("App.accentSoftText");
        }
    }
}
