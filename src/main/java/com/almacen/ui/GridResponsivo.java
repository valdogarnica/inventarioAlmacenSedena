package com.almacen.ui;

import java.awt.*;

/**
 * Cuadrícula que elige cuántas columnas usar según el ancho disponible: en pantallas
 * amplias pone varias tarjetas por fila y en pantallas angostas las apila.
 */
public class GridResponsivo implements LayoutManager {
    private final int anchoMinimoColumna;
    private final int maxColumnas;
    private final int hgap;
    private final int vgap;

    public GridResponsivo(int anchoMinimoColumna, int maxColumnas, int hgap, int vgap) {
        this.anchoMinimoColumna = anchoMinimoColumna;
        this.maxColumnas = maxColumnas;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    private int columnas(Container parent) {
        Container c = parent;
        while (c.getWidth() == 0 && c.getParent() != null) {
            c = c.getParent();
        }
        Insets in = parent.getInsets();
        int ancho = c.getWidth() - in.left - in.right;
        if (ancho <= 0) {
            return maxColumnas;
        }
        int cols = Math.max(1, (ancho + hgap) / (anchoMinimoColumna + hgap));
        return Math.min(Math.min(cols, maxColumnas), Math.max(1, parent.getComponentCount()));
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        synchronized (parent.getTreeLock()) {
            int cols = columnas(parent);
            int n = parent.getComponentCount();
            int filas = (n + cols - 1) / cols;
            int alto = 0;
            int anchoCol = 0;
            for (int f = 0; f < filas; f++) {
                int altoFila = 0;
                for (int c = 0; c < cols; c++) {
                    int i = f * cols + c;
                    if (i < n) {
                        Dimension d = parent.getComponent(i).getPreferredSize();
                        altoFila = Math.max(altoFila, d.height);
                        anchoCol = Math.max(anchoCol, Math.min(d.width, anchoMinimoColumna));
                    }
                }
                alto += altoFila;
            }
            Insets in = parent.getInsets();
            return new Dimension(in.left + in.right + cols * anchoCol + (cols - 1) * hgap,
                in.top + in.bottom + alto + Math.max(0, filas - 1) * vgap);
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Insets in = parent.getInsets();
        return new Dimension(in.left + in.right + anchoMinimoColumna / 2, in.top + in.bottom);
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets in = parent.getInsets();
            int cols = columnas(parent);
            int n = parent.getComponentCount();
            int ancho = parent.getWidth() - in.left - in.right;
            int anchoCol = (ancho - (cols - 1) * hgap) / cols;
            int y = in.top;
            for (int f = 0; f * cols < n; f++) {
                int altoFila = 0;
                for (int c = 0; c < cols && f * cols + c < n; c++) {
                    altoFila = Math.max(altoFila, parent.getComponent(f * cols + c).getPreferredSize().height);
                }
                for (int c = 0; c < cols && f * cols + c < n; c++) {
                    parent.getComponent(f * cols + c).setBounds(in.left + c * (anchoCol + hgap), y, anchoCol, altoFila);
                }
                y += altoFila + vgap;
            }
        }
    }
}
