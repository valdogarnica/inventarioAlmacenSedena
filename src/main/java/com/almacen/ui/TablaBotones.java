package com.almacen.ui;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.function.IntConsumer;

/**
 * Utilidad para poner un botón dentro de una columna de JTable.
 */
public final class TablaBotones {
    private TablaBotones() {
    }

    public enum Estilo { PRIMARIO, SECUNDARIO, PELIGRO, EXITO }

    public static void instalar(JTable tabla, int columna, String texto, Estilo estilo, IntConsumer accion) {
        tabla.getColumnModel().getColumn(columna).setCellRenderer(new Renderer(texto, estilo));
        tabla.getColumnModel().getColumn(columna).setCellEditor(new Editor(tabla, texto, estilo, accion));
    }

    private static void estilizar(JButton b, Estilo estilo) {
        switch (estilo) {
            case PRIMARIO:
                UIStyles.stylePrimaryButton(b);
                break;
            case PELIGRO:
                UIStyles.styleDangerButton(b);
                break;
            case EXITO:
                UIStyles.styleSuccessButton(b);
                break;
            default:
                UIStyles.styleSecondaryButton(b);
        }
    }

    private static class Renderer extends JButton implements TableCellRenderer {
        Renderer(String texto, Estilo estilo) {
            setText(texto);
            estilizar(this, estilo);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return this;
        }
    }

    private static class Editor extends AbstractCellEditor implements TableCellEditor {
        private final JButton button;
        private int fila;

        Editor(JTable tabla, String texto, Estilo estilo, IntConsumer accion) {
            button = new JButton(texto);
            estilizar(button, estilo);
            button.addActionListener(e -> {
                int filaModelo = tabla.convertRowIndexToModel(fila);
                fireEditingStopped();
                accion.accept(filaModelo);
            });
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.fila = row;
            return button;
        }
    }
}
