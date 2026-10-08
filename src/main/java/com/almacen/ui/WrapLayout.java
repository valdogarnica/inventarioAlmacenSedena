package com.almacen.ui;

import javax.swing.*;
import java.awt.*;

/**
 * FlowLayout que pasa los componentes a la siguiente línea cuando no caben y calcula
 * su alto en consecuencia, para que las barras de botones y filtros se acomoden en
 * pantallas angostas en lugar de recortarse.
 */
public class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return calcular(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension min = calcular(target, false);
        min.width -= getHgap() + 1;
        return min;
    }

    private Dimension calcular(Container target, boolean preferido) {
        synchronized (target.getTreeLock()) {
            int anchoDestino = target.getSize().width;
            Container contenedor = target;
            while (contenedor.getSize().width == 0 && contenedor.getParent() != null) {
                contenedor = contenedor.getParent();
            }
            anchoDestino = contenedor.getSize().width;
            if (anchoDestino == 0) {
                anchoDestino = Integer.MAX_VALUE;
            }
            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int margen = insets.left + insets.right + (hgap * 2);
            int anchoMax = anchoDestino - margen;

            Dimension dim = new Dimension(0, 0);
            int anchoFila = 0;
            int altoFila = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferido ? m.getPreferredSize() : m.getMinimumSize();
                if (anchoFila + d.width > anchoMax) {
                    agregarFila(dim, anchoFila, altoFila);
                    anchoFila = 0;
                    altoFila = 0;
                }
                if (anchoFila != 0) {
                    anchoFila += hgap;
                }
                anchoFila += d.width;
                altoFila = Math.max(altoFila, d.height);
            }
            agregarFila(dim, anchoFila, altoFila);
            dim.width += margen;
            dim.height += insets.top + insets.bottom + vgap * 2;
            // Dentro de un scroll horizontal se recalcula al cambiar de tamaño
            Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scroll != null && target.isValid()) {
                dim.width -= (hgap + 1);
            }
            return dim;
        }
    }

    private void agregarFila(Dimension dim, int anchoFila, int altoFila) {
        dim.width = Math.max(dim.width, anchoFila);
        if (dim.height > 0) {
            dim.height += getVgap();
        }
        dim.height += altoFila;
    }
}
